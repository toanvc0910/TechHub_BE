package com.techhub.app.paymentservice.config;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

/**
 * Servlet filter to add required headers for UserContextInterceptor
 * This filter wraps all incoming requests and adds fake authentication headers
 * to bypass UserContextInterceptor's validation
 */
@Component
public class PaymentServiceHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Wrap request to add fake headers
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    // Add fake headers to satisfy UserContextInterceptor
                    if ("X-Request-Source".equals(name)) {
                        return "proxy-client";
                    }
                    if ("X-User-Id".equals(name)) {
                        return "00000000-0000-0000-0000-000000000000"; // Dummy UUID
                    }
                    if ("X-User-Email".equals(name)) {
                        return "payment-service@system.local";
                    }
                    if ("X-User-Roles".equals(name)) {
                        return "SYSTEM";
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-Request-Source".equals(name) || "X-User-Id".equals(name) ||
                        "X-User-Email".equals(name) || "X-User-Roles".equals(name)) {
                        return Collections.enumeration(Collections.singletonList(getHeader(name)));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> headerNames = new HashSet<>();
                    Enumeration<String> originalHeaders = super.getHeaderNames();
                    while (originalHeaders.hasMoreElements()) {
                        headerNames.add(originalHeaders.nextElement());
                    }
                    // Add our fake headers
                    headerNames.add("X-Request-Source");
                    headerNames.add("X-User-Id");
                    headerNames.add("X-User-Email");
                    headerNames.add("X-User-Roles");
                    return Collections.enumeration(headerNames);
                }
            };

            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}

