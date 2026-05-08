package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.dto.UserStatusDto;
import org.springframework.stereotype.Service;

@Service
public class UserStatusMapper {

    public UserStatusMapper() {
    }

    public UserStatusDto entityToDto(UserStatus entity, Round round) {

        UserStatusDto dto = new UserStatusDto();

        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setGoal(entity.getGoal());
        dto.setTransition(entity.getTransition());
        dto.setTransactions(entity.getTransactions());
        dto.setOnTrack(entity.getStatus() * 100 >= round.getMyShareGoal());
        dto.setLocalOnTrack(entity.getStatus() * 100 >= round.getLocalMyShareGoal());
        dto.setName(entity.getUser().getFullName());
        dto.setSeasonYear(entity.getSeason().getSeasonYear());
        double roundGoal = round.getMyShareGoal() / 100 * entity.getGoal();
        int toOnTrack = (int) roundGoal - entity.getTransactions();
        dto.setToOnTrack(Math.max(toOnTrack, 0));
        return dto;
    }

}
