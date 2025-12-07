package com.dsl.urlshortener;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dsl.common.idgenerator.SnowflakeIdGenerator;
import com.dsl.urlshortener.handler.ShortenerHandler;
import com.dsl.urlshortener.repository.ScyllaUrlRepository;
import com.dsl.urlshortener.repository.UrlRepository;
import com.dsl.urlshortener.server.NettyServer;

import java.net.InetSocketAddress;

public class Application {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String host = "http://localhost:" + port + "/";

        String scyllaHost = System.getenv("SCYLLA_HOST");
        if (scyllaHost == null) {
            scyllaHost = "127.0.0.1";
        }

        int nodeId = Integer.parseInt(System.getenv("NODE_ID"));

        System.out.println("Connecting to ScyllaDB at " + scyllaHost + "...");

        CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(scyllaHost, 9042))
                .withLocalDatacenter("datacenter1")
                .build();

        initializeSchema(session);

        System.out.println("Initializing system components...");

        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId);

        UrlRepository repository = new ScyllaUrlRepository(session);

        ShortenerHandler shortenerHandler = new ShortenerHandler(idGenerator, repository, host);

        NettyServer<ShortenerHandler> server = new NettyServer<>(port, shortenerHandler);
        server.start();
    }

    private static void initializeSchema(CqlSession session) {
        session.execute("CREATE KEYSPACE IF NOT EXISTS shortener " +
                "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");

        session.execute("CREATE TABLE IF NOT EXISTS shortener.urls (" +
                "short_url text PRIMARY KEY, " +
                "original_url text, " +
                "id bigint, " +
                "created_at timestamp)");

        System.out.println("Schema initialized.");
    }
}

