package com.example.newsjava.repository;

import com.example.newsjava.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used by CustomUserDetailsService to load user by username
    Optional<User> findByUsername(String username);
    

    
    // Optional: delete by username if you ever need it (not used by AdminInitializer)
    @Modifying
    @Query("DELETE FROM User u WHERE u.username = :username")
    void deleteByUsername(String username);
}