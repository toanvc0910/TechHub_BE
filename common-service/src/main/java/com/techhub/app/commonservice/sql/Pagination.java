package com.techhub.app.commonservice.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {
    Integer pageSize;
    Integer page;
    Integer totalPage;
    Long totalCount;
}
