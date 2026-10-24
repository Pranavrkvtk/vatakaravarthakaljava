package com.example.newsjava.repository;

import com.example.newsjava.model.News;
import com.example.newsjava.model.NewsSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    // ===================== PUBLIC QUERIES (Paginated & Summary Only) =====================
    
    @Query("SELECT n FROM News n WHERE n.status = 'published' AND (n.scheduledDate IS NULL OR n.scheduledDate <= :now)")
    Page<NewsSummary> findPublishedNews(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'published' AND (n.scheduledDate IS NULL OR n.scheduledDate <= :now) ORDER BY n.publishedDate DESC")
    List<NewsSummary> findTop10LatestNews(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'published' AND n.breakingNews = true AND (n.scheduledDate IS NULL OR n.scheduledDate <= :now)")
    List<NewsSummary> findBreakingNews(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM News n WHERE n.status = 'published' AND (n.scheduledDate IS NULL OR n.scheduledDate <= :now) ORDER BY n.views DESC")
    List<NewsSummary> findTop10TrendingNews(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'published' AND n.category = :category AND (n.scheduledDate IS NULL OR n.scheduledDate <= :now)")
    Page<NewsSummary> findNewsByCategory(@Param("category") String category, @Param("now") LocalDateTime now, Pageable pageable);

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

    // ===================== FIX B19: ADVANCED DATABASE SEARCH =====================
    
    @Query("SELECT n FROM News n WHERE n.status = 'published' " +
           "AND (:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR n.category = :category) " +
           "AND (:author IS NULL OR LOWER(n.author) LIKE LOWER(CONCAT('%', :author, '%'))) " +
           "AND (:fromDate IS NULL OR n.publishedDate >= :fromDate) " +
           "AND (:toDate IS NULL OR n.publishedDate <= :toDate)")
    Page<NewsSummary> advancedSearch(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("author") String author,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    // ===================== NEW: SCHEDULER QUERY =====================
    
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.status = 'published', n.published = true, " +
           "n.publishedDate = n.scheduledDate, n.scheduledDate = null " +
           "WHERE n.status = 'scheduled' AND n.scheduledDate <= :now")
    int publishDueNews(@Param("now") LocalDateTime now);
    
}