package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import org.springframework.stereotype.Component;

/**
 * Converts between the {@link User} entity and its DTOs.
 *
 * <p>Keeping mapping in one place avoids duplicated conversion logic and keeps
 * the service focused on business rules. Implemented as a Spring bean so it can
 * be injected via the constructor.
 */
@Component
public class UserMapper {

    /** Builds a new entity from a create request. Audit fields are set by JPA. */
    public User toEntity(UserRequest request) {
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .build();
    }

    /** Applies request values onto an existing managed entity (for updates). */
    public void updateEntity(User user, UserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
    }

    /** Converts an entity to the API response DTO. */
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
