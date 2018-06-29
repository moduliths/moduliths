package com.acme.myproject.flight.service;

import com.acme.myproject.flight.data.Flight;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FlightService {

  public Optional<Flight> findFlight(String flightNumber) {
    return Optional.of(Flight.builder()
        .airline("Oceanic")
        .flightNumber("815")
        .build());
  }

}
