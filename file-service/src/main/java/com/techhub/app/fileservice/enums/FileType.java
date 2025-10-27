package com.techhub.app.fileservice.enums;

public enum FileType {
    IMAGE,
    VIDEO,
    DOCUMENT;

    public static FileType fromContentType(String contentType) {
        if (contentType == null) return DOCUMENT;
        
        if (contentType.startsWith("image/")) return IMAGE;
        if (contentType.startsWith("video/")) return VIDEO;
        return DOCUMENT;
    }

    public boolean isVideo() {
        return this == VIDEO;
    }

    public boolean isImage() {
        return this == IMAGE;
    }
}
