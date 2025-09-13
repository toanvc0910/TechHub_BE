package com.techhub.app.proxyclient.business.user.queryRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryRequest extends BaseQueryRequest {
  String fullName;
}