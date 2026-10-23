package com.example.newsjava.controller;

import com.example.newsjava.model.News;
import com.example.newsjava.repository.NewsRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
public class ShareController {

    private final NewsRepository newsRepository;
    private String indexHtml = "<html><head></head><body>React App Not Built Yet</body></html>";

    public ShareController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
        try {
            // Try to read the React index.html
            ClassPathResource resource = new ClassPathResource("static/index.html");
            this.indexHtml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            System.out.println("✅ Successfully loaded index.html for Open Graph tags.");
        } catch (IOException e) {
            // If it fails (like right now), just log a warning instead of crashing
            System.err.println("⚠️ WARNING: static/index.html not found. Open Graph tags will not work until you build the React app and place it in src/main/resources/static/.");
        }
    }

    @GetMapping(value = "/news/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareNews(@PathVariable Long id) {
        Optional<News> newsOpt = newsRepository.findById(id);
        
        if (newsOpt.isEmpty()) {
            return ResponseEntity.ok(indexHtml); // Fallback to normal index
        }

        News news = newsOpt.get();
        String title = HtmlUtils.htmlEscape(news.getTitle());
        String description = HtmlUtils.htmlEscape(news.getSubtitle() != null ? news.getSubtitle() : "Read the latest news on Vadakara Varthakal");
        
        // Ensure image URL is safe and absolute
        String image = news.getImage() != null ? HtmlUtils.htmlEscape(news.getImage()) : "";

        // Inject Open Graph tags into the <head>
        String ogTags = String.format(
            "<meta property=\"og:title\" content=\"%s\" />\n" +
            "<meta property=\"og:description\" content=\"%s\" />\n" +
            "<meta property=\"og:image\" content=\"%s\" />\n" +
            "<meta property=\"og:url\" content=\"https://vadakaravarthakal.in/news/%d\" />\n",
            title, description, image, id
        );

        // Insert tags right before </head>
        String modifiedHtml = indexHtml.replace("</head>", ogTags + "</head>");

        return ResponseEntity.ok(modifiedHtml);
    }
}