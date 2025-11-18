package com.techhub.app.learningpathservice.dto.response;

import com.techhub.app.learningpathservice.enums.LeaderboardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankResponse {
    private UUID userId;
    private LeaderboardType leaderboardType;
    private UUID referenceId;
    private Integer rank;
}
