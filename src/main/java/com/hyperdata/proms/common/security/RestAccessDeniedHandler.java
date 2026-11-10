package com.hyperdata.proms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.response.ErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
        HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
        throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorDetail errorDetail = new ErrorDetail(
            CommonErrorCode.FORBIDDEN, "Anda tidak memiliki izin untuk mengakses resource ini");
        RestResponse<Void> restResponse = RestResponse.error(
            HttpStatus.FORBIDDEN, "Akses ditolak", errorDetail);
        response.getWriter().write(objectMapper.writeValueAsString(restResponse));
    }
}
