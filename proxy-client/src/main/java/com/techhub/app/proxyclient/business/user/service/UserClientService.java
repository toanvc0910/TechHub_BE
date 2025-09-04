package com.techhub.app.proxyclient.business.user.service;

import com.techhub.app.proxyclient.business.user.model.UserDto;
import org.common.dbiz.dto.userDto.VariousUserDto;
import org.common.dbiz.dto.userDto.VariousUserParamDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.common.dbiz.request.userRequest.UserQueryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@FeignClient(name = "USER-SERVICE", contextId = "userClientService", path = "/user-service/api/v1/users", decode404 = true)
public interface UserClientService {


    //	@GetMapping
//	ResponseEntity<UserUserServiceCollectionDtoResponse> findAll();
//
//	@GetMapping("/{userId}")
//	ResponseEntity<UserDto> findById(
//			@PathVariable("userId")
//			@NotBlank(message = "*Input must not blank!**")
//			@Valid final String userId);
//
    @GetMapping("/username/{username}/{dTenantId}")
    ResponseEntity<UserDto> findByUsername(
            @RequestHeader("tenantId") Integer tenantId,
            @PathVariable("username")
            @NotBlank(message = "*Input must not blank!**")
            @Valid String username,
            @PathVariable("dTenantId") Integer dTenantId
    );

    @GetMapping("/username/{username}")
    ResponseEntity<UserDto> findByUsername(
            @PathVariable("username")
            @NotBlank(message = "*Input must not blank!**")
            @Valid String username
    );

    @PostMapping
    ResponseEntity<UserDto> save(
            @RequestBody
            @NotNull(message = "*Input must not NULL!**")
            @Valid final UserDto userDto);

    @PostMapping("/login")
    ResponseEntity<GlobalReponse> login(
            @RequestBody
            @NotNull(message = "*Input must not NULL!**")
            @Valid final UserDto userDto
    );

    @PostMapping("/register")
    ResponseEntity<GlobalReponse> register(
            @RequestBody
            @NotNull(message = "*Input must not NULL!**")
            @Valid final UserDto userDto
    );

    @GetMapping("/{id}")
    ResponseEntity<GlobalReponse> findById(
            @PathVariable("id")
            @NotNull(message = "*Input must not NULL!**")
            @Valid final Integer id
    );

    @GetMapping("/findAll")
    ResponseEntity<GlobalReponsePagination> findAll(
            @SpringQueryMap UserQueryRequest userQueryRequest
    );

    @GetMapping("/posTerminalIdAccess/{userId}/{orgId}")
    ResponseEntity<GlobalReponse> findPosTerminalIdAccessByUserIdAndOrgId(
            @PathVariable("userId") Integer userId,
            @PathVariable("orgId") Integer orgId);

    @GetMapping("/getOrgWarehouseAccess")
    ResponseEntity<GlobalReponse> getOrgWarehouseAccess(@RequestParam("userId") Integer userId, @RequestParam(value = "orgId") Integer orgId);

    @GetMapping("/getWarehouseAccess")
    ResponseEntity<GlobalReponse> getWarehouseAccess(@RequestParam("userId") Integer userId);

    @PostMapping("/update")
    ResponseEntity<GlobalReponse> update(
            @RequestBody
            @Valid final org.common.dbiz.dto.userDto.UserDto userDto
    );

    @PostMapping("/save")
    ResponseEntity<GlobalReponse> saveAll(
            @RequestBody
            @Valid final org.common.dbiz.dto.userDto.UserDto userDto
    );

    @GetMapping("/getOrgAccess/{userId}")
    public ResponseEntity<GlobalReponse> getOrgAccess(@PathVariable("userId") Integer userId);

    @GetMapping("/getOrgAcc")
    public ResponseEntity<GlobalReponsePagination> getOrgA(@RequestParam("userId") Integer userId,
                                                           @RequestParam("roleId") Integer roleId,
                                                           @RequestParam("page") Integer page,
                                                           @RequestParam("pageSize") Integer pageSize,
                                                           @RequestParam("name") String name, @RequestParam(value = "searchKey", required = false) String searchKey,
                                                           @RequestParam(value = "area", required = false) String area);

    @GetMapping("/getById")
    public ResponseEntity<GlobalReponse> getByIdAndRoleId(@RequestParam("currentUserId") Integer currentUserId, @RequestParam("userId") Integer userId, @RequestParam("roleId") Integer roleId);

    @PostMapping("/registerNoToken")
    public ResponseEntity<GlobalReponse> registerNoToken(@RequestBody UserDto userDto);

    @PostMapping("/variety")
    public ResponseEntity<GlobalReponse> createVariousUser(@RequestBody VariousUserDto userDto);

    @GetMapping("/variety")
    public ResponseEntity<GlobalReponsePagination> getVariousUser(@SpringQueryMap VariousUserParamDto userDto);




}










