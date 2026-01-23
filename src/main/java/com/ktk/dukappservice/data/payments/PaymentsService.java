package com.ktk.dukappservice.data.payments;

import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.enums.PaymentGoal;
import com.ktk.dukappservice.enums.PaymentStatus;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentsService extends BaseService<Payment, Long> {
    private final PaymentRepository repository;

    public PaymentsService(PaymentRepository repository, UserStatusService userStatusService) {
        this.repository = repository;
    }

    public List<Payment> fetchByQuery(PaymentStatus status, Long userId, Long donationId, String checkoutId, String checkoutReference, String dateFrom, String dateTo, PaymentGoal paymentGoal) {
        return repository.fetchByQuery(status, userId, donationId, checkoutId, checkoutReference, stringToDateFrom(dateFrom), stringToDateTo(dateTo), paymentGoal);
    }

    private LocalDateTime stringToDateFrom(String date) {
        if (date.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(date);
    }

    private LocalDateTime stringToDateTo(String date) {
        if (date.isEmpty()) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(date);
    }
    public Integer sumByDonation(Long donationId) {
        return repository.sumByDonation(donationId, PaymentStatus.PAID);
    }

    @Override
    protected JpaRepository<Payment, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<Payment> getEntityClass() {
        return Payment.class;
    }

    @Override
    public Payment createEntity() {
        return new Payment();
    }
}
