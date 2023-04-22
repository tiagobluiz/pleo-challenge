package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.BillingProcessingResults
import io.pleo.antaeus.models.CustomerProcessingResults
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
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
                successfulCharges += if (paymentProvider.charge(it)) 1 else 0
            } catch (ne: NetworkException) {
                logger.error { "Network error when processing invoice with id ${it.id} for customer ${it.customerId}" }
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
