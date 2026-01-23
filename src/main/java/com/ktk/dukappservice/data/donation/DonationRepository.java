package com.ktk.dukappservice.data.donation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    @Query(" SELECT d FROM Donation d WHERE  (d.startDateTime <= :dateTime AND d.endDateTime >= :dateTime ) or (cast(:dateTime as date) is null)" )
    List<Donation> fetchByQuery(@Param("dateTime") LocalDateTime dateTime);
}
