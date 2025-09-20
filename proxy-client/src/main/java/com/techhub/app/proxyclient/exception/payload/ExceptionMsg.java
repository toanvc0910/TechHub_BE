package com.techhub.app.proxyclient.exception.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionMsg {
    private ZonedDateTime timestamp;
    private HttpStatus status;
    private String error;
}
