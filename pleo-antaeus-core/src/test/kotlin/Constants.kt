import Customers.CUSTOMER_1_ID
import Customers.CUSTOMER_2_ID
import Customers.CUSTOMER_3_ID
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal

object Customers {
    const val CUSTOMER_1_ID = 1
    const val CUSTOMER_2_ID = 2
    const val CUSTOMER_3_ID = 3
}

object Invoices {
    val INVOICE_1 = Invoice(1, CUSTOMER_1_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PENDING)
    val INVOICE_2 = Invoice(2, CUSTOMER_1_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PENDING)
    val INVOICE_3 = Invoice(3, CUSTOMER_2_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PENDING)
    val INVOICE_4 = Invoice(4, CUSTOMER_2_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PAID)
    val INVOICE_5 = Invoice(5, CUSTOMER_2_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PAID)
    val INVOICE_6 = Invoice(6, CUSTOMER_2_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PAID)
    val INVOICE_7 = Invoice(7, CUSTOMER_3_ID, Money(BigDecimal.TEN, Currency.EUR), InvoiceStatus.PAID)

    val INVOICES_BY_CUSTOMER = mapOf(
        CUSTOMER_1_ID to listOf(INVOICE_1, INVOICE_2),
        CUSTOMER_2_ID to listOf(INVOICE_3, INVOICE_4, INVOICE_5, INVOICE_6),
        CUSTOMER_3_ID to listOf(INVOICE_7)
    )
}
