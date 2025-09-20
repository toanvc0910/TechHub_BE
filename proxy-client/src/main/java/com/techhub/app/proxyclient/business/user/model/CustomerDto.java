package com.techhub.app.proxyclient.business.user.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for {@link com.techhub.app.proxyclient.domain}
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CustomerDto implements Serializable {
    Integer id;
    @Size(max = 32)
    String code;
    @Size(max = 255)
    String name;
    @Size(max = 15)
    String phone1;
    @Size(max = 15)
    String phone2;
    @Size(max = 255)
    String address1;
    @Size(max = 255)
    String address2;
    Long customerPoint;
    @Size(max = 15)
    String taxCode;
    @Size(max = 64)
    String email;
    BigDecimal debitAmount;
    @Size(max = 255)
    String company;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate birthday;
    ImageDto image;
    String area;
    String wards;
    String isCustomerType;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE) // Only include in output (serialize)
    Integer partnerGroupId;
    String gender;
    String isActive = "Y";
    PartnerGroupDto partnerGroup;
    private String description;
    private BigDecimal discount;


    Integer[] ids;
}