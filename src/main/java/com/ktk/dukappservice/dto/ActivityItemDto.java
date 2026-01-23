package com.ktk.dukappservice.dto;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class ActivityItemDto {

    private long id;

    private ActivityDto activity;

    private String description;

    private UserDto user;

    private UserDto createUser;

    private TransactionType transactionType;

    private Account account;

    private double hours;

    private Round round;

    private LocalDateTime createDateTime;
}
