package com.github.tanyonghe.cortexcrawler.crawler;

import java.util.concurrent.*;
import java.util.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class WebCrawler {
    private final PriorityBlockingQueue<CrawlTask> queue = new PriorityBlockingQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final int maxThreads;
    private final RateLimiter rateLimiter;

    public WebCrawler(int maxThreads, double requestsPerSecond) {
        this.maxThreads = maxThreads;
        this.rateLimiter = new RateLimiter(requestsPerSecond);
    }

    public void addUrl(String url, int priority) {
        if (visited.add(url)) {
            queue.add(new CrawlTask(url, priority));
        }
    }

    public void start() {
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            executor.submit(this::worker);
        }
        executor.shutdown();
    }

    private void worker() {
        while (true) {
            try {
                CrawlTask task = queue.take();
                rateLimiter.acquire();
                process(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void process(CrawlTask task) {
        if (task.getPriority() <= 0) {
            return;
        }
        System.out.println("Crawling: " + task.getUrl() + ", priority: " + task.getPriority());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(task.getUrl());
            String html = httpClient.execute(request, response -> {
                return EntityUtils.toString(response.getEntity());
            });

            Document doc = Jsoup.parse(html);
            Elements links = doc.select("a[href]");
            
            int newPriority = task.getPriority() - 1;
            for (Element link : links) {
                String href = link.attr("abs:href");
                if (href != null && !href.isEmpty() && !href.startsWith("javascript:")) {
                    addUrl(href, newPriority);
                }
            }
        } catch (IOException e) {
            System.err.println("Error crawling " + task.getUrl() + ": " + e.getMessage());
        }
    }
}
