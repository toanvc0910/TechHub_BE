package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.request.LessonRequest;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.entity.LessonAsset;
import com.techhub.app.courseservice.enums.LessonAssetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LessonDurationCalculatorTest {

    private final LessonDurationCalculator calculator = new LessonDurationCalculator();

    @Test
    void calculatesVideoDurationPlusMinimumContentReadTime() {
        LessonRequest request = new LessonRequest();
        request.setVideoDuration(125);
        request.setContent("<p>Intro lesson content</p>");

        assertThat(calculator.calculate(request)).isEqualTo(185);
    }

    @Test
    void calculatesUploadedVideoAssetsPlusLessonContent() {
        Lesson lesson = new Lesson();
        lesson.setContent("Short text content");

        LessonAsset video = new LessonAsset();
        video.setAssetType(LessonAssetType.VIDEO);
        video.setMetadata(Map.of("duration", 300));

        LessonAsset document = new LessonAsset();
        document.setAssetType(LessonAssetType.DOCUMENT);
        document.setMetadata(Map.of("duration", 999));

        assertThat(calculator.calculate(lesson, List.of(video, document), null)).isEqualTo(360);
    }

    @Test
    void prefersFileServiceDurationOverAssetMetadataForUploadedVideos() {
        UUID fileId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        LessonAsset video = new LessonAsset();
        video.setAssetType(LessonAssetType.VIDEO);
        video.setFileId(fileId);
        video.setMetadata(Map.of("duration", 999));

        assertThat(calculator.calculate(lesson, List.of(video), null, Map.of(fileId, 240))).isEqualTo(240);
    }

    @Test
    void returnsNullWhenLessonHasNoTimedContent() {
        Lesson lesson = new Lesson();

        assertThat(calculator.calculate(lesson, List.of(), null)).isNull();
    }
}
