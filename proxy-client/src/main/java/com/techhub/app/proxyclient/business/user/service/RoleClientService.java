package com.techhub.app.proxyclient.business.user.service;

import org.common.dbiz.dto.userDto.RoleDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.common.dbiz.request.userRequest.RoleQueryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USER-SERVICE", contextId = "roleClientService", path = "/user-service/api/v1/role", decode404 = true)
public interface RoleClientService {
	
 	@GetMapping("/findAll")
	GlobalReponsePagination findAll(@SpringQueryMap RoleQueryRequest request);

	@PostMapping("/save")
	GlobalReponse save(@RequestBody RoleDto userDto);

	@GetMapping("/getRoleAccess")
	GlobalReponsePagination getRoleAccess(@RequestParam Integer roleId , @RequestParam Integer userId,@RequestParam("page") Integer page,@RequestParam("pageSize") Integer pageSize);

}










