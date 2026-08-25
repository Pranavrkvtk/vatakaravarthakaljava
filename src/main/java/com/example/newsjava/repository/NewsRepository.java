package com.example.newsjava.repository;

import com.example.newsjava.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    // ===================== PUBLIC QUERIES =====================
    
    // Get published news ordered by published date
    List<News> findByStatusOrderByPublishedDateDesc(String status);
    
    // Get top 10 published news ordered by published date
    List<News> findTop10ByStatusOrderByPublishedDateDesc(String status);
    
    // Get breaking news with published status
    List<News> findByBreakingNewsTrueAndStatus(String status);
    
    // Get news by category with published status
    List<News> findByCategoryAndStatus(String category, String status);
    
    // Search by title with published status
    List<News> findByTitleContainingIgnoreCaseAndStatus(String keyword, String status);
    
    // Get trending by views
    List<News> findTop10ByOrderByViewsDesc();

    // ===================== ADMIN QUERIES =====================
    
    // Get by status (for admin)
    List<News> findByStatus(String status);
    
    // Get by category (all statuses)
    List<News> findByCategory(String category);
    
    // Get latest by created date (all statuses)
    List<News> findTop10ByOrderByCreatedAtDesc();
    
    // Get breaking news (all statuses)
    List<News> findByBreakingNewsTrue();
    
    // Search by title (all statuses)
    List<News> findByTitleContainingIgnoreCase(String keyword);
    
    @Query("SELECT COUNT(n) FROM News n WHERE n.status = :status")
    long countByStatus(@Param("status") String status);
}