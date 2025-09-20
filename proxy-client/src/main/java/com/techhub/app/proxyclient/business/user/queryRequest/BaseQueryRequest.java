package com.techhub.app.proxyclient.business.user.queryRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseQueryRequest {
    private int page=0;
    private int pageSize=15;
    private String order="desc";
    private String sortBy="created";
}