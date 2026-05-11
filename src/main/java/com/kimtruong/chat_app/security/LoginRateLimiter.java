package com.kimtruong.chat_app.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Cấu hình bandwidth: 5 tokens, refill 5 mỗi 15 phút (sau 5 fail, block 15 phút)
    private final Bandwidth bandwidth = Bandwidth.classic(MAX_FAILED_ATTEMPTS, 
            Refill.intervally(MAX_FAILED_ATTEMPTS, BLOCK_DURATION));

    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(bandwidth).build());
        return bucket.tryConsume(1);  // Check và consume 1 token nếu có
    }

    public void consume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(bandwidth).build());
        bucket.tryConsume(1);  // Consume 1 token nếu có, không throw nếu hết
    }

    public String buildKey(String username, String ip) {
        return username + ":" + ip;
    }
}