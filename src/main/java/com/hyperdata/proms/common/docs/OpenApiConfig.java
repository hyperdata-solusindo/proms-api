package com.hyperdata.proms.common.docs;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.hyperdata.proms.common.response.ApiResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Proms ERP API Reference")
                .version("1.0.0")
                .description("Spesifikasi REST API Enterprise Proms ERP")
                .contact(new Contact().name("Hyperdata Solusindo").email("support@hyperdata.co.id")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
            .components(new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            var apiResponseSchema = new Schema<ApiResponse<?>>()
                .description("Envelope standar respon API");

            if (!responses.containsKey("400")) {
                responses.addApiResponse(
                    "400",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Format request tidak valid / validasi YAVI gagal")
                        .content(new Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            if (!responses.containsKey("500")) {
                responses.addApiResponse(
                    "500",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Kesalahan internal server")
                        .content(new Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            return operation;
        };
    }
}
