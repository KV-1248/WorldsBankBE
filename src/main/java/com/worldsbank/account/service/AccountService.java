package com.worldsbank.account.service;

import com.worldsbank.account.dto.*;
import com.worldsbank.account.repository.AccountRepository;
import com.worldsbank.account.repository.LinkedBankAccountRepository;
import com.worldsbank.auth.repository.UserRepository;
import com.worldsbank.entity.Account;
import com.worldsbank.entity.LinkedBankAccount;
import com.worldsbank.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LinkedBankAccountRepository linkedBankAccountRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${exchangerate.api.key}")
    private String exchangeRateApiKey;

    // ─── GET CURRENT USER ────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── GET MY ACCOUNT ──────────────────────────────────────
    public AccountResponse getMyAccount() {
        User user = getCurrentUser();

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return AccountResponse.builder()
                .wban(account.getWban())
                .balance(account.getBalance())
                .baseCurrency(account.getBaseCurrency())
                .status(account.getStatus().name())
                .ownerName(user.getFirstName() + " " + user.getLastName())
                .build();
    }

    // ─── LINK BANK ACCOUNT ───────────────────────────────────
    @Transactional
    public LinkedBankAccountResponse linkBankAccount(LinkedBankAccountRequest request) {
        User user = getCurrentUser();

        Account wbanAccount = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("WBAN account not found"));

        // Check if bank already linked
        if (linkedBankAccountRepository.existsByWbanAccountAndBankName(
                wbanAccount, request.getBankName())) {
            throw new RuntimeException(request.getBankName() + " is already linked");
        }

        // Create linked bank account using WBAN as account number
        LinkedBankAccount linkedBank = LinkedBankAccount.builder()
                .bankName(request.getBankName())
                .country(request.getCountry())
                .currency(request.getCurrency())
                .accountNumber(wbanAccount.getWban())
                .balance(BigDecimal.ZERO)
                .status(LinkedBankAccount.LinkedAccountStatus.ACTIVE)
                .wbanAccount(wbanAccount)
                .build();

        linkedBankAccountRepository.save(linkedBank);

        return LinkedBankAccountResponse.builder()
                .bankName(linkedBank.getBankName())
                .country(linkedBank.getCountry())
                .currency(linkedBank.getCurrency())
                .accountNumber(linkedBank.getAccountNumber())
                .balance(linkedBank.getBalance())
                .status(linkedBank.getStatus().name())
                .build();
    }

    // ─── GET LINKED BANKS ────────────────────────────────────
    public List<LinkedBankAccountResponse> getLinkedBanks() {
        User user = getCurrentUser();

        Account wbanAccount = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("WBAN account not found"));

        return linkedBankAccountRepository.findByWbanAccount(wbanAccount)
                .stream()
                .map(bank -> LinkedBankAccountResponse.builder()
                        .bankName(bank.getBankName())
                        .country(bank.getCountry())
                        .currency(bank.getCurrency())
                        .accountNumber(bank.getAccountNumber())
                        .balance(bank.getBalance())
                        .status(bank.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    // ─── REGIONAL CURRENCY CONVERSION ────────────────────────
    public CurrencyConversionResponse getRegionalConversion(
            String targetCurrency, String country) {

        User user = getCurrentUser();

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String baseCurrency = account.getBaseCurrency();
        BigDecimal balance = account.getBalance();

        // Call ExchangeRate API
        BigDecimal conversionRate = fetchConversionRate(baseCurrency, targetCurrency);

        // Convert balance
        BigDecimal convertedBalance = balance.multiply(conversionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Transaction fee = 2.5%
        BigDecimal feePercent = new BigDecimal("2.5");
        BigDecimal feeAmount = convertedBalance
                .multiply(feePercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return CurrencyConversionResponse.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .originalBalance(balance)
                .convertedBalance(convertedBalance)
                .conversionRate(conversionRate)
                .country(country)
                .transactionFeePercent(feePercent)
                .transactionFeeAmount(feeAmount)
                .build();
    }

    // ─── FETCH CONVERSION RATE ───────────────────────────────
    private BigDecimal fetchConversionRate(String base, String target) {
        try {
            String url = "https://v6.exchangerate-api.com/v6/"
                    + exchangeRateApiKey
                    + "/pair/" + base + "/" + target;

            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("conversion_rate")) {
                return new BigDecimal(response.get("conversion_rate").toString());
            }

            throw new RuntimeException("Could not fetch conversion rate");

        } catch (Exception e) {
            throw new RuntimeException("Currency conversion failed: " + e.getMessage());
        }
    }
}