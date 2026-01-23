package com.ktk.dukappservice.data.goals;

import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query(" SELECT g from Goal g where g.user = ?1 and g.season.seasonYear = ?2 ")
    Optional<Goal> findByUserAndSeasonSeasonYearOrUserSpouse(User user, Integer season);

    List<Goal> findBySeason(Season season);
}
