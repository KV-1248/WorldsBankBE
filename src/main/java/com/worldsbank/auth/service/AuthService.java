package com.worldsbank.auth.service;

import com.worldsbank.account.repository.AccountRepository;
import com.worldsbank.auth.dto.*;
import com.worldsbank.auth.repository.KycProfileRepository;
import com.worldsbank.auth.repository.OtpTokenRepository;
import com.worldsbank.auth.repository.UserRepository;
import com.worldsbank.config.JwtConfig;
import com.worldsbank.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final KycProfileRepository kycProfileRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    // ─── REGISTER ───────────────────────────────────────────
    @Transactional
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (kycProfileRepository.existsByNationalId(request.getNationalId())) {
            throw new RuntimeException("National ID already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .dateOfBirth(request.getDateOfBirth())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .enabled(false)
                .build();

        userRepository.save(user);

        KycProfile kycProfile = KycProfile.builder()
                .nationalId(request.getNationalId())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .address(request.getAddress())
                .verificationStatus(KycProfile.VerificationStatus.PENDING)
                .user(user)
                .build();

        kycProfileRepository.save(kycProfile);

        generateAndSendOtp(user, OtpToken.OtpType.ACCOUNT_ACTIVATION);

        return "Registration successful. Check your email for OTP verification.";
    }

    // ─── VERIFY ACCOUNT ACTIVATION OTP ──────────────────────
    @Transactional
    public String verifyOtp(OtpVerificationRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        OtpToken otpToken = otpTokenRepository
                .findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(
                        user, OtpToken.OtpType.ACCOUNT_ACTIVATION)
                .orElseThrow(() -> new RuntimeException("No OTP found"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Request a new one.");
        }

        if (!otpToken.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        user.setEnabled(true);
        userRepository.save(user);

        String wban = generateWban(user);
        Account account = Account.builder()
                .wban(wban)
                .balance(java.math.BigDecimal.ZERO)
                .baseCurrency("KES")
                .status(Account.AccountStatus.ACTIVE)
                .user(user)
                .build();

        accountRepository.save(account);

        kycProfileRepository.findByUser(user).ifPresent(kyc -> {
            kyc.setVerificationStatus(KycProfile.VerificationStatus.VERIFIED);
            kyc.setVerifiedAt(LocalDateTime.now());
            kycProfileRepository.save(kyc);
        });

        return "Account verified successfully. Your WBAN is: " + wban;
    }

    // ─── LOGIN — step 1: verify password, send OTP ──────────
    @Transactional
    public String login(LoginRequest request) {

        // Verify email + password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Check your email for OTP.");
        }

        // Credentials correct — send login OTP
        generateAndSendOtp(user, OtpToken.OtpType.LOGIN);

        return "Credentials verified. OTP sent to " + user.getEmail();
    }

    // ─── VERIFY LOGIN OTP — step 2: return JWT ──────────────
    @Transactional
    public AuthResponse verifyLoginOtp(OtpVerificationRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        OtpToken otpToken = otpTokenRepository
                .findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(
                        user, OtpToken.OtpType.LOGIN)
                .orElseThrow(() -> new RuntimeException("No login OTP found"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Try logging in again.");
        }

        if (!otpToken.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        // Now generate and return JWT
        String token = jwtConfig.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    // ─── RESEND OTP ─────────────────────────────────────────
    @Transactional
    public String resendOtp(String email, OtpToken.OtpType otpType) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (otpType == OtpToken.OtpType.ACCOUNT_ACTIVATION && user.isEnabled()) {
            throw new RuntimeException("Account already verified");
        }

        generateAndSendOtp(user, otpType);

        return "New OTP sent to " + email;
    }

    // ─── HELPERS ─────────────────────────────────────────────

    private void generateAndSendOtp(User user, OtpToken.OtpType otpType) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        OtpToken otpToken = OtpToken.builder()
                .otp(otp)
                .user(user)
                .otpType(otpType)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);

        String subject = otpType == OtpToken.OtpType.LOGIN
                ? "WorldsBank — Login Verification"
                : "WorldsBank — Account Activation";

        String body = "Dear " + user.getFirstName() + ",\n\n"
                + (otpType == OtpToken.OtpType.LOGIN
                ? "Your login OTP is: "
                : "Your account activation OTP is: ")
                + otp + "\n\n"
                + "This OTP expires in " + otpExpiryMinutes + " minutes.\n\n"
                + "If you did not request this, please secure your account immediately.\n\n"
                + "WorldsBank Security Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log the error but don't roll back the transaction
            System.err.println("Email sending failed: " + e.getMessage());
        }
    }

    private String generateWban(User user) {
        String countryCode = "KE";
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String sequence = String.format("%05d", userRepository.count());

        String wban = "WB-" + countryCode + "-" + year + "-" + sequence;

        while (accountRepository.existsByWban(wban)) {
            sequence = String.format("%05d", new Random().nextInt(99999));
            wban = "WB-" + countryCode + "-" + year + "-" + sequence;
        }

        return wban;
    }
}