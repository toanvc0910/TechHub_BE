package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.LessonAsset;
import com.techhub.app.courseservice.enums.LessonAssetType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class LessonDurationCalculator {

    private static final int WORDS_PER_MINUTE = 200;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MIN_CONTENT_SECONDS = 60;

    public Integer calculate(LessonRequest request) {
        if (request == null) {
            return null;
        }
        return normalizeTotal(videoDurationSeconds(request.getVideoDuration()), contentDurationSeconds(request.getContent()));
    }

    public Integer calculate(Lesson lesson, List<LessonAsset> assets, Integer requestVideoDuration) {
        return calculate(lesson, assets, requestVideoDuration, Map.of());
    }

    public Integer calculate(Lesson lesson, List<LessonAsset> assets, Integer requestVideoDuration,
            Map<UUID, Integer> videoFileDurations) {
        if (lesson == null) {
            return null;
        }
        int videoSeconds = videoDurationSeconds(requestVideoDuration);
        videoSeconds += uploadedVideoDurationSeconds(assets, videoFileDurations);
        return normalizeTotal(videoSeconds, contentDurationSeconds(lesson.getContent()));
    }

    private int uploadedVideoDurationSeconds(List<LessonAsset> assets, Map<UUID, Integer> videoFileDurations) {
        if (assets == null || assets.isEmpty()) {
            return 0;
        }
        return assets.stream()
                .filter(asset -> asset != null && asset.getAssetType() == LessonAssetType.VIDEO)
                .mapToInt(asset -> assetDurationSeconds(asset, videoFileDurations))
                .sum();
    }

    private int assetDurationSeconds(LessonAsset asset, Map<UUID, Integer> videoFileDurations) {
        if (asset.getFileId() != null && videoFileDurations != null) {
            int fileDuration = videoDurationSeconds(videoFileDurations.get(asset.getFileId()));
            if (fileDuration > 0) {
                return fileDuration;
            }
        }
        return metadataDurationSeconds(asset.getMetadata());
    }

    private int metadataDurationSeconds(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return 0;
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            if (key.equals("duration")
                    || key.equals("durationseconds")
                    || key.equals("duration_seconds")
                    || key.equals("videoduration")
                    || key.equals("video_duration")
                    || key.equals("videodurationseconds")
                    || key.equals("video_duration_seconds")) {
                int seconds = videoDurationSeconds(entry.getValue());
                if (seconds > 0) {
                    return seconds;
                }
            }
        }
        return 0;
    }

    private int videoDurationSeconds(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return Math.max(0, (int) Math.round(number.doubleValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, (int) Math.round(Double.parseDouble(text)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int contentDurationSeconds(String content) {
        int wordCount = wordCount(content);
        if (wordCount == 0) {
            return 0;
        }
        int seconds = (int) Math.ceil((wordCount * (double) SECONDS_PER_MINUTE) / WORDS_PER_MINUTE);
        return Math.max(MIN_CONTENT_SECONDS, seconds);
    }

    private int wordCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String plainText = content
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("[\\p{Punct}&&[^\\p{IsAlphabetic}\\p{IsDigit}]]+", " ")
                .trim();
        if (plainText.isEmpty()) {
            return 0;
        }
        return plainText.split("\\s+").length;
    }

    private Integer normalizeTotal(int videoSeconds, int contentSeconds) {
        int total = Math.max(0, videoSeconds) + Math.max(0, contentSeconds);
        return total == 0 ? null : total;
    }
}
