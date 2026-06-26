package com.worldsbank.auth.repository;

import com.worldsbank.entity.OtpToken;
import com.worldsbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // For account activation OTP
    Optional<OtpToken> findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(
            User user, OtpToken.OtpType otpType
    );
}