package com.worldsbank.account.repository;

import com.worldsbank.entity.Account;
import com.worldsbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUser(User user);
    Optional<Account> findByWban(String wban);
    boolean existsByWban(String wban);
}