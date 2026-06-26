package com.worldsbank.loan.repository;

import com.worldsbank.entity.Loan;
import com.worldsbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserOrderByAppliedAtDesc(User user);
    long countByUser(User user);
}