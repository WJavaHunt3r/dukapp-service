package com.ktk.dukappservice.data.userstatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @Query("SELECT s FROM UserStatus s WHERE s.user.id = ?1 and s.season.seasonYear = ?2")
    Optional<UserStatus> findByUserIdAndSeasonYear(Long userId, Integer seasonYear);

    @Query("SELECT s FROM UserStatus s WHERE s.season.seasonYear = ?1 " +
            " and (s.user.paceTeam.id = ?2 or ?2 is null ) ")
    List<UserStatus> fetchByQuery(Integer year, Long teamId);
}
