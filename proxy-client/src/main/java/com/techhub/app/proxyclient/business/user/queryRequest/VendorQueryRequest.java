package com.techhub.app.proxyclient.business.user.queryRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorQueryRequest extends BaseQueryRequest {
    private String keyword;
    private String code;
    private String name;
    private String phone1;
    private String address1;
    private String taxCode;
    private  String email;
    private BigDecimal debitAmountFrom;
    private BigDecimal debitAmountTo;
    private String area;
    private BigDecimal transactionAmountFrom;
    private BigDecimal transactionAmountTo;
    private  Integer partnerGroupId;
}