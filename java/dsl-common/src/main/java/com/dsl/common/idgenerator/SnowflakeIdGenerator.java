package com.dsl.common.idgenerator;

import java.time.Instant;

/**
 * A distributed unique ID generator inspired by Twitter's Snowflake.
 * <p>
 * Structure (64 bits):
 * 1 bit: Unused (sign bit)
 * 41 bits: Timestamp (milliseconds since custom epoch) -> 2,199,023,255,552 milliseconds or ~69 years
 * 10 bits: Node ID (configured per server) -> 1,023 servers
 * 12 bits: Sequence number (for IDs generated within the same millisecond) -> 4,096 IDs per millisecond
 */
public class SnowflakeIdGenerator {

    // Custom Epoch (e.g., Jan 1st, 2025) - Allows for ~69 years of IDs
    private static final long CUSTOM_EPOCH = 1735689600000L;

    private static final long NODE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // Bit shifts
    private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS;

    private final long nodeId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * @param nodeId Unique ID for this server/process (0 - 1023)
     */
    public SnowflakeIdGenerator(long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(String.format("Node ID must be between 0 and %d", MAX_NODE_ID));
        }
        this.nodeId = nodeId;
    }

    public synchronized long nextId() {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }

        if (currentTimestamp == lastTimestamp) {
            // Same millisecond: increment sequence
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted, wait for next millisecond
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // New millisecond: reset sequence
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // Bitwise OR to combine parts
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    private long timestamp() {
        return Instant.now().toEpochMilli();
    }
}