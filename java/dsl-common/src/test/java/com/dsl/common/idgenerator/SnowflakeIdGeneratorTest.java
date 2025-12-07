package com.dsl.common.idgenerator;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void testUniqueIdsSingleThread() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        Set<Long> ids = new HashSet<>();
        int iterations = 100_000;

        for (int i = 0; i < iterations; i++) {
            long id = generator.nextId();
            ids.add(id);
        }

        assertEquals(iterations, ids.size(), "All IDs should be unique");
    }

    @Test
    void testUniqueIdsMultiThreaded() throws InterruptedException, ExecutionException {
        int threadCount = 50;
        int requestsPerThread = 1_000;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);

        // A thread-safe Set to store IDs from all threads
        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> {
                    for (int j = 0; j < requestsPerThread; j++) {
                        ids.add(generator.nextId());
                    }
                    return null;
                });
            }

            // Run all threads
            List<Future<Void>> futures = executor.invokeAll(tasks);

            // Wait for completion
            for (Future<Void> future : futures) {
                future.get();
            }

            executor.shutdown();
        }

        assertEquals(threadCount * requestsPerThread, ids.size(),
                "Every ID generated across all threads should be unique");
    }

    @Test
    void testIdsAreSortableByTime() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        long id1 = generator.nextId();

        Thread.sleep(1);

        long id2 = generator.nextId();

        assertTrue(id2 > id1, "Later ID should be larger than earlier ID");
    }

    @Test
    void testNodeIdBits() {
        SnowflakeIdGenerator node1 = new SnowflakeIdGenerator(1);
        SnowflakeIdGenerator node2 = new SnowflakeIdGenerator(2);

        long id1 = node1.nextId();
        long id2 = node2.nextId();

        assertNotEquals(id1, id2);
    }
}