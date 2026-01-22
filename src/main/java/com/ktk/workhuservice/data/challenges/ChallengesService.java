package com.ktk.workhuservice.data.challenges;

import com.ktk.workhuservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ChallengesService extends BaseService<Challenges, Long> {

    private final ChallengesRepository repository;

    public ChallengesService(ChallengesRepository repository) {
        this.repository = repository;
    }

    public List<Challenges> fetchByQuery(String dateTime) {
        if (dateTime.isEmpty()) {
            return repository.fetchByQuery(null);
        }
        LocalDate date = LocalDate.parse(dateTime);
        return repository.fetchByQuery(date);
    }

    @Override
    protected JpaRepository<Challenges, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<Challenges> getEntityClass() {
        return Challenges.class;
    }

    @Override
    public Challenges createEntity() {
        return new Challenges();
    }
}
