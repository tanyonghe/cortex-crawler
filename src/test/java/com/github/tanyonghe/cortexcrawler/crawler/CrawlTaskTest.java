package com.github.tanyonghe.cortexcrawler.crawler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrawlTaskTest {

    @Test
    void testConstructorAndGetters() {
        String url = "https://example.com";
        int priority = 5;
        CrawlTask task = new CrawlTask(url, priority);
        
        assertEquals(url, task.getUrl());
        assertEquals(priority, task.getPriority());
    }

    @Test
    void testCompareTo() {
        CrawlTask highPriority = new CrawlTask("https://example.com", 10);
        CrawlTask mediumPriority = new CrawlTask("https://example.com", 5);
        CrawlTask lowPriority = new CrawlTask("https://example.com", 1);
        
        // Higher priority should come first (lower in natural ordering)
        assertTrue(highPriority.compareTo(mediumPriority) < 0);
        assertTrue(highPriority.compareTo(lowPriority) < 0);
        assertTrue(mediumPriority.compareTo(lowPriority) < 0);
        
        // Same priority should be equal
        CrawlTask samePriority = new CrawlTask("https://example.com", 5);
        assertEquals(0, mediumPriority.compareTo(samePriority));
        
        // Lower priority should come later (higher in natural ordering)
        assertTrue(lowPriority.compareTo(mediumPriority) > 0);
        assertTrue(lowPriority.compareTo(highPriority) > 0);
        assertTrue(mediumPriority.compareTo(highPriority) > 0);
    }

    @Test
    void testEqualsAndHashCode() {
        CrawlTask task1 = new CrawlTask("https://example.com", 5);
        CrawlTask task2 = new CrawlTask("https://example.com", 5);
        CrawlTask task3 = new CrawlTask("https://example.com", 3);
        CrawlTask task4 = new CrawlTask("https://other.com", 5);
        
        // Test equals - all tasks should be different objects
        assertNotEquals(task1, task2, "Different CrawlTask objects should not be equal");
        assertNotEquals(task1, task3, "Different CrawlTask objects should not be equal");
        assertNotEquals(task1, task4, "Different CrawlTask objects should not be equal");
        
        // Test hashCode - all tasks should have different hashcodes
        assertNotEquals(task1.hashCode(), task2.hashCode(), 
            "Different CrawlTask objects should have different hashcodes");
    }
} 