package com.ktk.dukappservice.data.challenges;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChallengesRepository extends JpaRepository<Challenges, Long> {
    @Query(" SELECT c FROM Challenges c WHERE  (c.startDate <= :dateTime AND c.endDate >= :dateTime ) or (cast(:dateTime as date) is null)")
    List<Challenges> fetchByQuery(@Param("dateTime") LocalDate dateTime);
}