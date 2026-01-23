package com.ktk.dukappservice.data.paceteam;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaceTeamService extends BaseService<PaceTeam, Long> {
    private PaceTeamRepository repository;

    public PaceTeamService(PaceTeamRepository repository) {
        this.repository = repository;
    }

    public List<PaceTeam> findActiveTeams(boolean isActive) {
        return repository.findByActive(isActive);
    }

    public List<PaceTeam> findActiveTeams() {
        return findActiveTeams(true);
    }

    @Override
    protected JpaRepository<PaceTeam, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<PaceTeam> getEntityClass() {
        return PaceTeam.class;
    }

    @Override
    public PaceTeam createEntity() {
        return new PaceTeam();
    }
}
