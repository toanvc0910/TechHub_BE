package com.techhub.app.learningpathservice.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SkillCategoryDeserializer extends JsonDeserializer<SkillCategory> {

    @Override
    public SkillCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }
        // try direct match first (case-insensitive)
        try {
            return SkillCategory.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // fallback mapping for user-friendly strings
            String n = s.toLowerCase();
            switch (n) {
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
