package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.mentormentee.MentorMentee;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.MentorMenteeDto;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;

@Service
public class MentorMenteeMapper extends BaseMapper<MentorMentee, MentorMenteeDto> {

    protected MentorMenteeMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        TypeMap<MentorMentee, MentorMenteeDto> propertyMapper = modelMapper.createTypeMap(MentorMentee.class, MentorMenteeDto.class);
        Converter<User, UserDto> userToDto = c -> userMapper.entityToDto(c.getSource());
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(MentorMentee::getMentor, MentorMenteeDto::setMentor)
        );
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(MentorMentee::getMentee, MentorMenteeDto::setMentee)
        );
    }

    @Override
    public MentorMenteeDto entityToDto(MentorMentee entity) {
        return modelMapper.map(entity, MentorMenteeDto.class);
    }

    @Override
    public MentorMentee dtoToEntity(MentorMenteeDto dto, MentorMentee entity) {
        return entity;
    }
}
