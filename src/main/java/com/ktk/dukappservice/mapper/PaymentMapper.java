package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.payments.Payment;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.PaymentDto;
import com.ktk.dukappservice.dto.UserDto;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;

@Service
public class PaymentMapper extends BaseMapper<Payment, PaymentDto> {
    protected PaymentMapper(ModelMapper modelMapper, UserMapper userMapper) {
        super(modelMapper);
        TypeMap<Payment, PaymentDto> propertyMapper = modelMapper.createTypeMap(Payment.class, PaymentDto.class);
        Converter<User, UserDto> userToDto = c -> c.getSource() == null ? null : userMapper.entityToDto(c.getSource());
        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(Payment::getUser, PaymentDto::setUser)
        );

        propertyMapper.addMappings(
                mapper -> mapper.using(userToDto).map(Payment::getRecipient, PaymentDto::setRecipient)
        );
    }

    @Override
    public PaymentDto entityToDto(Payment entity) {
        return modelMapper.map(entity, PaymentDto.class);
    }

    @Override
    public Payment dtoToEntity(PaymentDto dto, Payment entity) {
        Payment payment = modelMapper.map(dto, Payment.class);
        payment.setUser(entity.getUser());
        payment.setRecipient(entity.getRecipient());
        return payment;
    }
}
