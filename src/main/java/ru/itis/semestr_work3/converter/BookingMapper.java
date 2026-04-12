package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.BookingDto;
import ru.itis.semestr_work3.entity.Booking;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaymentMapper.class, InsuranceMapper.class})
public interface BookingMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "car.id", target = "carId")
    @Mapping(source = "car.brand", target = "carBrand")
    @Mapping(source = "car.model", target = "carModel")
    @Mapping(source = "car.imagePath", target = "carImagePath")
    BookingDto toDto(Booking booking);

    List<BookingDto> toDtoList(List<Booking> bookings);
}