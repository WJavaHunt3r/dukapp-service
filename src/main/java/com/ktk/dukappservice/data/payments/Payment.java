package com.ktk.dukappservice.data.payments;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.donation.Donation;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.PaymentGoal;
import com.ktk.dukappservice.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PAYMENTS")
@FieldNameConstants
public class Payment extends BaseEntity<Payment, Long> {

    @NotNull
    @Column(name = "AMOUNT", columnDefinition = "integer default 0")
    private Integer amount;

    @NotNull
    @NotEmpty
    @Column(name = "DESCRIPTION", length = 60)
    @Size(max = 60)
    private String description;

    @NotNull
    @NotEmpty
    @Column(name = "CHECKOUT_REFERENCE", length = 20)
    @Size(max = 20)
    private String checkoutReference;

    @NotNull
    @NotEmpty
    @Column(name = "CHECKOUT_ID", length = 40)
    @Size(max = 40)
    private String checkoutId;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "DATE_TIME")
    private LocalDateTime dateTime;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentStatus status;

    @Column(name = "PAYMENT_GOAL")
    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentGoal paymentGoal;

    @JoinColumn(name = "USERS")
    @ManyToOne
    private User user;

    @JoinColumn(name = "RECIPIENT")
    @ManyToOne
    private User recipient;

    @JoinColumn(name = "DONATION")
    @ManyToOne
    private Donation donation;
}
