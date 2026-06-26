package com.worldsbank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nationalId;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String nationality;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    // Response from Smile Identity or manual review
    private String verificationReference;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (verificationStatus == null)
            verificationStatus = VerificationStatus.PENDING;
    }

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED
    }
}