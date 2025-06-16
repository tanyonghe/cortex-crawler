package com.github.tanyonghe.cortexcrawler;

import java.util.HashSet;
import java.util.Set;

import com.github.tanyonghe.cortexcrawler.crawler.WebCrawler;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
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
    }
}