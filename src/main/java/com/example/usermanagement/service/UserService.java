package com.example.usermanagement.service;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for managing users.
 * The controller depends on this abstraction, not the implementation.
 */
public interface UserService {

    /** Creates a user, rejecting duplicate emails. */
    UserResponse createUser(UserRequest request);

    /** Returns a page of users according to the given pagination/sort. */
    Page<UserResponse> getUsers(Pageable pageable);

    /** Returns a single user or throws if not found. */
    UserResponse getUserById(Long id);

    /** Updates an existing user, allowing it to keep its own email. */
    UserResponse updateUser(Long id, UserRequest request);

    /** Deletes a user or throws if not found. */
    void deleteUser(Long id);
}
