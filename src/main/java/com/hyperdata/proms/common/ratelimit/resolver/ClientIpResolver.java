package com.hyperdata.proms.common.ratelimit.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    public String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            int commaIndex = xForwardedFor.indexOf(',');
            if (commaIndex > 0) {
                return xForwardedFor.substring(0, commaIndex).trim();
            }
            return xForwardedFor.trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
