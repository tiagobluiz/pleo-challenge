# Pleo Challenge by Tiago Luiz

When we start developing any feature, and especially when we use TDD, it is important to thoroughly define the
application requirements and expected behaviours.

## High-level requirements

- Generically, we want to schedule a piece of code that will run once per month for every Pleo's client.
- As Pleo website currently states, there are over 25k clients, even if each submits only 10 invoices
  per month, that already demands our application to process over 250k invoices. Therefore, we need to worry with the
  system's scaling capability and work parallelization. At the bare minimum, as clients are unrelated with each
  others, we should be able to process multiple clients at the same time.
- As we rely on an external API to make the invoice charging, we need to be able to handle with faulty responses (5XX
  status) and/or network fails. As such, we need to implement a retry mechanism for this client.
- The client may not accept the invoice, therefore we need to be able to save it to retry it in the next time.

# Assumptions

Across the development of this challenge, several assumptions were made in order to fill the gaps that the challenge
statement did not define. The list of those assumptions is provided below.

- If a given invoice fails either by having `PaymentProvider.charge()` returning `false` or by network fail (after the
  retry attempts) the invoice will only be charged next month. If the product defined that we should retry every day it
  would be just a matter of redefining the cron expression. Moreover, if it was important to separate between refused
  charges and network fails, we would need to create a new Invoice Status, `FAILED`. Ideally, we should have a different
  job to handle such invoices.
- If an invoice has a currency error, it is marked as `INVALID`. The status should be marked as `PENDING` when the
  invoice is updated. For that, a new REST endpoint (`PUT /rest/v1/invoices/{:id}`) was provided just for that intent.
- Whenever a `CustomerNotFoundException` is thrown by the payment provider the invoices will remain as `PENDING`.
  However, this is purely a product decision. The reason why I didn't mark it as `INVALID` is that the invoice itself
  does not contain invalid data. Rather, this is a failure of the payment provider that missed to create the customer on
  their side.
- Whenever an exception that required manual intervention, i.e., `CustomerNotFoundException`
  and `CurrencyMismatchException` occurred, we should notify the person(s) that are able to solve the issue. I didn't
  explore that in this solution, but one possibility would be an email notification.
- If we were to implement this aiming at production-level, to take advantage of having multiple servers running our
  microservice and better distribute the workload, we should use job queues such as RabbitMQ. The orchestrator would
  partition the dataset into multiple chunks and then each server would consume it from the queue. For simplicity’s
  sake, I assumed a single server, thus I don't deal with the distribution of work among multiple servers.
  Additionally, in the `BillinngService.chargeInvoices()`, we should split the processing of invoices per clients into
  several smaller batches to avoid overwhelming the Payment Provider (e.g.: 1000 clients at a time), and restrain memory
  consumption.
- Currency problems are assumed to be handled by the Payment service.
- The job will be run at the 1st day of each month at midnight. I've assumed that time zones are not a problem for
  simplicity’s sake - but either way if we had a cloud provider with servers in each continent, as we will always point
  to the closest server, this shouldn't be a problem (or at least we wouldn't fail by much).

# Brief Notes

- Kotlin version was bumped only to 1.6.21 due to problems with gradle build. I've decided to not invest time trying to
  fix the issue as it would not bring much value. Most probably there's a mismatch between gradle and kotlin/jvm
  versions.
- To guarantee better separation of responsibilities, it would be better to have a Dal for each table instead of a
  AntaeusDal.
- For the application's flow having the serializable transaction level does not seem to be justified as it comes with
  potentially big performance impact when dealing with big data, as it prevents any type of parallelization. Therefore,
  to increase parallelization and still give some guarantees of data consistency, I would've opted to lower the level to
  Repeatable Read. Note that, having phantom reads is acceptable given that invoices created after the processing starts
  should not be taken into account in the current month. However, SQLite does not support such level, so I'll stick with
  serializable.
- The in-memory groupBy performed in the AntaeusDal's `fetchInvoicesByStatusGroupedByCustomer` can become a problem if
  the number of invoices starts reaching the hundreds of millions. However, given the small size of the Invoice object,
  its performance for now is acceptable.
- The main function in the `BillingService` returns an object so that we can get some reports (besides the logs) of
  what's happening in our application and enable the API to return some data about the processing.
- To avoid overwhelming the Payment Provider with unnecessary calls, if a `CustomerNotFoundException` is thrown, the
  processing for that client is terminated.
- A REST endpoint `POST /rest/v1/billing` was created in order to enable anyone to run the job through an endpoint.
  The `POST` method was used given that several consecutive calls of this endpoint may produce different results, i.e.,
  it is not idempotent. This covers the scenario where someone created a client after the job was run or after solving
  invalid invoices (currency problems).
- In a production-ready application, the retry mechanism implemented in the `RetryablePaymentProvider` should receive
  its parameters through configuration.
- As in most scenarios, the parallelization is only beneficial when we have big datasets. For small datasets, the amount
  of extra resources we create slow down the program. For 62500 invoices, the parallel implementation achieved a speedup
  of 1,5.

# Final thoughts

This is a very interesting challenge. Despite it's very simple statement you have a lot of places where you can enter a
rabbit hole and explore a lot.

It was my first time working with Javalin and Exposed but both seem pretty intuitive and did not increase the challenge
difficulty. Overall, I've tried to worry more about performance, memory and the correctness of the solution than with
potential product decisions, as most of them are a matter of changing simple things on the code (e.g: dealing
differently with network and currency failures).
There are several possible / acceptable parallelization strategies, the main two that I thought were:

1. Processing multiple clients in parallel. The main problem with this strategy is that we may have unbalanced threads
   as client with many invoices may take more time than others. As it was mentioned above, this could be solved by
   processing the invoices in batches.
2. Having the list of all invoices, create several batches and process them in parallel. This would allow a better
   balance of the work in our threads than the current strategy. However, it would provide worse reports - which is
   typically important in these applications.

In total, I've spent 6 to 8 hours doing this challenge. 
