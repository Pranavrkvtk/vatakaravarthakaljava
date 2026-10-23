// src/main/java/com/example/newsjava/config/SecurityConfig.java
package com.example.newsjava.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // ✅ CRITICAL: Required for @PreAuthorize in AuthController to work
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // ===== PUBLIC ENDPOINTS (no authentication needed) =====
                
                // Static resources & Errors
                .requestMatchers("/error").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/uploads/content/**").permitAll()

                // ✅ FIXED: Only allow LOGIN to be public. Do NOT use /api/auth/** anymore.
                .requestMatchers("/api/auth/login").permitAll()

                // ===== PUBLIC AD ENDPOINTS =====
                .requestMatchers(HttpMethod.GET, "/api/ads/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ads/position/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ads/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ads/{id}/view").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ads/{id}/click").permitAll()

                // ===== PUBLIC NEWS ENDPOINTS =====
                .requestMatchers(HttpMethod.GET, "/api/news/published").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/latest").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/breaking").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/trending").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/news/{id}/like").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/news/{id}/view").permitAll() // Added based on Fix 16

                // SEARCH ENDPOINTS
                .requestMatchers(HttpMethod.GET, "/api/news/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/search/suggestions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/search/advanced").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/search/suggestions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/search/advanced/**").permitAll()

                // ===== PUBLIC CATEGORY ENDPOINTS =====
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                // ===== ADMIN-ONLY ENDPOINTS =====
                
                // User Management (from your AuthController fix)
                .requestMatchers("/api/auth/users").hasRole("ADMIN")

                // News Admin
                .requestMatchers(HttpMethod.POST, "/api/news/upload-image").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/news/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/news/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/news/bulk").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/news").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/news/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/news/**").hasRole("ADMIN")

                // Ad Admin (Note: /api/ads/stats must be BEFORE /api/ads/{id})
                .requestMatchers(HttpMethod.GET, "/api/ads/stats").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/ads").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/ads").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/ads/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/ads/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/ads/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/ads/bulk").hasRole("ADMIN")

                // Category Admin
                .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                // Dashboard & Media
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/api/media/**").hasRole("ADMIN")

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ FIX 7: Read from environment variables for production
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // ✅ FIX 7: Bearer tokens don't need credentials, and it's a security risk
        configuration.setAllowCredentials(false); 
        
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}