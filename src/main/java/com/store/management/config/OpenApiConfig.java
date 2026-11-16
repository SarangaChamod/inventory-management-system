package com.store.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata. Swagger UI: /swagger-ui/index.html, raw spec: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storeManagementOpenApi(@Value("${info.app.version:1.0.0}") String version) {
        return new OpenAPI().info(new Info()
                .title("Store Management API")
                .description("REST API for managing products, categories and inventory. "
                        + "Errors are returned as a consistent JSON body (ApiErrorResponse).")
                .version(version)
                .license(new License().name("For academic use")));
    }
}
