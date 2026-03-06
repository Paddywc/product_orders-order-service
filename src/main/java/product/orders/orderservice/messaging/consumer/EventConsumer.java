package product.orders.orderservice.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.orderservice.application.saga.OrderSagaHandler;
import product.orders.orderservice.persistance.ProcessedEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.function.Function;

/**
 * Helper parent class for consuming Kafka events.
 * @param <E> event with an id that should be unique. Idempotent checks are performed based on this id.
 */
public abstract class EventConsumer<E extends ProcessedEvent> {

    protected final OrderSagaHandler sagaHandler;

    protected final JpaRepository<E, Long> repository;

    private final Function<UUID, E> processedEntityFactory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected EventConsumer(OrderSagaHandler sagaHandler, JpaRepository<E, Long> repository, Function<UUID, E> processedEntityFactory) {
        this.sagaHandler = sagaHandler;
        this.repository = repository;
        this.processedEntityFactory = processedEntityFactory;
    }

    protected JsonNode extractJson(String rawJson) {
        return objectMapper.readValue(rawJson, JsonNode.class);
    }

    protected boolean eventAlreadyProcessed(UUID eventId) {
        try {
            E event = processedEntityFactory.apply(eventId);
            repository.saveAndFlush(event);
        } catch (DataIntegrityViolationException duplicateKey) {
            logger.debug("Event {} was already processed", eventId);
            return true;
        }

        return false;
    }

}
