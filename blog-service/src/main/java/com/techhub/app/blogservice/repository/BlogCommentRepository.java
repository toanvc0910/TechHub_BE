package com.techhub.app.blogservice.repository;

import com.techhub.app.blogservice.entity.BlogComment;
import com.techhub.app.blogservice.enums.CommentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogCommentRepository extends JpaRepository<BlogComment, UUID> {

    List<BlogComment> findByTargetIdAndTargetTypeAndIsActiveTrueOrderByCreatedAsc(UUID targetId, CommentTargetType targetType);

    Optional<BlogComment> findByIdAndTargetIdAndIsActiveTrue(UUID id, UUID targetId);
}
