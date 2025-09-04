package com.techhub.app.proxyclient.config.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final Map<String, String> ALLOWED_URI_V2 =
            Map.of(
                    "/app/api/v1/napas/notification", "POST",
                    "/app/api/v1/napas/reconciliation", "POST",
                    "/app/api/v1/napas/investigation", "POST"
            );

    @Value("${company.name}")
    private String companyName;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {

        LocalDateTime startTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.info("Begin filter proxy at {}", startTime.format(formatter));

        log.info("**JwtRequestFilter, once per request, validating and extracting token --rebuild*\n");


        String origin = request.getHeader("Origin");

        // Check if the origin is one of the allowed origins



        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        final var authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        Integer dTenantId = null;
        String issuer = null;

        if ( authorizationHeader != null && authorizationHeader.startsWith("Bearer ") ) {
            jwt = authorizationHeader.substring(7);

            try {
                issuer = jwtService.extractIssuer(jwt);
                username = jwtService.extractUsername(jwt);
                dTenantId = jwtService.extractTenantId(jwt);

            } catch (ExpiredJwtException e) {

                log.info("**JwtRequestFilter, once per request, token expired --rebuild*\n");
//				if(issuer != null && issuer.equalsIgnoreCase(companyName)) {
//					throw new ExpiredTokenException("Token has expired");
//				} else {
//					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
//				}
//				return;
//				throw new ExpiredTokenException("Token has expired");
                handleException(response, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
                return;
            } catch (SignatureException e) {
                log.info("**JwtRequestFilter, once per request, 1. invalid token --rebuild*\n");
                handleException(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token");
                return;
            } catch (MalformedJwtException e) {
                log.info("**JwtRequestFilter, once per request, 2. invalid token --rebuild*\n");
                handleException(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = null;
            String uri = request.getRequestURI();
            log.info("URL : {}", request.getRequestURL());
            log.info("URI : {}", uri);
            if(issuer != null && issuer.equalsIgnoreCase(companyName)) {

                userDetails = this.userDetailsService.loadByClientId(username);
                if(userDetails == null || !ALLOWED_URI_V2.containsKey(uri)) {
                    log.info("**JwtRequestFilter, once per request, 3. invalid token --rebuild*\n");
                    handleException(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token");
                    return;
                }
            } else {
                userDetails = this.userDetailsService.loadUserByUsernameAndTenantId(username,dTenantId);
                if(userDetails == null || ALLOWED_URI_V2.containsKey(uri)) {
                    log.info("**JwtRequestFilter, once per request, 3. invalid token --rebuild*\n");
                    handleException(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token");
                    return;
                }
            }


            if (this.jwtService.validateToken(jwt, userDetails)) {
                final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }

        }

        LocalDateTime endTime = LocalDateTime.now();
        log.info("End filter proxy at {}", endTime.format(formatter));
        filterChain.doFilter(request, response);
        log.info("**Jwt request filtered --rebuild!*\n");
    }

    private void handleException(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("code", "failure");
        body.put("message", message);
//		body.put("description", "Token has expired");
//		body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String json = new ObjectMapper().writeValueAsString(body);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

}


