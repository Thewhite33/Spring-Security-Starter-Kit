package com.auth.SecurityStarter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data 
@Configuration 
@ConfigurationProperties (prefix = "app.jwt")
public class AppProperties {
    private String secret;
    private long expirationMs;        // e.g., 15 minutes or 24 hours
    private long refreshExpirationMs; // e.g., 7 days
}
