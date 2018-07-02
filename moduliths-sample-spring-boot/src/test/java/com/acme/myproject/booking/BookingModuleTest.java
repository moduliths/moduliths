package com.acme.myproject.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.myproject.booking.service.BookingService;
import com.acme.myproject.booking.data.BookingRepository;
import com.acme.myproject.customer.data.CustomerRepository;
import com.acme.myproject.flight.service.FlightService;
import com.acme.myproject.flight.web.FlightController;
import de.olivergierke.moduliths.model.test.ModuleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ModuleTest
public class BookingModuleTest {

  @Autowired
  private ConfigurableApplicationContext applicationContext;

  private AssertableApplicationContext context;

  @MockBean
  private FlightService flightService;

  @Before
  public void setupContext() {
    this.context = AssertableApplicationContext.get(() -> this.applicationContext);
  }

  @Test
  public void bookingServiceIsLoaded() {
    assertThat(context).hasSingleBean(BookingService.class).withFailMessage(
        "BookingService has not been loaded into the ApplicationContext although it's part of the module under test.");
  }

  @Test
  public void bookingRepositoryIsLoaded() {
    assertThat(context).hasSingleBean(BookingRepository.class).withFailMessage(
        "BookingRepository has not been loaded into the ApplicationContext although it's part of the module under test.");
  }

  @Test
  public void customerRepositoryIsNotLoaded() {
    assertThat(context).doesNotHaveBean(CustomerRepository.class).withFailMessage(
        "CustomerRepository has been loaded into the ApplicationContext although it's part of another module.");
  }

  @Test
  public void flightControllerIsNotLoaded() {
    assertThat(context).doesNotHaveBean(FlightController.class).withFailMessage(
        "CustomerRepository has been loaded into the ApplicationContext although it's part of another module.");
  }

}
