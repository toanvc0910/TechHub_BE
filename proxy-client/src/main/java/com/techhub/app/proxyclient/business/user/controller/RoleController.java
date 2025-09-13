package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.service.RoleClientService;
import lombok.RequiredArgsConstructor;
import org.common.dbiz.dto.userDto.RoleDto;
import org.common.dbiz.payload.GlobalReponse;
import org.common.dbiz.payload.GlobalReponsePagination;
import org.common.dbiz.request.userRequest.RoleQueryRequest;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleController {
	
	private final RoleClientService userClientService;

	@GetMapping("/findAll")
	public GlobalReponsePagination findAll(@SpringQueryMap RoleQueryRequest request) {
		return this.userClientService.findAll(request);
	}

	@PostMapping("/save")
	public GlobalReponse save(@RequestBody RoleDto userDto) {
		return this.userClientService.save(userDto);
	}

	@GetMapping("/getRoleAccess")
	public GlobalReponsePagination getRoleAccess(@RequestParam Integer roleId , @RequestParam Integer userId,@RequestParam("page") Integer page,@RequestParam("pageSize") Integer pageSize) {

		return this.userClientService.getRoleAccess(roleId,userId,page,pageSize);
	}
}










