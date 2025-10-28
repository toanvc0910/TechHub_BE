package com.techhub.app.fileservice.dto.response;

import com.techhub.app.fileservice.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStatisticsResponse {

    private Long totalFiles;
    private Long totalSize;
    private Map<FileTypeEnum, TypeStatistics> byType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeStatistics {
        private Long count;
        private Long totalSize;
    }
}
