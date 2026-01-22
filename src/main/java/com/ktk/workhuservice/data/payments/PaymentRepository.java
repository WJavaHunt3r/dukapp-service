package com.ktk.workhuservice.data.payments;

import com.ktk.workhuservice.enums.PaymentGoal;
import com.ktk.workhuservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p " +
            "WHERE (:status IS NULL OR p.status = :status) " +
            "AND (:userId IS NULL OR p.user.id = :userId) " +
            "AND (:donationId IS NULL OR p.donation.id = :donationId) " +
            "AND (:checkoutId IS NULL OR p.checkoutId LIKE CONCAT('%', :checkoutId, '%')) " +
            "AND (:checkoutReference IS NULL OR p.checkoutReference LIKE CONCAT('%', :checkoutReference, '%')) " +
            "AND (cast(:dateFrom as timestamp ) IS NULL OR p.dateTime > :dateFrom) " +
            "AND (cast(:dateTo as timestamp )IS NULL OR p.dateTime < :dateTo) " +
            "AND (:paymentGoal IS NULL OR p.paymentGoal = :paymentGoal)")
    List<Payment> fetchByQuery(@Param("status") PaymentStatus status,
                               @Param("userId") Long userId,
                               @Param("donationId") Long donationId,
                               @Param("checkoutId") String checkoutId,
                               @Param("checkoutReference") String checkoutReference,
                               @Param("dateFrom") LocalDateTime dateFrom,
                               @Param("dateTo") LocalDateTime dateTo,
                               @Param("paymentGoal") PaymentGoal paymentGoal);


    @Query(value = "SELECT * FROM payments p left join public.donations d on d.id = p.donation left join public.users u on u.id = p.users" +
            " where (p.status = ?1 or ?1 is null) " +
            " AND (u.id = ?2 or ?2 is null) " +
            " AND (d.id = ?3 or ?3 is null) " +
            " AND (p.checkout_id like concat('%', concat(?4, '%')) or ?4 is null) " +
            " AND (p.checkout_reference like concat('%', concat(?5, '%')) or ?5 is null)" +
            " AND (p.date_time > ?6 or ?6 is null ) " +
            " AND (p.date_time < ?7 or ?7 is null ) " +
            " AND (p.payment_goal = ?8) or ?8 is null ", nativeQuery = true)
    List<Payment> fetchByQuery(PaymentStatus status, Long userId, Long donationId, String checkoutId, String checkoutReference, String dateFrom, String dateTo, PaymentGoal paymentGoal);

    @Query(" SELECT sum(p.amount) FROM Payment p WHERE p.status = ?2 and ( p.donation.id = ?1 or ?1 is null) ")
    Integer sumByDonation(Long donationId, PaymentStatus status);
}
