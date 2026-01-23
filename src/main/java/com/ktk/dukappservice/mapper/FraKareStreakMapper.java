package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.userfrakarestreak.UserFraKareWeek;
import com.ktk.dukappservice.dto.UserFraKareWeekDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class FraKareStreakMapper extends BaseMapper<UserFraKareWeek, UserFraKareWeekDto> {
    private UserMapper userMapper;

    protected FraKareStreakMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        this.userMapper = userMapper;
    }

    @Override
    public UserFraKareWeekDto entityToDto(UserFraKareWeek entity) {
        UserFraKareWeekDto userFraKareWeekDto = modelMapper.map(entity, UserFraKareWeekDto.class);
        userFraKareWeekDto.setUser(userMapper.entityToDto(entity.getUser()));
        return userFraKareWeekDto;
    }

    @Override
    public UserFraKareWeek dtoToEntity(UserFraKareWeekDto dto, UserFraKareWeek entity) {
        UserFraKareWeek userFraKareWeek = modelMapper.map(dto, UserFraKareWeek.class);
        userFraKareWeek.setUser(entity.getUser());
        return userFraKareWeek;
    }
}
