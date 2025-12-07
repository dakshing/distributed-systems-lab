package com.dsl.urlshortener.repository;

public interface UrlRepository {
    void save(long id, String shortUrl, String originalUrl);

    String getOriginalUrl(String shortUrl);
}