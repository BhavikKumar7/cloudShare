package in.com.cloudshareapi.controller;

import in.com.cloudshareapi.dto.CreditRequestDTO;
import in.com.cloudshareapi.dto.PaymentDTO;
import in.com.cloudshareapi.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/add-credits")
    public ResponseEntity<?> addCredits(@RequestBody CreditRequestDTO request) {
        PaymentDTO response = paymentService.addCredits(request);

        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
