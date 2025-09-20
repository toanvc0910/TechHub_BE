package com.techhub.app.proxyclient.business.auth.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTokenDto {
    private String username;
    private String password;
    @JsonProperty("d_tenant_id")
    private Integer d_tenant_id;
    @JsonProperty("user_id")
    private Integer userId;
    @JsonProperty("d_org_id")
    private Integer d_org_id;
    private String language;
}

