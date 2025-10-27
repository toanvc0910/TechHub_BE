package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    FileUploadResponse uploadFile(MultipartFile file, String folder);
    List<FileUploadResponse> uploadMultipleFiles(List<MultipartFile> files, String folder);
    void deleteFile(String publicId);
    FileUploadResponse getFileMetadata(Long id);
}
