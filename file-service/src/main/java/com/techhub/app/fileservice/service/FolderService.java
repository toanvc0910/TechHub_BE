package com.techhub.app.fileservice.service;

import com.techhub.app.fileservice.dto.request.CreateFolderRequest;
import com.techhub.app.fileservice.dto.request.UpdateFolderRequest;
import com.techhub.app.fileservice.dto.response.FolderResponse;
import com.techhub.app.fileservice.entity.FileFolderEntity;
import com.techhub.app.fileservice.repository.FileFolderRepository;
import com.techhub.app.fileservice.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {

    private final FileFolderRepository folderRepository;
    private final FileRepository fileRepository;

    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {
        // Check if folder with same name exists in the same parent
        if (folderRepository.existsByUserIdAndNameAndParentIdAndIsActive(
                request.getUserId(), request.getName(), request.getParentId(), "Y")) {
            throw new RuntimeException("Folder with this name already exists in the same location");
        }

        FileFolderEntity folder = new FileFolderEntity();
        folder.setUserId(request.getUserId());
        folder.setParentId(request.getParentId());
        folder.setName(request.getName());
        folder.setIsActive("Y");
        folder.setCreatedBy(request.getUserId());

        // Calculate path - will be set by trigger, but we can set it manually too
        if (request.getParentId() != null) {
            FileFolderEntity parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            folder.setPath(parent.getPath() + "/" + request.getName());
        } else {
            folder.setPath("/" + request.getName());
        }

        FileFolderEntity saved = folderRepository.save(folder);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFoldersByUser(UUID userId) {
        List<FileFolderEntity> folders = folderRepository.findByUserIdAndIsActive(userId, "Y");
        return folders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(UUID userId) {
        List<FileFolderEntity> folders = folderRepository.findByUserIdAndParentIdIsNullAndIsActive(userId, "Y");
        return folders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderById(UUID userId, UUID folderId) {
        FileFolderEntity folder = folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return mapToResponse(folder);
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderTree(UUID userId, UUID folderId) {
        FileFolderEntity folder = folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        FolderResponse response = mapToResponse(folder);
        response.setChildren(buildFolderTree(userId, folderId));
        return response;
    }

    private List<FolderResponse> buildFolderTree(UUID userId, UUID parentId) {
        List<FileFolderEntity> children = folderRepository.findByUserIdAndParentIdAndIsActive(userId, parentId, "Y");

        return children.stream()
                .map(child -> {
                    FolderResponse childResponse = mapToResponse(child);
                    childResponse.setChildren(buildFolderTree(userId, child.getId()));
                    return childResponse;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse updateFolder(UUID userId, UUID folderId, UpdateFolderRequest request) {
        FileFolderEntity folder = folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (request.getName() != null && !request.getName().equals(folder.getName())) {
            // Check for duplicate name
            if (folderRepository.existsByUserIdAndNameAndParentIdAndIsActive(
                    userId, request.getName(), folder.getParentId(), "Y")) {
                throw new RuntimeException("Folder with this name already exists in the same location");
            }
            folder.setName(request.getName());
        }

        if (request.getParentId() != null && !request.getParentId().equals(folder.getParentId())) {
            // Move folder
            moveFolder(folder, request.getParentId());
        }

        folder.setUpdatedBy(userId);
        FileFolderEntity updated = folderRepository.save(folder);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteFolder(UUID userId, UUID folderId) {
        FileFolderEntity folder = folderRepository.findByIdAndUserIdAndIsActive(folderId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        // Check if folder has files
        long fileCount = fileRepository.findByUserIdAndFolderIdAndIsActive(userId, folderId, "Y").size();
        if (fileCount > 0) {
            throw new RuntimeException("Cannot delete folder with files. Please move or delete files first.");
        }

        // Check if folder has subfolders
        long subfolderCount = folderRepository.findByUserIdAndParentIdAndIsActive(userId, folderId, "Y").size();
        if (subfolderCount > 0) {
            throw new RuntimeException("Cannot delete folder with subfolders. Please delete subfolders first.");
        }

        folder.setIsActive("N");
        folder.setUpdatedBy(userId);
        folderRepository.save(folder);
    }

    private void moveFolder(FileFolderEntity folder, UUID newParentId) {
        if (newParentId != null) {
            FileFolderEntity newParent = folderRepository.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("New parent folder not found"));

            // Check for circular reference
            if (isDescendant(folder.getId(), newParentId)) {
                throw new RuntimeException("Cannot move folder to its own descendant");
            }

            folder.setPath(newParent.getPath() + "/" + folder.getName());
        } else {
            folder.setPath("/" + folder.getName());
        }
        folder.setParentId(newParentId);
    }

    private boolean isDescendant(UUID ancestorId, UUID descendantId) {
        if (ancestorId.equals(descendantId)) {
            return true;
        }

        Optional<FileFolderEntity> descendant = folderRepository.findById(descendantId);
        if (descendant.isEmpty() || descendant.get().getParentId() == null) {
            return false;
        }

        return isDescendant(ancestorId, descendant.get().getParentId());
    }

    private FolderResponse mapToResponse(FileFolderEntity folder) {
        Integer fileCount = fileRepository.findByUserIdAndFolderIdAndIsActive(
                folder.getUserId(), folder.getId(), "Y").size();

        Long totalSize = fileRepository.findByUserIdAndFolderIdAndIsActive(
                folder.getUserId(), folder.getId(), "Y")
                .stream()
                .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0L)
                .sum();

        return FolderResponse.builder()
                .id(folder.getId())
                .userId(folder.getUserId())
                .parentId(folder.getParentId())
                .name(folder.getName())
                .path(folder.getPath())
                .fileCount(fileCount)
                .totalSize(totalSize)
                .created(folder.getCreated())
                .updated(folder.getUpdated())
                .build();
    }
}
