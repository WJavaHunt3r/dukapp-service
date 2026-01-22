package com.ktk.workhuservice.data.rounds;

import com.ktk.workhuservice.data.seasons.Season;
import com.ktk.workhuservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Optional;

@Service
public class RoundService extends BaseService<Round, Long> {

    private RoundRepository roundRepository;

    public RoundService(RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    public Iterable<Round> findAllByRoundYear(int year) {
        return roundRepository.findAllBySeasonSeasonYearAndActiveRound(year, true);
    }

    public Optional<Round> findRoundByDate(LocalDateTime dateTime) {
        return roundRepository.findRoundByDate(dateTime);
    }

    public Round getLastRound() {
        return findRoundByDate(LocalDateTime.now()).orElse(roundRepository.getLastRound(LocalDate.now().getYear()));
    }

    @Override
    protected JpaRepository<Round, Long> getRepository() {
        return roundRepository;
    }

    @Override
    public Class<Round> getEntityClass() {
        return Round.class;
    }

    @Override
    public Round createEntity() {
        return new Round();
    }

    public Round createNextRound(Season season) {

        Optional<Round> round = findRoundByDate(LocalDateTime.now());
        if (round.isEmpty()) {

            LocalDate date = LocalDate.now();
            Optional<Round> previousRound = findRoundByDate(LocalDateTime.now().minusDays(3));
            Round r = new Round();
            int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            r.setStartDateTime(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0));
            LocalDateTime sat = r.getStartDateTime().plusDays(6);
            r.setEndDateTime(LocalDateTime.of(date.getYear(), date.getMonth(), date.getMonth().maxLength(), 23, 59));

            r.setFreezeDateTime(r.getEndDateTime().minusDays(1));
            r.setSamvirkChurchGoal(0);
            r.setActiveRound(true);
            r.setSamvirkOnTrackPoints(0);
            r.setSamvirkMaxPoints(0);

            r.setMyShareGoal(previousRound.get().getMyShareGoal() + 7.0);
            previousRound.ifPresentOrElse(
                    p -> r.setLocalMyShareGoal(p.getLocalMyShareGoal() + 10),
                    () -> r.setLocalMyShareGoal(r.getMyShareGoal()));
            r.setRoundNumber(weekNumber + 1);
            r.setSeason(season);
            r.setUserRoundsCreated(false);
            r.setSamvirkGoal(0);

            return save(r);
        }
        return round.get();
    }
}
