package com.techhub.app.proxyclient.business.user.service;

import com.techhub.app.proxyclient.config.client.FeignClientConfig;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "USER-SERVICE",
        contextId = "FileIntegrationClientService",
        path = "/user-service/api/v1/fileIntegrate",
        decode404 = true,
        configuration = FeignClientConfig.class)
public interface FileIntegrationClientService {

    @PostMapping("/partnerGroup")
    ResponseEntity<GlobalReponse> integratePartnerGroup(@RequestParam final String created);

    @PostMapping("/customer")
    ResponseEntity<GlobalReponse> integrateCustomer(@RequestParam final String created);

}
