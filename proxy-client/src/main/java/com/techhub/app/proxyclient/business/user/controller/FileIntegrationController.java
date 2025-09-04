package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.service.FileIntegrationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fileIntegrate")
@RequiredArgsConstructor
@Slf4j
public class FileIntegrationController {

    private final FileIntegrationClientService fileIntegrationClientService;

    @PostMapping("/partnerGroup")
    public ResponseEntity<GlobalReponse> integratePartnerGroup(@RequestParam final String created)
    {
        log.info("*** created String, controller; integrate interface table PartnerGroup ***");
        return this.fileIntegrationClientService.integratePartnerGroup(created);
    }

    @PostMapping("/customer")
    public ResponseEntity<GlobalReponse> integrateCustomer(@RequestParam final String created)
    {
        log.info("*** created String, controller; integrate interface table Customer ***");
        return this.fileIntegrationClientService.integrateCustomer(created);
    }

}
