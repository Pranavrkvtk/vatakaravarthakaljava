package com.example.newsjava.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";   // ← change this to your desired password
        String encoded = encoder.encode(rawPassword);
        System.out.println("Encoded password: " + encoded);
    }
}