package com.ktk.workhuservice.mapper;

import com.ktk.workhuservice.data.rounds.RoundService;
import com.ktk.workhuservice.data.userstatus.UserStatus;
import com.ktk.workhuservice.dto.UserStatusDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class UserStatusMapper extends BaseMapper<UserStatus, UserStatusDto> {
    private final RoundService roundService;

    public UserStatusMapper(ModelMapper modelMapper, RoundService roundService) {
        super(modelMapper);
        this.roundService = roundService;
    }

    @Override
    public UserStatusDto entityToDto(UserStatus entity) {
        UserStatusDto dto = modelMapper.map(entity, UserStatusDto.class);
        dto.setOnTrack(entity.getStatus() * 100 >= roundService.getLastRound().getMyShareGoal());
        dto.setLocalOnTrack(entity.getStatus() * 100 >= roundService.getLastRound().getLocalMyShareGoal());
        return dto;
    }

    @Override
    public UserStatus dtoToEntity(UserStatusDto dto, UserStatus entity) {
        UserStatus userStatus = modelMapper.map(dto, UserStatus.class);
        userStatus.setUser(entity.getUser());
        return userStatus;
    }
}
