/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.external.RetryablePaymentProvider
import io.pleo.antaeus.core.jobs.InvoicingJob
import io.pleo.antaeus.core.jobs.InvoicingJobFactory
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.SchedulerFactory
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import setupInitialData
import java.io.File
import java.sql.Connection

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = ""
        )
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()
    val retryablePaymentProvider = RetryablePaymentProvider(paymentProvider)

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = retryablePaymentProvider, invoiceService = invoiceService)

    // Schedule job
    scheduleInvoicingJob(billingService)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        billingService = billingService,
        customerService = customerService
    ).run()
}

private fun scheduleInvoicingJob(billingService: BillingService) {
    val job = JobBuilder.newJob(InvoicingJob::class.java)
        .withIdentity("invoicingJob")
        .build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("invoicingTrigger")
//        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 * ?")) // every 1st day of month
        .withSchedule(CronScheduleBuilder.cronSchedule(" 0/10 0/1 * 1/1 * ? *")) // every 10s day of month
        .forJob(job)
        .build()

    val schedulerFactory: SchedulerFactory = StdSchedulerFactory()
    val scheduler = schedulerFactory.scheduler

    scheduler.setJobFactory(InvoicingJobFactory(billingService))
    scheduler.scheduleJob(job, trigger)
    scheduler.start()
}
