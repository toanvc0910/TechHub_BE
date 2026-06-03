package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.client.UserServiceClient;
import com.techhub.app.courseservice.dto.response.LeaderboardEntryResponse;
import com.techhub.app.courseservice.entity.Chapter;
import com.techhub.app.courseservice.entity.Course;
import com.techhub.app.courseservice.entity.Lesson;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void mapsStringUserIdReturnedByNativeQuery() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime firstAt = OffsetDateTime.now();

        Course course = new Course();
        course.setId(courseId);
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setChapter(chapter);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(submissionRepository.findLessonLeaderboard(lessonId, 10))
                .thenReturn(List.<Object[]>of(new Object[]{userId.toString(), 1.5d, 2L, firstAt}));

        List<LeaderboardEntryResponse> result = leaderboardService.getLessonLeaderboard(courseId, lessonId, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getScore()).isEqualTo(1.5d);
        assertThat(result.get(0).getAttempts()).isEqualTo(2L);
        assertThat(result.get(0).getFirstAt()).isEqualTo(firstAt);
    }
}
