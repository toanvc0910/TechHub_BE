package com.techhub.app.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.techhub.app.userservice.dto.response.InstructorProfileResponse;
import com.techhub.app.userservice.entity.InstructorApplication;
import com.techhub.app.userservice.entity.InstructorApplicationCertificate;
import com.techhub.app.userservice.entity.InstructorProfile;
import com.techhub.app.userservice.repository.InstructorApplicationCertificateRepository;
import com.techhub.app.userservice.repository.InstructorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorProfileSyncService {

    private final InstructorProfileRepository profileRepository;
    private final InstructorApplicationCertificateRepository certificateRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Optional<InstructorProfileResponse> getProfile(UUID userId, boolean includeSensitive) {
        return profileRepository.findById(userId)
                .map(p -> toResponse(p, includeSensitive));
    }

    private InstructorProfileResponse toResponse(InstructorProfile p, boolean includeSensitive) {
        InstructorProfileResponse.InstructorProfileResponseBuilder b = InstructorProfileResponse.builder()
                .userId(p.getUserId())
                .sourceApplicationId(p.getSourceApplicationId())
                .fullName(p.getFullName())
                .nationality(p.getNationality())
                .cvSummary(p.getCvSummary())
                .cvEmail(p.getCvEmail())
                .cvPhone(p.getCvPhone())
                .cvLocation(p.getCvLocation())
                .linkedinUrl(p.getLinkedinUrl())
                .githubUrl(p.getGithubUrl())
                .portfolioUrl(p.getPortfolioUrl())
                .yearsOfExperience(p.getYearsOfExperience())
                .skills(parseObj(p.getSkills()))
                .languages(parseObj(p.getLanguages()))
                .education(parseObj(p.getEducation()))
                .experience(parseObj(p.getExperience()))
                .projects(parseObj(p.getProjects()))
                .cvCertifications(parseObj(p.getCvCertifications()))
                .certificates(parseObj(p.getCertificates()))
                .created(p.getCreated())
                .updated(p.getUpdated());

        if (includeSensitive) {
            b.idNumber(p.getIdNumber())
             .dateOfBirth(p.getDateOfBirth())
             .gender(p.getGender())
             .placeOfOrigin(p.getPlaceOfOrigin())
             .placeOfResidence(p.getPlaceOfResidence())
             .cccdIssueDate(p.getCccdIssueDate())
             .cccdIssuePlace(p.getCccdIssuePlace())
             .cccdMrz(p.getCccdMrz())
             .identifyingFeatures(p.getIdentifyingFeatures());
        }
        return b.build();
    }

    private Object parseObj(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void syncFromApplication(InstructorApplication app) {
        UUID userId = app.getUserId();
        InstructorProfile profile = profileRepository.findById(userId)
                .orElse(InstructorProfile.builder().userId(userId).build());
        profile.setSourceApplicationId(app.getId());

        JsonNode cv = parse(app.getAiExtractedData());
        JsonNode front = parse(app.getCccdFrontData());
        JsonNode back = parse(app.getCccdBackData());

        // CCCD front
        if (front != null) {
            profile.setIdNumber(text(front, "idNumber"));
            profile.setFullName(text(front, "fullName"));
            profile.setDateOfBirth(text(front, "dateOfBirth"));
            profile.setGender(text(front, "gender"));
            profile.setNationality(text(front, "nationality"));
            profile.setPlaceOfOrigin(text(front, "placeOfOrigin"));
            profile.setPlaceOfResidence(text(front, "placeOfResidence"));
        }

        // CCCD back
        if (back != null) {
            profile.setCccdIssueDate(text(back, "dateOfIssue"));
            profile.setCccdIssuePlace(text(back, "placeOfIssue"));
            profile.setCccdMrz(text(back, "mrz"));
            profile.setIdentifyingFeatures(text(back, "identifyingFeatures"));
        }

        // CV
        if (cv != null) {
            profile.setCvSummary(text(cv, "summary"));
            profile.setCvEmail(text(cv, "email"));
            profile.setCvPhone(text(cv, "phone"));
            profile.setCvLocation(text(cv, "location"));
            JsonNode links = cv.path("links");
            profile.setLinkedinUrl(text(links, "linkedin"));
            profile.setGithubUrl(text(links, "github"));
            profile.setPortfolioUrl(text(links, "portfolio"));
            if (cv.hasNonNull("yearsOfExperience") && cv.get("yearsOfExperience").isNumber()) {
                profile.setYearsOfExperience(cv.get("yearsOfExperience").asInt());
            }
            profile.setSkills(jsonOrNull(cv.get("skills")));
            profile.setLanguages(jsonOrNull(cv.get("languages")));
            profile.setEducation(jsonOrNull(cv.get("education")));
            profile.setExperience(jsonOrNull(cv.get("experience")));
            profile.setProjects(jsonOrNull(cv.get("projects")));
            profile.setCvCertifications(jsonOrNull(cv.get("certifications")));
        }

        // Uploaded certificates (mảng data trích xuất)
        List<InstructorApplicationCertificate> certs = certificateRepository
                .findByApplicationIdAndIsActiveTrueOrderByCreatedAsc(app.getId());
        if (!certs.isEmpty()) {
            ArrayNode arr = mapper.createArrayNode();
            for (InstructorApplicationCertificate c : certs) {
                JsonNode data = parse(c.getAiData());
                if (data != null) arr.add(data);
            }
            profile.setCertificates(arr.size() > 0 ? arr.toString() : null);
        }

        profileRepository.save(profile);
        log.info("[InstructorProfile] Synced userId={} from applicationId={}", userId, app.getId());
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            log.warn("[InstructorProfile] Parse JSON fail: {}", e.getMessage());
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        return v.isTextual() ? v.asText() : v.toString();
    }

    private String jsonOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.toString();
    }
}
