package io.pleo.antaeus.core.services

import Customers.CUSTOMER_1_ID
import Customers.CUSTOMER_2_ID
import Invoices.INVOICES_BY_CUSTOMER
import Invoices.INVOICE_1
import Invoices.INVOICE_2
import Invoices.INVOICE_3
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.CustomerProcessingResults
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.INVALID
import org.junit.jupiter.api.Test

class BillingServiceTest {

    // Mocks
    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>() {
        every {
            fetchInvoicesGroupedByClient()
        } returns INVOICES_BY_CUSTOMER.mapValues { (_, invoices) -> invoices.filter { it.status == InvoiceStatus.PENDING } }
            .filter { it.value.isNotEmpty() }

        every { updateInvoiceStatus(any(), any()) } just Runs
    }

    private val billingService = BillingService(paymentProvider, invoiceService)

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

        billingResults.resultsByCustomer shouldContainExactly setOf(
            CustomerProcessingResults(CUSTOMER_1_ID, 2, 2, 0),
            CustomerProcessingResults(CUSTOMER_2_ID, 1, 1, 0)
        )
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

        billingResults.resultsByCustomer shouldContainExactly setOf(
            CustomerProcessingResults(CUSTOMER_1_ID, 2, 1, 1),
            CustomerProcessingResults(CUSTOMER_2_ID, 1, 1, 0)
        )
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

        billingResults.resultsByCustomer shouldContainExactly setOf(
            CustomerProcessingResults(CUSTOMER_1_ID, 2, 1, 1),
            CustomerProcessingResults(CUSTOMER_2_ID, 1, 1, 0)
        )
    }

    @Test
    fun `given currency mismatch, when charging customers, then the invoice is marked as invalid`() {
        // prepare
        every { paymentProvider.charge(any()) } answers {
            when (it.invocation.args[0]) {
                INVOICE_1 -> throw CurrencyMismatchException(INVOICE_1.id, INVOICE_1.customerId)
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

        verify { invoiceService.updateInvoiceStatus(INVOICE_1.id, INVALID) }

        billingResults shouldNotBe null
        billingResults.resultsByCustomer.isEmpty() shouldBe false
        billingResults.resultsByCustomer.size shouldBe 2

        billingResults.resultsByCustomer shouldContainExactly setOf(
            CustomerProcessingResults(CUSTOMER_1_ID, 2, 1, 1),
            CustomerProcessingResults(CUSTOMER_2_ID, 1, 1, 0)
        )
    }

    @Test
    fun `given customer not found, when charging customers, processing for that customer stops`() {
        // prepare
        every { paymentProvider.charge(any()) } answers {
            when (it.invocation.args[0]) {
                INVOICE_1 -> throw CustomerNotFoundException(INVOICE_1.customerId)
                else -> true
            }
        }

        // execute
        val billingResults = billingService.chargeInvoices()

        // assert
        verify(exactly = 2) {
            paymentProvider.charge(withArg {
                it shouldBeOneOf setOf(INVOICE_1, INVOICE_3)
            })
        }

        billingResults shouldNotBe null
        billingResults.resultsByCustomer.isEmpty() shouldBe false
        billingResults.resultsByCustomer.size shouldBe 2

        billingResults.resultsByCustomer shouldContainExactly setOf(
            CustomerProcessingResults(CUSTOMER_1_ID, 0,0 , 2),
            CustomerProcessingResults(CUSTOMER_2_ID, 1, 1, 0)
        )
    }
}
