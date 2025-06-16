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
import java.util.concurrent.ConcurrentHashMap;

public class WebCrawler {
    private final PriorityBlockingQueue<CrawlTask> queue = new PriorityBlockingQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final int maxThreads;
    private final RateLimiter rateLimiter;
    private final Map<String, Long> domainLastCrawlTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> domainCrawlDelays = new ConcurrentHashMap<>();
    private final Set<String> allowedDomains;
    private final Set<String> blockedDomains;
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
    private static final int DEFAULT_CRAWL_DELAY = 1000; // 1 second default delay

    public WebCrawler(int maxThreads, double requestsPerSecond) {
        this(maxThreads, requestsPerSecond, new HashSet<>(), new HashSet<>());
    }

    public WebCrawler(int maxThreads, double requestsPerSecond, Set<String> allowedDomains, Set<String> blockedDomains) {
        this.maxThreads = maxThreads;
        this.rateLimiter = new RateLimiter(requestsPerSecond);
        this.allowedDomains = new HashSet<>(allowedDomains);
        this.blockedDomains = new HashSet<>(blockedDomains);
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
            String host = parsedUrl.getHost();
            
            // Check domain allowlist/blocklist
            if (!allowedDomains.isEmpty() && !allowedDomains.contains(host)) {
                return false;
            }
            if (blockedDomains.contains(host)) {
                return false;
            }

            // Only allow HTTP and HTTPS protocols
            if (!parsedUrl.getProtocol().equals("http") && !parsedUrl.getProtocol().equals("https")) {
                return false;
            }
            // Validate host is not empty
            if (host.isEmpty()) {
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

    private void checkAndUpdateRobotsTxt(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            String robotsTxtUrl = parsedUrl.getProtocol() + "://" + host + "/robots.txt";

            if (!domainCrawlDelays.containsKey(host)) {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(robotsTxtUrl);
                    String content = httpClient.execute(request, response -> {
                        if (response.getCode() == 200) {
                            return EntityUtils.toString(response.getEntity());
                        }
                        return "";
                    });

                    // Parse robots.txt
                    boolean isUserAgentMatch = false;
                    int crawlDelay = DEFAULT_CRAWL_DELAY;
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("User-agent:")) {
                            isUserAgentMatch = line.substring(11).trim().equals("*");
                        } else if (isUserAgentMatch && line.startsWith("Crawl-delay:")) {
                            try {
                                crawlDelay = (int) (Double.parseDouble(line.substring(12).trim()) * 1000);
                            } catch (NumberFormatException e) {
                                // Use default if parsing fails
                            }
                        }
                    }
                    domainCrawlDelays.put(host, crawlDelay);
                } catch (Exception e) {
                    // If robots.txt is not accessible, use default delay
                    domainCrawlDelays.put(host, DEFAULT_CRAWL_DELAY);
                }
            }
        } catch (MalformedURLException e) {
            // Invalid URL, use default delay
        }
    }

    private void respectCrawlDelay(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            
            checkAndUpdateRobotsTxt(url);
            
            long lastCrawlTime = domainLastCrawlTime.getOrDefault(host, 0L);
            int crawlDelay = domainCrawlDelays.getOrDefault(host, DEFAULT_CRAWL_DELAY);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastCrawlTime < crawlDelay) {
                Thread.sleep(crawlDelay - (currentTime - lastCrawlTime));
            }
            
            domainLastCrawlTime.put(host, System.currentTimeMillis());
        } catch (Exception e) {
            // If anything goes wrong, just continue
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
                respectCrawlDelay(task.getUrl());
                
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

                Document doc = Jsoup.parse(html, task.getUrl());
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
                        Thread.sleep(1000L * retries); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
