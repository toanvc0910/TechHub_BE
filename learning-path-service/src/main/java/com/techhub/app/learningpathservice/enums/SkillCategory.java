package com.techhub.app.learningpathservice.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SkillCategoryDeserializer.class)
public enum SkillCategory {
    LANGUAGE, FRAMEWORK, TOOL, CONCEPT, OTHER
}
