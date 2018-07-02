package com.acme.myproject.booking.service;

class FlightDoesNotExistException extends RuntimeException {

  FlightDoesNotExistException(String flightNumber) {
    super(String.format("A flight with ID '%d' doesn't exist!", flightNumber));
  }

}
