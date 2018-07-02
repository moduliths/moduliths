package com.acme.myproject.flight.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlightCheckerJob {

  @Scheduled(fixedDelay = 1000)
  public void checkFlights() {
    log.info("scheduled job running...");
  }

}
