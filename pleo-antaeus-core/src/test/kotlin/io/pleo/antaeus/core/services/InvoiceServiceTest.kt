package io.pleo.antaeus.core.services

import Customers.CUSTOMER_1_ID
import Customers.CUSTOMER_2_ID
import Invoices.INVOICE_1
import Invoices.INVOICE_2
import Invoices.INVOICE_3
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceRequest
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `when fetching customers with invoices, then map of customers with their invoices is returned`() {
        // prepare
        every { dal.fetchInvoicesByStatusGroupedByCustomer(setOf(InvoiceStatus.PENDING)) } returns mapOf(
            CUSTOMER_1_ID to listOf(INVOICE_1, INVOICE_2),
            CUSTOMER_2_ID to listOf(INVOICE_3)
        )

        // execute
        val invoicesGroupedByClient = invoiceService.fetchInvoicesGroupedByClient()

        // assert
        invoicesGroupedByClient.keys shouldBe setOf(CUSTOMER_1_ID, CUSTOMER_2_ID)
        invoicesGroupedByClient.values.flatten() shouldBe setOf(INVOICE_1, INVOICE_2, INVOICE_3)
    }

    @Test
    fun `given that all invoices are paid, when updating invoice, then InvoiceNotFoundException is thrown`() {
        // prepare
        every { dal.updateInvoice(any(), any(), any()) } returns null

        // execute & assert
        assertThrows<InvoiceNotFoundException> {
            invoiceService.updateInvoice(INVOICE_1.id, InvoiceRequest(INVOICE_1.amount, INVOICE_1.status))
        }
    }

    @Test
    fun `given that invoice with id does not exist, when updating invoice status, then InvoiceNotFoundException is thrown`() {
        // prepare
        every { dal.updateInvoiceStatus(any(), any()) } returns false

        // execute & assert
        assertThrows<InvoiceNotFoundException> {
            invoiceService.updateInvoiceStatus(INVOICE_1.id, INVOICE_1.status)
        }
    }
}
