package com.kiran.urlshortener.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ShortenUrlResponse {
    
    private String shortUrl;

    private LocalDateTime expiryAt;
}
