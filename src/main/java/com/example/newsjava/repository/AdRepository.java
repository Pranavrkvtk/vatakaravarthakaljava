// src/main/java/com/example/newsjava/repository/AdRepository.java
package com.example.newsjava.repository;

import com.example.newsjava.model.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long> {

    // Find active ads by position
    List<Ad> findByPositionAndActiveTrueOrderByPriorityDesc(String position);

    // Find all active ads
    List<Ad> findByActiveTrueOrderByPriorityDesc();

    // Find active ads that are currently valid (within date range)
    @Query("SELECT a FROM Ad a WHERE a.active = true AND " +
           "(a.startDate IS NULL OR a.startDate <= :now) AND " +
           "(a.endDate IS NULL OR a.endDate >= :now) " +
           "ORDER BY a.priority DESC")
    List<Ad> findCurrentActiveAds(@Param("now") LocalDateTime now);

    // Find active ads by position with date validation
    @Query("SELECT a FROM Ad a WHERE a.position = :position AND a.active = true AND " +
           "(a.startDate IS NULL OR a.startDate <= :now) AND " +
           "(a.endDate IS NULL OR a.endDate >= :now) " +
           "ORDER BY a.priority DESC")
    List<Ad> findActiveAdsByPosition(@Param("position") String position, @Param("now") LocalDateTime now);

    // Find premium ads
    List<Ad> findByIsPremiumTrueAndActiveTrueOrderByPriorityDesc();

    // Increment view count
    @Modifying
    @Transactional
    @Query("UPDATE Ad a SET a.views = a.views + 1 WHERE a.id = :id")
    void incrementViews(@Param("id") Long id);

    // Increment click count
    @Modifying
    @Transactional
    @Query("UPDATE Ad a SET a.clicks = a.clicks + 1 WHERE a.id = :id")
    void incrementClicks(@Param("id") Long id);

    // Find expired ads
    @Query("SELECT a FROM Ad a WHERE a.endDate IS NOT NULL AND a.endDate < :now AND a.active = true")
    List<Ad> findExpiredAds(@Param("now") LocalDateTime now);
}