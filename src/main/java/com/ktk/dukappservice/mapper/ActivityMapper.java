package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.dto.ActivityDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityMapper extends BaseMapper<Activity, ActivityDto> {

    protected ActivityMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    public ActivityDto entityToDto(Activity entity) {
        var dto = modelMapper.map(entity, ActivityDto.class);
        dto.setCreateUserId(entity.getCreateUser().getId());
        dto.setCreateUserName(entity.getCreateUser().getFullName());
        dto.setEmployerId(entity.getEmployer().getId());
        dto.setEmployerName(entity.getEmployer().getFullName());
        dto.setResponsibleId(entity.getResponsible().getId());
        dto.setResponsibleName(entity.getResponsible().getFullName());
        return dto;
    }

    @Override
    public Activity dtoToEntity(ActivityDto dto, Activity entity) {
        Activity activity = modelMapper.map(dto, Activity.class);
        activity.setEmployer(entity.getEmployer());
        activity.setResponsible(entity.getResponsible());
        activity.setCreateUser(entity.getCreateUser());
        if (activity.getCreateDateTime() == null) {
            activity.setCreateDateTime(LocalDateTime.now());
        }

        return activity;
    }
}
