package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.userstatus.StatusTotals;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.dto.UserStatusDto;
import org.springframework.stereotype.Service;

@Service
public class UserStatusMapper {

    private final UserStatusService userStatusService;

    public UserStatusMapper(UserStatusService userStatusService) {
        this.userStatusService = userStatusService;
    }

    public UserStatusDto entityToDto(UserStatus entity, Round round) {

        UserStatusDto dto = new UserStatusDto();

        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setStatus(entity.getStatus());
        dto.setGoal(entity.getGoal());
        dto.setTransition(entity.getTransition());
        dto.setTransactions(entity.getTransactions());
        dto.setOnTrack(entity.getStatus() * 100 >= round.getMyShareGoal());
        dto.setLocalOnTrack(entity.getStatus() * 100 >= round.getLocalMyShareGoal());
        dto.setName(entity.getUser().getFullName());
        dto.setSeasonYear(entity.getSeason().getSeasonYear());
        StatusTotals totals = userStatusService.getStatusTotals(entity);
        double roundGoal = round.getMyShareGoal() / 100 * totals.goal();
        double localRoundGoal = round.getLocalMyShareGoal() / 100 * totals.goal();
        int toOnTrack = (int) roundGoal - totals.transactions();
        int toLocalOnTrack = (int) localRoundGoal - totals.transactions();
        dto.setToOnTrack(Math.max(toOnTrack, 0));
        dto.setToLocalOnTrack(Math.max(toLocalOnTrack, 0));
        return dto;
    }

}
