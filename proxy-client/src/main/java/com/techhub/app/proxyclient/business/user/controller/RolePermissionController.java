package com.techhub.app.proxyclient.business.user.controller;

import com.techhub.app.proxyclient.business.user.service.RolePermissionClientService;
import lombok.RequiredArgsConstructor;
import org.common.dbiz.dto.userDto.RolePermissionVDto;
import org.common.dbiz.payload.GlobalReponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rolePermission")
@RequiredArgsConstructor
public class RolePermissionController {
	
	private final RolePermissionClientService service;

	@GetMapping("")
	ResponseEntity<GlobalReponse> findById(@RequestParam(value = "roleId",defaultValue = "0") Integer id){

		return ResponseEntity.ok(service.findById(id).getBody());
	}

	@PostMapping("/save")
	ResponseEntity<GlobalReponse> save(@RequestBody RolePermissionVDto params) {
		return ResponseEntity.ok(service.save(params).getBody());
	}
}










