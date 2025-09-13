package com.techhub.app.proxyclient.business.user.service;

import org.common.dbiz.dto.userDto.RolePermissionVDto;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USER-SERVICE", contextId = "rolePermissionClientService", path = "/user-service/api/v1/rolePermission", decode404 = true)
public interface RolePermissionClientService {

	@GetMapping("")
	ResponseEntity<GlobalReponse> findById(@RequestParam(value = "roleId",defaultValue =  "0") Integer id);

	@PostMapping("/save")
	ResponseEntity<GlobalReponse> save(@RequestBody RolePermissionVDto param);
}










