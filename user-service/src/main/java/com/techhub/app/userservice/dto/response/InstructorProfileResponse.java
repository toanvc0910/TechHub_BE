package com.techhub.app.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfileResponse {

    private UUID userId;
    private UUID sourceApplicationId;

    // CCCD (chỉ trả cho owner và admin; ẩn khi public)
    private String idNumber;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String placeOfOrigin;
    private String placeOfResidence;
    private String cccdIssueDate;
    private String cccdIssuePlace;
    private String cccdMrz;
    private String identifyingFeatures;

    // CV
    private String cvSummary;
    private String cvEmail;
    private String cvPhone;
    private String cvLocation;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private Integer yearsOfExperience;
    // Parsed JSON arrays/objects (Object cho phép List/Map)
    private Object skills;
    private Object languages;
    private Object education;
    private Object experience;
    private Object projects;
    private Object cvCertifications;

    private Object certificates;

    private LocalDateTime created;
    private LocalDateTime updated;
}
