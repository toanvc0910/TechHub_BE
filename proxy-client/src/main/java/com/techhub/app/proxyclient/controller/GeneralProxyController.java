package com.techhub.app.proxyclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app")
@Slf4j
public class GeneralProxyController {

    @GetMapping("/health")
    public String health() {
        return "Proxy-Client is running! Ready to handle requests from API Gateway.";
    }

    @GetMapping("/info")
    public String info(HttpServletRequest request) {
        log.info("Proxy: Received request from API Gateway - Path: {}, Method: {}",
                request.getRequestURI(), request.getMethod());
        return "Proxy-Client is working as intermediary between API Gateway and microservices";
    }
}
