package com.hyperdata.proms.common.docs;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class OpenApiConfigTest {

    @Test
    @DisplayName("Given controller in module package, when operation customized, then domain tag groups are populated")
    void givenControllerInModulePackage_whenOperationCustomized_thenDomainTagGroupsPopulated() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var operationCustomizer = config.globalErrorResponsesCustomizer();
        var openApiCustomizer = config.domainTagGroupsCustomizer();

        // Simulate identity auth controller operation
        com.hyperdata.proms.module.identity.auth.controller.AuthController authController = new com.hyperdata.proms.module.identity.auth.controller.AuthController(
            null);
        Method loginMethod = authController.getClass()
            .getMethod("login", com.hyperdata.proms.module.identity.auth.dto.LoginRequest.class);
        HandlerMethod authHandler = new HandlerMethod(authController, loginMethod);

        Operation authOp = new Operation();
        authOp.setTags(List.of("Authentication"));
        operationCustomizer.customize(authOp, authHandler);

        // Simulate sample product controller operation
        com.hyperdata.proms.module.sample.product.controller.ProductController productController = new com.hyperdata.proms.module.sample.product.controller.ProductController(
            null);
        Method productMethod = productController.getClass().getMethod("getProductById", java.util.UUID.class);
        HandlerMethod productHandler = new HandlerMethod(productController, productMethod);

        Operation productOp = new Operation();
        productOp.setTags(List.of("Products"));
        operationCustomizer.customize(productOp, productHandler);

        // Customize openApi
        OpenAPI openAPI = config.customOpenAPI();
        openApiCustomizer.customise(openAPI);

        assertThat(openAPI.getExtensions()).isNotNull();
        assertThat(openAPI.getExtensions()).containsKey("x-tagGroups");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tagGroups = (List<Map<String, Object>>) openAPI.getExtensions().get("x-tagGroups");

        assertThat(tagGroups).hasSize(2);

        Map<String, Object> identityGroup = tagGroups.stream()
            .filter(g -> "Identity".equals(g.get("name")))
            .findFirst()
            .orElseThrow();
        assertThat(identityGroup.get("tags")).isEqualTo(List.of("Authentication"));

        Map<String, Object> sampleGroup = tagGroups.stream()
            .filter(g -> "Sample".equals(g.get("name")))
            .findFirst()
            .orElseThrow();
        assertThat(sampleGroup.get("tags")).isEqualTo(List.of("Products"));
    }

    @Test
    @DisplayName("ScalarDocController must be annotated with @Hidden")
    void scalarDocController_mustBeAnnotatedWithHidden() {
        assertThat(ScalarDocController.class.isAnnotationPresent(Hidden.class)).isTrue();
    }
}
