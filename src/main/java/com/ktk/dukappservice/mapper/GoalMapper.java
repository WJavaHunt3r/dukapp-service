package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.goals.Goal;
import com.ktk.dukappservice.dto.GoalDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class GoalMapper extends BaseMapper<Goal, GoalDto> {

    private UserMapper userMapper;

    public GoalMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        this.userMapper = userMapper;
    }

    @Override
    public GoalDto entityToDto(Goal entity) {
        GoalDto dto = modelMapper.map(entity, GoalDto.class);
        dto.setUser(userMapper.entityToDto(entity.getUser()));
        return dto;
    }

    @Override
    public Goal dtoToEntity(GoalDto dto, Goal entity) {
        Goal goal =  modelMapper.map(dto, Goal.class);
        goal.setUser(entity.getUser());
        return goal;
    }
}
