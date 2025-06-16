package com.github.tanyonghe.cortexcrawler.crawler;

import java.util.concurrent.*;
import java.util.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Pattern;

public class WebCrawler {
    private final PriorityBlockingQueue<CrawlTask> queue = new PriorityBlockingQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final int maxThreads;
    private final RateLimiter rateLimiter;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "text/html",
        "application/xhtml+xml"
    );
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar", ".exe", ".dll",
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".ico", ".mp3", ".mp4", ".avi"
    );
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
        "(?i)(?:jsessionid|phpsessid|sid|sessionid|aspsessionid)=[^&]+"
    );
    private static final int MAX_RETRIES = 3;

    public WebCrawler(int maxThreads, double requestsPerSecond) {
        this.maxThreads = maxThreads;
        this.rateLimiter = new RateLimiter(requestsPerSecond);
    }

    public void addUrl(String url, int priority) {
        String normalizedUrl = normalizeUrl(url);
        if (normalizedUrl != null && isValidUrl(normalizedUrl) && visited.add(normalizedUrl)) {
            queue.add(new CrawlTask(normalizedUrl, priority));
        }
    }

    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            String host = parsedUrl.getHost().toLowerCase();
            String path = parsedUrl.getPath();
            String query = parsedUrl.getQuery();

            // Remove trailing slash
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Remove default ports
            int port = parsedUrl.getPort();
            if (port == 80 || port == 443) {
                port = -1;
            }

            // Build normalized URL
            StringBuilder normalized = new StringBuilder();
            normalized.append(protocol).append("://").append(host);
            if (port != -1) {
                normalized.append(":").append(port);
            }
            normalized.append(path);
            if (query != null) {
                // Remove session IDs from query
                query = SESSION_ID_PATTERN.matcher(query).replaceAll("");
                if (!query.isEmpty()) {
                    normalized.append("?").append(query);
                }
            }

            return normalized.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private boolean isValidUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            // Only allow HTTP and HTTPS protocols
            if (!parsedUrl.getProtocol().equals("http") && !parsedUrl.getProtocol().equals("https")) {
                return false;
            }
            // Validate host is not empty
            if (parsedUrl.getHost().isEmpty()) {
                return false;
            }
            // Check for blocked file extensions
            String path = parsedUrl.getPath().toLowerCase();
            if (BLOCKED_EXTENSIONS.stream().anyMatch(path::endsWith)) {
                return false;
            }
            return true;
        } catch (MalformedURLException e) {
            return false;
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
        
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(task.getUrl());
                // Set timeouts
                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(10))
                    .setResponseTimeout(Timeout.ofSeconds(30))
                    .build();
                request.setConfig(requestConfig);

                String html = httpClient.execute(request, response -> {
                    int statusCode = response.getCode();
                    if (statusCode == 301 || statusCode == 302) {
                        String location = response.getHeader("Location").getValue();
                        addUrl(location, task.getPriority());
                        throw new IOException("Redirecting to: " + location);
                    }
                    if (statusCode >= 400) {
                        throw new IOException("HTTP " + statusCode);
                    }
                    // Check content type
                    String contentType = response.getHeader("Content-Type").getValue();
                    if (!ALLOWED_CONTENT_TYPES.stream().anyMatch(contentType::contains)) {
                        throw new IOException("Unsupported content type: " + contentType);
                    }
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
                break; // Success, exit retry loop
            } catch (IOException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    System.err.println("Error crawling " + task.getUrl() + " after " + MAX_RETRIES + " retries: " + e.getMessage());
                } else {
                    System.out.println("Retry " + retries + " for " + task.getUrl() + ": " + e.getMessage());
                    try {
                        Thread.sleep(1000 * retries); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
