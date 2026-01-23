package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class UserMapper extends BaseMapper<User, UserDto> {

    public UserMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    public User dtoToEntity(UserDto dto, User user) {
        user.setRole(dto.getRole());
        user.setPaceTeam(dto.getPaceTeam());
        user.setBirthDate(dto.getBirthDate());
        user.setLastname(dto.getLastname());
        user.setFirstname(dto.getFirstname());
        user.setBaseMyShareCredit(dto.getBaseMyShareCredit());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBufeId(dto.getBufeId());
        user.setFamilyId(dto.getFamilyId());
        user.setSpouseId(dto.getSpouseId());

        return user;
    }

    public UserDto entityToDto(User user) {
        return modelMapper.map(user, UserDto.class);
    }
}
