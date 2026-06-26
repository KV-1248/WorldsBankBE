package com.worldsbank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyConversionResponse {
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal originalBalance;
    private BigDecimal convertedBalance;
    private BigDecimal conversionRate;
    private String country;
    // Transaction cost if user wants to transact
    private BigDecimal transactionFeePercent;
    private BigDecimal transactionFeeAmount;
}