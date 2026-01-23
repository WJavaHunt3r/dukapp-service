package com.ktk.dukappservice.data.userfrakarestreak;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFraKareWeekRepository extends JpaRepository<UserFraKareWeek, Long> {
    @Query(" SELECT fks from UserFraKareWeek fks where ( fks.user.id = ?1 or  ?1 is null ) " +
            " and (fks.fraKareWeek.weekNumber = ?2 or ?2 is null) " +
            " and (fks.listened = ?3 OR ?3 IS NULL) " +
            " and (fks.user.paceTeam.id = ?4 or ?4 is null or fks.user.paceTeam is null) " +
            " and (fks.fraKareWeek.year = ?5 OR ?5 IS NULL) " +
            " order by fks.user.lastname, fks.user.firstname ")
    List<UserFraKareWeek> fetchByQuery(Long userId, Integer weekNumber, Boolean listened, Long teamId, Integer seasonYear );

}
