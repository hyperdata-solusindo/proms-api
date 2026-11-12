package com.hyperdata.proms.common.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.EdgeRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import com.hyperdata.proms.common.response.ErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PreAuthRateLimitingFilter extends OncePerRequestFilter {

    private final EdgeRateLimitResolver edgeResolver;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PreAuthRateLimitingFilter(EdgeRateLimitResolver edgeResolver, RateLimitService rateLimitService) {
        this(edgeResolver, rateLimitService, new ObjectMapper());
    }

    public PreAuthRateLimitingFilter(
        EdgeRateLimitResolver edgeResolver,
        RateLimitService rateLimitService,
        ObjectMapper objectMapper) {
        this.edgeResolver = edgeResolver;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        List<RateLimitTarget> acquiredTargets = new ArrayList<>(targets.size());

        try {
            for (RateLimitTarget target : targets) {
                RateLimitResult result = rateLimitService.tryAcquire(target);
                if (!result.allowed()) {
                    writeRateLimitError(response, result, target);
                    return;
                }
                acquiredTargets.add(target);
                response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
                response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, String.valueOf(result.remainingTokens()));
            }

            filterChain.doFilter(request, response);
        } finally {
            for (RateLimitTarget target : acquiredTargets) {
                rateLimitService.releaseInFlight(target);
            }
        }
    }

    private void writeRateLimitError(
        HttpServletResponse response,
        RateLimitResult result,
        RateLimitTarget target) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(RateLimitHeaders.RETRY_AFTER, String.valueOf(result.waitSeconds()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, "0");

        String message = "Terlalu banyak permintaan. Silakan coba lagi nanti.";
        String detail = "Batas permintaan terlampaui. Silakan tunggu " + result.waitSeconds()
            + " detik sebelum mencoba kembali.";

        ErrorDetail errorDetail = new ErrorDetail(CommonErrorCode.RATE_LIMIT_EXCEEDED, detail);
        RestResponse<Void> errorResponse = RestResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, errorDetail);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
