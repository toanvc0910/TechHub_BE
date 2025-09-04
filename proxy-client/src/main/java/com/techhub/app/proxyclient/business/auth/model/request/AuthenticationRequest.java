package com.techhub.app.proxyclient.business.auth.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AuthenticationRequest implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank(message = "*Username must not be empty!**")
	private String username;
	
	@NotNull(message = "*Password must not be null!**")
	private String password;

	@NotNull(message = "*dTenantId must not be null!**")
	@JsonProperty("d_tenant_id")
	private Integer dTenantId;

	@JsonProperty("d_org_id")
	private Integer dOrgId;

	private String language;

}










