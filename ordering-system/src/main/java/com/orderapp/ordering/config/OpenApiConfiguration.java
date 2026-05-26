package com.orderapp.ordering.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration per Swagger/SpringDoc
 * 
 * Documenta automaticamente tutti gli endpoint REST
 * Accessibile a: http://localhost:8080/swagger-ui.html
 *                http://localhost:8080/v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "OrderApp API",
        version = "1.0.0",
        description = "Multi-tenant ordering system API for venues (restaurants, bars, beach clubs)",
        contact = @Contact(
            name = "OrderApp Support",
            email = "support@orderapp.com"
        ),
        license = @License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.orderapp.com",
            description = "Production Server"
        )
    },
    security = @SecurityRequirement(name = "Bearer Token")
)
@SecurityScheme(
    name = "Bearer Token",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token obtained from login endpoint"
)
public class OpenApiConfiguration {
}
