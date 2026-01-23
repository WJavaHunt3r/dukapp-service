package com.ktk.dukappservice.data.userfrakarestreak;

import com.ktk.dukappservice.data.frakareweek.FraKareWeek;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.enums.TransactionType;
import com.ktk.dukappservice.service.BaseService;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserFraKareWeekService extends BaseService<UserFraKareWeek, Long> {

    private UserFraKareWeekRepository repository;
    private UserService userService;
    private TransactionItemService transactionItemService;
    private final RoundService roundService;
    private final TransactionServiceUtils transactionServiceUtils;

    public UserFraKareWeekService(UserFraKareWeekRepository repository, UserService userService, TransactionItemService transactionItemService, RoundService roundService, TransactionServiceUtils transactionServiceUtils) {
        this.repository = repository;
        this.userService = userService;
        this.transactionItemService = transactionItemService;
        this.roundService = roundService;
        this.transactionServiceUtils = transactionServiceUtils;
    }

    @Override
    protected JpaRepository<UserFraKareWeek, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<UserFraKareWeek> getEntityClass() {
        return UserFraKareWeek.class;
    }

    @Override
    public UserFraKareWeek createEntity() {
        return new UserFraKareWeek();
    }

    public List<UserFraKareWeek> fetchByQuery(Long userId, Integer weekNumber, Boolean listened, Long teamId, Integer seasonYear) {
        return repository.fetchByQuery(userId, weekNumber, listened, teamId, seasonYear);
    }

    public UserFraKareWeek setListened(UserFraKareWeek userFraKareWeek, boolean listened) {
        if (listened && !userFraKareWeek.isListened()) {
            userFraKareWeek.setListened(true);
            userFraKareWeek.setTransactionItemId(createTransactionItem(userFraKareWeek.getFraKareWeek().getTransactionId(), userFraKareWeek));

        } else if (!listened && userFraKareWeek.isListened()) {
            userFraKareWeek.setListened(false);
            transactionItemService.deleteById(userFraKareWeek.getTransactionItemId());
            userFraKareWeek.setTransactionItemId(null);
        }
        transactionServiceUtils.updateUserStatus(roundService.getLastRound(), userFraKareWeek.getUser());
        transactionServiceUtils.calculateAllTeamStatus();

        return save(userFraKareWeek);
    }

    private Long createTransactionItem(Long transactionId, UserFraKareWeek userFraKareWeek) {
        TransactionItem transactionItem = new TransactionItem();
        transactionItem.setHours(0);
        transactionItem.setPoints(10);
        transactionItem.setTransactionDate(LocalDate.now());
        transactionItem.setRound(roundService.getLastRound());
        transactionItem.setTransactionId(transactionId);
        transactionItem.setCreateDateTime(LocalDateTime.now());
        transactionItem.setCreateUser(userService.findAllByRole(Role.ADMIN).iterator().next());
        transactionItem.setUser(userFraKareWeek.getUser());
        transactionItem.setDescription(userFraKareWeek.getFraKareWeek().getWeekNumber() + ". hét Fra Kare Streak");
        transactionItem.setAccount(Account.OTHER);
        transactionItem.setTransactionType(TransactionType.BMM_PERFECT_WEEK);

        return transactionItemService.save(transactionItem).getId();
    }

    public List<UserFraKareWeek> fetchByQuery(Long userId, Integer weekNumber, Boolean listened, Long teamId) {
        return repository.fetchByQuery(userId, weekNumber, listened, teamId, LocalDate.now().getYear());
    }

    public void createUserFraKareWeeks(FraKareWeek week) {
        for (User u : userService.getYouth()) {
            if (fetchByQuery(u.getId(), week.getWeekNumber(), null, null).isEmpty()) {
                UserFraKareWeek streak = createEntity();
                streak.setUser(u);
                streak.setListened(false);
                streak.setFraKareWeek(week);
                save(streak);
            }
        }
    }
}
