/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatusGroupedByCustomer(status: Set<InvoiceStatus>): Map<Int, List<Invoice>> = transaction(db) {
        InvoiceTable
            .select { InvoiceTable.status.inList(status.map { it.name }) }
            .groupBy(InvoiceTable.customerId)
            .map { it.toInvoice() }
            .groupBy { it.customerId }

    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Boolean {
        val updatedRows = transaction(db) {
            InvoiceTable.update({ InvoiceTable.id eq id }) {
                it[this.status] = status.name
            }
        }
        // When updated rows are 1 it means that the invoice with that id exists and the update was successful
        return updatedRows == 1
    }

    fun updateInvoice(id: Int, amount: Money, status: InvoiceStatus): Invoice? {
        // Update operations (that not on status itself) are only accepted over non-paid invoices
        val updatedRows = transaction(db) {
            InvoiceTable.update({ (InvoiceTable.id eq id) and (InvoiceTable.status neq InvoiceStatus.PAID.name) }) {
                it[this.value] = amount.value
                it[this.currency] = amount.currency.name
                it[this.status] = status.name
            }
        }
        // When updated rows are 1 it means that valid (non-paid) invoice with that id exists and the update was successful
        return if (updatedRows == 1) fetchInvoice(id) else null
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
