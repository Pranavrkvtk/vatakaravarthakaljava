// src/main/java/com/example/newsjava/controller/NewsController.java
package com.example.newsjava.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl; // ✅ Added for caching
import com.example.newsjava.model.News;
import com.example.newsjava.model.NewsSummary; // ✅ Added
import com.example.newsjava.repository.NewsRepository;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit; // ✅ Added for CacheControl
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsRepository repository;
    private final String UPLOAD_DIR = "./uploads/content/";

    public NewsController(NewsRepository repository) {
        this.repository = repository;
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            System.out.println("✅ Upload directory created: " + UPLOAD_DIR);
        } catch (IOException e) {
            System.err.println("❌ Could not create upload directory: " + e.getMessage());
        }
    }

    // ============================================================
    // PUBLIC USER ENDPOINTS (Now Paginated & Cached)
    // ============================================================

    @GetMapping("/published")
    public ResponseEntity<Page<NewsSummary>> getPublishedNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        Page<NewsSummary> news = repository.findPublishedNews(LocalDateTime.now(), pageable);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<NewsSummary>> getLatestNews() {
        Pageable top10 = PageRequest.of(0, 10);
        List<NewsSummary> news = repository.findTop10LatestNews(LocalDateTime.now(), top10);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @GetMapping("/breaking")
    public ResponseEntity<List<NewsSummary>> getBreakingNews() {
        List<NewsSummary> news = repository.findBreakingNews(LocalDateTime.now());
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<NewsSummary>> getTrendingNews() {
        Pageable top10 = PageRequest.of(0, 10);
        List<NewsSummary> news = repository.findTop10TrendingNews(LocalDateTime.now(), top10);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<NewsSummary>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        Page<NewsSummary> news = repository.findNewsByCategory(category, LocalDateTime.now(), pageable);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsById(@PathVariable Long id) {
        Optional<News> optionalNews = repository.findById(id);
        
        if (optionalNews.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "News not found"));
        }
        
        News news = optionalNews.get();
        
        if (!"published".equals(news.getStatus())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "This content is not available");
            errorResponse.put("status", news.getStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        repository.incrementViews(id);
        news.setViews(news.getViews() + 1);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(news);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeNews(@PathVariable Long id) {
        Optional<News> optionalNews = repository.findById(id);
        if (optionalNews.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        
        News news = optionalNews.get();
        if (!"published".equals(news.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "This content is not available"));
        }
        
        repository.incrementLikes(id);
        news.setLikes(news.getLikes() + 1);
        return ResponseEntity.ok(news);
    }

    // ============================================================
    // ADVANCED SEARCH (Now Database-Driven Pagination)
    // ============================================================
    @GetMapping("/search/advanced")
    public ResponseEntity<Map<String, Object>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;
        try { if (fromDate != null && !fromDate.isEmpty()) fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00"); } catch (Exception e) {}
        try { if (toDate != null && !toDate.isEmpty()) toDateTime = LocalDateTime.parse(toDate + "T23:59:59"); } catch (Exception e) {}

        Sort sort = Sort.by("publishedDate").descending(); 
        if ("oldest".equals(sortBy)) sort = Sort.by("publishedDate").ascending();
        else if ("views".equals(sortBy)) sort = Sort.by("views").descending();
        else if ("likes".equals(sortBy)) sort = Sort.by("likes").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NewsSummary> results = repository.advancedSearch(
            keyword, category, author, fromDateTime, toDateTime, pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("news", results.getContent());
        response.put("total", results.getTotalElements());
        response.put("totalPages", results.getTotalPages());
        response.put("page", page);
        response.put("size", size);
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("keyword", keyword != null ? keyword : "");
        filters.put("category", category != null ? category : "");
        filters.put("author", author != null ? author : "");
        filters.put("fromDate", fromDate != null ? fromDate : "");
        filters.put("toDate", toDate != null ? toDate : "");
        filters.put("sortBy", sortBy != null ? sortBy : "latest");
        
        response.put("filters", filters);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ADMIN ENDPOINTS
    // ============================================================
    @GetMapping("/all")
    public List<News> getAllNewsAdmin() { return repository.findAll(); }

    @GetMapping("/status/{status}")
    public List<News> getNewsByStatus(@PathVariable String status) { return repository.findByStatus(status); }

    @PostMapping
    public News createNews(@RequestBody News news) {
        if (news.getStatus() == null || news.getStatus().isEmpty()) news.setStatus("draft");
        news.setViews(0); news.setLikes(0); news.setShares(0);
        if (news.getContent() != null) {
            news.setContent(Jsoup.clean(news.getContent(), Safelist.relaxed().addAttributes(":all", "style", "class")));
        }
        return repository.save(news);
    }

    @PutMapping("/{id}")
    public News updateNews(@PathVariable Long id, @RequestBody News news) {
        News existing = repository.findById(id).orElseThrow(() -> new NoSuchElementException("News not found"));
        if (news.getTitle() != null) existing.setTitle(news.getTitle());
        if (news.getSubtitle() != null) existing.setSubtitle(news.getSubtitle());
        if (news.getContent() != null) {
            existing.setContent(Jsoup.clean(news.getContent(), Safelist.relaxed().addAttributes(":all", "style", "class")));
        }
        if (news.getCategory() != null) existing.setCategory(news.getCategory());
        if (news.getImage() != null) existing.setImage(news.getImage());
        if (news.getStatus() != null) existing.setStatus(news.getStatus());
        if (news.getTags() != null) existing.setTags(news.getTags());
        if (news.getScheduledDate() != null) existing.setScheduledDate(news.getScheduledDate());
        if (news.getAuthor() != null) existing.setAuthor(news.getAuthor());
        existing.setBreakingNews(news.isBreakingNews());
        existing.setFeatured(news.isFeatured());
        existing.setTrending(news.isTrending());
        if (news.getViews() > 0) existing.setViews(news.getViews());
        if (news.getLikes() > 0) existing.setLikes(news.getLikes());
        if (news.getShares() > 0) existing.setShares(news.getShares());
        if ("published".equals(news.getStatus()) && existing.getPublishedDate() == null) {
            existing.setPublishedDate(LocalDateTime.now());
            existing.setScheduledDate(null);
        }
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deleteNews(@PathVariable Long id) { repository.deleteById(id); }

    @PostMapping("/bulk")
    @Transactional
    public void bulkAction(@RequestBody Map<String, Object> payload) {
        List<Integer> rawIds = (List<Integer>) payload.get("ids");
        String action = (String) payload.get("action");
        if (rawIds == null || rawIds.isEmpty()) throw new IllegalArgumentException("No ids provided");
        
        List<Long> ids = rawIds.stream().map(Integer::longValue).collect(Collectors.toList());
        List<News> newsList = repository.findAllById(ids);

        for (News n : newsList) {
            switch (action) {
                case "publish": n.setStatus("published"); n.setPublishedDate(LocalDateTime.now()); n.setScheduledDate(null); break;
                case "draft": n.setStatus("draft"); break;
                case "trash": n.setStatus("trash"); break;
                case "restore": n.setStatus("draft"); break;
                case "delete": repository.delete(n); continue;
                default: throw new IllegalArgumentException("Invalid action: " + action);
            }
            repository.save(n);
        }
    }

    // ============================================================
    // IMAGE UPLOAD ENDPOINTS
    // ============================================================
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadContentImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must have a valid extension."));
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png", "webp").contains(extension)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only JPG, JPEG, PNG, and WEBP images are allowed."));
            }
            if (file.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB limit"));

            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = Paths.get(UPLOAD_DIR, newFilename);
            Files.write(filePath, file.getBytes());

            String imageUrl = "/uploads/content/" + newFilename;
            Map<String, Object> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("success", true);
            response.put("filename", newFilename);
            response.put("size", file.getSize());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @GetMapping("/uploads/content/{filename}")
    public ResponseEntity<byte[]> getContentImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            if (!Files.exists(filePath)) return ResponseEntity.notFound().build();
            byte[] imageBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "image/jpeg";
            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "public, max-age=31536000")
                .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}