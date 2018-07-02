package com.acme.myproject.booking.data;

import static org.assertj.core.api.Java6Assertions.assertThat;

import de.olivergierke.moduliths.model.test.ModuleTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@ModuleTest
public class BookingRepositoryTest {

  @Autowired
  private BookingRepository bookingRepository;

  @Test
  public void test(){
    assertThat(bookingRepository.count()).isEqualTo(0);
  }

}