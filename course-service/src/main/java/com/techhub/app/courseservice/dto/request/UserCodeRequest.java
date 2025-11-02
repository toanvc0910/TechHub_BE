package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UserCodeRequest {

    @NotBlank
    private String language;

    @NotBlank
    @Size(max = 20000)
    private String code;
}
