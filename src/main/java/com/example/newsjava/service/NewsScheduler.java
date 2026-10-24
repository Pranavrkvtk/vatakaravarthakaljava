package com.example.newsjava.service;

import com.example.newsjava.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class NewsScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScheduler.class);
    private final NewsRepository repository;

    // Manually written constructor (replaces @RequiredArgsConstructor)
    public NewsScheduler(NewsRepository repository) {
        this.repository = repository;
    }

    // Runs every 60 seconds (60,000 milliseconds) after the previous run finishes
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now(); 
        
        // Use 'repository' here to match the field name above
        int publishedCount = repository.publishDueNews(now);
        
        if (publishedCount > 0) {
            log.info("Scheduler successfully published {} scheduled article(s).", publishedCount);
        }
    }
}