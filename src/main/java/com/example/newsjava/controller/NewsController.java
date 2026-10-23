// src/main/java/com/example/newsjava/controller/NewsController.java
package com.example.newsjava.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.example.newsjava.model.News;
import com.example.newsjava.repository.NewsRepository;

// ✅ FIX 11: Jsoup Imports for HTML Sanitization
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

// ✅ FIX B19: Pagination Imports
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
    // HELPER: Check if news is published or scheduled-past
    // ============================================================
    private boolean isPubliclyVisible(News news) {
        if (news == null) return false;
        
        // Only "published" status is visible
        if (!"published".equals(news.getStatus())) {
            return false;
        }
        
        // If scheduled date exists, it must be in the past
        if (news.getScheduledDate() != null) {
            return news.getScheduledDate().isBefore(LocalDateTime.now()) || 
                   news.getScheduledDate().isEqual(LocalDateTime.now());
        }
        
        return true;
    }

    private List<News> filterPublicVisible(List<News> newsList) {
        if (newsList == null) return new ArrayList<>();
        return newsList.stream()
                .filter(this::isPubliclyVisible)
                .collect(Collectors.toList());
    }

    // ============================================================
    // PUBLIC USER ENDPOINTS
    // ============================================================

    @GetMapping("/published")
    public List<News> getPublishedNews() {
        List<News> news = repository.findByStatusOrderByPublishedDateDesc("published");
        return filterPublicVisible(news);
    }

    @GetMapping("/latest")
    public List<News> getLatestNews() {
        List<News> news = repository.findTop10ByStatusOrderByPublishedDateDesc("published");
        return filterPublicVisible(news);
    }

    @GetMapping("/breaking")
    public List<News> getBreakingNews() {
        List<News> news = repository.findByBreakingNewsTrueAndStatus("published");
        return filterPublicVisible(news);
    }

    @GetMapping("/trending")
    public List<News> getTrendingNews() {
        // ✅ FIX B9: Changed from OrderByCreatedAtDesc to OrderByViewsDesc
        List<News> news = repository.findTop10ByStatusOrderByViewsDesc("published");
        return filterPublicVisible(news);
    }

    @GetMapping("/category/{category}")
    public List<News> getNewsByCategory(@PathVariable String category) {
        List<News> news = repository.findByCategoryAndStatus(category, "published");
        return filterPublicVisible(news);
    }

    // ============================================================
    // GET NEWS BY ID - Fixed NPE and Atomic Views
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsById(@PathVariable Long id) {
        Optional<News> optionalNews = repository.findById(id);
        
        if (optionalNews.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "News not found"));
        }
        
        News news = optionalNews.get();
        
        // Check if news is publicly visible
        if (!isPubliclyVisible(news)) {
            // ✅ FIX B4: Use HashMap instead of Map.of to allow null values
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "This content is not available");
            errorResponse.put("status", news.getStatus());
            errorResponse.put("scheduledDate", news.getScheduledDate());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        // ✅ FIX 13: Atomic View Increment
        repository.incrementViews(id);
        
        // Update the local object so the UI sees the new count immediately
        news.setViews(news.getViews() + 1);
        
        return ResponseEntity.ok(news);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeNews(@PathVariable Long id) {
        Optional<News> optionalNews = repository.findById(id);
        
        if (optionalNews.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        News news = optionalNews.get();
        
        if (!isPubliclyVisible(news)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "This content is not available"));
        }
        
        // ✅ FIX 13: Atomic Like Increment
        repository.incrementLikes(id);
        news.setLikes(news.getLikes() + 1);
        
        return ResponseEntity.ok(news);
    }

    // ============================================================
    // SEARCH API
    // ============================================================
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "relevance") String sortBy) {
        
        List<News> allNews = repository.findByTitleContainingIgnoreCaseAndStatus(keyword, "published");
        List<News> visibleNews = filterPublicVisible(allNews);
        
        // Sort results
        switch (sortBy) {
            case "latest":
                visibleNews.sort((a, b) -> {
                    LocalDateTime dateA = a.getPublishedDate() != null ? a.getPublishedDate() : a.getCreatedAt();
                    LocalDateTime dateB = b.getPublishedDate() != null ? b.getPublishedDate() : b.getCreatedAt();
                    return dateB.compareTo(dateA);
                });
                break;
            case "oldest":
                visibleNews.sort((a, b) -> {
                    LocalDateTime dateA = a.getPublishedDate() != null ? a.getPublishedDate() : a.getCreatedAt();
                    LocalDateTime dateB = b.getPublishedDate() != null ? b.getPublishedDate() : b.getCreatedAt();
                    return dateA.compareTo(dateB);
                });
                break;
            case "views":
                visibleNews.sort((a, b) -> Integer.compare(b.getViews(), a.getViews()));
                break;
            case "likes":
                visibleNews.sort((a, b) -> Integer.compare(b.getLikes(), a.getLikes()));
                break;
        }
        
        // Pagination
        int total = visibleNews.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<News> paginatedNews = start < total ? visibleNews.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("news", paginatedNews);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("keyword", keyword != null ? keyword : "");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam String prefix) {
        List<News> news = repository.findByTitleContainingIgnoreCaseAndStatus(prefix, "published");
        List<News> visibleNews = filterPublicVisible(news);
        
        Set<String> suggestions = visibleNews.stream()
                .map(News::getTitle)
                .filter(title -> title != null)
                .limit(5)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        
        return ResponseEntity.ok(new ArrayList<>(suggestions));
    }

    // ============================================================
    // ADVANCED SEARCH - FIX B19 (Database Driven)
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

        // Parse dates safely
        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;
        try { if (fromDate != null && !fromDate.isEmpty()) fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00"); } catch (Exception e) {}
        try { if (toDate != null && !toDate.isEmpty()) toDateTime = LocalDateTime.parse(toDate + "T23:59:59"); } catch (Exception e) {}

        // Map sortBy to Spring Data Sort
        Sort sort = Sort.by("publishedDate").descending(); // default latest
        if ("oldest".equals(sortBy)) sort = Sort.by("publishedDate").ascending();
        else if ("views".equals(sortBy)) sort = Sort.by("views").descending();
        else if ("likes".equals(sortBy)) sort = Sort.by("likes").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Query database directly using the new repository method
        Page<News> results = repository.advancedSearch(
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
    public List<News> getAllNewsAdmin() {
        return repository.findAll();
    }

    @GetMapping("/status/{status}")
    public List<News> getNewsByStatus(@PathVariable String status) {
        return repository.findByStatus(status);
    }

    // ============================================================
    // ADMIN: CRUD Operations
    // ============================================================

    @PostMapping
    public News createNews(@RequestBody News news) {
        if (news.getStatus() == null || news.getStatus().isEmpty()) {
            news.setStatus("draft");
        }
        news.setViews(0);
        news.setLikes(0);
        news.setShares(0);

        // ✅ FIX 11: Sanitize HTML to prevent XSS
        if (news.getContent() != null) {
            String cleanContent = Jsoup.clean(news.getContent(), Safelist.relaxed().addAttributes(":all", "style", "class"));
            news.setContent(cleanContent);
        }

        return repository.save(news);
    }

    @PutMapping("/{id}")
    public News updateNews(@PathVariable Long id, @RequestBody News news) {
        News existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("News not found"));

        if (news.getTitle() != null) existing.setTitle(news.getTitle());
        if (news.getSubtitle() != null) existing.setSubtitle(news.getSubtitle());
        
        // ✅ FIX 11: Sanitize HTML before updating
        if (news.getContent() != null) {
            String cleanContent = Jsoup.clean(news.getContent(), Safelist.relaxed().addAttributes(":all", "style", "class"));
            existing.setContent(cleanContent);
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
    public void deleteNews(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/bulk")
    @Transactional // ✅ FIX B17: Ensures all-or-nothing database transactions
    public void bulkAction(@RequestBody Map<String, Object> payload) {
        List<Integer> rawIds = (List<Integer>) payload.get("ids");
        String action = (String) payload.get("action");

        if (rawIds == null || rawIds.isEmpty()) {
            throw new IllegalArgumentException("No ids provided");
        }

        List<Long> ids = rawIds.stream().map(Integer::longValue).collect(Collectors.toList());
        List<News> newsList = repository.findAllById(ids);

        for (News n : newsList) {
            switch (action) {
                case "publish":
                    n.setStatus("published");
                    n.setPublishedDate(LocalDateTime.now());
                    n.setScheduledDate(null);
                    break;
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
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // ✅ FIX S5 & B10: Secure File Uploads (Extension Allow-list)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must have a valid extension."));
            }
            
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png", "webp").contains(extension)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only JPG, JPEG, PNG, and WEBP images are allowed."));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB limit"));
            }

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
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