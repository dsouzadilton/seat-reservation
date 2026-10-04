package com.dilton.paytm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Public endpoints
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Show availability is public
        if ("GET".equalsIgnoreCase(request.getMethod())
                && path.matches("/shows/\\d+")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Missing or invalid Authorization header\"}"
            );
            return;
        }

        String userId = authorization.substring(7).trim();

        if (userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Invalid bearer token\"}"
            );
            return;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId);

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}