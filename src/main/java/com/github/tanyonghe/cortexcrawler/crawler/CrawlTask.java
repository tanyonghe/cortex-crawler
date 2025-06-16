package com.github.tanyonghe.cortexcrawler.crawler;

public class CrawlTask implements Comparable<CrawlTask> {
    private final String url;
    private final int priority;

    public CrawlTask(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    public String getUrl() { return url; }

    public int getPriority() { return priority; }

    @Override
    public int compareTo(CrawlTask other) {
        return Integer.compare(other.priority, this.priority); // Higher priority first
    }
}
