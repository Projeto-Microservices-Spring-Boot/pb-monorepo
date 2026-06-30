package com.edu.infnet.pb.stickers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.edu.infnet.pb")
public class StickersApplication {

  public static void main(String[] args) {
    SpringApplication.run(StickersApplication.class, args);
  }

}
