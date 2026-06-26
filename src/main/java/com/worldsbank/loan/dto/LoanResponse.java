package com.worldsbank.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanResponse {
    private Long id;
    private String loanType;
    private BigDecimal amount;
    private int durationMonths;
    private String purpose;
    private String linkedBankName;
    private String status;
    private BigDecimal interestRate;
    private BigDecimal monthlyRepayment;
    private BigDecimal totalRepayment;
    private LocalDateTime appliedAt;
}