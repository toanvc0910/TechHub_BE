package com.techhub.app.paymentservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MarkPaidPayoutRequest {

    @NotBlank(message = "paymentReference is required")
    private String paymentReference;

    private String note;
}
