package com.kiran.urlshortener.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kiran.urlshortener.events.UrlCreatedEvent;


@Service
public class UrlEventProducer {

    private final KafkaTemplate<String, UrlCreatedEvent> kafkaTemplate;

    public UrlEventProducer(KafkaTemplate<String, UrlCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUrlCreatedEvent(UrlCreatedEvent event) {
        kafkaTemplate.send("url-created-events", event.getShortCode(), event);
    }
}
