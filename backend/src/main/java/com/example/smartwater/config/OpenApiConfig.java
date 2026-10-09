package com.example.smartwater.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartWaterOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Smart Water Monitoring & Billing API").version("1.0.0")
                        .description("REST API for apartment water monitoring, tiered billing, cost distribution, alerts and invoices. "
                                + "Authenticate with POST /api/auth/login and click 'Authorize' with the returned JWT."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /** Adds the standard error responses to every documented operation. */
    @Bean
    public OpenApiCustomizer errorResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
            op.getResponses().addApiResponse("400", new ApiResponse().description("Validation error / invalid input"));
            op.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid JWT"));
            op.getResponses().addApiResponse("403", new ApiResponse().description("Role not allowed / not your data"));
            op.getResponses().addApiResponse("404", new ApiResponse().description("Resource not found"));
            op.getResponses().addApiResponse("409", new ApiResponse().description("Conflict (duplicate or locked state)"));
        }));
    }
}
