package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itis.semestr_work3.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

}
