package com.hyperdata.proms.common.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResponseTest {

    @Test
    @DisplayName("Given payload data, when calling ok(), then return success envelope with 200")
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.ok("test-data", "Operation successful");

        assertTrue(response.success());
        assertEquals(HttpStatus.OK.value(), response.status());
        assertEquals("Operation successful", response.message());
        assertEquals("test-data", response.data());
        assertNull(response.errors());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName(
            "Given error map, when calling error(), then return error envelope with status code")
    void shouldCreateErrorResponse() {
        Map<String, String> errors = Map.of("email", "Format invalid");
        ApiResponse<Void> response =
                ApiResponse.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);

        assertFalse(response.success());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("Validation failed", response.message());
        assertNull(response.data());
        assertEquals("Format invalid", response.errors().get("email"));
    }
}
