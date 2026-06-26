package com.worldsbank.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanRequest {

    @NotNull(message = "Loan type is required")
    private String loanType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    private int durationMonths;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String linkedBankName;
}