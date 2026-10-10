package com.example.usermanagement.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.usermanagement.config.JpaAuditingConfig;
import com.example.usermanagement.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Persistence tests for {@link UserRepository} against in-memory H2
 * (configured via the "test" profile). Verifies auditing and email uniqueness.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        return User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email(email)
                .phone("1234567890")
                .build();
    }

    @Test
    void save_persistsUserAndPopulatesAuditFields() {
        User saved = userRepository.save(newUser("jane@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsByEmail_returnsTrue_whenPresent() {
        userRepository.save(newUser("present@example.com"));

        assertThat(userRepository.existsByEmail("present@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNot_ignoresOwnRecord() {
        User saved = userRepository.save(newUser("owner@example.com"));

        assertThat(userRepository.existsByEmailAndIdNot("owner@example.com", saved.getId())).isFalse();
        assertThat(userRepository.existsByEmailAndIdNot("owner@example.com", saved.getId() + 1)).isTrue();
    }

    @Test
    void uniqueConstraint_rejectsDuplicateEmail() {
        userRepository.saveAndFlush(newUser("dup@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
