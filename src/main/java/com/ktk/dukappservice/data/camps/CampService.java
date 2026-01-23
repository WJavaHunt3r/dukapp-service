package com.ktk.dukappservice.data.camps;

import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CampService extends BaseService<Camp, Long> {

    private CampRepository campRepository;
    private SeasonService seasonService;

    public CampService(CampRepository campRepository, SeasonService seasonService) {
        this.campRepository = campRepository;
        this.seasonService = seasonService;
    }

    @Override
    protected JpaRepository<Camp, Long> getRepository() {
        return campRepository;
    }

    public List<Camp> findAllBySeason(Season season) {
        return campRepository.findAllBySeason(season);
    }

    @Override
    public Class<Camp> getEntityClass() {
        return Camp.class;
    }

    @Override
    public Camp createEntity() {
        return new Camp();
    }

    public Camp createPaskeCamp() {
        Optional<Camp> camp = campRepository.findByCampName("Påske Camp");
        if (camp.isPresent()) {
            return camp.get();
        }

        Camp paskeCamp = createEntity();
        paskeCamp.setCampDate(LocalDate.of(2025, 4, 12));
        paskeCamp.setCampName("Påske Camp");
        paskeCamp.setFinanceCheckDate(LocalDate.of(2025, 4, 1));
        paskeCamp.setO18BrunstadFee(100000);
        paskeCamp.setO18LocalFee(160000);
        paskeCamp.setU18BrunstadFee(60000);
        paskeCamp.setU18LocalFee(100000);
        Optional<Season> season = seasonService.findBySeasonYear(2025);
        if (season.isEmpty()) {
            paskeCamp.setSeason(seasonService.createSeasonForYear(LocalDate.now().getYear()+1));
        } else {
            season.ifPresent(paskeCamp::setSeason);
        }
        return save(paskeCamp);
    }
}
