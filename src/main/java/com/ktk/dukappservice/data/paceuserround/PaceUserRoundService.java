package com.ktk.dukappservice.data.paceuserround;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.enums.TransactionType;
import com.ktk.dukappservice.service.BaseService;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaceUserRoundService extends BaseService<PaceUserRound, Long> {
    private final PaceUserRoundRepository repository;
    private final TransactionItemService transactionItemService;
    private final RoundService roundService;
    private final MicrosoftService microsoftService;
    private final UserStatusService userStatusService;

    private final UserService userService;
    private final TransactionService transactionService;

    public PaceUserRoundService(PaceUserRoundRepository repository, TransactionItemService transactionItemService, RoundService roundService, MicrosoftService microsoftService, UserStatusService userStatusService, UserService userService, TransactionService transactionService) {
        this.repository = repository;
        this.transactionItemService = transactionItemService;
        this.roundService = roundService;
        this.microsoftService = microsoftService;
        this.userStatusService = userStatusService;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    public List<PaceUserRound> findByQuery(Long userId, Long roundId, Integer seasonYear, Long paceTeamId) {
        return repository.findByQuery(userId, roundId, seasonYear, paceTeamId);
    }

    public Optional<PaceUserRound> findByUserAndRound(User u, Round r) {
        return repository.findByUserAndRound(u, r);
    }

    public Integer countOnTrackByTeamAndRound(PaceTeam team, Round round) {
        return repository.countOnTrackByTeamAndRound(team, round);
    }

    public int countByRoundAndTeam(Round r, PaceTeam t) {
        return repository.countByRoundAndTeam(r, t);
    }

    public Integer calculatePaceTeamRoundCoins(PaceTeam t, Round round) {
        return repository.calculatePaceTeamRoundCoins(t, round);
    }

    public Double sumByUserAndSeason(User u, Integer seasonYear) {
        return repository.sumByUserAndSeason(u, seasonYear);
    }

    public void createAllPaceUserRounds(Round round) {
        for (UserStatus us : userStatusService.fetchByQuery(round.getSeason().getSeasonYear(), null)) {
            userStatusService.calculateUserStatus(us);
            Optional<PaceUserRound> pur = findByUserAndRound(us.getUser(), round);
            if (pur.isPresent()) {
                calculateUserRoundStatus(round, us.getUser());
            } else {
                save(createPaceUserRound(us.getUser(), round));
            }
        }
        round.setUserRoundsCreated(true);
    }

    public void calculateUserRoundStatus(User u) {
        calculateUserRoundStatus(roundService.getLastRound(), u);
    }

    public void calculateUserRoundStatus(Round r, User u) {
        findByUserAndRound(u, r).ifPresent(pur -> {
            calculateUserRoundStatus(pur);
            save(pur);
        });
    }

    public void createPaceUserRound(User u) {
        var usr = findByUserAndRound(u, roundService.getLastRound());
        if (usr.isPresent()) {
            calculateUserRoundStatus(usr.get());
            save(usr.get());
        } else {
            save(createPaceUserRound(u, roundService.getLastRound()));
        }
    }

    private PaceUserRound createPaceUserRound(User u, Round round) {
        PaceUserRound pur = new PaceUserRound();
        pur.setRound(round);
        pur.setUser(u);
        pur.setRoundCredits(0);
        pur.setRoundCoins(0.0);
        pur.setOnTrack(false);
        calculateUserRoundStatus(pur);

        return pur;
    }

    private int calculateCurrRoundMyShareGoal(Round round, User u) {
        Optional<UserStatus> us = userStatusService.findByUserId(u.getId(), round.getSeason().getSeasonYear());
        return us.map(userStatus -> Math.max(0, (int) Math.round(userStatus.getGoal() * (round.getLocalMyShareGoal() / 100)) - userStatus.getTransactions())).orElse(0);
    }

    private void calculateUserRoundStatus(PaceUserRound pur) {
        pur.setRoundMyShareGoal(calculateCurrRoundMyShareGoal(pur.getRound(), pur.getUser()));
        pur.setRoundCoins(0.0);
        Optional<UserStatus> us1 = userStatusService.findByUserId(pur.getUser().getId(), pur.getRound().getSeason().getSeasonYear());
        us1.ifPresent(userStatusService::calculateUserStatus);
        Optional<UserStatus> us = userStatusService.findByUserId(pur.getUser().getId(), pur.getRound().getSeason().getSeasonYear());
        String myShareOnTrackName = "MyShare On Track " + pur.getRound().getRoundNumber() + ". hét (" + pur.getRound().getSeason().getSeasonYear() + ")";
        if (pur.getRound() == roundService.getLastRound() && us.isPresent()) {
            if (us.get().getStatus() * 100 >= pur.getRound().getLocalMyShareGoal() && !pur.isMyShareOnTrackPoints()) {
                pur.setOnTrack(true);
            } else if (us.get().getStatus() * 100 < pur.getRound().getLocalMyShareGoal() && pur.isMyShareOnTrackPoints()) {
                pur.setOnTrack(false);
            }
        }
        Integer credits = transactionItemService.sumCreditsByUserAndRound(pur.getUser(), pur.getRound());
        pur.setRoundCredits(credits == null ? 0 : credits);
        userService.save(pur.getUser());
    }

    @Override
    protected JpaRepository<PaceUserRound, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<PaceUserRound> getEntityClass() {
        return PaceUserRound.class;
    }

    @Override
    public PaceUserRound createEntity() {
        return new PaceUserRound();
    }

    @Scheduled(cron = "0 0 17 * * TUE")
    private void sendOnTrackEmails() {
        Round currentRound = roundService.getLastRound();
        for (UserStatus u : userStatusService.fetchByQuery(LocalDate.now().getYear(), null)) {
            Optional<PaceUserRound> ur = repository.findByUserAndRound(u.getUser(), currentRound);
            if (ur.isPresent()) {
                if (!ur.get().isOnTrack() && !u.getUser().getEmail().isEmpty()) {
                    try {
//                        if (u.getId() == 255) {
                        microsoftService.sendStatusUpdate(u.getTransactions(), u.getStatus() * 100, ur.get().getRoundMyShareGoal(), u.getUser(), currentRound);
//                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void saveOnTrackItems(TransactionItem transactionItem, String description) {
        Optional<Transaction> onTrackTransaction = transactionService.findByName(description);
        if (onTrackTransaction.isEmpty()) {
            Transaction newTransaction = new Transaction();
            newTransaction.setName(description);
            newTransaction.setAccount(Account.OTHER);
            newTransaction.setCreateDateTime(LocalDateTime.now());
            newTransaction.setCreateUser(userService.findAllByRole(Role.ADMIN).iterator().next());
            Transaction savedTransaction = transactionService.save(newTransaction);

            transactionItem.setTransactionId(savedTransaction.getId());
        } else {
            transactionItem.setTransactionId(onTrackTransaction.get().getId());
        }

        transactionItemService.save(transactionItem);
    }

    private TransactionItem createOnTrackTransactionItem(User u, Round r, String description, double points) {
        TransactionItem item = new TransactionItem();
        item.setAccount(Account.OTHER);
        item.setCredit(0);
        item.setHours(0);
        item.setUser(u);
        item.setRound(r);
        item.setPoints(points);
        item.setTransactionDate(LocalDate.now());
        item.setCreateDateTime(LocalDateTime.now());
        item.setTransactionType(TransactionType.POINT);
        item.setDescription(description);
        item.setCreateUser(userService.findAllByRole(Role.ADMIN).iterator().next());
        return item;
    }

    public void deleteByUserAndSeason(User user, Season season) {

    }
}
