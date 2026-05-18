package com.kiran.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortCodeGeneratorTest {

    private ShortCodeGenerator shortCodeGenerator;

    @BeforeEach
    void setUp() {
        shortCodeGenerator = new ShortCodeGenerator();
    }

    @Test
    void encode_withNullId_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> shortCodeGenerator.encode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Id cannot be null");
    }

    @Test
    void encode_withZeroId_shouldReturnFirstCharacter() {
        // Act
        String result = shortCodeGenerator.encode(0L);

        // Assert
        assertThat(result).isEqualTo("a");
    }

    @ParameterizedTest
    @CsvSource({
            "1, b",
            "10, k",
            "61, 9",
            "62, ba",
            "100, bM"
    })
    void encode_withVariousIds_shouldReturnCorrectShortCode(Long id, String expected) {
        // Act
        String result = shortCodeGenerator.encode(id);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void encode_withLargeId_shouldReturnValidShortCode() {
        // Act
        String result = shortCodeGenerator.encode(1000000L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.length()).isGreaterThan(0);
        assertThat(result).matches("[a-zA-Z0-9]+");
    }

    @Test
    void encode_shouldProduceDifferentCodesForDifferentIds() {
        // Act
        String code1 = shortCodeGenerator.encode(1L);
        String code2 = shortCodeGenerator.encode(2L);
        String code3 = shortCodeGenerator.encode(100L);

        // Assert
        assertThat(code1).isNotEqualTo(code2);
        assertThat(code2).isNotEqualTo(code3);
        assertThat(code1).isNotEqualTo(code3);
    }

    @Test
    void encode_shouldUseBase62Characters() {
        // Arrange
        String base62Pattern = "^[a-zA-Z0-9]+$";

        // Act
        String result1 = shortCodeGenerator.encode(1L);
        String result2 = shortCodeGenerator.encode(100L);
        String result3 = shortCodeGenerator.encode(10000L);

        // Assert
        assertThat(result1).matches(base62Pattern);
        assertThat(result2).matches(base62Pattern);
        assertThat(result3).matches(base62Pattern);
    }

    @Test
    void encode_shouldProduceConsistentResults() {
        // Arrange
        Long testId = 12345L;

        // Act
        String result1 = shortCodeGenerator.encode(testId);
        String result2 = shortCodeGenerator.encode(testId);
        String result3 = shortCodeGenerator.encode(testId);

        // Assert
        assertThat(result1).isEqualTo(result2);
        assertThat(result2).isEqualTo(result3);
    }
}