package com.kiran.urlshortener.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "url_mapping",
    indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, unique = true)
    private String shortCode;

    @Column(nullable = false, unique = true,columnDefinition = "TEXT")
    @NotBlank
    private String longUrl;

    private LocalDateTime expiryAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private Long clickCount = 0L;

    private boolean active = true;
}
