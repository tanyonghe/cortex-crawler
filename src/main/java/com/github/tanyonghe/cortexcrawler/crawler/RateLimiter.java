package com.github.tanyonghe.cortexcrawler.crawler;

public class RateLimiter {
    private final long intervalMillis;
    private long nextAllowedTime = System.currentTimeMillis();

    public RateLimiter(double permitsPerSecond) {
        this.intervalMillis = (long) (1000 / permitsPerSecond);
    }

    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        if (now < nextAllowedTime) {
            Thread.sleep(nextAllowedTime - now);
        }
        nextAllowedTime = System.currentTimeMillis() + intervalMillis;
    }
}
