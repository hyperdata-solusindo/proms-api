package com.hyperdata.proms.common.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResponseTest {

    @Test
    @DisplayName("Given payload data, when calling ok(), then return envelope with 200")
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.ok("test-data", "Operation successful");

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals("Operation successful", response.message());
        assertEquals("test-data", response.data());
    }

    @Test
    @DisplayName("Given error map, when calling error(), then return error envelope with status code")
    void shouldCreateErrorResponse() {
        Map<String, String> errors = Map.of("email", "Format invalid");
        ApiResponse<Map<String, String>> response = ApiResponse.error(HttpStatus.BAD_REQUEST, "Validation failed",
            errors);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Validation failed", response.message());
        assertEquals(errors, response.data());
    }

    @Test
    @DisplayName("Given error status without data, when calling error(), then return envelope with null data")
    void shouldCreateErrorWithoutData() {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.NOT_FOUND, "Not found");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
        assertEquals("Not found", response.message());
        assertNull(response.data());
    }
}
