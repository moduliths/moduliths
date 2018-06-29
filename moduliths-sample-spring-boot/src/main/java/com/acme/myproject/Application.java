package com.acme.myproject;

import de.olivergierke.moduliths.Modulith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Modulith
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
