package com.techhub.app.courseservice.repository;

import com.techhub.app.courseservice.entity.Comment;
import com.techhub.app.courseservice.enums.CommentTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.parent p " +
           "WHERE c.targetId = :targetId " +
           "AND c.targetType = :targetType " +
           "AND c.isActive = true " +
           "ORDER BY c.created ASC")
    List<Comment> findAllByTarget(@Param("targetId") UUID targetId, @Param("targetType") CommentTarget targetType);

    Optional<Comment> findByIdAndIsActiveTrue(UUID id);
}
