package com.techhub.app.proxyclient.business.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TenantDto implements Serializable {
    Integer id;
    String code;
    String name;
    String domainUrl;
    String taxCode;
     Integer imageId;
     Integer industryId;

}