package com.worldsbank.transaction.dto;

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
public class TransactionResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private String description;
    private String senderWban;
    private String receiverWban;
    private String linkedBankName;
    private String linkedBankCountry;
    private String status;
    private LocalDateTime createdAt;
}