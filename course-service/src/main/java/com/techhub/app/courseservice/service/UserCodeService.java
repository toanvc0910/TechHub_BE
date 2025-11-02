package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateUserCodeDTO;
import com.techhub.app.courseservice.dto.UserCodeDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Lesson;
import com.techhub.app.courseservice.model.UserCode;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.repository.UserCodeRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserCodeService {

    private final UserCodeRepository userCodeRepository;
    private final LessonRepository lessonRepository;
    private final MapperUtil mapperUtil;

    public UserCodeService(UserCodeRepository userCodeRepository, LessonRepository lessonRepository, MapperUtil mapperUtil) {
        this.userCodeRepository = userCodeRepository;
        this.lessonRepository = lessonRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public UserCodeDTO saveUserCode(CreateUserCodeDTO createUserCodeDTO) {
        Lesson lesson = lessonRepository.findById(createUserCodeDTO.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", createUserCodeDTO.getLessonId()));

        UserCode userCode = userCodeRepository.findByUserIdAndLessonIdAndLanguage(
                createUserCodeDTO.getUserId(),
                createUserCodeDTO.getLessonId(),
                createUserCodeDTO.getLanguage())
                .orElse(new UserCode());

        userCode.setUserId(createUserCodeDTO.getUserId());
        userCode.setLesson(lesson);
        userCode.setCode(createUserCodeDTO.getCode());
        userCode.setLanguage(createUserCodeDTO.getLanguage());
        userCode.setSavedAt(LocalDateTime.now());
        userCode.setIsActive(true);

        UserCode savedUserCode = userCodeRepository.save(userCode);
        UserCodeDTO dto = mapperUtil.map(savedUserCode, UserCodeDTO.class);
        dto.setLessonId(lesson.getId());
        return dto;
    }

    public UserCodeDTO getUserCodeById(UUID id) {
        UserCode userCode = userCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserCode", "id", id));
        UserCodeDTO dto = mapperUtil.map(userCode, UserCodeDTO.class);
        dto.setLessonId(userCode.getLesson().getId());
        return dto;
    }

    public List<UserCodeDTO> getUserCodesByUser(UUID userId) {
        List<UserCode> userCodes = userCodeRepository.findByUserId(userId);
        return mapperUtil.mapList(userCodes, UserCodeDTO.class);
    }

    public List<UserCodeDTO> getUserCodesByLesson(UUID lessonId) {
        List<UserCode> userCodes = userCodeRepository.findByLessonId(lessonId);
        return mapperUtil.mapList(userCodes, UserCodeDTO.class);
    }

    @Transactional
    public void deleteUserCode(UUID id) {
        UserCode userCode = userCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserCode", "id", id));
        userCode.setIsActive(false);
        userCodeRepository.save(userCode);
    }
}

