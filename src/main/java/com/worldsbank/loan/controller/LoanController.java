package com.worldsbank.loan.controller;

import com.worldsbank.loan.dto.LoanEligibilityResponse;
import com.worldsbank.loan.dto.LoanRequest;
import com.worldsbank.loan.dto.LoanResponse;
import com.worldsbank.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/eligibility")
    public ResponseEntity<LoanEligibilityResponse> checkEligibility() {
        return ResponseEntity.ok(loanService.checkEligibility());
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> applyLoan(
            @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.ok(loanService.applyLoan(request));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<LoanResponse>> getMyLoans() {
        return ResponseEntity.ok(loanService.getMyLoans());
    }
}