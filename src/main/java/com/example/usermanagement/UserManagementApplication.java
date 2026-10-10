package com.example.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * <p>JPA auditing (automatic {@code createdAt}/{@code updatedAt}) is enabled in
 * {@code config.JpaAuditingConfig} so it stays out of web-only test slices.
 */
@SpringBootApplication
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
