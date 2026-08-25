package com.example.newsjava.controller;

import com.example.newsjava.model.Media;
import com.example.newsjava.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:3000")
public class MediaController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private final MediaRepository mediaRepository;

    public MediaController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    // ---------- Video URL ----------
    @GetMapping("/video")
    public Map<String, String> getVideo() {
        // Assuming you store the video URL in a config or the first "video" type media.
        // For simplicity, we'll store it as a property in the database.
        // You can implement a single-row config table, but we'll use a simple approach:
        // if there's a media with type "video" and filename "home", use its url.
        List<Media> videos = mediaRepository.findByType("video");
        if (!videos.isEmpty()) {
            return Map.of("videoUrl", videos.get(0).getUrl());
        }
        return Map.of("videoUrl", "");
    }

    @PutMapping("/video")
    public Map<String, String> updateVideo(@RequestBody Map<String, String> payload) {
        String url = payload.get("videoUrl");
        // Remove old video entries (if any)
        mediaRepository.deleteAll(mediaRepository.findByType("video"));
        Media video = new Media();
        video.setType("video");
        video.setUrl(url);
        video.setFilename("home-video");
        mediaRepository.save(video);
        return Map.of("videoUrl", url);
    }

    // ---------- Banners ----------
    @GetMapping("/banners")
    public List<Media> getBanners() {
        return mediaRepository.findByType("banner");
    }

    @PostMapping("/banners")
    public Media uploadBanner(@RequestParam("file") MultipartFile file) throws IOException {
        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(newFilename);
        Files.write(filePath, file.getBytes());

        // Save to database
        Media banner = new Media();
        banner.setFilename(originalFilename);
        banner.setUrl("/uploads/" + newFilename); // Static resource path
        banner.setType("banner");
        banner.setSize(file.getSize());
        return mediaRepository.save(banner);
    }

    @DeleteMapping("/banners/{id}")
    public void deleteBanner(@PathVariable Long id) throws IOException {
        Media banner = mediaRepository.findById(id).orElseThrow();
        // Delete file from disk
        Path filePath = Paths.get(uploadDir).resolve(banner.getUrl().substring(banner.getUrl().lastIndexOf("/") + 1));
        Files.deleteIfExists(filePath);
        mediaRepository.deleteById(id);
    }
}