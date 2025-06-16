# CortexCrawler

A robust, multi-threaded web crawler written in Java with support for rate limiting, robots.txt compliance, and domain filtering.

## Features

- **Multi-threaded Crawling**: Process multiple URLs concurrently with configurable thread count
- **Rate Limiting**: Control request rate per second to avoid overwhelming servers
- **Robots.txt Support**: Respects robots.txt rules including crawl delays
- **Domain Control**: 
  - Allowlist/blocklist for domains
  - Domain-specific crawl delays
- **URL Handling**:
  - URL normalization
  - Relative/absolute URL resolution
  - File extension filtering
  - Session ID removal
- **Content Type Filtering**: Only processes HTML content
- **Error Handling**:
  - Automatic retries with exponential backoff
  - HTTP redirect handling
  - Timeout configuration
- **Priority-based Crawling**: URLs are processed based on their priority level

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Dependencies

- Apache HttpClient 5.x
- JSoup HTML Parser
- JUnit 5 (for testing)

## Usage

```java
// Create sets for domain control
Set<String> allowedDomains = new HashSet<>();
allowedDomains.add("example.com");

Set<String> blockedDomains = new HashSet<>();
blockedDomains.add("blocked.com");

// Create crawler with 5 threads and 2 requests per second
WebCrawler crawler = new WebCrawler(5, 2.0, allowedDomains, blockedDomains);

// Add initial URLs to crawl
crawler.addUrl("https://example.com", 5);

// Start crawling
crawler.start();
```

## Configuration

The crawler can be configured with the following parameters:

- `maxThreads`: Number of concurrent crawling threads
- `requestsPerSecond`: Rate limit for requests
- `allowedDomains`: Set of allowed domains (empty set means all domains are allowed)
- `blockedDomains`: Set of blocked domains

## URL Priority

URLs are processed based on their priority:
- Higher priority numbers are processed first
- Each discovered link gets a priority one less than its parent
- URLs with priority <= 0 are not processed

## Error Handling

The crawler includes:
- Automatic retries (default: 3 attempts)
- Exponential backoff between retries
- HTTP redirect handling (301, 302)
- Content type validation
- Timeout settings (10s connect, 30s response)

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
