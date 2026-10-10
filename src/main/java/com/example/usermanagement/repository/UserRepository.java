package com.example.usermanagement.repository;

import com.example.usermanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data-access layer for {@link User}.
 *
 * <p>The {@code existsByEmail*} queries let the service check uniqueness
 * cheaply (a {@code COUNT}/{@code EXISTS} query) without loading full rows.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** True if any user already uses the given email. */
    boolean existsByEmail(String email);

    /** True if a user other than {@code id} uses the given email (used on update). */
    boolean existsByEmailAndIdNot(String email, Long id);
}
