package com.kiran.urlshortener.service;

import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final char[] BASE62 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public String encode(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (id == 0) {
            return String.valueOf(BASE62[0]);
        }

        StringBuilder result = new StringBuilder();

        while (id > 0) {
            int remainder = (int) (id % 62);
            result.append(BASE62[remainder]);
            id /= 62;
        }

        return result.reverse().toString();
    }
}
