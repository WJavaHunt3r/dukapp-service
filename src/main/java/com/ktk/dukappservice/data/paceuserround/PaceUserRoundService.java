package com.ktk.dukappservice.data.paceuserround;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class PaceUserRoundService extends BaseService<PaceUserRound, Long> {
    private final PaceUserRoundRepository repository;
    private final TransactionItemService transactionItemService;
    private final RoundService roundService;
    private final UserStatusService userStatusService;
    private final UserService userService;

    public PaceUserRoundService(PaceUserRoundRepository repository, TransactionItemService transactionItemService, RoundService roundService, UserStatusService userStatusService, UserService userService) {
        this.repository = repository;
        this.transactionItemService = transactionItemService;
        this.roundService = roundService;
        this.userStatusService = userStatusService;
        this.userService = userService;
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

    public void createAllPaceUserRounds(Round round) {
        Page<UserStatus> statuses = userStatusService.fetchByQuery(round.getSeason().getSeasonYear(), null);

        List<CompletableFuture<Void>> futures = statuses.stream()
                .map(us -> CompletableFuture.runAsync(() -> {
                    // This now runs in a background Virtual Thread
                    processSingleUserRound(us, round);
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        round.setUserRoundsCreated(true);
        roundService.save(round); // Assuming you want to persist the 'true' state
    }

    private void processSingleUserRound(UserStatus us, Round round) {
        userStatusService.calculateUserStatus(us);
        Optional<PaceUserRound> pur = findByUserAndRound(us.getUser(), round);
        if (pur.isPresent()) {
            calculateUserRoundStatus(pur.get());
            save(pur.get());
        } else {
            save(createPaceUserRound(us.getUser(), round));
        }
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
        return userStatusService.findByUserId(u.getId(), round.getSeason().getSeasonYear())
                .map(userStatus -> {
                    double goalPercentage = round.getLocalMyShareGoal() / 100.0;
                    int targetAmount = (int) Math.round(userStatus.getGoal() * goalPercentage);
                    return Math.max(0, targetAmount - userStatus.getTransactions());
                }).orElse(0);
    }

    private void calculateUserRoundStatus(PaceUserRound pur) {
        // 1. Fetch the necessary data
        UserStatus status = userStatusService.findByUserId(pur.getUser().getId(), pur.getRound().getSeason().getSeasonYear())
                .orElseThrow(() -> new RuntimeException("Status not found"));

        Integer credits = transactionItemService.sumCreditsByUserAndRound(pur.getUser(), pur.getRound());

        RoundStatusCalculator calculator = new RoundStatusCalculator(
                status.getGoal(),
                status.getTransactions(),
                pur.getRound().getLocalMyShareGoal(),
                status.getStatus(),
                credits == null ? 0 : credits
        );

        pur.setRoundMyShareGoal(calculator.calculateMyShareGoal());
        pur.setOnTrack(calculator.isOnTrack());
        pur.setRoundCredits(calculator.resolveCredits());
        pur.setRoundCoins(0.0); // Reset or apply logic as needed

        // 4. Save
        userService.save(pur.getUser());
    }

    public int getOnTrackCountByRound(Round r){
        return repository.countByRoundAndOnTrack(r, true);
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

}
