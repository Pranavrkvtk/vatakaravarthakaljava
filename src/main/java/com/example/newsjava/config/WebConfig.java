// src/main/java/com/example/newsjava/config/WebConfig.java
package com.example.newsjava.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Serve uploaded images from the uploads directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);
        
        // ✅ Also serve specifically from content folder
        registry.addResourceHandler("/uploads/content/**")
                .addResourceLocations("file:./uploads/content/")
                .setCachePeriod(3600);
        
        System.out.println("✅ Resource handlers configured for /uploads/**");
        System.out.println("✅ Upload directory: " + System.getProperty("user.dir") + "/uploads/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}