package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.goals.Goal;
import com.ktk.dukappservice.dto.GoalDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class GoalMapper extends BaseMapper<Goal, GoalDto> {

    public GoalMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    public GoalDto entityToDto(Goal entity) {
        GoalDto dto = new GoalDto();
        dto.setUserId(entity.getUser().getId());
        dto.setSeasonYear(entity.getSeason().getSeasonYear());
        dto.setGoal(entity.getGoal());
        dto.setUsername(entity.getUser().getFullName());
        dto.setId(entity.getId());
        return dto;
    }

    @Override
    public Goal dtoToEntity(GoalDto dto, Goal entity) {
        Goal goal = new Goal();
        goal.setUser(entity.getUser());
        goal.setSeason(entity.getSeason());
        goal.setId(entity.getId());
        goal.setGoal(dto.getGoal());
        return goal;
    }
}
