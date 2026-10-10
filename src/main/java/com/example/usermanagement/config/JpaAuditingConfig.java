package com.example.usermanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * fields on entities are populated automatically.
 *
 * <p>Kept separate from the main application class so web-only test slices
 * (e.g. {@code @WebMvcTest}) do not attempt to wire the auditing infrastructure.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
