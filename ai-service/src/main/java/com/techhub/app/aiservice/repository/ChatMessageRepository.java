package com.techhub.app.aiservice.repository;

import com.techhub.app.aiservice.entity.ChatMessage;
import com.techhub.app.aiservice.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionOrderByTimestampAsc(ChatSession session);
}
