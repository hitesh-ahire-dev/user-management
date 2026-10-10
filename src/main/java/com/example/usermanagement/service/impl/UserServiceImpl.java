package com.example.usermanagement.service.impl;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.DuplicateResourceException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user management.
 *
 * <p>Write methods use {@code @Transactional}; reads use a read-only
 * transaction. Dependencies are injected via the constructor.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("User creation started for email={}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Duplicate email detected on create: {}", request.email());
            throw new DuplicateResourceException(
                    "A user with email '" + request.email() + "' already exists");
        }

        User saved = userRepository.save(userMapper.toEntity(request));
        log.info("User created successfully with id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        log.debug("User list requested: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("User lookup requested for id={}", id);
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("User update started for id={}", id);
        User user = findUserOrThrow(id);

        // Allow the user to keep their own email; reject if it belongs to someone else.
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            log.warn("Duplicate email detected on update for id={}: {}", id, request.email());
            throw new DuplicateResourceException(
                    "A user with email '" + request.email() + "' already exists");
        }

        userMapper.updateEntity(user, request);
        User updated = userRepository.save(user);
        log.info("User updated successfully with id={}", updated.getId());
        return userMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("User deletion started for id={}", id);
        User user = findUserOrThrow(id);
        userRepository.delete(user);
        log.info("User deleted successfully with id={}", id);
    }

    /** Central lookup helper so the not-found rule is defined in one place. */
    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for id={}", id);
                    return new ResourceNotFoundException("User not found with id " + id);
                });
    }
}
