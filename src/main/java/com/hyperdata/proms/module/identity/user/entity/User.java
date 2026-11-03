package com.hyperdata.proms.module.identity.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "users", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idp_subject", nullable = false)
    private String idpSubject = "local";

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String locale = "id";

    @Column
    private String timezone;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_platform_staff", nullable = false)
    private boolean isPlatformStaff = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
