package com.techhub.app.fileservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techhub.app.fileservice.dto.response.FileUploadResponse;
import com.techhub.app.fileservice.entity.FileMetadata;
import com.techhub.app.fileservice.enums.FileType;
import com.techhub.app.fileservice.repository.FileMetadataRepository;
import com.techhub.app.fileservice.service.FileService;
import com.techhub.app.fileservice.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileServiceImpl implements FileService {

    private final Cloudinary cloudinary;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileValidator fileValidator;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) {
        // Validate file
        fileValidator.validate(file);

        try {
            // Determine resource type
            String resourceType = determineResourceType(file);
            
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "techhub/" + folder,
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true
                    ));

            // Extract metadata
            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            Long size = ((Number) uploadResult.get("bytes")).longValue();
            String format = (String) uploadResult.get("format");
            
            // Save metadata to database
            FileMetadata metadata = FileMetadata.builder()
                    .originalFilename(file.getOriginalFilename())
                    .url(url)
                    .publicId(publicId)
                    .fileType(FileType.fromContentType(file.getContentType()))
                    .size(size)
                    .format(format)
                    .folder(folder)
                    .build();
            
            metadata = fileMetadataRepository.save(metadata);

            log.info("File uploaded successfully: {}", url);

            return FileUploadResponse.fromEntity(metadata);

        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public List<FileUploadResponse> uploadMultipleFiles(List<MultipartFile> files, String folder) {
        return files.stream()
                .map(file -> uploadFile(file, folder))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String publicId) {
        try {
            // Find metadata
            FileMetadata metadata = fileMetadataRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new RuntimeException("File not found with publicId: " + publicId));

            // Delete from Cloudinary
            String resourceType = metadata.getFileType().isVideo() ? "video" : "image";
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));

            // Delete metadata
            fileMetadataRepository.delete(metadata);

            log.info("File deleted successfully: {}", publicId);

        } catch (IOException e) {
            log.error("Error deleting file from Cloudinary", e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public FileUploadResponse getFileMetadata(Long id) {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
        return FileUploadResponse.fromEntity(metadata);
    }

    private String determineResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return "auto";
        
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("image/")) return "image";
        return "auto";
    }
}
