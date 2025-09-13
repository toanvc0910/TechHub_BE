package com.techhub.app.proxyclient.exception.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GlobalReponseException implements Serializable {
    private Integer status = HttpStatus.OK.value();
    private String message;
    private Object data;
    private String errors;

}