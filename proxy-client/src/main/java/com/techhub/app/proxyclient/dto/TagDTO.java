package com.techhub.app.proxyclient.dto;

import java.util.UUID;

public class TagDTO {
    private UUID id;
    private String name;

    public TagDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
