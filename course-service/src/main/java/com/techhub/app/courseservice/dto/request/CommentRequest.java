package com.techhub.app.courseservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.UUID;

@Getter
@Setter
public class CommentRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;

    private UUID parentId;
}
