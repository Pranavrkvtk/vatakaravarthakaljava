package com.example.newsjava.repository;

import com.example.newsjava.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    // ===================== PUBLIC QUERIES =====================
    
    List<News> findByStatusOrderByPublishedDateDesc(String status);
    List<News> findTop10ByStatusOrderByPublishedDateDesc(String status);
    List<News> findByBreakingNewsTrueAndStatus(String status);
    List<News> findByCategoryAndStatus(String category, String status);
    List<News> findByTitleContainingIgnoreCaseAndStatus(String keyword, String status);
    
    // ✅ FIX B9: Filter by status in the query, not in memory
    List<News> findTop10ByStatusOrderByViewsDesc(String status);

    // ===================== ADMIN QUERIES =====================
    
    List<News> findByStatus(String status);
    List<News> findByCategory(String category);
    List<News> findTop10ByOrderByCreatedAtDesc();
    List<News> findByBreakingNewsTrue();
    List<News> findByTitleContainingIgnoreCase(String keyword);
    
    @Query("SELECT COUNT(n) FROM News n WHERE n.status = :status")
    long countByStatus(@Param("status") String status);

    // ===================== FIX 13: ATOMIC COUNTERS =====================

    @Modifying
    @Transactional
    @Query("update News n set n.views = n.views + 1 where n.id = :id and n.status = 'published'")
    int incrementViews(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update News n set n.likes = n.likes + 1 where n.id = :id and n.status = 'published'")
    int incrementLikes(@Param("id") Long id);
}