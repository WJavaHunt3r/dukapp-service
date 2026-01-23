package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByMyShareID(Long id);

    Iterable<User> findAllByRole(Role role);

    @Query("SELECT u FROM User u where u.familyId = ?1 and u.id <> ?1 and u.spouseId is null")
    List<User> findChildren(Long familyId);

    @Query("SELECT u FROM Goal g JOIN g.user u JOIN g.season s where s = ?2 and u.paceTeam = ?1")
    Iterable<User> findAllByPaceTeamAndSeasonAndGoal(PaceTeam t, Season s);

    @Query("SELECT u FROM Goal g JOIN g.user u JOIN g.season s where s = ?2 and u.team = ?1")
    Iterable<User> findAllByTeamAndSeasonAndGoal(Team t, Season s);

    @Query("SELECT COUNT(u) FROM Goal g JOIN g.user u JOIN g.season s where s.seasonYear = ?2 and u.paceTeam = ?1")
    Long countAllByPaceTeamAndSeasonAndGoal(PaceTeam t, Integer s);

    @Query("SELECT COUNT(u) FROM Goal g JOIN g.user u JOIN g.season s where s.seasonYear = ?2 and u.team = ?1")
    Long countAllByTeamAndSeasonAndGoal(Team t, Integer s);

    @Query("SELECT u FROM Goal g JOIN g.user u JOIN g.season s where s.seasonYear = ?1 and u IS NOT null ")
    Iterable<User> findAllBUKBySeason(Integer seasonYear);
}
