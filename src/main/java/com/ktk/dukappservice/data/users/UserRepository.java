package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    Optional<User> findByMyShareID(Long id);

    Iterable<User> findAllByRole(Role role);

    @Query("SELECT u FROM User u where u.familyId = ?1 and u.id <> ?2")
    List<User> findFamily(Long familyId, Long userId);

    @Query("SELECT u FROM Goal g JOIN g.user u JOIN g.season s where s = ?2 and u.paceTeam = ?1")
    Iterable<User> findAllByPaceTeamAndSeasonAndGoal(PaceTeam t, Season s);

    @Query("SELECT COUNT(u) FROM Goal g JOIN g.user u JOIN g.season s where s.seasonYear = ?2 and u.team = ?1")
    Long countAllByTeamAndSeasonAndGoal(Team t, Integer s);

    @Query("SELECT u FROM Goal g JOIN g.user u JOIN g.season s where s.seasonYear = ?1 and u IS NOT null ")
    Iterable<User> findAllBUKBySeason(Integer seasonYear);

    @Query(value = "SELECT u FROM User u " +
            "LEFT JOIN FETCH u.paceTeam pt " +
            "LEFT JOIN FETCH u.church c " +
            "WHERE (:familyId IS NULL OR u.familyId = :familyId) " +
            "AND (:spouseId IS NULL OR u.spouseId = :spouseId) " +
            "AND (:teamId IS NULL OR pt.id = :teamId) " +
            "AND (:churchId IS NULL OR c.id = :churchId) " +
            "AND (:kw IS NULL OR (" +
            "   lower(u.firstname) LIKE lower(concat('%', :kw, '%')) OR " +
            "   lower(u.lastname) LIKE lower(concat('%', :kw, '%')) OR " +
            "   lower(u.username) LIKE lower(concat('%', :kw, '%'))" +
            "))",
            countQuery = "SELECT count(u) FROM User u " +
                    "LEFT JOIN u.paceTeam pt " +
                    "LEFT JOIN u.church c " +
                    "WHERE (:familyId IS NULL OR u.familyId = :familyId) " +
                    "AND (:spouseId IS NULL OR u.spouseId = :spouseId) " +
                    "AND (:teamId IS NULL OR pt.id = :teamId) " +
                    "AND (:churchId IS NULL OR c.id = :churchId) " +
                    "AND (:kw IS NULL OR (" +
                    "   lower(u.firstname) LIKE lower(concat('%', :kw, '%')) OR " +
                    "   lower(u.lastname) LIKE lower(concat('%', :kw, '%')) OR " +
                    "   lower(u.username) LIKE lower(concat('%', :kw, '%'))" +
                    "))")
    Page<User> fetchByQuery(
            @Param("familyId") Long familyId,
            @Param("spouseId") Long spouseId,
            @Param("teamId") Long teamId,
            @Param("churchId") Long churchId,
            @Param("kw") String keyword,
            Pageable pageable);
}
