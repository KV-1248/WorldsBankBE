package com.worldsbank.transaction.service;

import com.worldsbank.account.repository.AccountRepository;
import com.worldsbank.account.repository.LinkedBankAccountRepository;
import com.worldsbank.auth.repository.UserRepository;
import com.worldsbank.entity.*;
import com.worldsbank.transaction.dto.TransactionRequest;
import com.worldsbank.transaction.dto.TransactionResponse;
import com.worldsbank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LinkedBankAccountRepository linkedBankAccountRepository;
    private final UserRepository userRepository;

    // ─── GET CURRENT USER ────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── DEPOSIT ─────────────────────────────────────────────
    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        User user = getCurrentUser();

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Add to balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .currency(account.getBaseCurrency())
                .exchangeRate(BigDecimal.ONE)
                .convertedAmount(request.getAmount())
                .description(request.getDescription() != null
                        ? request.getDescription() : "Deposit")
                .receiverAccount(account)
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    // ─── WITHDRAW ────────────────────────────────────────────
    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {
        User user = getCurrentUser();

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Deduct from balance
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .currency(account.getBaseCurrency())
                .exchangeRate(BigDecimal.ONE)
                .convertedAmount(request.getAmount())
                .description(request.getDescription() != null
                        ? request.getDescription() : "Withdrawal")
                .senderAccount(account)
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    // ─── TRANSFER ────────────────────────────────────────────
    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        User user = getCurrentUser();

        // Sender account
        Account senderAccount = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        // Check sufficient balance
        if (senderAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Receiver account by WBAN
        Account receiverAccount = accountRepository.findByWban(request.getReceiverWban())
                .orElseThrow(() -> new RuntimeException("Receiver WBAN not found"));

        // Can't transfer to yourself
        if (senderAccount.getWban().equals(receiverAccount.getWban())) {
            throw new RuntimeException("Cannot transfer to your own account");
        }

        // Deduct from sender
        senderAccount.setBalance(
                senderAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(senderAccount);

        // Add to receiver
        receiverAccount.setBalance(
                receiverAccount.getBalance().add(request.getAmount()));
        accountRepository.save(receiverAccount);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .type(Transaction.TransactionType.TRANSFER)
                .amount(request.getAmount())
                .currency(senderAccount.getBaseCurrency())
                .exchangeRate(BigDecimal.ONE)
                .convertedAmount(request.getAmount())
                .description(request.getDescription() != null
                        ? request.getDescription() : "Transfer")
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    // ─── CROSS BANK TRANSFER ─────────────────────────────────
    @Transactional
    public TransactionResponse crossBankTransfer(TransactionRequest request) {
        User user = getCurrentUser();

        // Get WBAN master account
        Account wbanAccount = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Find linked bank account
        LinkedBankAccount linkedBank = linkedBankAccountRepository
                .findByAccountNumberAndBankName(
                        wbanAccount.getWban(), request.getTargetBankName())
                .orElseThrow(() -> new RuntimeException(
                        request.getTargetBankName() + " not linked to your account"));

        // Check sufficient balance
        if (wbanAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Handle currency conversion if different
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = request.getAmount();

        if (!wbanAccount.getBaseCurrency().equals(linkedBank.getCurrency())) {
            // Simple rate — in real world this comes from exchange rate API
            exchangeRate = new BigDecimal("0.0076"); // example KES to USD
            convertedAmount = request.getAmount()
                    .multiply(exchangeRate)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Deduct from WBAN
        wbanAccount.setBalance(
                wbanAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(wbanAccount);

        // Add to linked bank
        linkedBank.setBalance(
                linkedBank.getBalance().add(convertedAmount));
        linkedBankAccountRepository.save(linkedBank);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .type(Transaction.TransactionType.CROSS_BANK_TRANSFER)
                .amount(request.getAmount())
                .currency(wbanAccount.getBaseCurrency())
                .exchangeRate(exchangeRate)
                .convertedAmount(convertedAmount)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Transfer to " + linkedBank.getBankName())
                .senderAccount(wbanAccount)
                .linkedBankName(linkedBank.getBankName())
                .linkedBankCountry(linkedBank.getCountry())
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    // ─── TRANSACTION HISTORY ─────────────────────────────────
    public List<TransactionResponse> getTransactionHistory() {
        User user = getCurrentUser();

        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return transactionRepository
                .findBySenderAccountOrReceiverAccountOrderByCreatedAtDesc(
                        account, account)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── MAP TO RESPONSE ─────────────────────────────────────
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .exchangeRate(transaction.getExchangeRate())
                .convertedAmount(transaction.getConvertedAmount())
                .description(transaction.getDescription())
                .senderWban(transaction.getSenderAccount() != null
                        ? transaction.getSenderAccount().getWban() : null)
                .receiverWban(transaction.getReceiverAccount() != null
                        ? transaction.getReceiverAccount().getWban() : null)
                .linkedBankName(transaction.getLinkedBankName())
                .linkedBankCountry(transaction.getLinkedBankCountry())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}