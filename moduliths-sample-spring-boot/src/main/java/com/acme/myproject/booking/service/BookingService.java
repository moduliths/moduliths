package com.acme.myproject.booking.service;

import com.acme.myproject.booking.data.BookingEntity;
import com.acme.myproject.booking.data.BookingRepository;
import com.acme.myproject.customer.data.Customer;
import com.acme.myproject.customer.data.CustomerRepository;
import com.acme.myproject.flight.data.Flight;
import com.acme.myproject.flight.service.FlightService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

  private BookingRepository bookingRepository;

  private CustomerRepository customerRepository;

  FlightService flightService;

  public BookingService(
      BookingRepository bookingRepository,
      CustomerRepository customerRepository, FlightService flightService) {
    this.bookingRepository = bookingRepository;
    this.customerRepository = customerRepository;
    this.flightService = flightService;
  }

  /**
   * Books the given flight for the given customer.
   */
  public BookingEntity bookFlight(Long customerId, String flightNumber) {

    Optional<Customer> customer = customerRepository.findById(customerId);
    if (!customer.isPresent()) {
      throw new CustomerDoesNotExistException(customerId);
    }

    Optional<Flight> flight = flightService.findFlight(flightNumber);
    if (!flight.isPresent()) {
      throw new FlightDoesNotExistException(flightNumber);
    }

    BookingEntity booking = BookingEntity.builder()
        .customer(customer.get())
        .flightNumber(flight.get().getFlightNumber())
        .build();

    return this.bookingRepository.save(booking);
  }

}
