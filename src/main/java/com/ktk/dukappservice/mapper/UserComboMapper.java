package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.UserComboDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class UserComboMapper extends BaseMapper<User, UserComboDto> {

    public UserComboMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    public User dtoToEntity(UserComboDto dto, User user) {

        return user;
    }

    public UserComboDto entityToDto(User user) {
        var dto = new UserComboDto();
        dto.setFirstname(user.getFirstname());
        dto.setId(user.getId());
        dto.setLastname(user.getLastname());
        dto.setAge(user.getAge());
        dto.setId(user.getId());
        dto.setChurchName(user.getChurch().getChurchName());
        dto.setComboText("%s - %d".formatted(user.getFullName(), user.getAge()));
        return dto;
    }
}
