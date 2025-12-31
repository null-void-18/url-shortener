package com.kiran.urlshortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiran.urlshortener.entity.UrlMapping;

public interface UrlMappingRepository extends JpaRepository<UrlMapping,Long>{
    Optional<UrlMapping> findByShortCodeAndActiveTrue(String shortCode);
    Optional<UrlMapping> findByLongUrl(String longUrl);
}
