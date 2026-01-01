package com.kiran.urlshortener.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kiran.urlshortener.dto.ShortenUrlRequest;
import com.kiran.urlshortener.dto.ShortenUrlResponse;
import com.kiran.urlshortener.service.UrlService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {

        String longUrl = urlService.resolveLongUrl(shortCode);

        if (longUrl == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", longUrl)
                .build();
    }



    @PostMapping("/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    public ShortenUrlResponse shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request) {

        LocalDateTime expiryAt = null;

        if (request.getExpiryDays() != null) {
            expiryAt = LocalDateTime.now().plusDays(request.getExpiryDays());
        }

        String shortCode = urlService.createShortUrl(request.getLongUrl(), expiryAt); 
        return new ShortenUrlResponse(shortCode, expiryAt);
    }
}
