package com.ktk.dukappservice.data.goals;

import com.ktk.dukappservice.data.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query(" SELECT g from Goal g where g.user = ?1 and g.season.seasonYear = ?2 ")
    Optional<Goal> findByUserAndSeasonSeasonYearOrUserSpouse(User user, Integer season);

    @Query(value = " SELECT g from Goal g join fetch g.user join fetch g.season where " +
            " (g.user.id = ?2 OR ?2 IS NULL) " +
            " AND (g.season.seasonYear = ?1 OR ?1 IS NULL) " ,
            countQuery = " SELECT count(g) from Goal g where " +
                    " (g.user.id = ?2 OR ?2 IS NULL) " +
                    " AND (g.season.seasonYear = ?1 OR ?1 IS NULL) " )
    Page<Goal> fetchByQuery(Integer seasonYear,Long userId, Pageable pageable);
}
