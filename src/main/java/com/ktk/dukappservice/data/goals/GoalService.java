package com.ktk.dukappservice.data.goals;

import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GoalService extends BaseService<Goal, Long> {
    private GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public Optional<Goal> findByUserAndSeasonYear(User user, Integer year){
        return goalRepository.findByUserAndSeasonSeasonYearOrUserSpouse(user, year);
    }

    public List<Goal> findBySeason(Season season){
        return goalRepository.findBySeason(season);
    }

    @Override
    protected JpaRepository<Goal, Long> getRepository() {
        return goalRepository;
    }

    @Override
    public Class<Goal> getEntityClass() {
        return Goal.class;
    }

    @Override
    public Goal createEntity() {
        return new Goal();
    }


}
