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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {

    private final FileFolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final ObjectStorageService objectStorageService;

    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {
        String folderName = normalizeFolderName(request.getName());

        // Check if folder with same name exists in the same parent
        if (folderRepository.existsByUserIdAndNameAndParentIdAndIsActive(
                request.getUserId(), folderName, request.getParentId(), "Y")) {
            throw new RuntimeException("Folder with this name already exists in the same location");
        }

        FileFolderEntity folder = new FileFolderEntity();
        folder.setUserId(request.getUserId());
        folder.setParentId(request.getParentId());
        folder.setName(folderName);
        folder.setIsActive("Y");
        folder.setCreatedBy(request.getUserId());

        // Calculate path - will be set by trigger, but we can set it manually too
        if (request.getParentId() != null) {
            FileFolderEntity parent = folderRepository
                    .findByIdAndUserIdAndIsActive(request.getParentId(), request.getUserId(), "Y")
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            folder.setPath(parent.getPath() + "/" + folderName);
        } else {
            folder.setPath("/" + folderName);
        }

        FileFolderEntity saved = folderRepository.save(folder);
        createFolderMarker(saved);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFoldersByUser(UUID userId) {
        List<FileFolderEntity> folders = folderRepository.findAllByUserIdOrderByPath(userId, "Y");
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

        String oldPath = folder.getPath();
        if (request.getName() != null) {
            String folderName = normalizeFolderName(request.getName());
            if (folderName.equals(folder.getName())) {
                folder.setName(folderName);
            } else {
                // Check for duplicate name
                if (folderRepository.existsByUserIdAndNameAndParentIdAndIsActive(
                        userId, folderName, folder.getParentId(), "Y")) {
                    throw new RuntimeException("Folder with this name already exists in the same location");
                }
                folder.setName(folderName);
                folder.setPath(buildFolderPath(userId, folder.getParentId(), folderName));
            }
        }

        if (request.getParentId() != null && !request.getParentId().equals(folder.getParentId())) {
            // Move folder
            moveFolder(folder, request.getParentId());
        }

        folder.setUpdatedBy(userId);
        FileFolderEntity updated = folderRepository.save(folder);
        updateDescendantPaths(userId, oldPath, updated.getPath());
        replaceFolderMarker(updated, oldPath);
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
        deleteFolderMarker(folder);
    }

    private void moveFolder(FileFolderEntity folder, UUID newParentId) {
        if (newParentId != null) {
            FileFolderEntity newParent = folderRepository.findByIdAndUserIdAndIsActive(newParentId, folder.getUserId(),
                    "Y")
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

    private String buildFolderPath(UUID userId, UUID parentId, String folderName) {
        if (parentId == null) {
            return "/" + folderName;
        }
        FileFolderEntity parent = folderRepository.findByIdAndUserIdAndIsActive(parentId, userId, "Y")
                .orElseThrow(() -> new RuntimeException("Parent folder not found"));
        return parent.getPath() + "/" + folderName;
    }

    private void updateDescendantPaths(UUID userId, String oldPath, String newPath) {
        if (oldPath == null || newPath == null || oldPath.equals(newPath)) {
            return;
        }
        List<FileFolderEntity> descendants = folderRepository.findByUserIdAndPathStartsWith(
                userId, oldPath + "/%", "Y");
        for (FileFolderEntity descendant : descendants) {
            descendant.setPath(newPath + descendant.getPath().substring(oldPath.length()));
            descendant.setUpdatedBy(userId);
        }
        folderRepository.saveAll(descendants);
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

    private void createFolderMarker(FileFolderEntity folder) {
        String markerObjectKey = buildFolderMarkerObjectKey(folder);
        objectStorageService.upload(new ByteArrayInputStream(new byte[0]), 0, "application/x-directory",
                markerObjectKey);
    }

    private void replaceFolderMarker(FileFolderEntity folder, String oldPath) {
        createFolderMarker(folder);
        if (oldPath == null || oldPath.equals(folder.getPath())) {
            return;
        }
        deleteFolderMarker(folder.getUserId(), oldPath);
    }

    private void deleteFolderMarker(FileFolderEntity folder) {
        deleteFolderMarker(folder.getUserId(), folder.getPath());
    }

    private void deleteFolderMarker(UUID userId, String path) {
        String markerObjectKey = buildFolderMarkerObjectKey(userId, path);
        Runnable cleanup = () -> CompletableFuture.runAsync(() -> {
            try {
                objectStorageService.delete(markerObjectKey);
            } catch (RuntimeException ex) {
                log.warn("Failed to delete MinIO folder marker for user {} path {}", userId, path, ex);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
            return;
        }

        cleanup.run();
    }

    private String buildFolderMarkerObjectKey(FileFolderEntity folder) {
        return buildFolderMarkerObjectKey(folder.getUserId(), folder.getPath());
    }

    private String buildFolderMarkerObjectKey(UUID userId, String path) {
        String folderPath = sanitizeFolderPath(path);
        if (folderPath == null || folderPath.isBlank()) {
            throw new RuntimeException("Folder path is invalid");
        }
        return "users/" + userId + "/library/" + folderPath + "/.keep";
    }

    private String normalizeFolderName(String value) {
        if (value == null) {
            throw new RuntimeException("Folder name is required");
        }
        String normalized = value.trim().replaceAll("[\\\\/]+", "-");
        if (normalized.isBlank()) {
            throw new RuntimeException("Folder name is required");
        }
        return normalized;
    }

    private String sanitizeFolderPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return Arrays.stream(path.split("/+"))
                .map(this::sanitizePathSegment)
                .filter(segment -> !segment.isBlank())
                .collect(Collectors.joining("/"));
    }

    private String sanitizePathSegment(String value) {
        if (value == null) {
            return "";
        }
        String asciiValue = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return asciiValue
                .replaceAll("[\\\\/]+", "-")
                .replaceAll("[^a-zA-Z0-9._ -]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_ .-]+|[_ .-]+$", "");
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
