package com.worldsbank.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanEligibilityResponse {
    private boolean eligible;
    private String message;
    private BigDecimal maxLoanAmount;
    private BigDecimal accountBalance;
    private long totalTransactions;
}