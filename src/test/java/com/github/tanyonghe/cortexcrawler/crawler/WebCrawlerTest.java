package com.github.tanyonghe.cortexcrawler.crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WebCrawlerTest {

    private WebCrawler crawler;
    private Set<String> allowedDomains;
    private Set<String> blockedDomains;

    @BeforeEach
    void setUp() {
        allowedDomains = new HashSet<>();
        blockedDomains = new HashSet<>();
        crawler = new WebCrawler(1, 1.0, allowedDomains, blockedDomains);
    }

    @Test
    void testConstructor() {
        assertNotNull(crawler);
        WebCrawler crawler2 = new WebCrawler(5, 2.0, new HashSet<>(), new HashSet<>());
        assertNotNull(crawler2);
    }

    @Test
    void testAddUrlWithValidUrl() {
        String url = "https://example.com";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithInvalidUrl() {
        String url = "invalid-url";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithBlockedDomain() {
        blockedDomains.add("blocked.com");
        String url = "https://blocked.com/page";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithAllowedDomain() {
        allowedDomains.add("allowed.com");
        String url = "https://allowed.com/page";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithNonAllowedDomain() {
        allowedDomains.add("allowed.com");
        String url = "https://other.com/page";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithFileExtension() {
        String url = "https://example.com/image.jpg";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithProtocol() {
        String url = "ftp://example.com";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithEmptyHost() {
        String url = "https://";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithPriority() {
        String url1 = "https://example.com/page1";
        String url2 = "https://example.com/page2";
        crawler.addUrl(url1, 10);
        crawler.addUrl(url2, 5);
        assertTrue(crawler.isVisited(url1));
        assertTrue(crawler.isVisited(url2));
    }

    @Test
    void testAddUrlWithZeroPriority() {
        String url = "https://example.com";
        crawler.addUrl(url, 0);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithNegativePriority() {
        String url = "https://example.com";
        crawler.addUrl(url, -1);
        assertFalse(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithDuplicateUrl() {
        String url = "https://example.com";
        crawler.addUrl(url, 5);
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithDifferentPriorities() {
        String url = "https://example.com";
        crawler.addUrl(url, 5);
        crawler.addUrl(url, 10);
        assertTrue(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithSessionId() {
        String url = "https://example.com/page?jsessionid=123456";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited("https://example.com/page"));
    }

    @Test
    void testAddUrlWithDefaultPort() {
        String url = "https://example.com:443/page";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited("https://example.com/page"));
    }

    @Test
    void testAddUrlWithMixedCase() {
        String url = "HTTPS://EXAMPLE.COM/PAGE";
        crawler.addUrl(url, 5);
        assertFalse(crawler.isVisited("https://example.com/page"));
        assertTrue(crawler.isVisited("https://example.com/PAGE"));
    }

    @Test
    void testAddUrlWithEmptyAllowlist() {
        String url = "https://example.com";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited(url));
    }

    @Test
    void testAddUrlWithEmptyBlocklist() {
        String url = "https://example.com";
        crawler.addUrl(url, 5);
        assertTrue(crawler.isVisited(url));
    }
} 