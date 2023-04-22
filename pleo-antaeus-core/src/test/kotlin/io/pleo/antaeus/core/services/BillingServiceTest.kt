package io.pleo.antaeus.core.services

import Customers.CUSTOMER_1_ID
import Customers.CUSTOMER_2_ID
import Invoices.INVOICES_BY_CUSTOMER
import Invoices.INVOICE_1
import Invoices.INVOICE_2
import Invoices.INVOICE_3
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.CustomerProcessingResults
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Test

class BillingServiceTest {

    // Mocks
    private val paymentProvider = mockk<PaymentProvider>()
    private val customerService = mockk<CustomerService> {
        every {
            fetchInvoicesGroupedByClient()
        } returns INVOICES_BY_CUSTOMER.mapValues { (_, invoices) -> invoices.filter { it.status == InvoiceStatus.PENDING } }
    }

    private val billingService = BillingService(paymentProvider, customerService)

    @Test
    fun `given existing pending invoices, when charging customers, then all invoices are marked as paid`() {
        // prepare
        every { paymentProvider.charge(any()) } returns true

        // execute
        val billingResults = billingService.chargeInvoices()

        // assert
        verify(exactly = 3) {
            paymentProvider.charge(withArg {
                it shouldBeOneOf setOf(INVOICE_1, INVOICE_2, INVOICE_3)
            })
        }

        billingResults shouldNotBe null
        billingResults.resultsByCustomer.isEmpty() shouldBe false
        billingResults.resultsByCustomer.size shouldBe 2

        billingResults.resultsByCustomer[CUSTOMER_1_ID] shouldBe CustomerProcessingResults(CUSTOMER_1_ID, 1, 1, 0)
        billingResults.resultsByCustomer[CUSTOMER_2_ID] shouldBe CustomerProcessingResults(CUSTOMER_2_ID, 2, 2, 0)
    }

    @Test
    fun `given a refused charge, when charging customers, then not all invoices are marked as paid`() {
        // prepare
        every { paymentProvider.charge(any()) } answers {
            when (it.invocation.args[0]) {
                INVOICE_2 -> false
                else -> true
            }
        }

        // execute
        val billingResults = billingService.chargeInvoices()

        // assert
        verify(exactly = 3) {
            paymentProvider.charge(withArg {
                it shouldBeOneOf setOf(INVOICE_1, INVOICE_2, INVOICE_3)
            })
        }

        billingResults shouldNotBe null
        billingResults.resultsByCustomer.isEmpty() shouldBe false
        billingResults.resultsByCustomer.size shouldBe 2

        billingResults.resultsByCustomer[CUSTOMER_1_ID] shouldBe CustomerProcessingResults(CUSTOMER_1_ID, 1, 1, 0)
        billingResults.resultsByCustomer[CUSTOMER_2_ID] shouldBe CustomerProcessingResults(CUSTOMER_2_ID, 2, 1, 1)
    }

    @Test
    fun `given network fails, when charging customers, then not all invoices are marked as paid`() {
        // prepare
        every { paymentProvider.charge(any()) } answers {
            when (it.invocation.args[0]) {
                INVOICE_1 -> throw NetworkException()
                else -> true
            }
        }

        // EXECUTE
        val billingResults = billingService.chargeInvoices()

        // assert
        verify(exactly = 3) {
            paymentProvider.charge(withArg {
                it shouldBeOneOf setOf(INVOICE_1, INVOICE_2, INVOICE_3)
            })
        }

        billingResults shouldNotBe null
        billingResults.resultsByCustomer.isEmpty() shouldBe false
        billingResults.resultsByCustomer.size shouldBe 2

        billingResults.resultsByCustomer[CUSTOMER_1_ID] shouldBe CustomerProcessingResults(CUSTOMER_1_ID, 1, 0, 1)
        billingResults.resultsByCustomer[CUSTOMER_2_ID] shouldBe CustomerProcessingResults(CUSTOMER_2_ID, 2, 2, 0)
    }
}
