package com.worldsbank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "linked_bank_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String currency;

    // This IS the WBAN — same number used as account number in this bank
    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private LinkedAccountStatus status;

    // Links back to the master WorldsBank account
    @ManyToOne
    @JoinColumn(name = "wban_account_id", nullable = false)
    private Account wbanAccount;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = LinkedAccountStatus.ACTIVE;
    }

    public enum LinkedAccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }
}