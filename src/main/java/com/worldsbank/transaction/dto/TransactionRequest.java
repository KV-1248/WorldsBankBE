package com.worldsbank.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String receiverWban;

    private String description;

    // For cross bank transfers
    private String targetBankName;
    private String targetCountry;

    // Currency user is transacting in
    private String currency;
}