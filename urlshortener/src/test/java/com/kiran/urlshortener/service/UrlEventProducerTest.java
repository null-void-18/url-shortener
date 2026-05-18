package com.kiran.urlshortener.service;

import com.kiran.urlshortener.events.UrlCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UrlEventProducerTest {

    @Mock
    private KafkaTemplate<String, UrlCreatedEvent> kafkaTemplate;

    @InjectMocks
    private UrlEventProducer urlEventProducer;

    @Test
    void sendUrlCreatedEvent_shouldSendEventToKafka() {
        // Arrange
        Long id = 123L;
        String shortCode = "abc123";
        Instant createdAt = Instant.now();

        UrlCreatedEvent event = new UrlCreatedEvent(id, shortCode, createdAt);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UrlCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UrlCreatedEvent.class);

        // Act
        urlEventProducer.sendUrlCreatedEvent(event);

        // Assert
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("url-created-events");
        assertThat(keyCaptor.getValue()).isEqualTo(shortCode);

        UrlCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getId()).isEqualTo(id);
        assertThat(capturedEvent.getShortCode()).isEqualTo(shortCode);
        assertThat(capturedEvent.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void sendUrlCreatedEvent_withDifferentEvents_shouldSendAllToKafka() {
        // Arrange
        UrlCreatedEvent event1 = new UrlCreatedEvent(1L, "code1", Instant.now());
        UrlCreatedEvent event2 = new UrlCreatedEvent(2L, "code2", Instant.now());

        // Act
        urlEventProducer.sendUrlCreatedEvent(event1);
        urlEventProducer.sendUrlCreatedEvent(event2);

        // Assert
        verify(kafkaTemplate).send("url-created-events", "code1", event1);
        verify(kafkaTemplate).send("url-created-events", "code2", event2);
    }
}