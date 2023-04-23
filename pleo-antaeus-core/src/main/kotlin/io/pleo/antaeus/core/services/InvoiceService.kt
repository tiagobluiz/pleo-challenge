/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceRequest
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchPendingInvoicesByClient(): Map<Int, List<Invoice>> =
        dal.fetchInvoicesByStatusGroupedByCustomer(setOf(InvoiceStatus.PENDING))

    fun updateInvoice(id: Int, invoiceRequest: InvoiceRequest): Invoice =
        dal.updateInvoice(id, invoiceRequest.amount, invoiceRequest.status) ?: throw InvoiceNotFoundException(id)

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus) {
        if (!dal.updateInvoiceStatus(id, status)) {
            throw InvoiceNotFoundException(id)
        }
    }
}
