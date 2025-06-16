package com.github.tanyonghe.cortexcrawler;

import com.github.tanyonghe.cortexcrawler.crawler.WebCrawler;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler(5, 2.0); // 5 threads, 2 requests/sec
        crawler.addUrl("https://demo.cyotek.com", 3);
        crawler.start();
    }
}