package com.techhub.app.paymentservice.dto.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreatePayoutRequestRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    private String note;

    /** Currency của request: VND hoặc USD. Mặc định VND. BE sẽ quy đổi sang VND để xử lý. */
    private String currency;
}
