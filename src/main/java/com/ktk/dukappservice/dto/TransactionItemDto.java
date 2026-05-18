package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
public class TransactionItemDto {

    private Long id;

    private Long transactionId;

    private LocalDate transactionDate;

    private String description;

    private Long userId;

    private String userName;

    private Long createUserId;

    private double points;

    private TransactionType transactionType;

    private Account account;

    private double hours;

    private Integer credit;

    private Long roundId;
}
