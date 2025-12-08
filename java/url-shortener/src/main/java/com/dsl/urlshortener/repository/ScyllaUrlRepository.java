package com.dsl.urlshortener.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import java.time.Instant;

public class ScyllaUrlRepository implements UrlRepository {

    private final CqlSession session;
    private final PreparedStatement insertStmt;
    private final PreparedStatement selectStmt;

    public ScyllaUrlRepository(CqlSession session) {
        this.session = session;

        // Prepare statements once (Performance Best Practice)
        this.insertStmt = session.prepare(
                "INSERT INTO shortener.urls (short_url, original_url, id, created_at) VALUES (?, ?, ?, ?)"
        );

        this.selectStmt = session.prepare(
                "SELECT original_url FROM shortener.urls WHERE short_url = ?"
        );
    }

    @Override
    public void save(long id, String shortUrl, String originalUrl) {
        session.execute(insertStmt.bind(shortUrl, originalUrl, id, Instant.now()));
    }

    @Override
    public String getOriginalUrl(String shortUrl) {
        ResultSet rs = session.execute(selectStmt.bind(shortUrl));
        Row row = rs.one();
        return (row != null) ? row.getString("original_url") : null;
    }
}
