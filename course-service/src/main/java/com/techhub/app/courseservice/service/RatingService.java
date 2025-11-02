package com.techhub.app.courseservice.service;

import com.techhub.app.courseservice.dto.CreateRatingDTO;
import com.techhub.app.courseservice.dto.RatingDTO;
import com.techhub.app.courseservice.exception.ResourceNotFoundException;
import com.techhub.app.courseservice.model.Rating;
import com.techhub.app.courseservice.repository.RatingRepository;
import com.techhub.app.courseservice.utils.MapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MapperUtil mapperUtil;

    public RatingService(RatingRepository ratingRepository, MapperUtil mapperUtil) {
        this.ratingRepository = ratingRepository;
        this.mapperUtil = mapperUtil;
    }

    @Transactional
    public RatingDTO createRating(CreateRatingDTO createRatingDTO) {
        // Check if user already rated this target
        ratingRepository.findByUserIdAndTargetIdAndTargetType(
                createRatingDTO.getUserId(),
                createRatingDTO.getTargetId(),
                createRatingDTO.getTargetType())
                .ifPresent(r -> {
                    throw new IllegalArgumentException("User already rated this " + createRatingDTO.getTargetType());
                });

        Rating rating = mapperUtil.map(createRatingDTO, Rating.class);
        rating.setIsActive(true);

        Rating savedRating = ratingRepository.save(rating);
        return mapperUtil.map(savedRating, RatingDTO.class);
    }

    public RatingDTO getRatingById(UUID id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", "id", id));
        return mapperUtil.map(rating, RatingDTO.class);
    }

    public List<RatingDTO> getRatingsByUser(UUID userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);
        return mapperUtil.mapList(ratings, RatingDTO.class);
    }

    public List<RatingDTO> getRatingsByTarget(UUID targetId, String targetType) {
        List<Rating> ratings = ratingRepository.findByTargetIdAndTargetType(targetId, targetType);
        return mapperUtil.mapList(ratings, RatingDTO.class);
    }

    public Double getAverageRating(UUID targetId, String targetType) {
        List<Rating> ratings = ratingRepository.findByTargetIdAndTargetType(targetId, targetType);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    @Transactional
    public RatingDTO updateRating(UUID id, CreateRatingDTO updateRatingDTO) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", "id", id));

        if (updateRatingDTO.getScore() != null) {
            rating.setScore(updateRatingDTO.getScore());
        }
        if (updateRatingDTO.getComment() != null) {
            rating.setComment(updateRatingDTO.getComment());
        }

        Rating updatedRating = ratingRepository.save(rating);
        return mapperUtil.map(updatedRating, RatingDTO.class);
    }

    @Transactional
    public void deleteRating(UUID id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", "id", id));
        rating.setIsActive(false);
        ratingRepository.save(rating);
    }
}
