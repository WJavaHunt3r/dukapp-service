package com.ktk.dukappservice.security;

import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    int deleteByUserId(Long userId);

    @Modifying
    @Transactional
    int deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(Instant now);

    void deleteByToken(String token);
}