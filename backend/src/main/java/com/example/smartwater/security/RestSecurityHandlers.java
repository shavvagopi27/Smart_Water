package com.example.smartwater.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.smartwater.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** JSON 401 / 403 responses for failures that happen inside the security filter chain. */
@Component
@RequiredArgsConstructor
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        write(res, 401, "Authentication required: missing or invalid token");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(res, 403, "You do not have permission to perform this action");
    }

    private void write(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), ApiError.of(status, message));
    }
}
