package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.ActivityDto;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityMapper extends BaseMapper<Activity, ActivityDto> {

    protected ActivityMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        TypeMap<Activity, ActivityDto> propertyMapper = modelMapper.createTypeMap(Activity.class, ActivityDto.class);
        Converter<User, UserDto> userToDto = c -> userMapper.entityToDto(c.getSource());
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(Activity::getCreateUser, ActivityDto::setCreateUser)
        );
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(Activity::getEmployer, ActivityDto::setEmployer)
        );
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(Activity::getResponsible, ActivityDto::setResponsible)
        );
    }

    @Override
    public ActivityDto entityToDto(Activity entity) {
        return modelMapper.map(entity, ActivityDto.class);
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
