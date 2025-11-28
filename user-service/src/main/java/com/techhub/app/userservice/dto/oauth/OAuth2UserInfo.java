package com.techhub.app.userservice.dto.oauth;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        // Create mutable copy to allow email resolution
        this.attributes = new HashMap<>(attributes);
    }

    public abstract String getId();

    public abstract String getName();

    public abstract String getEmail();

    public abstract String getImageUrl();

    public abstract Boolean getEmailVerified();
}
