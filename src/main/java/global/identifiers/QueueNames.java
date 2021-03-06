package global.identifiers;

/**
 * @author Maximilian, daan
 * Used in Client, Server and MicroService for standardized communication between them via RabbitMQ.
 */
public enum QueueNames {

    CLIENT_REQUEST_QUEUE_NAME("clientRequestQueue"),
    TASK_QUEUE_NAME("taskQueue"),
    MICROSERVICE_REGISTRATION_QUEUE_NAME("registrationQueue"),
    MICROSERVICE_STOP_QUEUE_NAME("stopQueueName"),
    STOP_EXCHANGE_NAME("stopExchange"),
    CLIENT_CALLBACK_EXCHANGE_NAME("clientCallbackExchangeName");

    private final String nameOfQueue;

    QueueNames(String nameOfQueue) {
        this.nameOfQueue = nameOfQueue;
    }

    @Override
    public String toString() {
        return nameOfQueue;
    }
}