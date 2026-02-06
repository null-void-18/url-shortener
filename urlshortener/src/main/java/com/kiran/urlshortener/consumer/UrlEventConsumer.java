package com.kiran.urlshortener.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.kiran.urlshortener.events.UrlCreatedEvent;

import lombok.extern.slf4j.Slf4j;



@Slf4j
@Component
public class UrlEventConsumer {

    @KafkaListener(
        topics = "url-created-events",
        groupId = "url-analytics-group"
    )
    public void consume(UrlCreatedEvent event) {
        log.info("URL created: {}", event.getShortCode());
    }
}

