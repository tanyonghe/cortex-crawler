package com.github.tanyonghe.cortexcrawler;

import java.util.HashSet;
import java.util.Set;

import com.github.tanyonghe.cortexcrawler.crawler.WebCrawler;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Example domains to crawl
        Set<String> allowedDomains = new HashSet<>();
        allowedDomains.add("crawler-test.com");

        // Example domains to block
        Set<String> blockedDomains = new HashSet<>();
        blockedDomains.add("blocked.com");
        blockedDomains.add("malicious.org");

        // Create crawler with 5 threads and 2 requests per second
        WebCrawler crawler = new WebCrawler(5, 2.0, allowedDomains, blockedDomains);

        // Add some initial URLs to crawl
        crawler.addUrl("https://crawler-test.com/", 5);

        crawler.start();
    }
}