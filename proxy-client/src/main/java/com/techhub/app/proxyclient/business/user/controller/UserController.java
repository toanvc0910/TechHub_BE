package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.model.UserDto;
import com.techhub.app.proxyclient.business.user.service.UserClientService;
import lombok.RequiredArgsConstructor;
import org.common.dbiz.dto.userDto.VariousUserDto;
import org.common.dbiz.dto.userDto.VariousUserParamDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.common.dbiz.request.userRequest.UserQueryRequest;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserClientService userClientService;


    @GetMapping("/username/{username}/{dTenantId}")
    public ResponseEntity<UserDto> findByUsername(@PathVariable("username") final String username, @PathVariable("dTenantId") Integer dTenantId) {
        return ResponseEntity.ok(this.userClientService.findByUsername(dTenantId, username, 0).getBody());
    }

    @PostMapping
    public ResponseEntity<UserDto> save(@RequestBody final UserDto userDto) {

        return ResponseEntity.ok(this.userClientService.save(userDto).getBody());
    }

    @PostMapping("/login")
    public ResponseEntity<GlobalReponse> login(@RequestBody final UserDto userDto) {

        GlobalReponse globalReponse = this.userClientService.login(userDto).getBody();

        return ResponseEntity.ok(globalReponse);
    }

    @PostMapping("/register")
    public ResponseEntity<GlobalReponse> register(@RequestBody final UserDto userDto) {

        GlobalReponse globalReponse = this.userClientService.register(userDto).getBody();

        return ResponseEntity.ok(globalReponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalReponse> findById(@PathVariable("id") final Integer id) {
        return ResponseEntity.ok(this.userClientService.findById(id).getBody());
    }

    @GetMapping("/findAll")
    public ResponseEntity<GlobalReponsePagination> findAll(
            @SpringQueryMap UserQueryRequest request
    ) {
        return ResponseEntity.ok(this.userClientService.findAll(request).getBody());
    }


    @GetMapping("/posTerminalIdAccess/{userId}/{orgId}")
    public ResponseEntity<GlobalReponse> findPosTerminalIdAccessByUserIdAndOrgId(
            @PathVariable("userId") Integer userId,
            @PathVariable("orgId") Integer orgId) {
        return ResponseEntity.ok(this.userClientService.findPosTerminalIdAccessByUserIdAndOrgId(userId, orgId).getBody());
    }

    @GetMapping("/getOrgWarehouseAccess")
    public ResponseEntity<GlobalReponse> getOrgWarehouseAccess(@RequestParam("userId") Integer userId, @RequestParam(value = "orgId") Integer orgId) {
        return ResponseEntity.ok(this.userClientService.getOrgWarehouseAccess(userId, orgId).getBody());
    }

    @GetMapping("/getWarehouseAccess")
    public ResponseEntity<GlobalReponse> getWarehouseAccess(@RequestParam("userId") Integer userId) {
        return ResponseEntity.ok(this.userClientService.getWarehouseAccess(userId).getBody());
    }

    @PostMapping("/update")
    public ResponseEntity<GlobalReponse> update(@RequestBody final org.common.dbiz.dto.userDto.UserDto userDto) {

        return ResponseEntity.ok(this.userClientService.update(userDto).getBody());
    }

    @PostMapping("/save")
    public ResponseEntity<GlobalReponse> save(@RequestBody final org.common.dbiz.dto.userDto.UserDto userDto) {

        return ResponseEntity.ok(this.userClientService.saveAll(userDto).getBody());
    }

    @GetMapping("/getOrgAccess/{userId}")
    public ResponseEntity<GlobalReponse> getOrgAccess(@PathVariable("userId") Integer userId) {
        return ResponseEntity.ok(this.userClientService.getOrgAccess(userId).getBody());
    }

    @GetMapping("/getOrgAcc")
    public ResponseEntity<GlobalReponsePagination> getOrgA(@RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId,
                                                           @RequestParam(value = "page", defaultValue = "0") Integer page
            , @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
                                                           @RequestParam(value = "name", defaultValue = "") String name, @RequestParam(value = "searchKey", required = false,defaultValue = "") String searchKey,
                                                           @RequestParam(value = "area", required = false,defaultValue = "") String area) {

        return ResponseEntity.ok(this.userClientService.getOrgA(userId, roleId, page, pageSize, name,searchKey,area).getBody());
    }

    @GetMapping("/getById")
    public ResponseEntity<GlobalReponse> getByIdAndRoleId(@RequestParam("currentUserId") Integer currentUserId, @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId) {
        return ResponseEntity.ok(this.userClientService.getByIdAndRoleId(currentUserId, userId, roleId).getBody());
    }


    @PostMapping("/registerNoToken")

    public ResponseEntity<GlobalReponse> registerNoToken(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(this.userClientService.registerNoToken(userDto).getBody());
    }

    @PostMapping("/variety")
    public ResponseEntity<GlobalReponse> createVariousUser(@RequestBody VariousUserDto userDto) {

        return ResponseEntity.ok(this.userClientService.createVariousUser(userDto)).getBody();
    }

    @GetMapping("/variety")
    public ResponseEntity<GlobalReponsePagination> getVariousUser(@SpringQueryMap VariousUserParamDto userDto) {

        return ResponseEntity.ok(this.userClientService.getVariousUser(userDto)).getBody();
    }

}










