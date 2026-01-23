package com.ktk.dukappservice.data.seasons;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class SeasonService extends BaseService<Season, Long> {

    private SeasonRepository seasonRepository;

    public SeasonService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    public Optional<Season> findBySeasonYear(Integer year) {
        return seasonRepository.findBySeasonYear(year);
    }

    public Season findCurrentSeason() {
        return seasonRepository.findCurrentSeason(LocalDate.now()).orElse(createSeasonForYear(LocalDate.now().getYear()));
    }

    @Override
    protected JpaRepository<Season, Long> getRepository() {
        return seasonRepository;
    }

    @Override
    public Class<Season> getEntityClass() {
        return Season.class;
    }

    @Override
    public Season createEntity() {
        return new Season();
    }

    public Season createSeasonForYear(Integer year) {
        Optional<Season> season = findBySeasonYear(year);
        if (season.isEmpty()) {
            Season s = new Season();
            s.setEndDate(LocalDate.of(year, 12, 31));
            s.setSeasonYear(year);
            s.setStartDate(LocalDate.of(year, 1, 1));
            return save(s);
        }
        return season.get();
    }
}
