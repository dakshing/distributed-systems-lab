package com.dsl.urlshortener.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUrlRepository implements UrlRepository {

    private final Map<String, String> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String shortUrl, String originalUrl) {
        storage.put(shortUrl, originalUrl);
    }

    @Override
    public String getOriginalUrl(String shortUrl) {
        return storage.get(shortUrl);
    }
}