package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.service.impl.StoredObjectDetails;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface ObjectStorageService {

    StoredObjectDetails upload(MultipartFile file, String objectKey);

    StoredObjectDetails upload(InputStream inputStream, long size, String contentType, String objectKey);

    InputStream getObject(String objectKey);

    void delete(String objectKey);
}