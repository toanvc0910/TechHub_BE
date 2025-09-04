package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.queryRequest.VendorQueryRequest;
import com.techhub.app.proxyclient.business.user.service.VendorClientService;
import lombok.RequiredArgsConstructor;
import org.common.dbiz.dto.userDto.VendorDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {
    private final VendorClientService vendorClientService;

//    redis
//    private final RedisConnectionFactory redisConnectionFactory;
    @GetMapping("/{vendorId}")
//    @Cacheable(value = "vendors", key = "#vendorId")
    public GlobalReponse findById(@PathVariable(name = "vendorId") Integer vendorId, HttpSession session) {
//        System.out.println("SessionID: " + session.getId());
//        String testSession = (String) session.getAttribute("test_session");
//        System.out.println("call vendor proyx session: " + testSession +" ID: "+session.getId());
//        session.setAttribute("test_session_id", "find by id session");
        return this.vendorClientService.findById(vendorId);
    }
    @GetMapping("/findAll")
//    @Cacheable(value = "allVendors",key = "'allVendorsKey'")
    public GlobalReponsePagination   findAll(@SpringQueryMap VendorQueryRequest request) {
//        String testSession = (String) session.getAttribute("test_session_id");
//        System.out.println("call find all vendor session: " + testSession +" ID: "+session.getId());
//        session.setAttribute("test_session", page);
//        System.out.println("SessionID: " + session.getId());

        return this.vendorClientService.findALl(request);
    }

    @PostMapping("/save")
//    @Caching(
//            evict = { @CacheEvict(value = "allVendors",key = "'allVendorsKey'", allEntries = true) },
//            put = { @CachePut(value = "vendors", key = "#result.data.id") }
//    )
    public GlobalReponse save(@RequestBody VendorDto vendorDto) {
        return this.vendorClientService.save(vendorDto);
    }

    @PutMapping("/update")
//    @CachePut(value = "vendors", key = "#vendorDto.id")
//    @CacheEvict(value = "allVendors", allEntries = true)
    public GlobalReponse update(@RequestBody VendorDto vendorDto) {
        return this.vendorClientService.update(vendorDto);
    }

    @DeleteMapping("/delete/{vendorId}")
//    @Caching(
//            evict = {
//                    @CacheEvict(value = "vendors", key = "#vendorId"),
//                    @CacheEvict(value = "allVendors",key = "'allVendorsKey'", allEntries = true)
//            }
//    )
    public GlobalReponse deleteVendorById(@PathVariable(name = "vendorId") Integer vendorId) {
        return this.vendorClientService.deleteVendorById(vendorId);
    }


    @PostMapping("/deleteAllByIds")
    public GlobalReponse deleteAllByIds(@RequestBody VendorDto vendorDto) {
        return this.vendorClientService.deleteAllByIds(vendorDto);
    }
}
