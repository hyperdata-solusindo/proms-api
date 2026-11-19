package com.hyperdata.proms.common.docs;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.docs.annotation.ApiConcurrencyControl;
import com.hyperdata.proms.common.docs.annotation.ApiResponseNotFound;
import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import com.hyperdata.proms.module.identity.auth.controller.AuthController;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.work.board.controller.BoardController;
import com.hyperdata.proms.module.work.board.dto.UpdateBoardRequest;
import com.hyperdata.proms.module.sample.product.controller.ProductController;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Method;
import java.time.Instant;
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

    @Test
    @DisplayName("Given method with ApiConcurrencyControl and ApiResponseNotFound, when customized, then 404, 409, 428 and If-Match param are added")
    void givenApiConcurrencyControlAndNotFound_whenCustomized_thenAllResponsesAndParamAdded() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var operationCustomizer = config.globalErrorResponsesCustomizer();

        DummyConcurrencyController controller = new DummyConcurrencyController();
        Method method = controller.getClass().getMethod("updateResource");
        HandlerMethod handler = new HandlerMethod(controller, method);

        Operation op = new Operation();
        operationCustomizer.customize(op, handler);

        assertThat(op.getResponses()).containsKey("404");
        assertThat(op.getResponses().get("404").getDescription()).isEqualTo("Resource tidak ditemukan");

        assertThat(op.getResponses()).containsKey("409");
        assertThat(op.getResponses().get("409").getDescription()).contains("STALE_VERSION");

        assertThat(op.getResponses()).containsKey("428");
        assertThat(op.getResponses().get("428").getDescription()).contains("IF_MATCH_REQUIRED");

        assertThat(op.getParameters()).isNotNull().hasSize(1);
        var param = op.getParameters().get(0);
        assertThat(param.getName()).isEqualTo("If-Match");
        assertThat(param.getIn()).isEqualTo("header");
        assertThat(param.getRequired()).isTrue();
        assertThat(param.getExamples()).containsKey("Epoch Milliseconds");
        assertThat(param.getExamples()).containsKey("ISO-8601 Timestamp");
    }

    @Test
    @DisplayName("Given BoardController implementing BoardApi with ApiConcurrencyControl, then interface annotations are inherited")
    void givenBoardController_whenCustomized_thenInterfaceAnnotationsInherited() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var operationCustomizer = config.globalErrorResponsesCustomizer();

        BoardController boardController = new BoardController(null);
        Method updateMethod = boardController.getClass()
            .getMethod(
                "updateBoard",
                UUID.class,
                UpdateBoardRequest.class);
        HandlerMethod handler = new HandlerMethod(boardController, updateMethod);

        Operation op = new Operation();
        operationCustomizer.customize(op, handler);

        assertThat(op.getResponses()).containsKey("404");
        assertThat(op.getResponses()).containsKey("409");
        assertThat(op.getResponses()).containsKey("428");

        assertThat(op.getParameters()).anyMatch(p -> "If-Match".equalsIgnoreCase(p.getName())
            && "header".equalsIgnoreCase(p.getIn())
            && Boolean.TRUE.equals(p.getRequired()));
    }

    @Test
    @DisplayName("Given property with VersionEpochSchema, when customized, then integer/int64 and epoch example are set")
    void givenPropertyWithVersionEpochSchema_whenCustomized_thenSchemaIsEpochInt64() throws Exception {
        OpenApiConfig config = new OpenApiConfig();
        var propertyCustomizer = config.versionEpochPropertyCustomizer();

        var schema = new Schema<Object>();
        var annotations = DummyVersionDto.class.getDeclaredField("version").getAnnotations();
        var annotatedType = new AnnotatedType().ctxAnnotations(annotations);

        propertyCustomizer.customize(schema, annotatedType);

        assertThat(schema.getType()).isEqualTo("integer");
        assertThat(schema.getFormat()).isEqualTo("int64");
        assertThat(schema.getExample()).isEqualTo(1731301234000L);
        assertThat(schema.getDescription()).contains("Epoch Milliseconds");
    }

    static class DummyVersionDto {

        @VersionEpochSchema
        private Instant version;
    }

    static class DummyConcurrencyController {

        @ApiResponseNotFound
        @ApiConcurrencyControl
        public void updateResource() {
        }
    }

    @Hidden
    static class HiddenController {

        public void hiddenEndpoint() {
        }
    }
}
