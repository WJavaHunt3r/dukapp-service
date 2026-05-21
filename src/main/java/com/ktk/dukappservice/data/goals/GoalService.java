package com.ktk.dukappservice.data.goals;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoalService extends BaseService<Goal, Long> {
    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public Optional<Goal> findByUserAndSeasonYear(User user, Integer year) {
        return goalRepository.findByUserAndSeasonSeasonYearOrUserSpouse(user, year);
    }

    public Page<Goal> fetchByQuery(Integer seasonYear, Long userId, Pageable pageable) {
        return goalRepository.fetchByQuery(seasonYear, userId, pageable);
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
