package com.techhub.app.proxyclient.jwt.config;

import com.techhub.app.proxyclient.exception.payload.ExceptionMsg;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) throws IOException, ServletException {
        ObjectMapper mapper = new ObjectMapper();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ExceptionMsg errorDetails = ExceptionMsg.builder()
                .timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
                .status(HttpStatus.UNAUTHORIZED)
                .error("JWT has expired")
                .build();
        String jsonResponse = mapper.writeValueAsString(errorDetails);
        response.getWriter().write(jsonResponse);

    }
}
