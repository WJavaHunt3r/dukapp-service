package com.ktk.dukappservice.data.transactions;

import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Iterable<Transaction> findAllByCreateUser(User createUser);

    Optional<Transaction> findByName(String name);

    @Query("SELECT t FROM Transaction t where t.createDateTime > ?1 and t.createDateTime < ?2")
    List<Transaction> findAllByRoundNr(LocalDateTime startDateTime, LocalDateTime endDateTime);
}
