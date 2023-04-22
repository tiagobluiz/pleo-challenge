package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.BillingProcessingResults

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService
) {
    fun chargeInvoices(): BillingProcessingResults {
        TODO("Not yet implemented")
    }
}
