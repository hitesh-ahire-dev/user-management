package com.example.usermanagement.dto;

import java.time.LocalDateTime;

/**
 * Outgoing representation of a user. The JPA entity is never serialized
 * directly; the mapper converts {@code User} -> {@code UserResponse}.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
