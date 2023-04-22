package io.pleo.antaeus.core.services

import Customers.CUSTOMER_1_ID
import Customers.CUSTOMER_2_ID
import Invoices.INVOICE_1
import Invoices.INVOICE_2
import Invoices.INVOICE_3
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }

    @Test
    fun `when fetching customers with invoices, then map of customers with their invoices is returned`() {
        // prepare
        every { dal.fetchInvoicesByStatusGroupedByCustomer(setOf(PENDING)) } returns mapOf(
            CUSTOMER_1_ID to listOf(INVOICE_1, INVOICE_2),
            CUSTOMER_2_ID to listOf(INVOICE_3)
        )

        // execute
        val invoicesGroupedByClient = customerService.fetchInvoicesGroupedByClient()

        // assert
        invoicesGroupedByClient.keys shouldBe setOf(CUSTOMER_1_ID, CUSTOMER_2_ID)
        invoicesGroupedByClient.values.flatten() shouldBe setOf(INVOICE_1, INVOICE_2, INVOICE_3)
    }
}
