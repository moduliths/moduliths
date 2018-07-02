package com.acme.myproject.booking.web;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResultResource {

  private boolean success;

}
