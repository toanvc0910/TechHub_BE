package com.techhub.app.commonservice.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SkillCategoryDeserializer.class)
public enum SkillCategory {
    LANGUAGE,
    FRAMEWORK,
    TOOL,
    CONCEPT,
    OTHER
}
