package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateExerciseDTO;
import com.techhub.app.courseservice.dto.ExerciseDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Exercise;
import com.techhub.app.courseservice.model.Lesson;
import com.techhub.app.courseservice.repository.ExerciseRepository;
import com.techhub.app.courseservice.repository.LessonRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final MapperUtil mapperUtil;

    public ExerciseService(ExerciseRepository exerciseRepository, LessonRepository lessonRepository, MapperUtil mapperUtil) {
        this.exerciseRepository = exerciseRepository;
        this.lessonRepository = lessonRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public ExerciseDTO createExercise(CreateExerciseDTO createExerciseDTO) {
        Lesson lesson = lessonRepository.findById(createExerciseDTO.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", createExerciseDTO.getLessonId()));

        Exercise exercise = new Exercise();
        exercise.setType(createExerciseDTO.getType());
        exercise.setQuestion(createExerciseDTO.getQuestion());
        exercise.setTestCases(createExerciseDTO.getTestCases());
        exercise.setLesson(lesson);
        exercise.setOptions(createExerciseDTO.getOptions());
        exercise.setIsActive(true);

        Exercise savedExercise = exerciseRepository.save(exercise);
        ExerciseDTO dto = mapperUtil.map(savedExercise, ExerciseDTO.class);
        dto.setLessonId(lesson.getId());
        return dto;
    }

    public ExerciseDTO getExerciseById(UUID id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", id));
        ExerciseDTO dto = mapperUtil.map(exercise, ExerciseDTO.class);
        dto.setLessonId(exercise.getLesson().getId());
        return dto;
    }

    public List<ExerciseDTO> getAllExercises() {
        List<Exercise> exercises = exerciseRepository.findAll();
        return mapperUtil.mapList(exercises, ExerciseDTO.class);
    }

    public List<ExerciseDTO> getExercisesByLesson(UUID lessonId) {
        List<Exercise> exercises = exerciseRepository.findByLessonId(lessonId);
        return mapperUtil.mapList(exercises, ExerciseDTO.class);
    }

    @Transactional
    public ExerciseDTO updateExercise(UUID id, CreateExerciseDTO updateExerciseDTO) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", id));

        if (updateExerciseDTO.getType() != null) {
            exercise.setType(updateExerciseDTO.getType());
        }
        if (updateExerciseDTO.getQuestion() != null) {
            exercise.setQuestion(updateExerciseDTO.getQuestion());
        }
        if (updateExerciseDTO.getTestCases() != null) {
            exercise.setTestCases(updateExerciseDTO.getTestCases());
        }
        if (updateExerciseDTO.getOptions() != null) {
            exercise.setOptions(updateExerciseDTO.getOptions());
        }

        Exercise updatedExercise = exerciseRepository.save(exercise);
        ExerciseDTO dto = mapperUtil.map(updatedExercise, ExerciseDTO.class);
        dto.setLessonId(updatedExercise.getLesson().getId());
        return dto;
    }

    @Transactional
    public void deleteExercise(UUID id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", id));
        exercise.setIsActive(false);
        exerciseRepository.save(exercise);
    }
}

