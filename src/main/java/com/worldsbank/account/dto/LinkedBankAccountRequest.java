package com.worldsbank.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkedBankAccountRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Currency is required")
    private String currency;
}