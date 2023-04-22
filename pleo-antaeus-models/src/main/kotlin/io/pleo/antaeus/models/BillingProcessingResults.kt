package io.pleo.antaeus.models

data class BillingProcessingResults(
    val resultsByCustomer: Map<Int, CustomerProcessingResults>
)

data class CustomerProcessingResults(
    val customerId: Int,
    val totalInvoicesProcessed: Int,
    val successfulInvoicesProcessed: Int,
    val failedInvoicesProcessed: Int
)
