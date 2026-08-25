package com.example.newsjava.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.newsjava.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

}