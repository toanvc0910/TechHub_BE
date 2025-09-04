package com.techhub.app.blogservice.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/blogs")
@Slf4j
public class BlogResource {

	@GetMapping("/findAll")
	public ResponseEntity<String> findAll() {
		return ResponseEntity.ok("find All1");
	}
}
