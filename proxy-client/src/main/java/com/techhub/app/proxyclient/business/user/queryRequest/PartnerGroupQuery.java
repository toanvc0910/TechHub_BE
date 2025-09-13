package com.techhub.app.proxyclient.business.user.queryRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerGroupQuery extends BaseQueryRequest {
    String keyword;
    String code;
    String name;
    String isSummary;
    String isCustomer;
}
