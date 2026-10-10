package com.example.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.DuplicateResourceException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.impl.UserServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link UserServiceImpl} using Mockito.
 * The repository and mapper are mocked so no database is involved.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest request;
    private User entity;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        request = new UserRequest("Jane", "Doe", "jane.doe@example.com", "1234567890");
        entity = User.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .phone("1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        response = new UserResponse(1L, "Jane", "Doe", "jane.doe@example.com",
                "1234567890", entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    void createUser_succeeds_whenEmailIsUnique() {
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponse(entity)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).save(entity);
    }

    @Test
    void createUser_throwsDuplicate_whenEmailExists() {
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_returnsUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toResponse(entity)).thenReturn(response);

        UserResponse result = userService.getUserById(1L);

        assertThat(result.email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void getUserById_throwsNotFound_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_succeeds_whenEmailBelongsToSameUser() {
        UserRequest update = new UserRequest("Janet", "Doe", "jane.doe@example.com", "999");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.existsByEmailAndIdNot(update.email(), 1L)).thenReturn(false);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponse(entity)).thenReturn(response);

        UserResponse result = userService.updateUser(1L, update);

        assertThat(result).isEqualTo(response);
        verify(userMapper).updateEntity(entity, update);
        verify(userRepository).save(entity);
    }

    @Test
    void updateUser_throwsDuplicate_whenEmailBelongsToAnotherUser() {
        UserRequest update = new UserRequest("Janet", "Doe", "taken@example.com", "999");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.existsByEmailAndIdNot(update.email(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, update))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_throwsNotFound_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_succeeds_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        userService.deleteUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void deleteUser_throwsNotFound_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void getUsers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toResponse(entity)).thenReturn(response);

        Page<UserResponse> result = userService.getUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(response);
    }
}
