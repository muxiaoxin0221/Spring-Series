package com.spring.schedulingtasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchedulingTaskApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchedulingTaskApplication.class, args);
  }

}
