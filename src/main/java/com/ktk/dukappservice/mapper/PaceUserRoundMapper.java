package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.paceuserround.PaceUserRound;
import com.ktk.dukappservice.dto.PaceUserRoundDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class PaceUserRoundMapper extends BaseMapper<PaceUserRound, PaceUserRoundDto> {
    private UserMapper userMapper;

    protected PaceUserRoundMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        this.userMapper = userMapper;
    }

    @Override
    public PaceUserRoundDto entityToDto(PaceUserRound entity) {
        PaceUserRoundDto dto = modelMapper.map(entity, PaceUserRoundDto.class);
        dto.setUser(userMapper.entityToDto(entity.getUser()));
        return dto;

    }

    @Override
    public PaceUserRound dtoToEntity(PaceUserRoundDto dto, PaceUserRound entity) {
        return null;
    }
}
