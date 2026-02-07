package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.payments.Payment;
import com.ktk.dukappservice.data.payments.PaymentsService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.PaymentDto;
import com.ktk.dukappservice.enums.PaymentGoal;
import com.ktk.dukappservice.enums.PaymentStatus;
import com.ktk.dukappservice.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentsService paymentService;
    private final PaymentMapper paymentMapper;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getPayments(@Nullable @RequestParam("userId") Long userId,
                                         @Nullable @RequestParam("status") PaymentStatus status,
                                         @Nullable @RequestParam("checkoutId") String checkoutId,
                                         @Nullable @RequestParam("checkoutReference") String checkoutReference,
                                         @Nullable @RequestParam("donationId") Long donationId,
                                         @Nullable @RequestParam("dateFrom") String dateFrom,
                                         @Nullable @RequestParam("dateTo") String dateTo,
                                         @Nullable @RequestParam("paymentGoal") PaymentGoal paymentGoal) {
        return ResponseEntity.status(200).body(paymentService.fetchByQuery(status, userId, donationId, checkoutId, checkoutReference, dateFrom, dateTo, paymentGoal).stream().map(paymentMapper::entityToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPayment(@PathVariable Long id) {
        var payment = paymentService.findById(id);
        if (payment.isEmpty()) {
            return ResponseEntity.status(404).body("No payment with id: " + id);
        }
        return ResponseEntity.status(200).body(paymentMapper.entityToDto(payment.get()));
    }

    @PostMapping
    public ResponseEntity<?> postPayment(@Valid @RequestBody PaymentDto payment) {
        Payment entity = new Payment();
        if (payment.getUser() != null) {
            Optional<User> createUser = userService.findById(payment.getUser().getId());
            if (createUser.isEmpty()) {
                return ResponseEntity.status(400).body("No user with id:" + payment.getUser().getId());
            }
            Optional<User> recipientUser = userService.findById(payment.getRecipient().getId());
            if (recipientUser.isEmpty()) {
                return ResponseEntity.status(400).body("No user with id:" + payment.getUser().getId());
            }
            entity.setUser(createUser.get());
            entity.setRecipient(recipientUser.get());
        }

        return ResponseEntity.status(200).body(paymentMapper.entityToDto(paymentService.save(paymentMapper.dtoToEntity(payment, entity))));
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<?> putPayment(@Valid @RequestBody PaymentDto paymentDto, @PathVariable Long paymentId) {
        Optional<Payment> payment = paymentService.findById(paymentId);
        if (payment.isEmpty() || !paymentDto.getId().equals(paymentId)) {
            return ResponseEntity.status(400).body("Invalid paymentId");
        }
        return ResponseEntity.status(200).body(paymentMapper.entityToDto(paymentService.save(paymentMapper.dtoToEntity(paymentDto, payment.get()))));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable Long paymentId) {
        if (paymentService.findById(paymentId).isEmpty()) {
            return ResponseEntity.status(400).body("Invalid paymentId");
        }
        paymentService.deleteById(paymentId);
        return ResponseEntity.status(200).body("Delete Successful");
    }
}
