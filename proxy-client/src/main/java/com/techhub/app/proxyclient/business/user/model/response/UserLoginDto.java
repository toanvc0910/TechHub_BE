package com.techhub.app.proxyclient.business.user.model.response;

import com.techhub.app.proxyclient.business.user.model.TenantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO: Data Transfer Object
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserLoginDto implements Serializable {
    Integer dUserId;
    String userName;
    String fullName;
    String phone;
    Integer dImageId;
    @Email(message = "*Input must be in Email format!**")
    String email;
    LocalDate birthDay;
    TenantDto dTenant;
}