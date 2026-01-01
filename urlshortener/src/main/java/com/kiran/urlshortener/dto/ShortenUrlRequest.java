package com.kiran.urlshortener.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ShortenUrlRequest {

    @NotBlank(message="URL cannot be blank")
    private String longUrl;
    
    @Min(value = 1, message = "Expiry days must be at least 1")
    private Integer expiryDays;
}