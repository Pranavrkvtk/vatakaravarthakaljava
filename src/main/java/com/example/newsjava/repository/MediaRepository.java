package com.example.newsjava.repository;

import com.example.newsjava.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByType(String type);
}