package com.techhub.app.courseservice.service;

import java.util.UUID;

/**
 * Service for sending course-related notifications.
 * - New course: Broadcast to all users
 * - New lesson/asset/exercise: Send to enrolled students only
 */
public interface CourseNotificationService {

    /**
     * Notify all users about a new course (broadcast)
     * 
     * @param courseId    The course ID
     * @param courseTitle The course title
     */
    void notifyNewCourse(UUID courseId, String courseTitle);

    /**
     * Notify enrolled students about a new lesson
     * 
     * @param courseId    The course ID
     * @param courseTitle The course title
     * @param lessonId    The lesson ID
     * @param lessonTitle The lesson title
     */
    void notifyNewLesson(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle);

    /**
     * Notify enrolled students about new content (asset)
     * 
     * @param courseId    The course ID
     * @param courseTitle The course title
     * @param lessonId    The lesson ID
     * @param lessonTitle The lesson title
     * @param assetType   The type of asset
     */
    void notifyNewContent(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle, String assetType);

    /**
     * Notify enrolled students about a new exercise
     * 
     * @param courseId    The course ID
     * @param courseTitle The course title
     * @param lessonId    The lesson ID
     * @param lessonTitle The lesson title
     */
    void notifyNewExercise(UUID courseId, String courseTitle, UUID lessonId, String lessonTitle);
}
