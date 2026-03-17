package com.techhub.app.commonservice.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SkillCategoryDeserializer extends JsonDeserializer<SkillCategory> {

    @Override
    public SkillCategory deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return SkillCategory.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            String lower = normalized.toLowerCase();
            switch (lower) {
                case "backend":
                case "server":
                case "server-side":
                case "framework":
                    return SkillCategory.FRAMEWORK;
                case "language":
                case "lang":
                case "languages":
                    return SkillCategory.LANGUAGE;
                case "tool":
                case "tools":
                    return SkillCategory.TOOL;
                case "concept":
                case "concepts":
                    return SkillCategory.CONCEPT;
                default:
                    return SkillCategory.OTHER;
            }
        }
    }
}
