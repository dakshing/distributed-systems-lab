package com.dsl.urlshortener.repository;

public interface UrlRepository {
    void save(String shortUrl, String originalUrl);
    String getOriginalUrl(String shortUrl);
}