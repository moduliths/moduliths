package com.acme.myproject.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.myproject.flight.web.FlightController;
import de.olivergierke.moduliths.model.test.ModuleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@WebMvcTest
@ModuleTest
public class BookingControllerTest {

  @Autowired
  private ConfigurableApplicationContext applicationContext;

  private AssertableApplicationContext context;

  @Before
  public void setupContext() {
    this.context = AssertableApplicationContext.get(() -> this.applicationContext);
  }

  @Test
  public void flightControllerIsNotLoaded() {
    assertThat(context).doesNotHaveBean(FlightController.class);
  }

}