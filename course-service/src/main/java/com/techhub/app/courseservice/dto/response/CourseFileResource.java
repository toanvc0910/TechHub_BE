package com.techhub.app.courseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFileResource {

    private UUID fileId;
    private String name;
    private String originalName;
    private String url;
    private String secureUrl;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer duration;
}
