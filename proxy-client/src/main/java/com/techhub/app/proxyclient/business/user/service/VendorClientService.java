package com.techhub.app.proxyclient.business.user.service;

import com.techhub.app.proxyclient.business.user.queryRequest.VendorQueryRequest;
import com.techhub.app.proxyclient.config.client.FeignClientConfig;
import org.common.dbiz.dto.userDto.VendorDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USER-SERVICE", contextId = "vendorClientService", path = "/user-service/api/v1/vendors", decode404 = true
, configuration = FeignClientConfig.class )
public interface VendorClientService {

    @GetMapping("/{vendorId}")
    GlobalReponse findById(@PathVariable(name = "vendorId") Integer vendorId);

    @GetMapping("/findAll")
    GlobalReponsePagination findALl
            (
//            @NotBlank(message = "*Param must not blank!**")
//            @Valid @ModelAttribute CustomerQueryRequest requesta
                    @SpringQueryMap VendorQueryRequest request
                    );

    @PostMapping("/save")
    GlobalReponse save(@RequestBody VendorDto vendorDto);

    @PutMapping("/update")
    GlobalReponse update(@RequestBody VendorDto vendorDto);

    @DeleteMapping("/delete/{vendorId}")
    GlobalReponse deleteVendorById(@PathVariable(name = "vendorId") Integer vendorId);

    @PostMapping("/deleteAllByIds")
    GlobalReponse deleteAllByIds(@RequestBody VendorDto vendorDto);
}
