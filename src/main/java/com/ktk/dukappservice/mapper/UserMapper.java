package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class UserMapper extends BaseMapper<User, UserDto> {

    public UserMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    /** Copies everything, including the family links. Only for callers who may manage users. */
    public User dtoToEntity(UserDto dto, User user) {
        return dtoToEntity(dto, user, true);
    }

    /**
     * @param mayChangeFamily false when users edit their own profile: familyId and spouseId then stay as they are,
     *                        since other features (job registration for children, family status) trust them.
     */
    public User dtoToEntity(UserDto dto, User user, boolean mayChangeFamily) {
        user.setPaceTeam(dto.getPaceTeam());
        user.setBirthDate(dto.getBirthDate());
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        user.setLastname(dto.getLastname());
        user.setFirstname(dto.getFirstname());
        user.setBaseMyShareCredit(dto.getBaseMyShareCredit());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBufeId(dto.getBufeId());
        if (mayChangeFamily) {
            user.setFamilyId(dto.getFamilyId());
            user.setSpouseId(dto.getSpouseId());
        }

        return user;
    }

    public UserDto entityToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setRoleNames(user.getRoles().stream().map(AppRole::getName).sorted().toList());
        dto.setPermissions(user.getPermissions());
        dto.setProfileIncomplete(user.isProfileIncomplete());
        return dto;
    }
}
