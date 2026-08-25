// src/main/java/com/example/newsjava/controller/AdController.java
package com.example.newsjava.controller;

import com.example.newsjava.model.Ad;
import com.example.newsjava.repository.AdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
@CrossOrigin(origins = "http://localhost:3000")
public class AdController {

    @Autowired
    private AdRepository adRepository;

    // ─── Get all ads ──────────────────────────────────────────────────
    @GetMapping
    public List<Ad> getAllAds() {
        return adRepository.findAll();
    }

    // ─── Get active ads ──────────────────────────────────────────────
    @GetMapping("/active")
    public List<Ad> getActiveAds() {
        return adRepository.findCurrentActiveAds(LocalDateTime.now());
    }

    // ─── Get ads by position ─────────────────────────────────────────
    @GetMapping("/position/{position}")
    public List<Ad> getAdsByPosition(@PathVariable String position) {
        return adRepository.findActiveAdsByPosition(position, LocalDateTime.now());
    }

    // ─── Get single ad ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Ad> getAdById(@PathVariable Long id) {
        return adRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Create ad ────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Ad> createAd(@RequestBody Ad ad) {
        try {
            if (ad.getStartDate() == null) {
                ad.setStartDate(LocalDateTime.now());
            }
            Ad savedAd = adRepository.save(ad);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAd);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Update ad ────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<Ad> updateAd(@PathVariable Long id, @RequestBody Ad ad) {
        return adRepository.findById(id)
                .map(existingAd -> {
                    // Update fields
                    if (ad.getTitle() != null) existingAd.setTitle(ad.getTitle());
                    if (ad.getDescription() != null) existingAd.setDescription(ad.getDescription());
                    if (ad.getImageUrl() != null) existingAd.setImageUrl(ad.getImageUrl());
                    if (ad.getLinkUrl() != null) existingAd.setLinkUrl(ad.getLinkUrl());
                    if (ad.getPosition() != null) existingAd.setPosition(ad.getPosition());
                    if (ad.getSize() != null) existingAd.setSize(ad.getSize());
                    if (ad.getStartDate() != null) existingAd.setStartDate(ad.getStartDate());
                    if (ad.getEndDate() != null) existingAd.setEndDate(ad.getEndDate());
                    if (ad.getTargetUrl() != null) existingAd.setTargetUrl(ad.getTargetUrl());
                    if (ad.getAltText() != null) existingAd.setAltText(ad.getAltText());
                    if (ad.getBackgroundColor() != null) existingAd.setBackgroundColor(ad.getBackgroundColor());
                    if (ad.getBorderColor() != null) existingAd.setBorderColor(ad.getBorderColor());
                    
                    existingAd.setActive(ad.isActive());
                    existingAd.setClickable(ad.isClickable());
                    existingAd.setPriority(ad.getPriority());
                    existingAd.setPremium(ad.isPremium());
                    
                    return ResponseEntity.ok(adRepository.save(existingAd));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Delete ad ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id) {
        if (adRepository.existsById(id)) {
            adRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ─── Toggle ad status ────────────────────────────────────────────
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Ad> toggleAdStatus(@PathVariable Long id) {
        return adRepository.findById(id)
                .map(ad -> {
                    ad.setActive(!ad.isActive());
                    return ResponseEntity.ok(adRepository.save(ad));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Record ad view ──────────────────────────────────────────────
    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, String>> recordView(@PathVariable Long id) {
        if (adRepository.existsById(id)) {
            adRepository.incrementViews(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "View recorded successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    // ─── Record ad click ─────────────────────────────────────────────
    @PostMapping("/{id}/click")
    public ResponseEntity<Map<String, String>> recordClick(@PathVariable Long id) {
        if (adRepository.existsById(id)) {
            adRepository.incrementClicks(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Click recorded successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    // ─── Get ad statistics ───────────────────────────────────────────
    @GetMapping("/stats")
    public Map<String, Object> getAdStats() {
        List<Ad> allAds = adRepository.findAll();
        List<Ad> activeAds = adRepository.findByActiveTrueOrderByPriorityDesc();
        List<Ad> expiredAds = adRepository.findExpiredAds(LocalDateTime.now());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAds", allAds.size());
        stats.put("activeAds", activeAds.size());
        stats.put("expiredAds", expiredAds.size());

        int totalClicks = allAds.stream().mapToInt(Ad::getClicks).sum();
        int totalViews = allAds.stream().mapToInt(Ad::getViews).sum();

        stats.put("totalClicks", totalClicks);
        stats.put("totalViews", totalViews);

        if (totalViews > 0) {
            stats.put("clickThroughRate", String.format("%.2f%%", (double) totalClicks / totalViews * 100));
        } else {
            stats.put("clickThroughRate", "0%");
        }

        return stats;
    }

    // ─── Bulk delete ──────────────────────────────────────────────────
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDelete(@RequestBody List<Long> ids) {
        adRepository.deleteAllById(ids);
        return ResponseEntity.ok().build();
    }
}