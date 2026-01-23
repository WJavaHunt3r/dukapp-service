package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.rounds.Round;
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

    private long id;

    private Long transactionId;

    private LocalDate transactionDate;

    private String description;

    private UserDto user;

    private Long createUserId;

    private double points;

    private TransactionType transactionType;

    private Account account;

    private double hours;

    private Integer credit;

    private Round round;
}
