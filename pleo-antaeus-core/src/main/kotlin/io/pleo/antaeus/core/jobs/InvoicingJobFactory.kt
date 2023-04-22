package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle

class InvoicingJobFactory(private val billingService: BillingService) : JobFactory {

    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job {
        val jobDetail = bundle.jobDetail
        val jobClass = jobDetail.jobClass

        return jobClass.getConstructor(BillingService::class.java).newInstance(billingService)
    }
}
