package com.acme.myproject.booking.web;

import com.acme.myproject.booking.business.BookingService;
import com.acme.myproject.booking.data.BookingEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

  private BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @PostMapping("/booking")
  public ResponseEntity<BookingResultResource> bookFlight(
          @RequestParam("customerId") Long customerId,
          @RequestParam("flightNumber") String flightNumber) {
    BookingEntity booking = bookingService.bookFlight(customerId, flightNumber);
    BookingResultResource bookingResult = BookingResultResource.builder()
            .success(true)
            .build();
    return ResponseEntity.ok(bookingResult);
  }

  @GetMapping("/hello")
  public ResponseEntity<String> helloWorld() {
    return ResponseEntity.ok("Hello World");
  }

}
