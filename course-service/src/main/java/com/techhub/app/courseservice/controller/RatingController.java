package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.CreateRatingDTO;
import com.techhub.app.courseservice.dto.RatingDTO;
import com.techhub.app.courseservice.service.RatingService;
import com.techhub.app.courseservice.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ratings")
@Tag(name = "Rating Management", description = "APIs for managing ratings and reviews")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @Operation(summary = "Create a new rating")
    public ResponseEntity<ResponseWrapper<RatingDTO>> createRating(@Valid @RequestBody CreateRatingDTO createRatingDTO) {
        RatingDTO ratingDTO = ratingService.createRating(createRatingDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(ratingDTO, "Rating created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rating by ID")
    public ResponseEntity<ResponseWrapper<RatingDTO>> getRatingById(@PathVariable UUID id) {
        RatingDTO ratingDTO = ratingService.getRatingById(id);
        return ResponseEntity.ok(ResponseWrapper.success(ratingDTO));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get ratings by user")
    public ResponseEntity<ResponseWrapper<List<RatingDTO>>> getRatingsByUser(@PathVariable UUID userId) {
        List<RatingDTO> ratings = ratingService.getRatingsByUser(userId);
        return ResponseEntity.ok(ResponseWrapper.success(ratings));
    }

    @GetMapping("/target/{targetId}")
    @Operation(summary = "Get ratings by target")
    public ResponseEntity<ResponseWrapper<List<RatingDTO>>> getRatingsByTarget(
            @PathVariable UUID targetId,
            @RequestParam String targetType) {
        List<RatingDTO> ratings = ratingService.getRatingsByTarget(targetId, targetType);
        return ResponseEntity.ok(ResponseWrapper.success(ratings));
    }

    @GetMapping("/target/{targetId}/average")
    @Operation(summary = "Get average rating for target")
    public ResponseEntity<ResponseWrapper<Double>> getAverageRating(
            @PathVariable UUID targetId,
            @RequestParam String targetType) {
        Double average = ratingService.getAverageRating(targetId, targetType);
        return ResponseEntity.ok(ResponseWrapper.success(average));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update rating")
    public ResponseEntity<ResponseWrapper<RatingDTO>> updateRating(
            @PathVariable UUID id,
            @RequestBody CreateRatingDTO updateRatingDTO) {
        RatingDTO ratingDTO = ratingService.updateRating(id, updateRatingDTO);
        return ResponseEntity.ok(ResponseWrapper.success(ratingDTO, "Rating updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete rating")
    public ResponseEntity<ResponseWrapper<Void>> deleteRating(@PathVariable UUID id) {
        ratingService.deleteRating(id);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Rating deleted successfully"));
    }
}
