package com.techhub.app.fileservice.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public final class StorageObjectKeyUtils {

    private static final String USER_ROOT = "users";
    private static final String LIBRARY_ROOT = "library";

    private StorageObjectKeyUtils() {
    }

    public static String buildFolderStoragePath(String parentPath, String folderName) {
        String segment = sanitizePathSegment(folderName);
        if (segment.isBlank()) {
            throw new RuntimeException("Folder name is invalid for object storage");
        }

        String sanitizedParentPath = sanitizeFolderPath(parentPath);
        if (sanitizedParentPath == null || sanitizedParentPath.isBlank()) {
            return "/" + segment;
        }
        return "/" + sanitizedParentPath + "/" + segment;
    }

    public static String normalizeStoragePath(String path) {
        String sanitizedPath = sanitizeFolderPath(path);
        if (sanitizedPath == null || sanitizedPath.isBlank()) {
            return null;
        }
        return "/" + sanitizedPath;
    }

    public static String buildLibraryPrefix(UUID userId, String folderPath) {
        String prefix = USER_ROOT + "/" + userId + "/" + LIBRARY_ROOT;
        String sanitizedPath = sanitizeFolderPath(folderPath);
        if (sanitizedPath == null || sanitizedPath.isBlank()) {
            return prefix;
        }
        return prefix + "/" + sanitizedPath;
    }

    public static String buildFolderMarkerObjectKey(UUID userId, String folderPath) {
        String sanitizedPath = sanitizeFolderPath(folderPath);
        if (sanitizedPath == null || sanitizedPath.isBlank()) {
            throw new RuntimeException("Folder path is invalid");
        }
        return buildLibraryPrefix(userId, sanitizedPath) + "/";
    }

    public static String buildLegacyFolderMarkerObjectKey(UUID userId, String folderPath) {
        String sanitizedPath = sanitizeFolderPath(folderPath);
        if (sanitizedPath == null || sanitizedPath.isBlank()) {
            throw new RuntimeException("Folder path is invalid");
        }
        return buildLibraryPrefix(userId, sanitizedPath) + "/.keep";
    }

    public static String sanitizeFolderPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return Arrays.stream(path.split("/+"))
                .map(StorageObjectKeyUtils::sanitizePathSegment)
                .filter(segment -> !segment.isBlank())
                .collect(Collectors.joining("/"));
    }

    private static String sanitizePathSegment(String value) {
        if (value == null) {
            return "";
        }
        String asciiValue = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
        return asciiValue
                .replaceAll("[\\\\/]+", "-")
                .replaceAll("[^a-zA-Z0-9._ -]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_ .-]+|[_ .-]+$", "");
    }
}
