// src/main/java/com/example/newsjava/controller/NewsController.java
package com.example.newsjava.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.example.newsjava.model.News;
import com.example.newsjava.repository.NewsRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "http://localhost:3000")
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
    // PUBLIC USER ENDPOINTS (Only show published & scheduled-past)
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
        List<News> news = repository.findTop10ByOrderByViewsDesc();
        return filterPublicVisible(news);
    }

    @GetMapping("/category/{category}")
    public List<News> getNewsByCategory(@PathVariable String category) {
        List<News> news = repository.findByCategoryAndStatus(category, "published");
        return filterPublicVisible(news);
    }

    // ============================================================
    // GET NEWS BY ID - Only return if published & visible
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error", "This content is not available",
                        "status", news.getStatus(),
                        "scheduledDate", news.getScheduledDate()
                    ));
        }
        
        // Increment views
        news.setViews(news.getViews() + 1);
        repository.save(news);
        
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
        
        news.setLikes(news.getLikes() + 1);
        return ResponseEntity.ok(repository.save(news));
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
        
        System.out.println("🔍 Searching news with keyword: " + keyword);
        
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
            default: // relevance
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
        
        System.out.println("✅ Found " + total + " visible results for keyword: " + keyword);
        
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
                .collect(Collectors.toSet());
        
        return ResponseEntity.ok(new ArrayList<>(suggestions));
    }

    // ============================================================
    // ADVANCED SEARCH - FIXED: No NullPointerException
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
        
        System.out.println("🔍 Advanced search - Keyword: " + keyword + ", Category: " + category);
        
        // Get all published news first
        List<News> allNews = repository.findByStatusOrderByPublishedDateDesc("published");
        List<News> visibleNews = filterPublicVisible(allNews);
        
        // Apply filters
        List<News> filteredNews = visibleNews.stream()
                .filter(news -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String searchTerm = keyword.toLowerCase();
                        boolean titleMatch = news.getTitle() != null && 
                            news.getTitle().toLowerCase().contains(searchTerm);
                        boolean contentMatch = news.getContent() != null && 
                            news.getContent().toLowerCase().contains(searchTerm);
                        if (!titleMatch && !contentMatch) return false;
                    }
                    
                    if (category != null && !category.trim().isEmpty()) {
                        if (news.getCategory() == null || 
                            !news.getCategory().equalsIgnoreCase(category)) {
                            return false;
                        }
                    }
                    
                    if (author != null && !author.trim().isEmpty()) {
                        if (news.getAuthor() == null || 
                            !news.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    LocalDateTime newsDate = news.getPublishedDate() != null ? 
                        news.getPublishedDate() : news.getCreatedAt();
                        
                    if (fromDate != null && !fromDate.trim().isEmpty()) {
                        try {
                            LocalDateTime fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00");
                            if (newsDate.isBefore(fromDateTime)) return false;
                        } catch (Exception e) {
                            // Skip invalid date
                        }
                    }
                    
                    if (toDate != null && !toDate.trim().isEmpty()) {
                        try {
                            LocalDateTime toDateTime = LocalDateTime.parse(toDate + "T23:59:59");
                            if (newsDate.isAfter(toDateTime)) return false;
                        } catch (Exception e) {
                            // Skip invalid date
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Sort
        switch (sortBy) {
            case "latest":
                filteredNews.sort((a, b) -> {
                    LocalDateTime dateA = a.getPublishedDate() != null ? a.getPublishedDate() : a.getCreatedAt();
                    LocalDateTime dateB = b.getPublishedDate() != null ? b.getPublishedDate() : b.getCreatedAt();
                    return dateB.compareTo(dateA);
                });
                break;
            case "oldest":
                filteredNews.sort((a, b) -> {
                    LocalDateTime dateA = a.getPublishedDate() != null ? a.getPublishedDate() : a.getCreatedAt();
                    LocalDateTime dateB = b.getPublishedDate() != null ? b.getPublishedDate() : b.getCreatedAt();
                    return dateA.compareTo(dateB);
                });
                break;
            case "views":
                filteredNews.sort((a, b) -> Integer.compare(b.getViews(), a.getViews()));
                break;
            case "likes":
                filteredNews.sort((a, b) -> Integer.compare(b.getLikes(), a.getLikes()));
                break;
            default:
                break;
        }
        
        // Pagination
        int total = filteredNews.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<News> paginatedNews = start < total ? filteredNews.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("news", paginatedNews);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        
        // ✅ FIXED: Use HashMap instead of Map.of() to handle null values
        Map<String, Object> filters = new HashMap<>();
        filters.put("keyword", keyword != null ? keyword : "");
        filters.put("category", category != null ? category : "");
        filters.put("author", author != null ? author : "");
        filters.put("fromDate", fromDate != null ? fromDate : "");
        filters.put("toDate", toDate != null ? toDate : "");
        filters.put("sortBy", sortBy != null ? sortBy : "latest");
        
        response.put("filters", filters);
        
        System.out.println("✅ Advanced search found " + total + " results");
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ADMIN ENDPOINTS (All news, no filtering)
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
    // ADMIN: Auto-publish scheduled posts endpoint
    // ============================================================
    @PostMapping("/check-scheduled")
    public ResponseEntity<Map<String, Object>> checkScheduledPosts() {
        System.out.println("🔄 Checking scheduled posts...");
        
        List<News> scheduledNews = repository.findByStatus("scheduled");
        LocalDateTime now = LocalDateTime.now();
        int publishedCount = 0;
        List<String> publishedTitles = new ArrayList<>();
        
        for (News news : scheduledNews) {
            if (news.getScheduledDate() != null && 
                (news.getScheduledDate().isBefore(now) || news.getScheduledDate().isEqual(now))) {
                news.setStatus("published");
                news.setPublishedDate(now);
                news.setScheduledDate(null);
                repository.save(news);
                publishedCount++;
                publishedTitles.add(news.getTitle());
                System.out.println("✅ Auto-published: " + news.getTitle());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("published", publishedCount);
        response.put("titles", publishedTitles);
        response.put("checkedAt", now);
        
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ADMIN: CRUD Operations
    // ============================================================

    @PostMapping
    public News createNews(@RequestBody News news) {
        System.out.println("=== CREATE NEWS ===");
        System.out.println("Title: " + news.getTitle());

        if (news.getStatus() == null || news.getStatus().isEmpty()) {
            news.setStatus("draft");
        }

        news.setViews(0);
        news.setLikes(0);
        news.setShares(0);

        News saved = repository.save(news);
        System.out.println("✅ Saved news ID: " + saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public News updateNews(@PathVariable Long id, @RequestBody News news) {
        News existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        if (news.getTitle() != null) existing.setTitle(news.getTitle());
        if (news.getSubtitle() != null) existing.setSubtitle(news.getSubtitle());
        if (news.getContent() != null) existing.setContent(news.getContent());
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

        // Auto-set published date when status changes to published
        if ("published".equals(news.getStatus()) && existing.getPublishedDate() == null) {
            existing.setPublishedDate(LocalDateTime.now());
            existing.setScheduledDate(null); // Clear scheduled date on publish
        }

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deleteNews(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/bulk")
    public void bulkAction(@RequestBody Map<String, Object> payload) {
        List<Integer> rawIds = (List<Integer>) payload.get("ids");
        String action = (String) payload.get("action");

        if (rawIds == null || rawIds.isEmpty()) {
            throw new RuntimeException("No ids provided");
        }

        List<Long> ids = rawIds.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());

        List<News> newsList = repository.findAllById(ids);

        for (News n : newsList) {
            switch (action) {
                case "publish":
                    n.setStatus("published");
                    n.setPublishedDate(LocalDateTime.now());
                    n.setScheduledDate(null);
                    repository.save(n);
                    break;
                case "draft":
                    n.setStatus("draft");
                    repository.save(n);
                    break;
                case "trash":
                    n.setStatus("trash");
                    repository.save(n);
                    break;
                case "restore":
                    n.setStatus("draft");
                    repository.save(n);
                    break;
                case "delete":
                    repository.delete(n);
                    break;
                default:
                    throw new RuntimeException("Invalid action: " + action);
            }
        }
    }

    // ============================================================
    // IMAGE UPLOAD ENDPOINTS
    // ============================================================

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadContentImage(@RequestParam("file") MultipartFile file) {
        System.out.println("📸 Uploading image...");
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
            }

            long fileSize = file.getSize();
            if (fileSize > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB limit"));
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            Path filePath = Paths.get(UPLOAD_DIR, newFilename);
            Files.write(filePath, file.getBytes());

            String imageUrl = "/uploads/content/" + newFilename;
            Map<String, Object> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("success", true);
            response.put("filename", newFilename);
            response.put("size", fileSize);

            System.out.println("✅ Image uploaded successfully: " + imageUrl);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            System.err.println("❌ IO Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @GetMapping("/uploads/content/{filename}")
    public ResponseEntity<byte[]> getContentImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "public, max-age=31536000")
                .body(imageBytes);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/debug/images")
    public ResponseEntity<Map<String, Object>> listUploadedImages() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            List<Map<String, Object>> files = new ArrayList<>();
            
            if (Files.exists(uploadPath)) {
                Files.list(uploadPath).forEach(path -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", path.getFileName().toString());
                    try {
                        fileInfo.put("size", Files.size(path));
                        fileInfo.put("lastModified", Files.getLastModifiedTime(path).toString());
                    } catch (IOException e) {
                        fileInfo.put("error", e.getMessage());
                    }
                    files.add(fileInfo);
                });
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", files.size());
            response.put("files", files);
            response.put("directory", uploadPath.toAbsolutePath().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug/{id}")
    public Map<String, Object> debugNews(@PathVariable Long id) {
        News news = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("id", news.getId());
        debug.put("title", news.getTitle());
        debug.put("status", news.getStatus());
        debug.put("scheduledDate", news.getScheduledDate());
        debug.put("publishedDate", news.getPublishedDate());
        debug.put("imageExists", news.getImage() != null);
        debug.put("isPubliclyVisible", isPubliclyVisible(news));
        
        if (news.getImage() != null) {
            debug.put("imageLength", news.getImage().length());
            debug.put("imageType", news.getImage().startsWith("data:image") ? "base64" : "url");
            debug.put("imagePreview", news.getImage().substring(0, Math.min(200, news.getImage().length())));
        }
        
        return debug;
    }
}