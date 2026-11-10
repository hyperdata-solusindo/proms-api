package com.hyperdata.proms.common.docs;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.hyperdata.proms.common.response.RestResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
    private static final String MODULE_PACKAGE_PREFIX = "com.hyperdata.proms.module.";

    private final Map<String, Set<String>> domainTagMap = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

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
            collectDomainTags(operation, handlerMethod);

            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            var apiResponseSchema = new Schema<RestResponse<?>>()
                .description("Envelope standar respon API");

            boolean hasRequestBody = operation.getRequestBody() != null
                || java.util.Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class));

            if (hasRequestBody && !responses.containsKey("400")) {
                var badRequestMedia = new MediaType().schema(apiResponseSchema);
                badRequestMedia.addExamples(
                    "BadRequestExample",
                    new Example()
                        .summary("Format payload JSON tidak valid")
                        .value(
                            """
                                {
                                  "statusCode": 400,
                                  "message": "Format data tidak valid",
                                  "data": null,
                                  "error": {
                                    "code": "BAD_REQUEST",
                                    "detail": "Format payload request JSON tidak valid atau rusak"
                                  }
                                }
                                """));

                responses.addApiResponse(
                    "400",
                    new ApiResponse()
                        .description("Format request tidak valid (Malformed JSON)")
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE, badRequestMedia)));
            }

            if (hasRequestBody && !responses.containsKey("422")) {
                var validationMedia = new MediaType().schema(apiResponseSchema);
                validationMedia.addExamples(
                    "ValidationExample",
                    new Example()
                        .summary("Validasi gagal (YAVI / Field validation)")
                        .value(
                            """
                                {
                                  "statusCode": 422,
                                  "message": "Validation failed",
                                  "data": null,
                                  "error": {
                                    "code": "VALIDATION",
                                    "detail": "Periksa kembali field yang diisi.",
                                    "fields": [
                                      {
                                        "field": "email",
                                        "message": "Format email tidak valid"
                                      }
                                    ]
                                  }
                                }
                                """));

                responses.addApiResponse(
                    "422",
                    new ApiResponse()
                        .description("Kesalahan validasi atau aturan bisnis (Unprocessable Entity)")
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE, validationMedia)));
            }

            if (!responses.containsKey("500")) {
                var serverErrorMedia = new MediaType().schema(apiResponseSchema);
                serverErrorMedia.addExamples(
                    "InternalServerErrorExample",
                    new Example()
                        .summary("Kesalahan internal server")
                        .value(
                            """
                                {
                                  "statusCode": 500,
                                  "message": "An unexpected internal server error occurred",
                                  "data": null,
                                  "error": {
                                    "code": "INTERNAL_SERVER_ERROR",
                                    "detail": "Terjadi kesalahan internal pada server"
                                  },
                                  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                                }
                                """));

                responses.addApiResponse(
                    "500",
                    new ApiResponse()
                        .description("Kesalahan internal server")
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE, serverErrorMedia)));
            }

            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer domainTagGroupsCustomizer() {
        return openApi -> {
            if (!domainTagMap.isEmpty()) {
                List<Map<String, Object>> tagGroups = new ArrayList<>();
                domainTagMap.forEach((domain, tags) -> {
                    if (!tags.isEmpty()) {
                        List<String> sortedTags = new ArrayList<>(tags);
                        Collections.sort(sortedTags);
                        tagGroups.add(Map.of(
                            "name", domain,
                            "tags", sortedTags));
                    }
                });
                openApi.addExtension("x-tagGroups", tagGroups);
            }
        };
    }

    private void collectDomainTags(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getTags() == null || operation.getTags().isEmpty()) {
            return;
        }

        String packageName = handlerMethod.getBeanType().getPackageName();
        if (!packageName.startsWith(MODULE_PACKAGE_PREFIX)) {
            return;
        }

        String subPackage = packageName.substring(MODULE_PACKAGE_PREFIX.length());
        String[] parts = subPackage.split("\\.");
        if (parts.length < 1) {
            return;
        }

        String rawDomain = parts[0];
        String domainName = formatDomainName(rawDomain);

        domainTagMap.computeIfAbsent(domainName, k -> ConcurrentHashMap.newKeySet())
            .addAll(operation.getTags());
    }

    private String formatDomainName(String rawDomain) {
        if (rawDomain == null || rawDomain.isBlank()) {
            return rawDomain;
        }
        String[] words = rawDomain.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
