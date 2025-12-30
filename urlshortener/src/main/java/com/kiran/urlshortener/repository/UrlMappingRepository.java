package com.kiran.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiran.urlshortener.entity.UrlMapping;

public interface UrlMappingRepository extends JpaRepository<UrlMapping,Long>{
    
}
