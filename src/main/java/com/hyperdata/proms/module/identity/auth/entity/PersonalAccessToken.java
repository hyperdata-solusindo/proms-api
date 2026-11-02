package com.hyperdata.proms.module.identity.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "personal_access_tokens", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tokenable_type", nullable = false)
    private String tokenableType;

    @Column(name = "tokenable_id", nullable = false)
    private UUID tokenableId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String token;

    @Column
    private String abilities;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
