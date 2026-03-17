package com.techhub.app.fileservice.service.impl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoredObjectDetails {

    String bucket;
    String objectKey;
    String publicUrl;
}