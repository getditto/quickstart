package com.ditto.example.spring.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class QuickstartApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(
    final SpringApplicationBuilder application
  ) {
    return application.sources(QuickstartApplication.class);
  }

  public static void main(final String[] args) {
    SpringApplication.run(QuickstartApplication.class, args);
  }
}
