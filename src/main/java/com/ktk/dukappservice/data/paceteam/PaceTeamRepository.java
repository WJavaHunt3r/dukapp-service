package com.ktk.dukappservice.data.paceteam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaceTeamRepository extends JpaRepository<PaceTeam, Long> {

    List<PaceTeam> findByActive(Boolean active);
}
