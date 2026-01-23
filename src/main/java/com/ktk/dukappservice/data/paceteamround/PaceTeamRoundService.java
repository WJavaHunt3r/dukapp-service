package com.ktk.dukappservice.data.paceteamround;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.paceteam.PaceTeamService;
import com.ktk.dukappservice.data.paceuserround.PaceUserRound;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundService;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaceTeamRoundService extends BaseService<PaceTeamRound, Long> {
    private final PaceTeamRoundRepository repository;
    private final PaceTeamService paceTeamService;
    private final RoundService roundService;
    private final PaceUserRoundService paceUserRoundService;
    private final SeasonService seasonService;
    private final TransactionItemService transactionItemService;

    public PaceTeamRoundService(PaceTeamRoundRepository repository, PaceTeamService paceTeamService, RoundService roundService, PaceUserRoundService paceUserRoundService, SeasonService seasonService, TransactionItemService transactionItemService) {
        this.repository = repository;
        this.paceTeamService = paceTeamService;
        this.roundService = roundService;
        this.paceUserRoundService = paceUserRoundService;
        this.seasonService = seasonService;
        this.transactionItemService = transactionItemService;
    }

    public Iterable<PaceTeamRound> findAllActiveTeamRounds(int seasonYear) {
        return findAllActiveTeamRounds(seasonYear, null);
    }

    public Iterable<PaceTeamRound> findAllActiveTeamRounds(int seasonYear, PaceTeam team) {
        return repository.findAllActiveBySeasonYear(seasonYear, team);
    }

    public void calculateAllTeamRoundPoints() {
//        calculateAllTeamRoundPoints(roundService.getLastRound());
        for(PaceUserRound pur : paceUserRoundService.findByQuery(null, roundService.getLastRound().getId(), null, null)){
            paceUserRoundService.calculateUserRoundStatus(pur.getRound(), pur.getUser());

        }

    }

    public void calculateAllTeamAllRoundPoints() {
        for (Round r : roundService.findAllByRoundYear(2025)) {
            paceUserRoundService.createAllPaceUserRounds(r);
            for (PaceTeamRound pct : repository.findAllByRound(r)) {
                calculateRoundPointsForTeam(pct);
            }
        }
    }

    public void calculateAllTeamRoundPoints(Round round) {
        paceUserRoundService.createAllPaceUserRounds(round);
        for (PaceTeamRound pct : repository.findAllByRound(round)) {
            calculateRoundPointsForTeam(pct);
        }
    }

    private void calculateRoundPointsForTeam(PaceTeamRound ptr) {
        Integer sumCoins = paceUserRoundService.calculatePaceTeamRoundCoins(ptr.getTeam(), ptr.getRound());
        int teamMemberCount = paceUserRoundService.countByRoundAndTeam(ptr.getRound(), ptr.getTeam());
        Double sumHours = transactionItemService.sumHoursByTeamAndRound(ptr.getTeam(), ptr.getRound());
        Integer sumCredits = transactionItemService.sumCreditsByTeamAndRound(ptr.getTeam(), ptr.getRound());
        Integer onTrack = paceUserRoundService.countOnTrackByTeamAndRound(ptr.getTeam(), ptr.getRound());

        ptr.setTeamHours(sumHours == null ? 0 : sumHours);
        ptr.setPayments(sumCredits == null ? 0 : sumCredits);
        ptr.setOnTrack(onTrack == null ? 0 : onTrack);
        ptr.setMaxTeamRoundCoins(teamMemberCount);
        ptr.setTeamRoundCoins(sumCoins == null ? 0 : sumCoins);
        ptr.setTeamRoundStatus(sumCoins == null ? 0 : (double) sumCoins / (double) teamMemberCount);
        save(ptr);
    }

    private PaceTeamRound createTeamRound(PaceTeam t, Round r) {
        PaceTeamRound teamRound = new PaceTeamRound();
        teamRound.setTeam(t);
        teamRound.setRound(r);
        teamRound.setTeamRoundCoins(0);
        return save(teamRound);
    }

    @Override
    protected JpaRepository<PaceTeamRound, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<PaceTeamRound> getEntityClass() {
        return PaceTeamRound.class;
    }

    @Override
    public PaceTeamRound createEntity() {
        return new PaceTeamRound();
    }

    //    @Scheduled(cron = "0 1 0 1 * ?")
    @Scheduled(cron = "10 0 0 1 * ?")
    public void createTeamRounds() {
        Season season = seasonService.findBySeasonYear(LocalDate.now().getYear()).orElseGet(() -> seasonService.createSeasonForYear(LocalDate.now().getYear()));
        Round currRound = roundService.findRoundByDate(LocalDateTime.now()).orElseGet(() -> roundService.createNextRound(season));

//        getWinnerTeam();
        paceUserRoundService.createAllPaceUserRounds(currRound);
        roundService.save(currRound);
        paceTeamService.findActiveTeams().forEach(paceTeam -> {
//            paceUserRoundService.createAllPaceUserRounds(currRound);
//            calculateRoundPointsForTeam(repository.findByTeamAndRound(paceTeam, currRound).orElseGet(() -> createTeamRound(paceTeam, currRound)));
//            roundService.save(currRound);
        });
    }

    private void getWinnerTeam() {
        Optional<Round> prevRound = roundService.findRoundByDate(LocalDateTime.now().minusDays(2));
        prevRound.ifPresent(r -> {
            if (!r.getActiveRound()) return;
            PaceTeamRound winner = repository.findTopByRoundOrderByTeamRoundStatusDesc(r);

            winner.getTeam().addCoins(1.0);
            r.setWinnerTeam(winner.getTeam());
            paceTeamService.save(winner.getTeam());
            roundService.save(r);
        });

    }

}
