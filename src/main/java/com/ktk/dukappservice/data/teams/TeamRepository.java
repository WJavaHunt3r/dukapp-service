package com.ktk.dukappservice.data.teams;

import com.ktk.dukappservice.enums.TeamColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findTeamByColor(TeamColor c);

    Optional<Team> findTeamByTeamLeaderMyShareId(Long id);
}
