package com.hyperdata.proms.common.ratelimit.resolver;

import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface RateLimitKeyResolver {

    List<RateLimitTarget> resolve(HttpServletRequest request);
}
