package com.techhub.app.courseservice.dto.response;

import com.techhub.app.courseservice.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailResponse {

    private CourseSummaryResponse summary;
    private List<ChapterResponse> chapters;
    private EnrollmentStatus enrollmentStatus;
    private boolean enrolled;
    private long totalChapters;
    private long totalLessons;
    private long totalEstimatedDurationMinutes;
    private Double overallProgress;
    private UUID currentChapterId;
    private List<UUID> unlockedChapterIds;
    private List<UUID> lockedChapterIds;
    private long completedLessons;
}
