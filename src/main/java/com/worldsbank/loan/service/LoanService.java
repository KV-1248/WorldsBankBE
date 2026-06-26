package com.worldsbank.loan.service;

import com.worldsbank.account.repository.AccountRepository;
import com.worldsbank.auth.repository.UserRepository;
import com.worldsbank.entity.Account;
import com.worldsbank.entity.Loan;
import com.worldsbank.entity.User;
import com.worldsbank.loan.dto.LoanEligibilityResponse;
import com.worldsbank.loan.dto.LoanRequest;
import com.worldsbank.loan.dto.LoanResponse;
import com.worldsbank.loan.repository.LoanRepository;
import com.worldsbank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── CHECK ELIGIBILITY ───────────────────────────────
    public LoanEligibilityResponse checkEligibility() {
        User user = getCurrentUser();
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal balance = account.getBalance();
        long txCount = transactionRepository
                .findBySenderAccountOrReceiverAccountOrderByCreatedAtDesc(
                        account, account).size();

        // Eligibility rules
        // Rule 1 — minimum balance of KES 1,000
        if (balance.compareTo(new BigDecimal("1000")) < 0) {
            return LoanEligibilityResponse.builder()
                    .eligible(false)
                    .message("Minimum account balance of KES 1,000 required.")
                    .accountBalance(balance)
                    .totalTransactions(txCount)
                    .maxLoanAmount(BigDecimal.ZERO)
                    .build();
        }

        // Rule 2 — at least 1 transaction
        if (txCount < 1) {
            return LoanEligibilityResponse.builder()
                    .eligible(false)
                    .message("At least 1 transaction required to qualify.")
                    .accountBalance(balance)
                    .totalTransactions(txCount)
                    .maxLoanAmount(BigDecimal.ZERO)
                    .build();
        }

        // Max loan = 3x account balance, will change to 2
        BigDecimal maxLoan = balance.multiply(new BigDecimal("3"))
                .setScale(2, RoundingMode.HALF_UP);

        return LoanEligibilityResponse.builder()
                .eligible(true)
                .message("You qualify for a loan of up to KES " + maxLoan.toPlainString())
                .accountBalance(balance)
                .totalTransactions(txCount)
                .maxLoanAmount(maxLoan)
                .build();
    }

    // ─── APPLY FOR LOAN ──────────────────────────────────
    public LoanResponse applyLoan(LoanRequest request) {
        User user = getCurrentUser();
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        LoanEligibilityResponse eligibility = checkEligibility();
        if (!eligibility.isEligible()) {
            throw new RuntimeException("You are not eligible: " + eligibility.getMessage());
        }

        if (request.getAmount().compareTo(eligibility.getMaxLoanAmount()) > 0) {
            throw new RuntimeException(
                    "Loan amount exceeds maximum allowed: KES " +
                            eligibility.getMaxLoanAmount().toPlainString()
            );
        }

        // Interest rate based on loan type
        BigDecimal interestRate = switch (request.getLoanType()) {
            case "PERSONAL" -> new BigDecimal("14.0");
            case "BUSINESS" -> new BigDecimal("13.5");
            case "MORTGAGE" -> new BigDecimal("12.5");
            default -> new BigDecimal("14.0");
        };

        // Monthly repayment = (P * r * (1+r)^n) / ((1+r)^n - 1)
        BigDecimal monthlyRate = interestRate
                .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        int n = request.getDurationMonths();
        double r = monthlyRate.doubleValue();
        double P = request.getAmount().doubleValue();
        double monthlyPayment = (P * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        BigDecimal monthlyRepayment = new BigDecimal(monthlyPayment)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = monthlyRepayment
                .multiply(new BigDecimal(n))
                .setScale(2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .user(user)
                .account(account)
                .loanType(Loan.LoanType.valueOf(request.getLoanType()))
                .amount(request.getAmount())
                .durationMonths(request.getDurationMonths())
                .purpose(request.getPurpose())
                .linkedBankName(request.getLinkedBankName())
                .interestRate(interestRate)
                .monthlyRepayment(monthlyRepayment)
                .totalRepayment(totalRepayment)
                .status(Loan.LoanStatus.PENDING)
                .build();

        loanRepository.save(loan);
        return mapToResponse(loan);
    }

    // ─── GET MY LOANS ────────────────────────────────────
    public List<LoanResponse> getMyLoans() {
        User user = getCurrentUser();
        return loanRepository.findByUserOrderByAppliedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LoanResponse mapToResponse(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanType(loan.getLoanType().name())
                .amount(loan.getAmount())
                .durationMonths(loan.getDurationMonths())
                .purpose(loan.getPurpose())
                .linkedBankName(loan.getLinkedBankName())
                .status(loan.getStatus().name())
                .interestRate(loan.getInterestRate())
                .monthlyRepayment(loan.getMonthlyRepayment())
                .totalRepayment(loan.getTotalRepayment())
                .appliedAt(loan.getAppliedAt())
                .build();
    }
}