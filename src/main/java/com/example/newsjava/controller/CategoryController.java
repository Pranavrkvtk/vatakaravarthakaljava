package com.example.newsjava.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.newsjava.model.Category;
import com.example.newsjava.model.News;
import com.example.newsjava.repository.CategoryRepository;
import com.example.newsjava.repository.NewsRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final NewsRepository newsRepository;

    public CategoryController(CategoryRepository categoryRepository,
                              NewsRepository newsRepository) {
        this.categoryRepository = categoryRepository;
        this.newsRepository = newsRepository;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return categoryRepository.save(category);
    }

    // Rename category by name – updates all news with that category
    @PutMapping("/{name}")
    public ResponseEntity<?> renameCategory(@PathVariable String name,
                                            @RequestBody Map<String, String> payload) {
        String newName = payload.get("name");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("New name is required");
        }

        // Update all news with old category name
        List<News> newsList = newsRepository.findByCategory(name);
        for (News n : newsList) {
            n.setCategory(newName);
        }
        newsRepository.saveAll(newsList);

        // Update the category entry itself (if it exists)
        Optional<Category> optionalCategory = categoryRepository.findByName(name);
        optionalCategory.ifPresent(cat -> {
            cat.setName(newName);
            categoryRepository.save(cat);
        });

        return ResponseEntity.ok().build();
    }

    // Delete category by name – sets news category to null (or "Uncategorized")
    @DeleteMapping("/{name}")
    public ResponseEntity<?> deleteCategory(@PathVariable String name) {
        List<News> newsList = newsRepository.findByCategory(name);
        for (News n : newsList) {
            n.setCategory(null); // or "Uncategorized"
        }
        newsRepository.saveAll(newsList);

        Optional<Category> optionalCategory = categoryRepository.findByName(name);
        optionalCategory.ifPresent(categoryRepository::delete);

        return ResponseEntity.ok().build();
    }
}