package com.techhub.app.proxyclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSkillRequest {
    private String name;
    private String thumbnail;
    private String category;
}
