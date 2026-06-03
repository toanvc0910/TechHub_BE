package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

       // Initialize courseSkills + skill on the given (already-paged) courses in a single query.
       // Fetch one collection per query to avoid MultipleBagFetchException.
       @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.courseSkills cs LEFT JOIN FETCH cs.skill " +
                     "WHERE c.id IN :ids")
       List<Course> fetchWithSkills(@Param("ids") List<UUID> ids);

       // Initialize courseTags + tag on the given (already-paged) courses in a single query.
       @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.courseTags ct LEFT JOIN FETCH ct.tag " +
                     "WHERE c.id IN :ids")
       List<Course> fetchWithTags(@Param("ids") List<UUID> ids);

       @Query(value = "SELECT * FROM courses c " +
                     "WHERE c.is_active = 'Y' " +
                     "AND (:status IS NULL OR c.status = CAST(:status AS course_status)) " +
                     "AND (" +
                     "  :search IS NULL OR " +
                     "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
                     ")", countQuery = "SELECT COUNT(*) FROM courses c " +
                                   "WHERE c.is_active = 'Y' " +
                                   "AND (:status IS NULL OR c.status = CAST(:status AS course_status)) " +
                                   "AND (" +
                                   "  :search IS NULL OR " +
                                   "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                   "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
                                   ")", nativeQuery = true)
       Page<Course> searchCourses(@Param("status") String status,
                     @Param("search") String search,
                     Pageable pageable);

       @Query(value = "SELECT * FROM courses c " +
                     "WHERE c.is_active = 'Y' " +
                     "AND c.instructor_id = CAST(:instructorId AS uuid) " +
                     "AND (" +
                     "  :search IS NULL OR " +
                     "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
                     ")", countQuery = "SELECT COUNT(*) FROM courses c " +
                                   "WHERE c.is_active = 'Y' " +
                                   "AND c.instructor_id = CAST(:instructorId AS uuid) " +
                                   "AND (" +
                                   "  :search IS NULL OR " +
                                   "  LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                   "  LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
                                   ")", nativeQuery = true)
       Page<Course> searchInstructorCourses(@Param("instructorId") UUID instructorId,
                     @Param("search") String search,
                     Pageable pageable);

       Optional<Course> findByIdAndIsActiveTrue(UUID id);
}
