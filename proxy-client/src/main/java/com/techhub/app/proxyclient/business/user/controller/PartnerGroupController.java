package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.model.PartnerGroupDto;
import com.techhub.app.proxyclient.business.user.queryRequest.DeletePartnerGroupByIds;
import com.techhub.app.proxyclient.business.user.queryRequest.PartnerGroupQuery;
import com.techhub.app.proxyclient.business.user.service.PartnerGroupClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partnerGroups")
@RequiredArgsConstructor
@Slf4j
public class PartnerGroupController {
    private final PartnerGroupClientService clientService;

    @GetMapping("/findAll")
    public GlobalReponsePagination   findAll(@SpringQueryMap final PartnerGroupQuery request ) {
        log.info("Find All PartnerGroup Controller");
        return this.clientService.findALl(request);
    }

    @PostMapping("/save")
    public GlobalReponse save(@RequestBody PartnerGroupDto dto) {
        log.info("Save PartnerGroup Controller");
        return this.clientService.save(dto);
    }

    @PutMapping("/update")
    public GlobalReponse update(@RequestBody PartnerGroupDto dto) {
        log.info("Update PartnerGroup Controller");
        return this.clientService.update(dto);
    }

    @GetMapping("/{id}")
    public GlobalReponse findById(@PathVariable final Integer id) {
        log.info("Find By Id PartnerGroup Controller");
        return this.clientService.findById(id);
    }

    @DeleteMapping("/delete/{id}")
    public GlobalReponse deleteById(@PathVariable final Integer id) {
        log.info("Delete By Id PartnerGroup Controller");
        return this.clientService.deleteById(id);
    }

    @PostMapping("/removeChildPartner")
    public  GlobalReponse removeChildPartner(@RequestBody final PartnerGroupDto request){
        log.info("Remove Child Partner Controller");
        return this.clientService.removeChildPartner(request);
    }

    @PostMapping("/deleteAllByIds")
    public GlobalReponse deleteAllByIds(@RequestBody final DeletePartnerGroupByIds request) {
        log.info("Delete All By Ids PartnerGroup Controller");
        return this.clientService.deleteAllByIds(request);
    }

}
