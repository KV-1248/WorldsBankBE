package com.worldsbank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    // Currency of the transaction e.g KES, INR, USD
    @Column(nullable = false)
    private String currency;

    // Exchange rate at time of transaction
    private BigDecimal exchangeRate;

    // Amount after conversion
    private BigDecimal convertedAmount;

    private String description;

    @ManyToOne
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;

    @ManyToOne
    @JoinColumn(name = "receiver_account_id")
    private Account receiverAccount;

    // For linked bank transactions
    private String linkedBankName;
    private String linkedBankCountry;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.SUCCESS;
        if (exchangeRate == null) exchangeRate = BigDecimal.ONE;
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, CROSS_BANK_TRANSFER
    }

    public enum TransactionStatus {
        SUCCESS, FAILED, PENDING
    }
}