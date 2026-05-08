package com.ktk.dukappservice.dto;

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

    private Long activityId;

    private String description;

    private Long userId;

    private String userName;

    private Long createUserId;

    private String createUserName;

    private TransactionType transactionType;

    private Account account;

    private double hours;

    private Long roundId;

    private LocalDateTime createDateTime;
}
