package com.example.newsjava.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subtitle;

    @Column(columnDefinition = "TEXT")   // was LONGTEXT — Postgres has no LONGTEXT
    private String content;

    private String category;

    @Column(columnDefinition = "TEXT")   // was LONGTEXT
    private String image;

    // NEW: thumbnail URL for list responses (audit Fix 5)
    private String thumbnailUrl;

    private String status;          // draft, pending, published, scheduled, trash
    private String tags;
    private String author;

    // ---- All NOT NULL booleans with database defaults ----
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean featured = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean trending = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean published = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean breakingNews = false;

    // ---- Numeric fields with defaults ----
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int shares = 0;

    private int views = 0;
    private int likes = 0;

    // ---- Dates ----
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime publishedDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- Lifecycle callbacks ----
    @PrePersist
    protected void onCreate() {
        if (status == null) status = "draft";
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        published = "published".equals(status);
        if ("published".equals(status) && publishedDate == null) {
            publishedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        published = "published".equals(status);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isTrending() { return trending; }
    public void setTrending(boolean trending) { this.trending = trending; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public boolean isBreakingNews() { return breakingNews; }
    public void setBreakingNews(boolean breakingNews) { this.breakingNews = breakingNews; }
    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDateTime publishedDate) { this.publishedDate = publishedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}