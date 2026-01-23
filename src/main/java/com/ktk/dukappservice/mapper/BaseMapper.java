package com.ktk.dukappservice.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

abstract class BaseMapper<C, D> implements BaseMapperInterface<C, D>{

    ModelMapper modelMapper;

    protected BaseMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }
}
