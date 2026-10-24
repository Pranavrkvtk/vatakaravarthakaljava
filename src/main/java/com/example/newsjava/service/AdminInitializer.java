package com.example.newsjava.service;

import com.example.newsjava.model.User;
import com.example.newsjava.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-password:}")
    private String initialPassword;

    @Override
    public void run(String... args) throws Exception {
        // If any user exists, do nothing.
        if (userRepository.count() > 0) {
            return;
        }

        // Check if the password meets the 12-character security requirement
        if (initialPassword == null || initialPassword.length() < 12) {
            throw new IllegalArgumentException(
                "CRITICAL: You must set ADMIN_INITIAL_PASSWORD (12+ characters) for the first start."
            );
        }

        // Create the secure admin user
        User admin = new User();
        admin.setUsername("admin"); // You can change "admin" to a custom username here
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setRole("ADMIN");
        userRepository.save(admin);
        
        System.out.println("✅ Secure admin user created from environment variable.");
    }
}