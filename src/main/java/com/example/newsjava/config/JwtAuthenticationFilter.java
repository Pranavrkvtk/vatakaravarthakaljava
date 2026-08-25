// src/main/java/com/example/newsjava/config/JwtAuthenticationFilter.java
package com.example.newsjava.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.newsjava.service.CustomUserDetailsService;
import com.example.newsjava.util.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // ✅ Public paths that don't need authentication
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
        // Auth
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        
        // Public News GET endpoints
        "/api/news/published",
        "/api/news/latest",
        "/api/news/breaking",
        "/api/news/trending",
        "/api/news/category",
        
        // ✅ CRITICAL: Search endpoints - MUST be public
        "/api/news/search",
        "/api/news/search/suggestions",
        "/api/news/search/advanced",
        
        // Categories
        "/api/categories",
        
        // Static resources
        "/error",
        "/uploads",
        "/uploads/content"
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ✅ Skip authentication for public paths
        if (isPublicPath(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ For protected paths, validate JWT
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
                return;
            }

            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            chain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            System.out.println("JWT Token expired: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expired\", \"message\": \"Please login again\"}");
            
        } catch (SignatureException | MalformedJwtException e) {
            System.out.println("Invalid JWT Token: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid token\", \"message\": \"Please provide a valid token\"}");
            
        } catch (Exception e) {
            System.out.println("Authentication error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authentication failed\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ✅ Helper method to check if path is public
    private boolean isPublicPath(String path, String method) {
        // Check exact matches and path prefixes
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }

        // GET request to /api/news/{id} - public
        if (method.equals("GET") && path.matches("/api/news/\\d+$")) {
            return true;
        }

        // POST to /api/news/{id}/like - public
        if (method.equals("POST") && path.matches("/api/news/\\d+/like$")) {
            return true;
        }

        // GET to /api/ads - public
        if (method.equals("GET") && path.startsWith("/api/ads")) {
            // Admin endpoints are not public
            if (path.contains("/stats") || path.contains("/bulk")) {
                return false;
            }
            return true;
        }

        // POST to /api/ads/{id}/view or /api/ads/{id}/click - public
        if (method.equals("POST") && path.matches("/api/ads/\\d+/(view|click)$")) {
            return true;
        }

        return false;
    }
}