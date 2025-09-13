package com.techhub.app.proxyclient.business.user.service;

import com.techhub.app.proxyclient.business.user.model.PartnerGroupDto;
import com.techhub.app.proxyclient.business.user.queryRequest.DeletePartnerGroupByIds;
import com.techhub.app.proxyclient.business.user.queryRequest.PartnerGroupQuery;
import com.techhub.app.proxyclient.config.client.FeignClientConfig;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USER-SERVICE", contextId = "partnerGroupClientService", path = "/user-service/api/v1/partnerGroups", decode404 = true
, configuration = FeignClientConfig.class )
public interface PartnerGroupClientService {

//    @GetMapping("/{vendorId}")
//    GlobalReponse findById(@PathVariable(name = "vendorId") Integer vendorId);

    @GetMapping("/findAll")
    GlobalReponsePagination findALl
            ( @SpringQueryMap final PartnerGroupQuery request  );

    @PostMapping("/save")
    GlobalReponse save(@RequestBody PartnerGroupDto dto);

    @PutMapping("/update")
    GlobalReponse update(@RequestBody PartnerGroupDto dto);

    @GetMapping("/{id}")
    GlobalReponse findById(@PathVariable final Integer id);

    @DeleteMapping("/delete/{id}")
    GlobalReponse deleteById(@PathVariable final Integer id);

    @PostMapping("/removeChildPartner")
    GlobalReponse removeChildPartner(@RequestBody final PartnerGroupDto request);

    @PostMapping("/deleteAllByIds")
    GlobalReponse deleteAllByIds(@RequestBody final DeletePartnerGroupByIds request);
}
