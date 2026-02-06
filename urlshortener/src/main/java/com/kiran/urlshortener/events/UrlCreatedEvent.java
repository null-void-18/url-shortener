package com.kiran.urlshortener.events;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UrlCreatedEvent {
    private Long id;
    private String shortCode;
    private Instant createdAt;
}

