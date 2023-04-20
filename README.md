# Pleo Challenge by Tiago Luiz

When we start developing any feature, and especially when we use TDD, it is important to thoroughly define the
application
requirements and expected behaviours.

## High-level requirements

- Generically, we want to schedule a piece of code that will run once per month for every Pleo's client.
- As Pleo website currently states, there are over 25k clients, even if each submits only 10 invoices
  per month, that already demands our application to process over 250k invoices. Therefore, we need to worry with the
  system's scaling capability and work parallelization. At the bare minimum, as clients are unrelated with each
  others, we should be able to process multiple clients at the same time.
- As we rely on an external API to make the invoice charging, we need to be able to handle with faulty responses (5XX
  status) and/or network fails. As such, we need to implement a retry mechanism for this client.
- The client may not accept the invoice, therefore we need to be able to save it to retry it in the next time.

## Assumptions

- If a given invoice fails either by having `PaymentProvider.charge()` returning `false` or by network fail (after the
  retry attempts) the invoice will only be charged next month. This is just a matter of how we define the scheduler,
  if the product defined that we should retry every day it would be just a matter of redefining the cron expression.
- If we were to implement this aiming at production-level, to take advantage of having multiple servers running our
  microservice and better distribute the workload, we should use job queues such as RabbitMQ. The orchestrator would
  partition the dataset into multiple chunks and then each server would consume it from the queue. For simplicity’s
  sake, I assumed a single server, thus I don't deal with the distribution of work among multiple servers.

## Brief Notes

- To guarantee better separation of responsibilities, it would be better to have a Dal for each table instead of a
  AntaeusDal.
- For the application's flow having the serializable transaction level does not seem to be justified as it comes with
  potentially big performance impact when dealing with big data, as it prevents any type of parallelization. Therefore,
  to increase parallelization and still give some guarantees of data consistency, I've opted to lower the level to
  Repeatable Read. Note that, having phantom reads is acceptable given that invoices created after the processing starts
  should not be taken into account in the current month. 
