package com.worldsbank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The global WorldsBank Account Number
    @Column(nullable = false, unique = true)
    private String wban;

    @Column(nullable = false)
    private BigDecimal balance;

    // Base currency of account e.g KES
    @Column(nullable = false)
    private String baseCurrency;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = AccountStatus.ACTIVE;
    }

    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }
}