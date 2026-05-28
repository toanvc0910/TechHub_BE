package com.techhub.app.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instructor_profiles")
@TypeDef(name = "jsonb", typeClass = com.techhub.app.userservice.config.JsonbType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_application_id")
    private UUID sourceApplicationId;

    // CCCD front
    @Column(name = "id_number", length = 20)
    private String idNumber;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "date_of_birth", length = 20)
    private String dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "place_of_origin", columnDefinition = "TEXT")
    private String placeOfOrigin;

    @Column(name = "place_of_residence", columnDefinition = "TEXT")
    private String placeOfResidence;

    // CCCD back
    @Column(name = "cccd_issue_date", length = 20)
    private String cccdIssueDate;

    @Column(name = "cccd_issue_place", columnDefinition = "TEXT")
    private String cccdIssuePlace;

    @Column(name = "cccd_mrz", columnDefinition = "TEXT")
    private String cccdMrz;

    @Column(name = "identifying_features", columnDefinition = "TEXT")
    private String identifyingFeatures;

    // CV
    @Column(name = "cv_summary", columnDefinition = "TEXT")
    private String cvSummary;

    @Column(name = "cv_email", length = 200)
    private String cvEmail;

    @Column(name = "cv_phone", length = 50)
    private String cvPhone;

    @Column(name = "cv_location", length = 200)
    private String cvLocation;

    @Column(name = "linkedin_url", columnDefinition = "TEXT")
    private String linkedinUrl;

    @Column(name = "github_url", columnDefinition = "TEXT")
    private String githubUrl;

    @Column(name = "portfolio_url", columnDefinition = "TEXT")
    private String portfolioUrl;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Type(type = "jsonb")
    @Column(name = "skills", columnDefinition = "jsonb")
    private String skills;

    @Type(type = "jsonb")
    @Column(name = "languages", columnDefinition = "jsonb")
    private String languages;

    @Type(type = "jsonb")
    @Column(name = "education", columnDefinition = "jsonb")
    private String education;

    @Type(type = "jsonb")
    @Column(name = "experience", columnDefinition = "jsonb")
    private String experience;

    @Type(type = "jsonb")
    @Column(name = "projects", columnDefinition = "jsonb")
    private String projects;

    @Type(type = "jsonb")
    @Column(name = "cv_certifications", columnDefinition = "jsonb")
    private String cvCertifications;

    @Type(type = "jsonb")
    @Column(name = "certificates", columnDefinition = "jsonb")
    private String certificates;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (created == null) created = now;
        updated = now;
    }

    @PreUpdate
    void onUpdate() {
        updated = LocalDateTime.now();
    }
}
