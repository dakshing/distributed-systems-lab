package com.dsl.urlshortener;

import com.dsl.common.idgenerator.SnowflakeIdGenerator;
import com.dsl.urlshortener.handler.ShortenerHandler;
import com.dsl.urlshortener.repository.InMemoryUrlRepository;
import com.dsl.urlshortener.repository.UrlRepository;
import com.dsl.urlshortener.server.NettyServer;

public class Application {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        int nodeId = 1; // Unique Server ID
        String host = "http://localhost:" + port + "/";

        System.out.println("Initializing system components...");

        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId);

        UrlRepository repository = new InMemoryUrlRepository();

        ShortenerHandler shortenerHandler = new ShortenerHandler(idGenerator, repository, host);

        NettyServer<ShortenerHandler> server = new NettyServer<>(port, shortenerHandler);
        server.start();
    }
}

