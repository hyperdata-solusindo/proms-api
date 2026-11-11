package com.hyperdata.proms.common.docs;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.identity.auth.controller.AuthController;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.sample.product.controller.ProductController;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        AuthController authController = new AuthController(null);
        Method loginMethod = authController.getClass().getMethod("login", LoginRequest.class);
        HandlerMethod authHandler = new HandlerMethod(authController, loginMethod);

        Operation authOp = new Operation();
        authOp.setTags(List.of("Authentication"));
        operationCustomizer.customize(authOp, authHandler);

        // Simulate sample product controller operation
        ProductController productController = new ProductController(null);
        Method productMethod = productController.getClass().getMethod("getProductById", UUID.class);
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
        assertThat(tagGroups).extracting(tg -> tg.get("name")).containsExactlyInAnyOrder("Identity", "Sample");
    }

    @Test
    @DisplayName("Given Hidden annotated controller, when operation customized, then it is ignored")
    void givenHiddenController_whenOperationCustomized_thenIgnored() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var operationCustomizer = config.globalErrorResponsesCustomizer();
        var openApiCustomizer = config.domainTagGroupsCustomizer();

        HiddenController hiddenController = new HiddenController();
        Method hiddenMethod = hiddenController.getClass().getMethod("hiddenEndpoint");
        HandlerMethod hiddenHandler = new HandlerMethod(hiddenController, hiddenMethod);

        Operation hiddenOp = new Operation();
        operationCustomizer.customize(hiddenOp, hiddenHandler);

        OpenAPI openAPI = config.customOpenAPI();
        openApiCustomizer.customise(openAPI);

        assertThat(openAPI.getExtensions()).isNull();
    }

    @Test
    @DisplayName("Given bodyless endpoint like logout, when operation customized, then 400 and 422 are not added")
    void givenBodylessEndpoint_whenOperationCustomized_then400And422AreNotAdded() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var operationCustomizer = config.globalErrorResponsesCustomizer();

        AuthController authController = new AuthController(null);
        Method logoutMethod = authController.getClass().getMethod("logout");
        HandlerMethod authHandler = new HandlerMethod(authController, logoutMethod);

        Operation logoutOp = new Operation();
        operationCustomizer.customize(logoutOp, authHandler);

        assertThat(logoutOp.getResponses()).containsKey("500");
        assertThat(logoutOp.getResponses()).doesNotContainKey("400");
    }

    @Hidden
    static class HiddenController {

        public void hiddenEndpoint() {
        }
    }
}
