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
public class AccountResponse {
    private String wban;
    private BigDecimal balance;
    private String baseCurrency;
    private String status;
    private String ownerName;
    // Regional conversion fields
    private String regionalCurrency;
    private BigDecimal regionalBalance;
    private BigDecimal conversionRate;
}