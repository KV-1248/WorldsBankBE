package com.worldsbank.ai.service;

import com.worldsbank.account.repository.AccountRepository;
import com.worldsbank.ai.dto.AiRequest;
import com.worldsbank.ai.dto.AiResponse;
import com.worldsbank.auth.repository.UserRepository;
import com.worldsbank.entity.Account;
import com.worldsbank.entity.Transaction;
import com.worldsbank.entity.User;
import com.worldsbank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Value("${openai.model}")
    private String openAiModel;

    // ─── GET CURRENT USER ────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── ASK ASSISTANT ───────────────────────────────────────
    public AiResponse askAssistant(AiRequest request) {
        User user = getCurrentUser();

        // Get account
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Get transaction history
        List<Transaction> transactions = transactionRepository
                .findBySenderAccountOrReceiverAccountOrderByCreatedAtDesc(
                        account, account);

        // Build financial context for AI
        String financialContext = buildFinancialContext(user, account, transactions);

        // Build prompt
        String systemPrompt = "You are WorldsBank's AI financial assistant. "
                + "You help users understand their finances, analyze spending patterns, "
                + "and provide personalized financial advice. "
                + "Be concise, friendly and professional. "
                + "Always respond in the context of the user's actual financial data provided. "
                + "Here is the user's current financial data:\n\n"
                + financialContext;

        // Call OpenAI
        return callOpenAi(systemPrompt, request.getQuestion());
    }

    // ─── BUILD FINANCIAL CONTEXT ─────────────────────────────
    private String buildFinancialContext(User user, Account account,
                                         List<Transaction> transactions) {
        StringBuilder context = new StringBuilder();

        context.append("Customer Name: ")
                .append(user.getFirstName()).append(" ")
                .append(user.getLastName()).append("\n");

        context.append("WBAN: ").append(account.getWban()).append("\n");

        context.append("Current Balance: ")
                .append(account.getBalance())
                .append(" ").append(account.getBaseCurrency()).append("\n");

        context.append("Account Status: ")
                .append(account.getStatus().name()).append("\n\n");

        context.append("Recent Transactions (last 10):\n");

        // Only take last 10 transactions to keep prompt size manageable
        transactions.stream()
                .limit(10)
                .forEach(t -> context
                        .append("- ")
                        .append(t.getType().name())
                        .append(": ")
                        .append(t.getAmount())
                        .append(" ")
                        .append(t.getCurrency())
                        .append(t.getDescription() != null
                                ? " (" + t.getDescription() + ")" : "")
                        .append(" [").append(t.getStatus().name()).append("]")
                        .append(" on ").append(t.getCreatedAt())
                        .append("\n"));

        return context.toString();
    }

    // ─── CALL OPENAI API ─────────────────────────────────────
    private AiResponse callOpenAi(String systemPrompt, String userQuestion) {
        try {
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // Build messages array
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userQuestion);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAiModel);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            // Call OpenAI
            ResponseEntity<Map> response = restTemplate.exchange(
                    openAiApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Parse response
            Map responseBody = response.getBody();
            if (responseBody != null) {
                List<Map> choices = (List<Map>) responseBody.get("choices");
                Map firstChoice = choices.get(0);
                Map message = (Map) firstChoice.get("message");
                String answer = (String) message.get("content");

                // Get token usage
                Map usage = (Map) responseBody.get("usage");
                int tokensUsed = (Integer) usage.get("total_tokens");

                return AiResponse.builder()
                        .answer(answer)
                        .model(openAiModel)
                        .tokensUsed(tokensUsed)
                        .build();
            }

            throw new RuntimeException("Empty response from OpenAI");

        } catch (Exception e) {
            throw new RuntimeException("AI Assistant error: " + e.getMessage());
        }
    }
}