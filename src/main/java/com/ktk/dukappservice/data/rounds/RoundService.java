package com.ktk.dukappservice.data.rounds;

import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Optional;

@Service
public class RoundService extends BaseService<Round, Long> {

    private final RoundRepository roundRepository;

    public RoundService(RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    public Iterable<Round> findAllByRoundYear(int year) {
        return fetchByQuery(year, true, null);
    }

    public Iterable<Round> fetchByQuery(int year, boolean isActive, Pageable pageable) {
        return roundRepository.fetchByQuery(year, isActive, pageable);
    }

    public Optional<Round> findRoundByDate(LocalDateTime dateTime) {
        return roundRepository.findRoundByDate(dateTime);
    }

    public Round getCurrentRound() {
        return roundRepository.getLastRound();
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

            r.setMyShareGoal((double) (weekNumber * 11));
            r.setLocalMyShareGoal((double) (weekNumber * 11));
            r.setRoundNumber(LocalDate.now().getMonthValue());
            r.setSeason(season);
            r.setUserRoundsCreated(false);
            r.setSamvirkGoal(0);

            return save(r);
        }
        return round.get();
    }
}
