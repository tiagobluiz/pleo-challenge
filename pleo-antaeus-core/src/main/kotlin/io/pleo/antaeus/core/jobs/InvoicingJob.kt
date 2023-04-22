package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

class InvoicingJob(private val billingService: BillingService) : Job {

    private val logger = KotlinLogging.logger { }

    override fun execute(context: JobExecutionContext?) {
        logger.info { "Starting to execute the Invoicing Job" }
        val startTime = System.currentTimeMillis()

        billingService.chargeInvoices()

        val finishTime = System.currentTimeMillis()
        val executionTime = finishTime - startTime

        logger.info { "Finished executing the Invoicing Job in $executionTime ms" }
    }
}
