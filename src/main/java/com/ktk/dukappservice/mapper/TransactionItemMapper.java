package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.dto.TransactionItemDto;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class TransactionItemMapper extends BaseMapper<TransactionItem, TransactionItemDto> {
    protected TransactionItemMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    public TransactionItemDto entityToDto(TransactionItem entity) {
        TransactionItemDto dto = modelMapper.map(entity, TransactionItemDto.class);
        dto.setUser(modelMapper.map(entity.getUser(), UserDto.class));
        dto.setCreateUserId(entity.getCreateUser().getId());
        return dto;
    }

    @Override
    public TransactionItem dtoToEntity(TransactionItemDto dto, TransactionItem entity) {
        TransactionItem transaction = modelMapper.map(dto, TransactionItem.class);
        transaction.setUser(entity.getUser());
        transaction.setCreateUser(entity.getCreateUser());
        return transaction;
    }
}
