package com.edu.infnet.pb.store.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().info(new Info()
        .title("Microservice Store").description("Microservice de loja").version("v1.0.0")
        .contact(new Contact().name("Nathan Rodrigues").email("nathan.vieria@al.infnet.edu.br")))
        .servers(List.of(new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8088")))
        .externalDocs(new ExternalDocumentation()
            .description("Docs do Microservice de store").url("github"));
  }
}
