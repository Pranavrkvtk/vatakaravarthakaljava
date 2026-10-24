package com.example.newsjava.model;

import java.time.LocalDateTime;

public interface NewsSummary {
    Long getId();
    String getTitle();
    String getSubtitle();
    String getCategory();
    String getImage();
    String getThumbnailUrl(); // Added this since you have it in News.java
    String getAuthor();
    LocalDateTime getPublishedDate();
    LocalDateTime getCreatedAt();
    boolean isBreakingNews();
    boolean isFeatured();
    int getViews();
    int getLikes();
}