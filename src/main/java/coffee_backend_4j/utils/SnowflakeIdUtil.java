package coffee_backend_4j.utils;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdUtil {
    private final long workerId = 1L;
    private final long datacenterId = 1L;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private final long twepoch = 1609459200000L;

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << 22) |
                (datacenterId << 17) |
                (workerId << 12) |
                sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    public String generateOrderNo() {
        return "ORD" + nextId();
    }
}
