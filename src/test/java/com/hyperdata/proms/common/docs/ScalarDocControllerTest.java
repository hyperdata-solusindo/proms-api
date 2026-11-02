package com.hyperdata.proms.common.docs;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ScalarDocControllerTest {

    @InjectMocks
    private ScalarDocController scalarDocController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scalarDocController).build();
    }

    @Test
    @DisplayName("GIVEN GET /docs WHEN requested THEN returns 200 with HTML containing scalar script")
    void givenDocsEndpoint_whenGet_thenReturnsScalarHtml() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("api-reference")))
                .andExpect(content().string(containsString("@scalar/api-reference")));
    }
}
