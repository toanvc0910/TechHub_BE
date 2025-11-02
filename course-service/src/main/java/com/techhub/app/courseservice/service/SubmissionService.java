package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateSubmissionDTO;
import com.techhub.app.courseservice.dto.SubmissionDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Exercise;
import com.techhub.app.courseservice.model.Submission;
import com.techhub.app.courseservice.repository.ExerciseRepository;
import com.techhub.app.courseservice.repository.SubmissionRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExerciseRepository exerciseRepository;
    private final MapperUtil mapperUtil;

    public SubmissionService(SubmissionRepository submissionRepository, ExerciseRepository exerciseRepository, MapperUtil mapperUtil) {
        this.submissionRepository = submissionRepository;
        this.exerciseRepository = exerciseRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public SubmissionDTO createSubmission(CreateSubmissionDTO createSubmissionDTO) {
        Exercise exercise = exerciseRepository.findById(createSubmissionDTO.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", createSubmissionDTO.getExerciseId()));

        Submission submission = new Submission();
        submission.setUserId(createSubmissionDTO.getUserId());
        submission.setExercise(exercise);
        submission.setAnswer(createSubmissionDTO.getAnswer());
        submission.setIsActive(true);

        Submission savedSubmission = submissionRepository.save(submission);
        SubmissionDTO dto = mapperUtil.map(savedSubmission, SubmissionDTO.class);
        dto.setExerciseId(exercise.getId());
        return dto;
    }

    public SubmissionDTO getSubmissionById(UUID id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
        SubmissionDTO dto = mapperUtil.map(submission, SubmissionDTO.class);
        dto.setExerciseId(submission.getExercise().getId());
        return dto;
    }

    public List<SubmissionDTO> getSubmissionsByUser(UUID userId) {
        List<Submission> submissions = submissionRepository.findByUserId(userId);
        return mapperUtil.mapList(submissions, SubmissionDTO.class);
    }

    public List<SubmissionDTO> getSubmissionsByExercise(UUID exerciseId) {
        List<Submission> submissions = submissionRepository.findByExerciseId(exerciseId);
        return mapperUtil.mapList(submissions, SubmissionDTO.class);
    }

    @Transactional
    public SubmissionDTO gradeSubmission(UUID id, Double grade) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));

        submission.setGrade(grade);
        submission.setGradedAt(LocalDateTime.now());

        Submission updatedSubmission = submissionRepository.save(submission);
        SubmissionDTO dto = mapperUtil.map(updatedSubmission, SubmissionDTO.class);
        dto.setExerciseId(updatedSubmission.getExercise().getId());
        return dto;
    }

    @Transactional
    public void deleteSubmission(UUID id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
        submission.setIsActive(false);
        submissionRepository.save(submission);
    }
}

