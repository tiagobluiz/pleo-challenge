package io.pleo.antaeus.core.external

import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class RetryablePaymentProvider(private val paymentProvider: PaymentProvider) : PaymentProvider {

    private val logger = KotlinLogging.logger { }

    override fun charge(invoice: Invoice): Boolean {
        return runBlocking {
            retry(limitAttempts(5) + binaryExponentialBackoff(base = 10L, max = 5000L)) {
                logger.debug { "Charging invoice ${invoice.id} through payment provider." }

                paymentProvider.charge(invoice)
            }
        }
    }
}
