package com.kiran.urlshortener.service;

import com.kiran.urlshortener.entity.UrlMapping;
import com.kiran.urlshortener.events.UrlCreatedEvent;
import com.kiran.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private UrlEventProducer urlEventProducer;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createShortUrl_withValidUrl_shouldCreateNewMapping() {
        // Arrange
        String originalUrl = "https://example.com/very-long-url";
        LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);

        // First save returns entity without ID, second save returns with ID
        UrlMapping mappingWithoutId = new UrlMapping();
        mappingWithoutId.setLongUrl(originalUrl);
        mappingWithoutId.setExpiryAt(expiryTime);
        mappingWithoutId.setActive(true);

        UrlMapping mappingWithId = new UrlMapping();
        mappingWithId.setId(1L);
        mappingWithId.setLongUrl(originalUrl);
        mappingWithId.setExpiryAt(expiryTime);
        mappingWithId.setActive(true);

        when(urlMappingRepository.findByLongUrl(originalUrl)).thenReturn(Optional.empty());
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenAnswer(invocation -> {
                    UrlMapping mapping = invocation.getArgument(0);
                    if (mapping.getId() == null) {
                        // First save - return with ID set
                        mapping.setId(1L);
                    }
                    return mapping;
                });
        when(shortCodeGenerator.encode(1L)).thenReturn("abc123");

        // Act
        String result = urlService.createShortUrl(originalUrl, expiryTime);

        // Assert
        assertThat(result).isEqualTo("abc123");
        verify(urlMappingRepository, times(2)).save(any(UrlMapping.class));
        verify(shortCodeGenerator).encode(1L);
        verify(urlEventProducer).sendUrlCreatedEvent(any(UrlCreatedEvent.class));
    }

    @Test
    void createShortUrl_withNullUrl_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> urlService.createShortUrl(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Original URL cannot be null or blank");
    }

    @Test
    void createShortUrl_withBlankUrl_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> urlService.createShortUrl("   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Original URL cannot be null or blank");
    }

    @Test
    void createShortUrl_withExistingUrl_shouldReturnExistingShortCode() {
        // Arrange
        String originalUrl = "https://example.com/existing";
        LocalDateTime oldExpiry = LocalDateTime.now().plusDays(5);
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(10);

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setId(1L);
        existingMapping.setShortCode("existing123");
        existingMapping.setLongUrl(originalUrl);
        existingMapping.setExpiryAt(oldExpiry);

        when(urlMappingRepository.findByLongUrl(originalUrl)).thenReturn(Optional.of(existingMapping));
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(existingMapping);

        // Act
        String result = urlService.createShortUrl(originalUrl, newExpiry);

        // Assert
        assertThat(result).isEqualTo("existing123");
        verify(urlMappingRepository).save(existingMapping);
        assertThat(existingMapping.getExpiryAt()).isEqualTo(newExpiry);
    }

    @Test
    void createShortUrl_withExistingUrlAndNoNewExpiry_shouldNotUpdateExpiry() {
        // Arrange
        String originalUrl = "https://example.com/existing";
        LocalDateTime existingExpiry = LocalDateTime.now().plusDays(10);

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setId(1L);
        existingMapping.setShortCode("existing123");
        existingMapping.setLongUrl(originalUrl);
        existingMapping.setExpiryAt(existingExpiry);

        when(urlMappingRepository.findByLongUrl(originalUrl)).thenReturn(Optional.of(existingMapping));

        // Act
        String result = urlService.createShortUrl(originalUrl, LocalDateTime.now().plusDays(5));

        // Assert
        assertThat(result).isEqualTo("existing123");
        verify(urlMappingRepository, never()).save(any());
    }

    @Test
    void createShortUrl_withNullExpiry_shouldUseDefaultTTL() {
        // Arrange
        String originalUrl = "https://example.com/no-expiry";

        when(urlMappingRepository.findByLongUrl(originalUrl)).thenReturn(Optional.empty());
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenAnswer(invocation -> {
                    UrlMapping mapping = invocation.getArgument(0);
                    if (mapping.getId() == null) {
                        mapping.setId(1L);
                    }
                    return mapping;
                });
        when(shortCodeGenerator.encode(1L)).thenReturn("abc123");

        // Act
        String result = urlService.createShortUrl(originalUrl, null);

        // Assert
        assertThat(result).isEqualTo("abc123");
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("short:abc123"), eq(originalUrl), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertThat(ttlCaptor.getValue()).isEqualTo(TimeUnit.HOURS.toSeconds(24));
    }

    @Test
    void resolveLongUrl_withCachedUrl_shouldReturnFromCache() {
        // Arrange
        String shortCode = "abc123";
        String cachedUrl = "https://example.com/cached";

        when(valueOperations.get("short:abc123")).thenReturn(cachedUrl);

        // Act
        String result = urlService.resolveLongUrl(shortCode);

        // Assert
        assertThat(result).isEqualTo(cachedUrl);
        verify(valueOperations).increment("click:abc123");
        verify(urlMappingRepository, never()).findByShortCodeAndActiveTrue(anyString());
    }

    @Test
    void resolveLongUrl_withNonCachedUrl_shouldFetchFromDatabase() {
        // Arrange
        String shortCode = "abc123";
        String longUrl = "https://example.com/non-cached";

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(longUrl);
        mapping.setExpiryAt(LocalDateTime.now().plusDays(7));
        mapping.setActive(true);

        when(valueOperations.get("short:abc123")).thenReturn(null);
        when(urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)).thenReturn(Optional.of(mapping));

        // Act
        String result = urlService.resolveLongUrl(shortCode);

        // Assert
        assertThat(result).isEqualTo(longUrl);
        verify(valueOperations).set(eq("short:abc123"), eq(longUrl), anyLong(), eq(TimeUnit.SECONDS));
        verify(valueOperations).increment("click:abc123");
    }

    @Test
    void resolveLongUrl_withNonExistentShortCode_shouldReturnNull() {
        // Arrange
        String shortCode = "notfound";

        when(valueOperations.get("short:notfound")).thenReturn(null);
        when(urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)).thenReturn(Optional.empty());

        // Act
        String result = urlService.resolveLongUrl(shortCode);

        // Assert
        assertThat(result).isNull();
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void resolveLongUrl_withExpiredUrl_shouldReturnNull() {
        // Arrange
        String shortCode = "expired123";
        LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode(shortCode);
        mapping.setLongUrl("https://example.com/expired");
        mapping.setExpiryAt(pastExpiry);
        mapping.setActive(true);

        when(valueOperations.get("short:expired123")).thenReturn(null);
        when(urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)).thenReturn(Optional.of(mapping));

        // Act
        String result = urlService.resolveLongUrl(shortCode);

        // Assert
        assertThat(result).isNull();
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void resolveLongUrl_withRedisException_shouldFallbackToDatabase() {
        // Arrange
        String shortCode = "abc123";
        String longUrl = "https://example.com/fallback";

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(longUrl);
        mapping.setExpiryAt(LocalDateTime.now().plusDays(7));
        mapping.setActive(true);

        when(valueOperations.get("short:abc123")).thenThrow(new RuntimeException("Redis connection failed"));
        when(urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)).thenReturn(Optional.of(mapping));

        // Act
        String result = urlService.resolveLongUrl(shortCode);

        // Assert
        assertThat(result).isEqualTo(longUrl);
        verify(urlMappingRepository).findByShortCodeAndActiveTrue(shortCode);
    }

    @Test
    void createShortUrl_shouldSendUrlCreatedEvent() {
        // Arrange
        String originalUrl = "https://example.com/test";
        LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);

        when(urlMappingRepository.findByLongUrl(originalUrl)).thenReturn(Optional.empty());
        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenAnswer(invocation -> {
                    UrlMapping mapping = invocation.getArgument(0);
                    if (mapping.getId() == null) {
                        mapping.setId(1L);
                    }
                    return mapping;
                });
        when(shortCodeGenerator.encode(1L)).thenReturn("abc123");

        ArgumentCaptor<UrlCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UrlCreatedEvent.class);

        // Act
        urlService.createShortUrl(originalUrl, expiryTime);

        // Assert
        verify(urlEventProducer).sendUrlCreatedEvent(eventCaptor.capture());
        UrlCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getId()).isEqualTo(1L);
        assertThat(capturedEvent.getShortCode()).isEqualTo("abc123");
        assertThat(capturedEvent.getCreatedAt()).isNotNull();
    }
}