package product.orders.orderservice.messaging.producer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import product.orders.orderservice.config.KafkaTopicsProperties;
import product.orders.orderservice.messaging.event.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @InjectMocks
    private OrderEventProducer producer;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    private static final String ORDER_EVENTS_TOPIC = "order.events.v1";

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        when(topics.getOrderEvents()).thenReturn(ORDER_EVENTS_TOPIC);
    }


    @Test
    void testPublish_GivenValidOrderEvent_PublishesToKafka() {
        // Arrange
        final OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                10000l,
                "EUR",
                UUID.randomUUID(),
                "email@email.com",
                "My fictional address",
                List.of(new OrderItem(UUID.randomUUID(), 2))
        );

        // Act
        producer.publish(event);

        // Assert
        verifyMessageSent(event, event.getClass().getSimpleName());
    }

    @Test
    void testPublish_GivenOrderCancelledEvent_PublishesToKafka() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.USER_CANCELLED);
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, event.getClass().getSimpleName());
    }

    @Test
    void testPublish_GivenInvalidPaymentMadeEvent_PublishesToKafka() {
        // Arrange
        InvalidPaymentMadeEvent event = InvalidPaymentMadeEvent.of(orderId, UUID.randomUUID());
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, event.getClass().getSimpleName());
    }


    private void verifyMessageSent(Object event, String expectedType) {
        ArgumentCaptor<Message<Object>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        Message<Object> message = captor.getValue();


        assertThat(message.getPayload()).isEqualTo(event);
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue(KafkaHeaders.TOPIC, ORDER_EVENTS_TOPIC);
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue(KafkaHeaders.KEY, orderId.toString());
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue("eventType", expectedType);
    }

}