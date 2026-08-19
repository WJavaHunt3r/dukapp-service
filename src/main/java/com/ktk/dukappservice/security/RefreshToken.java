package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;

@Entity
@Table(name = "REFRESH_TOKEN")
@Getter
@Setter
@FieldNameConstants
@RequiredArgsConstructor
public class RefreshToken extends BaseEntity<RefreshToken, Long> {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERS")
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private String deviceInfo;

}