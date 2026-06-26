package com.worldsbank.account.repository;

import com.worldsbank.entity.Account;
import com.worldsbank.entity.LinkedBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, Long> {
    List<LinkedBankAccount> findByWbanAccount(Account wbanAccount);
    Optional<LinkedBankAccount> findByAccountNumberAndBankName(String accountNumber, String bankName);
    boolean existsByWbanAccountAndBankName(Account wbanAccount, String bankName);
}