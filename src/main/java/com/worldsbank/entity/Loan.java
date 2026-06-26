package com.worldsbank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private int durationMonths;

    @Column(nullable = false)
    private String purpose;

    private String linkedBankName;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private BigDecimal interestRate;
    private BigDecimal monthlyRepayment;
    private BigDecimal totalRepayment;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = LoanStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum LoanType {
        PERSONAL, BUSINESS, MORTGAGE
    }

    public enum LoanStatus {
        PENDING, APPROVED, REJECTED, DISBURSED, REPAID
    }
}