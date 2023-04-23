package io.pleo.antaeus.models

data class InvoiceRequest(
    val amount: Money,
    val status: InvoiceStatus
)
