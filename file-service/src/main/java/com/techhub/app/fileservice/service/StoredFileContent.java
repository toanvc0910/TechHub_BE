package com.techhub.app.fileservice.service;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class StoredFileContent {
    private InputStream inputStream;
    private String filename;
    private String contentType;
    private Long contentLength;
}
