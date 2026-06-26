package com.worldsbank.account.controller;

import com.worldsbank.account.dto.*;
import com.worldsbank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount() {
        return ResponseEntity.ok(accountService.getMyAccount());
    }

    @PostMapping("/link-bank")
    public ResponseEntity<LinkedBankAccountResponse> linkBankAccount(
            @Valid @RequestBody LinkedBankAccountRequest request) {
        return ResponseEntity.ok(accountService.linkBankAccount(request));
    }

    @GetMapping("/linked-banks")
    public ResponseEntity<List<LinkedBankAccountResponse>> getLinkedBanks() {
        return ResponseEntity.ok(accountService.getLinkedBanks());
    }

    @GetMapping("/regional-conversion")
    public ResponseEntity<CurrencyConversionResponse> getRegionalConversion(
            @RequestParam String targetCurrency,
            @RequestParam String country) {
        return ResponseEntity.ok(
                accountService.getRegionalConversion(targetCurrency, country));
    }
}