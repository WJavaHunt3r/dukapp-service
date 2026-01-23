package com.ktk.dukappservice.data.userstatus;

import com.ktk.dukappservice.data.goals.Goal;
import com.ktk.dukappservice.data.goals.GoalService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserStatusService extends BaseService<UserStatus, Long> {

    private final UserStatusRepository repository;
    private final GoalService goalService;
    private final TransactionItemService transactionItemService;
    private final SeasonService seasonService;

    public UserStatusService(UserStatusRepository repository, GoalService goalService, TransactionItemService transactionItemService, SeasonService seasonService) {
        this.repository = repository;
        this.goalService = goalService;
        this.transactionItemService = transactionItemService;
        this.seasonService = seasonService;
    }

    public Optional<UserStatus> findByUserId(Long userId, Integer seasonYear) {
        return repository.findByUserIdAndSeasonYear(userId, seasonYear);
    }

    public Optional<UserStatus> findByUserId(Long userId) {
        return repository.findByUserIdAndSeasonYear(userId, seasonService.findCurrentSeason().getSeasonYear());
    }

    public List<UserStatus> fetchByQuery(Integer seasonYear, Long teamId) {
        return repository.fetchByQuery(seasonYear, teamId);
    }

    public void createUserStatusForAllUsers(Integer seasonYear) {
        Optional<Season> season = seasonService.findBySeasonYear(seasonYear);
        if (season.isEmpty()) {
            return;
        }
        for (Goal goal : goalService.findBySeason(season.get())) {
            createUserStatus(goal.getUser(), goal.getGoal(), season.get());
        }
    }

    public void createUserStatus(User u, Integer goal, Season season) {
        if (findByUserId(u.getId(), season.getSeasonYear()).isEmpty()) {
            UserStatus status = createEntity();
            status.setUser(u);
            status.setGoal(goal);
            status.setSeason(season);
            calculateUserStatus(status);
        }
    }

    public void calculateUserStatus(User u, Integer goal) {
        findByUserId(u.getId()).ifPresentOrElse(this::calculateUserStatus, () -> createUserStatus(u, goal, seasonService.findCurrentSeason()));
    }

    public void calculateUserStatus(UserStatus us) {
        Integer transactions = 0;
        Integer sumCredit = transactionItemService.sumCreditByUserAndSeasonYear(us.getUser(), us.getSeason().getSeasonYear(), Account.MYSHARE);
        if (sumCredit != null) {
            transactions += sumCredit;
        }
        Optional<UserStatus> lastYearStatus = findByUserId(us.getUser().getId(), us.getSeason().getSeasonYear() - 1);
        if (lastYearStatus.isPresent()) {
            int transition = Math.max(lastYearStatus.get().getTransition(), 0);
            transactions += transition;
        } else {
            transactions += us.getUser().getBaseMyShareCredit();
        }
        Optional<Goal> userGoal = goalService.findByUserAndSeasonYear(us.getUser(), us.getSeason().getSeasonYear());
        if (userGoal.isEmpty()) return;
        us.setGoal(userGoal.get().getGoal());
        us.setStatus((double) transactions / us.getGoal());
        us.setTransactions(transactions);
        us.setTransition(Math.max(us.getTransactions() - us.getGoal(), 0));
        save(us);
    }

    @Override
    protected JpaRepository<UserStatus, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<UserStatus> getEntityClass() {
        return UserStatus.class;
    }

    @Override
    public UserStatus createEntity() {
        return new UserStatus();
    }
}
