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
public class TransactionDto {
    private Long id;

    private String name;

    private Account account;

    private LocalDateTime createDateTime;

    private Integer transactionCount;

    private TransactionType transactionType;
}
