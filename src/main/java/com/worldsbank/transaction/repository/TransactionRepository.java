package com.worldsbank.transaction.repository;

import com.worldsbank.entity.Account;
import com.worldsbank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccountOrReceiverAccountOrderByCreatedAtDesc(
            Account senderAccount, Account receiverAccount
    );
    List<Transaction> findByLinkedBankNameAndSenderAccount(
            String linkedBankName, Account senderAccount
    );
}