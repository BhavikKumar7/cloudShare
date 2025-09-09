package in.com.cloudshareapi.service;

import in.com.cloudshareapi.document.PaymentTransaction;
import in.com.cloudshareapi.document.ProfileDocument;
import in.com.cloudshareapi.dto.PaymentDTO;
import in.com.cloudshareapi.dto.CreditRequestDTO;
import in.com.cloudshareapi.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentDTO addCredits(CreditRequestDTO request) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            int creditsToAdd;
            int amount; // real amount in INR
            String plan;

            switch (request.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    amount = 500;  // ₹500
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    amount = 2500; // ₹2500
                    plan = "ULTIMATE";
                    break;
                default:
                    return PaymentDTO.builder()
                            .success(false)
                            .message("Invalid plan selected")
                            .build();
            }

            // Add credits
            userCreditsService.addCredits(clerkId, creditsToAdd, plan);

            // Save transaction with real amount
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .planId(request.getPlanId())
                    .amount(amount) // ✅ real plan price
                    .currency("INR")
                    .status("SUCCESS")
                    .transactionDate(LocalDateTime.now())
                    .creditsAdded(creditsToAdd)
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastName())
                    .build();
            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .success(true)
                    .message("Credits added successfully")
                    .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                    .amount(amount)   // ✅ include in response
                    .currency("INR")
                    .planId(request.getPlanId())
                    .build();

        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error adding credits: " + e.getMessage())
                    .build();
        }
    }
}
