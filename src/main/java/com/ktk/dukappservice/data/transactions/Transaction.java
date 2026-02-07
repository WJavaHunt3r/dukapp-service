package com.ktk.dukappservice.data.transactions;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TRANSACTIONS")
@FieldNameConstants
public class Transaction extends BaseEntity<Transaction, Long> {

    @Size(max = 150)
    @Column(name = "NAME", length = 150)
    @NotNull
    private String name;

    @Column(name = "ACCOUNT")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Account account;

    @ManyToOne
    @JoinColumn(name = "CREATE_USER")
    private User createUser;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @Column(name = "CREATE_DATE_TIME")
    private LocalDateTime createDateTime;

    @Column(name = "TRANSACTION_TYPE")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
}
