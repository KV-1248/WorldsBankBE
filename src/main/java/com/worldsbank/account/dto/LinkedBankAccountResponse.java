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
public class LinkedBankAccountResponse {
    private String bankName;
    private String country;
    private String currency;
    private String accountNumber;
    private BigDecimal balance;
    private String status;
}