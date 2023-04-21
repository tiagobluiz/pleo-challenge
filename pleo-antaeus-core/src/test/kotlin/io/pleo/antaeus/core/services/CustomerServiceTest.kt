package io.pleo.antaeus.core.services

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency.EUR
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

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
        val invoicesGroupedByClient: Map<Int, List<Invoice>> = customerService.fetchInvoicesGroupedByClient()

        // assert
        invoicesGroupedByClient.keys shouldBe setOf(CUSTOMER_1_ID, CUSTOMER_2_ID)
        invoicesGroupedByClient.values shouldBe setOf(INVOICE_1, INVOICE_2, INVOICE_3)
    }

    companion object {
        private const val CUSTOMER_1_ID = 1
        private const val CUSTOMER_2_ID = 2

        private val INVOICE_1 = Invoice(1, CUSTOMER_1_ID, Money(BigDecimal.ONE, EUR), PENDING)
        private val INVOICE_2 = Invoice(2, CUSTOMER_1_ID, Money(BigDecimal.TEN, EUR), PENDING)
        private val INVOICE_3 = Invoice(3, CUSTOMER_2_ID, Money(BigDecimal.TEN, EUR), PENDING)
    }
}
