package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.donation.Donation;
import com.ktk.dukappservice.enums.PaymentGoal;
import com.ktk.dukappservice.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
public class PaymentDto {
    private Long id;

    private Integer amount;

    private String description;

    private String checkoutReference;

    private String checkoutId;

    private LocalDateTime dateTime;

    private PaymentStatus status;

    private PaymentGoal paymentGoal;

    private UserDto user;

    private UserDto recipient;

    private Donation donation;

}
