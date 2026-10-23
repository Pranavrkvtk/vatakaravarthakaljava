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

import io.jsonwebtoken.JwtException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // ✅ FIX 9: Removed PUBLIC_PATHS and isPublicPath(). 
    // The filter now ONLY reads the token. SecurityConfig decides access.
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");

        if (h != null && h.startsWith("Bearer ")) {
            try {
                String jwt = h.substring(7);
                String username = jwtUtil.extractUsername(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validateToken(jwt, user)) {
                        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | UsernameNotFoundException e) {
                // Token is invalid or expired. 
                // We clear the context and let Spring Security treat it as anonymous.
                SecurityContextHolder.clearContext(); 
            }
        }
        
        // Always continue the chain. 
        // If it's a public route, SecurityConfig allows it.
        // If it's an admin route and the user is anonymous, SecurityConfig returns 401/403.
        chain.doFilter(req, res);
    }
}