package com.example.newsjava.controller;

import com.example.newsjava.repository.NewsRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    private final NewsRepository newsRepository;

    public DashboardController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /**
     * Returns the KPI array matching the React DashboardTab structure.
     * Uses the existing NewsRepository to fetch real counts.
     */
    @GetMapping("/stats")
    public List<KPI> getDashboardStats() {
        long total = newsRepository.count();
        long published = newsRepository.countByStatus("published");
        long scheduled = newsRepository.countByStatus("scheduled");
        long drafts = newsRepository.countByStatus("draft") +
                      newsRepository.countByStatus("pending");
        long trash = newsRepository.countByStatus("trash");

        return Arrays.asList(
            new KPI("Total Posts", (int) total, "📄", "#eef2ff", "#4f46e5"),
            new KPI("Published", (int) published, "✅", "#dcfce7", "#16a34a"),
            new KPI("Scheduled", (int) scheduled, "🗓️", "#dbeafe", "#2563eb"),
            new KPI("Drafts", (int) drafts, "✏️", "#fef3c7", "#d97706"),
            new KPI("Trash", (int) trash, "🗑️", "#fee2e2", "#dc2626")
        );
    }

    // ----- Inner KPI DTO -----
    public static class KPI {
        private final String label;
        private final int value;
        private final String icon;
        private final String iconBg;
        private final String color;

        public KPI(String label, int value, String icon, String iconBg, String color) {
            this.label = label;
            this.value = value;
            this.icon = icon;
            this.iconBg = iconBg;
            this.color = color;
        }

        // Getters (required for JSON serialization)
        public String getLabel() { return label; }
        public int getValue() { return value; }
        public String getIcon() { return icon; }
        public String getIconBg() { return iconBg; }
        public String getColor() { return color; }
    }
}