package com.github.tanyonghe.cortexcrawler.crawler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void testConstructor() {
        RateLimiter limiter = new RateLimiter(10.0); // 10 requests per second
        assertNotNull(limiter);
    }

    @Test
    @Timeout(2) // Test should complete within 2 seconds
    void testAcquireWithNoDelay() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(10.0); // 10 requests per second
        
        long startTime = System.currentTimeMillis();
        limiter.acquire();
        long endTime = System.currentTimeMillis();
        
        // First request should not be delayed
        assertTrue(endTime - startTime < 100);
    }

    @Test
    @Timeout(3) // Test should complete within 3 seconds
    void testAcquireWithDelay() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1.0); // 1 request per second
        
        // First request
        limiter.acquire();
        
        // Second request should be delayed
        long startTime = System.currentTimeMillis();
        limiter.acquire();
        long endTime = System.currentTimeMillis();
        
        // Should have waited approximately 1 second
        long waitTime = endTime - startTime;
        assertTrue(waitTime >= 900 && waitTime <= 1100, 
            "Expected wait time between 900-1100ms, but was " + waitTime);
    }

    @Test
    @Timeout(4) // Test should complete within 4 seconds
    void testMultipleAcquires() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(2.0); // 2 requests per second
        
        // First request
        limiter.acquire();
        
        // Second request (should be delayed by ~500ms)
        long startTime = System.currentTimeMillis();
        limiter.acquire();
        long endTime = System.currentTimeMillis();
        
        // Should have waited approximately 0.5 seconds
        long waitTime = endTime - startTime;
        assertTrue(waitTime >= 400 && waitTime <= 600, 
            "Expected wait time between 400-600ms, but was " + waitTime);
        
        // Third request (should be delayed by another ~500ms)
        startTime = System.currentTimeMillis();
        limiter.acquire();
        endTime = System.currentTimeMillis();
        
        waitTime = endTime - startTime;
        assertTrue(waitTime >= 400 && waitTime <= 600, 
            "Expected wait time between 400-600ms, but was " + waitTime);
    }

    @Test
    @Timeout(2) // Test should complete within 2 seconds
    void testHighRateLimit() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(100.0); // 100 requests per second
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            limiter.acquire();
        }
        long endTime = System.currentTimeMillis();
        
        // 5 requests at 100/sec should take less than 100ms
        assertTrue(endTime - startTime < 100);
    }
} 