package com.kiran.urlshortener.service;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kiran.urlshortener.entity.UrlMapping;
import com.kiran.urlshortener.repository.UrlMappingRepository;

@Component
public class ClickCountFlushJob {


    private final StringRedisTemplate redisTemplate;
    private final UrlMappingRepository urlMappingRepository;


    public ClickCountFlushJob(UrlMappingRepository urlMappingRepository,StringRedisTemplate redisTemplate) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisTemplate = redisTemplate;
    }

    public String extractCode(String key) {
        return key.substring(key.indexOf(":") + 1);
    }
    
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void flushClicks() {
        Set<String> keys = redisTemplate.keys("click:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for(String key : keys) {
            String shortCode = extractCode(key);
            String value = redisTemplate
                    .opsForValue()
                    .get(key);

            Long count = value == null ? 0L : Long.valueOf(value);

            if(count <= 0) continue;

            UrlMapping urlMapping = urlMappingRepository.findByShortCodeAndActiveTrue(shortCode).orElse(null);

            if(urlMapping == null) continue;

            try {
                urlMapping.setClickCount(count + urlMapping.getClickCount());
                urlMappingRepository.save(urlMapping);
                redisTemplate.delete(key);
            }catch(Exception ex) {
            }
        }
    }

}
