package com.techhub.app.proxyclient.business.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDto {
	private Integer userId;
	private String userName;
	private String fullName;
	private ImageDto image;
	private String email;
	private String phone;
	private Integer tenantId;
	 String password;
	
}










