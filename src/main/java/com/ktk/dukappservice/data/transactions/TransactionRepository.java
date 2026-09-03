package com.ktk.dukappservice.data.transactions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByName(String name);

    @Query(value = "SELECT t FROM Transaction t join fetch t.createUser where t.createDateTime >= ?1 and t.createDateTime <= ?2 " +
            " and (t.createUser.id = ?3 or ?3 is null) ", countQuery = "SELECT count(t) FROM Transaction t where t.createDateTime > ?1 and t.createDateTime < ?2 " +
            " and (t.createUser.id = ?3 or ?3 is null) ")
    Page<Transaction> fetchByQuery(LocalDateTime startDateTime, LocalDateTime endDateTime, Long userId, Pageable pageable);
}
