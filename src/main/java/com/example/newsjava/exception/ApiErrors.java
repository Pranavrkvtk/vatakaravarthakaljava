package com.example.newsjava.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> notFound() { 
        return ResponseEntity.status(404).body(Map.of("error", "Not found")); 
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) { 
        return ResponseEntity.badRequest().body(Map.of("error", e.getBindingResult().getFieldErrors().toString())); 
    }
    
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<?> auth() { 
        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password")); 
    }
    
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> other(Exception e) { 
        // Log the error to your console (so you can see it)
        System.err.println("Unhandled Exception: " + e.getMessage());
        // Return a clean error to the frontend
        return ResponseEntity.status(500).body(Map.of("error", "Server error. Please try again.")); 
    }
}