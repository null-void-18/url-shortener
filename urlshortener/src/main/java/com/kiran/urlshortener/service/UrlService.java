package com.kiran.urlshortener.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.kiran.urlshortener.entity.UrlMapping;
import com.kiran.urlshortener.repository.UrlMappingRepository;


@Service
public class UrlService {

    private final UrlMappingRepository urlMappingRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final StringRedisTemplate redisTemplate;
    private static final String SHORT_PREFIX = "short:";
    private static final String CLICK_PREFIX = "click:";


    public UrlService(UrlMappingRepository urlMappingRepository,
                      ShortCodeGenerator shortCodeGenerator,StringRedisTemplate redisTemplate) {
        this.urlMappingRepository = urlMappingRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.redisTemplate = redisTemplate;
    }

    public String createShortUrl(String originalUrl, LocalDateTime expiryTime) {

        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("Original URL cannot be null or blank");
        }

        UrlMapping urlMapping = urlMappingRepository.findByLongUrl(originalUrl).orElse(null);

        if(urlMapping != null) {
            String shortCode = urlMapping.getShortCode();

            LocalDateTime expiryAt = urlMapping.getExpiryAt();

            if (expiryAt == null || expiryAt.isBefore(expiryTime)) {
                urlMapping.setExpiryAt(expiryTime);
                urlMappingRepository.save(urlMapping);
            }
            return shortCode;
        }
        

        urlMapping = new UrlMapping();
        urlMapping.setLongUrl(originalUrl);
        urlMapping.setExpiryAt(expiryTime);
        urlMapping.setActive(true);

        urlMappingRepository.save(urlMapping);

        String shortCode = shortCodeGenerator.encode(urlMapping.getId());

        String key = SHORT_PREFIX + shortCode;

        urlMapping.setShortCode(shortCode);

        urlMappingRepository.save(urlMapping);

        try {
            long ttlSeconds = urlMapping.getExpiryAt() == null
                    ? TimeUnit.HOURS.toSeconds(24)
                    : Duration.between(LocalDateTime.now(), urlMapping.getExpiryAt()).getSeconds();

                String longUrl = urlMapping.getLongUrl();
                if (longUrl != null && ttlSeconds > 0) {
                    redisTemplate.opsForValue()
                                .set(key, 
                                    longUrl, 
                                    ttlSeconds, 
                                    TimeUnit.SECONDS);
                    
                }
        } catch (Exception e) {
        }

        return shortCode;
    }


    public String resolveLongUrl(String shortCode) {

        String key = SHORT_PREFIX + shortCode;
        String clickKey = CLICK_PREFIX + shortCode;

        try {
            String cachedUrl = redisTemplate.opsForValue().get(key);
            if (cachedUrl != null) {
                redisTemplate.opsForValue().increment(clickKey);
                return cachedUrl;
            }
        } catch (Exception e) {
        }

        UrlMapping mapping =
            urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)
                                .orElse(null);

        if (mapping == null) return null;

        LocalDateTime expiryAt = mapping.getExpiryAt();
        if (expiryAt != null && expiryAt.isBefore(LocalDateTime.now())) {
            return null;
        }

        try {
            long ttlSeconds = expiryAt == null
                    ? TimeUnit.HOURS.toSeconds(24)
                    : Duration.between(LocalDateTime.now(), expiryAt).getSeconds();

            String longUrl = mapping.getLongUrl();
            if (longUrl != null && ttlSeconds > 0) {
                redisTemplate.opsForValue()
                            .set(key, 
                                longUrl, 
                                ttlSeconds, 
                                TimeUnit.SECONDS);
                
                redisTemplate.opsForValue().increment(clickKey);
            }

        } catch (Exception e) {
        }

        return mapping.getLongUrl();
    }

}
