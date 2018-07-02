package com.acme.myproject.booking.data;

import org.springframework.data.repository.CrudRepository;

public interface BookingRepository extends CrudRepository<BookingEntity, Long> {
}
