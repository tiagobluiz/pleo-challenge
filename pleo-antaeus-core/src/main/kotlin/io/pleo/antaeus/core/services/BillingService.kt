package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.BillingProcessingResults
import io.pleo.antaeus.models.CustomerProcessingResults
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.INVALID
import io.pleo.antaeus.models.InvoiceStatus.PAID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger { }

    fun chargeInvoices(): BillingProcessingResults {
        logger.info { "Starting to charge invoices for all customers" }
        val startTime = System.currentTimeMillis()

        val results = runBlocking {
            val invoicesByCustomer = invoiceService.fetchInvoicesGroupedByClient()

            invoicesByCustomer
                .map { (client, invoices) ->
                    async {
                        processCustomerInvoices(client, invoices)
                    }
                }
                .awaitAll()
                .map { it }
                .toSet()
        }

        val finishTime = System.currentTimeMillis()
        val executionTime = finishTime - startTime

        logger.info { "Finished charging invoices for all customers in $executionTime ms" }

        return BillingProcessingResults(results)
    }

    private fun processCustomerInvoices(customerId: Int, invoices: List<Invoice>): CustomerProcessingResults {
        logger.debug { "Processing customer $customerId invoices" }

        var successfulCharges = 0

        invoices.forEach {
            try {
                if (paymentProvider.charge(it)) {
                    invoiceService.updateInvoiceStatus(it.id, PAID)
                    successfulCharges++
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkException -> logger.error { "Network error when processing invoice with id ${it.id} for customer ${it.customerId}" }
                    is CurrencyMismatchException -> {
                        logger.error { "Invoice ${it.id} has the wrong currency (${it.amount.currency}). Marking it as invalid." }
                        // Ideally, customer should be notified of this failure in order to address it (e.g. through email)
                        invoiceService.updateInvoiceStatus(it.id, INVALID)
                    }

                    is CustomerNotFoundException -> {
                        logger.error { "Customer ${it.customerId} was not found in the payment provider. Stopping charging operations for this customer." }
                        // Ideally, admin should be notified of this failure in order to address it (e.g. through email)
                        return CustomerProcessingResults(customerId, 0, 0, invoices.size)
                    }

                    else -> logger.error { "An unexpected error occurred. More details: ${e.message}" }
                }
            }
        }

        return CustomerProcessingResults(
            customerId,
            invoices.size,
            successfulCharges,
            invoices.size - successfulCharges
        )
    }
}
