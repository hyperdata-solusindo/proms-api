package com.hyperdata.proms.module.sample.employee.entity;

import jakarta.persistence.*;
import com.hyperdata.proms.module.sample.employee.constant.EmployeeStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_number", nullable = false, unique = true, length = 32)
    private String employeeNumber;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 64)
    private String department;

    @Column(nullable = false, length = 64)
    private String position;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmployeeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
