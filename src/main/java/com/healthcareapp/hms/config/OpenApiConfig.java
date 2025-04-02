package com.healthcareapp.hms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Management System API")
                        .version("1.0")
                        .description("API documentation for the Healthcare Application managing patients, doctors, appointments, and medications.")
                        .termsOfService("http://example.com/terms/")
                        .contact(new Contact().name("Healthcare App Support").email("support@healthcareapp.com").url("http://healthcareapp.com/support"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
        // You can add SecuritySchemes here later for JWT/OAuth etc.
    }
}