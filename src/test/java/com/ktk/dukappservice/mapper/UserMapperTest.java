package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper(new ModelMapper());

    private static User user() {
        User user = new User();
        user.setFirstname("Anna");
        user.setLastname("Kis");
        user.setFamilyId(10L);
        user.setSpouseId(11L);
        return user;
    }

    private static UserDto dto() {
        UserDto dto = new UserDto();
        dto.setFirstname("Anna");
        dto.setLastname("Nagy");
        dto.setFamilyId(99L);
        dto.setSpouseId(98L);
        return dto;
    }

    @Test
    void selfEditKeepsTheFamilyLinksButChangesTheName() {
        User user = mapper.dtoToEntity(dto(), user(), false);

        assertThat(user.getLastname()).isEqualTo("Nagy");
        assertThat(user.getFamilyId()).isEqualTo(10L);
        assertThat(user.getSpouseId()).isEqualTo(11L);
    }

    @Test
    void userManagersCanChangeTheFamilyLinks() {
        User user = mapper.dtoToEntity(dto(), user(), true);

        assertThat(user.getFamilyId()).isEqualTo(99L);
        assertThat(user.getSpouseId()).isEqualTo(98L);
    }
}
