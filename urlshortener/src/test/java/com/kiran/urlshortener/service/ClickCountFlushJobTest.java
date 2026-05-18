package com.kiran.urlshortener.service;

import com.kiran.urlshortener.entity.UrlMapping;
import com.kiran.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickCountFlushJobTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ClickCountFlushJob clickCountFlushJob;

    @Test
    void extractCode_shouldExtractShortCodeFromKey() {
        // Act
        String result = clickCountFlushJob.extractCode("click:abc123");

        // Assert
        assertThat(result).isEqualTo("abc123");
    }

    @Test
    void extractCode_withDifferentKeys_shouldExtractCorrectly() {
        // Act & Assert
        assertThat(clickCountFlushJob.extractCode("click:xyz789")).isEqualTo("xyz789");
        assertThat(clickCountFlushJob.extractCode("short:test")).isEqualTo("test");
        assertThat(clickCountFlushJob.extractCode("prefix:code123")).isEqualTo("code123");
    }

    @Test
    void flushClicks_withNoKeys_shouldDoNothing() {
        // Arrange
        when(redisTemplate.keys("click:*")).thenReturn(Collections.emptySet());

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(redisTemplate).keys("click:*");
        verify(urlMappingRepository, never()).findByShortCodeAndActiveTrue(anyString());
        verify(urlMappingRepository, never()).save(any());
    }

    @Test
    void flushClicks_withNullKeys_shouldDoNothing() {
        // Arrange
        when(redisTemplate.keys("click:*")).thenReturn(null);

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(redisTemplate).keys("click:*");
        verify(urlMappingRepository, never()).findByShortCodeAndActiveTrue(anyString());
    }

    @Test
    void flushClicks_withValidClicks_shouldUpdateDatabase() {
        // Arrange
        String key = "click:abc123";
        String shortCode = "abc123";
        Set<String> keys = Set.of(key);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode(shortCode);
        mapping.setClickCount(10L);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("5");
        when(urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)).thenReturn(Optional.of(mapping));
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(mapping);

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository).findByShortCodeAndActiveTrue(shortCode);
        verify(urlMappingRepository).save(mapping);
        verify(redisTemplate).delete(key);
        assertThat(mapping.getClickCount()).isEqualTo(15L);
    }

    @Test
    void flushClicks_withMultipleKeys_shouldProcessAll() {
        // Arrange
        Set<String> keys = Set.of("click:abc123", "click:xyz789");

        UrlMapping mapping1 = new UrlMapping();
        mapping1.setShortCode("abc123");
        mapping1.setClickCount(5L);

        UrlMapping mapping2 = new UrlMapping();
        mapping2.setShortCode("xyz789");
        mapping2.setClickCount(10L);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("click:abc123")).thenReturn("3");
        when(valueOperations.get("click:xyz789")).thenReturn("7");
        when(urlMappingRepository.findByShortCodeAndActiveTrue("abc123")).thenReturn(Optional.of(mapping1));
        when(urlMappingRepository.findByShortCodeAndActiveTrue("xyz789")).thenReturn(Optional.of(mapping2));

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository).save(mapping1);
        verify(urlMappingRepository).save(mapping2);
        verify(redisTemplate).delete("click:abc123");
        verify(redisTemplate).delete("click:xyz789");
        assertThat(mapping1.getClickCount()).isEqualTo(8L);
        assertThat(mapping2.getClickCount()).isEqualTo(17L);
    }

    @Test
    void flushClicks_withZeroCount_shouldSkip() {
        // Arrange
        String key = "click:abc123";
        Set<String> keys = Set.of(key);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("0");

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository, never()).findByShortCodeAndActiveTrue(anyString());
        verify(urlMappingRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void flushClicks_withNullCount_shouldSkip() {
        // Arrange
        String key = "click:abc123";
        Set<String> keys = Set.of(key);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository, never()).findByShortCodeAndActiveTrue(anyString());
        verify(urlMappingRepository, never()).save(any());
    }

    @Test
    void flushClicks_withNonExistentMapping_shouldSkip() {
        // Arrange
        String key = "click:notfound";
        Set<String> keys = Set.of(key);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("5");
        when(urlMappingRepository.findByShortCodeAndActiveTrue("notfound")).thenReturn(Optional.empty());

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository).findByShortCodeAndActiveTrue("notfound");
        verify(urlMappingRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void flushClicks_withException_shouldContinueProcessing() {
        // Arrange
        String key = "click:abc123";
        Set<String> keys = Set.of(key);

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("abc123");
        mapping.setClickCount(5L);

        when(redisTemplate.keys("click:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("3");
        when(urlMappingRepository.findByShortCodeAndActiveTrue("abc123")).thenReturn(Optional.of(mapping));
        when(urlMappingRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act
        clickCountFlushJob.flushClicks();

        // Assert
        verify(urlMappingRepository).save(mapping);
        verify(redisTemplate, never()).delete(key); // Should not delete if save fails
    }
}