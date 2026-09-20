package com.codingarena.common.ratelimit;

import com.codingarena.auth.model.User;
import com.codingarena.auth.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.regex.Pattern;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Pattern SUBMISSION_PATTERN = Pattern.compile("^/api/matches/[^/]+/submissions/?$");

    private final RateLimitService rateLimitService;
    private final JwtTokenProvider tokenProvider;

    public RateLimitInterceptor(RateLimitService rateLimitService, JwtTokenProvider tokenProvider) {
        this.rateLimitService = rateLimitService;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        if ("/api/auth/login".equals(path)) {
            String ip = getClientIp(request);
            if (!rateLimitService.tryConsume("AUTH_LOGIN:" + ip, 5, 5, Duration.ofMinutes(1))) {
                throw new RateLimitExceededException("Rate limit exceeded, try again later");
            }
        } else if ("/api/auth/register".equals(path)) {
            String ip = getClientIp(request);
            if (!rateLimitService.tryConsume("AUTH_REGISTER:" + ip, 3, 3, Duration.ofMinutes(1))) {
                throw new RateLimitExceededException("Rate limit exceeded, try again later");
            }
        } else if ("/api/matchmaking/queue".equals(path)) {
            String userId = getUserId(request);
            if (!rateLimitService.tryConsume("MATCHMAKING_QUEUE:" + userId, 10, 10, Duration.ofMinutes(1))) {
                throw new RateLimitExceededException("Rate limit exceeded, try again later");
            }
        } else if (SUBMISSION_PATTERN.matcher(path).matches()) {
            String userId = getUserId(request);
            if (!rateLimitService.tryConsume("MATCH_SUBMISSION:" + userId, 10, 10, Duration.ofMinutes(1))) {
                throw new RateLimitExceededException("Rate limit exceeded, try again later");
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown-ip";
    }

    private String getUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId().toString();
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenProvider.validateToken(token)) {
                return tokenProvider.getUserIdFromToken(token).toString();
            }
        }

        return getClientIp(request);
    }
}
