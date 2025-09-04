package com.techhub.app.proxyclient.business.user.controller;


import com.techhub.app.proxyclient.business.user.queryRequest.CustomerQueryRequest;
import com.techhub.app.proxyclient.business.user.service.CustomerClientService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.userDto.CustomerDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerClientService customerClientService;

    @GetMapping("/{customerId}")
    @Operation(
    summary = "Find book by ID", description = "Returns a single book", tags = { "book" })
    public GlobalReponse findById(@PathVariable("customerId") Integer customerId, HttpSession session) {
//        String testSession = (String) session.getAttribute("test_session");
//        System.out.println("call vendor resource session: " + testSession);
        return this.customerClientService.findById(customerId);
    }

    
    @GetMapping("/findAll")
    public GlobalReponsePagination   findAll(@SpringQueryMap CustomerQueryRequest request) {
        return this.customerClientService.findALl(request);
    }

    @PostMapping("/save")
    public GlobalReponse saveCustomer(@RequestBody CustomerDto customerDto) {
        return this.customerClientService.saveCustomer(customerDto);
    }

    @PutMapping("/update")
    public GlobalReponse updateCustomer(@RequestBody CustomerDto customerDto) {
        return this.customerClientService.updateCustomer(customerDto);
    }

    @DeleteMapping("/delete/{customerId}")
    public GlobalReponse deleteCustomer(@PathVariable("customerId") Integer customerId) {
        return this.customerClientService.deleteCustomer(customerId);
    }

    @PostMapping("/deleteAllCustomerByIds")
    ResponseEntity<GlobalReponse> deleteAllCustomerByIds(@RequestBody CustomerDto ids) {
        return this.customerClientService.deleteAllCustomerByIds(ids);
    }

    @PostMapping("/updateDebit")
    public ResponseEntity<GlobalReponse> updateDebit(@RequestBody CustomerDto customerDto) {
        log.info("*** CustomerDto, resource; save Update Debit *");
        return this.customerClientService.updateDebit(customerDto);
    }

    @PostMapping("/intSave")
    public GlobalReponse intSave(@RequestBody List<CustomerDto> listInt) {
        log.info("*** CustomerDto, resource; save all customers ***");

        return this.customerClientService.intSave(listInt);
    }
}
