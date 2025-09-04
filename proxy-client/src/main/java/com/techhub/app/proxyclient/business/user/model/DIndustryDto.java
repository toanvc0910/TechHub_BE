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
public class DIndustryDto implements Serializable {
    Integer id;

    String code;
    String name;
}