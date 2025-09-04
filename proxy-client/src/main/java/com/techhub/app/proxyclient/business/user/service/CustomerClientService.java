package com.techhub.app.proxyclient.business.user.service;


import com.techhub.app.proxyclient.business.user.queryRequest.CustomerQueryRequest;
import org.common.dbiz.dto.userDto.CustomerDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "USER-SERVICE", contextId = "customerClientService", path = "/user-service/api/v1/customers", decode404 = true)
public interface CustomerClientService {

    @GetMapping("/{customerId}")
    GlobalReponse findById(@PathVariable("customerId") Integer customerId);
    @GetMapping("/findAll")
    GlobalReponsePagination findALl(@SpringQueryMap CustomerQueryRequest request
    );

    @PostMapping("/save")
    GlobalReponse saveCustomer(@RequestBody CustomerDto customerDto);

    @PutMapping("/update")
    GlobalReponse updateCustomer(@RequestBody CustomerDto customerDto);

    @DeleteMapping("/delete/{customerId}")
    GlobalReponse deleteCustomer(@PathVariable("customerId") Integer customerId);

    @PostMapping("/deleteAllCustomerByIds")
    ResponseEntity<GlobalReponse> deleteAllCustomerByIds(@RequestBody CustomerDto ids) ;

    @PostMapping("/intSave")
    public GlobalReponse intSave(@RequestBody List<CustomerDto> listInt);
    @PostMapping("/updateDebit")
    public ResponseEntity<GlobalReponse> updateDebit(@RequestBody CustomerDto customerDto);
}
