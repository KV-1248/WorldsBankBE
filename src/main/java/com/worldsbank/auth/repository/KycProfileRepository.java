package com.worldsbank.auth.repository;

import com.worldsbank.entity.KycProfile;
import com.worldsbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByUser(User user);
    boolean existsByNationalId(String nationalId);
}