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
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    private val logger = KotlinLogging.logger { }

    fun chargeInvoices(): BillingProcessingResults {
        val invoicesByCustomer = customerService.fetchInvoicesGroupedByClient()

        val resultsByCustomer = invoicesByCustomer
            .map { (client, invoices) -> processCustomerInvoices(client, invoices) }
            .toSet()

        return BillingProcessingResults(resultsByCustomer)
    }

    private fun processCustomerInvoices(customerId: Int, invoices: List<Invoice>): CustomerProcessingResults {
        var successfulCharges = 0

        invoices.forEach {
            try {
                if (paymentProvider.charge(it)) {
                    invoiceService.setInvoiceStatus(it.id, PAID)
                    successfulCharges++
                }
            } catch (e: Exception) {
                when(e) {
                    is NetworkException -> logger.error { "Network error when processing invoice with id ${it.id} for customer ${it.customerId}" }
                    is CurrencyMismatchException -> {
                        logger.error { "Invoice ${it.id} has the wrong currency (${it.amount.currency}. Marking it as invalid." }
                        invoiceService.setInvoiceStatus(it.id, INVALID)
                    }
                    is CustomerNotFoundException -> {
                        logger.error { "Customer ${it.customerId} was not found in the payment provider. Stopping charging operations for this customer." }
                        return@forEach
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
