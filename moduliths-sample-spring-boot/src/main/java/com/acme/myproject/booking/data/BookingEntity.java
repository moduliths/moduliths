package com.acme.myproject.booking.data;

import com.acme.myproject.customer.Customer;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
public class BookingEntity {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne
  private Customer customer;

  @Column
  private String flightNumber;

}
