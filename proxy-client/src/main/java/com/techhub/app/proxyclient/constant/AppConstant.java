package com.techhub.app.proxyclient.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class AppConstant {

    public static final String LOCAL_DATE_FORMAT = "dd-MM-yyyy";
    public static final String LOCAL_DATE_TIME_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";
    public static final String ZONED_DATE_TIME_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";
    public static final String INSTANT_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public abstract class DiscoveredDomainsApi {

        public static final String USER_SERVICE_HOST = "http://USER-SERVICE/user-service";
        public static final String USER_SERVICE_API_URL = "http://USER-SERVICE/user-service/api/users";

        public static final String COURSE_SERVICE_HOST = "http://COURSE-SERVICE/course-service";
        public static final String COURSE_SERVICE_API_URL = "http://COURSE-SERVICE/course-service/api/courses";

        public static final String BLOG_SERVICE_HOST = "http://BLOG-SERVICE/blog-service";
        public static final String BLOG_SERVICE_API_URL = "http://BLOG-SERVICE/blog-service/api/blogs";

        public static final String LEARNING_PATH_SERVICE_HOST = "http://LEARNING-PATH-SERVICE/learning-path-service";
        public static final String LEARNING_PATH_SERVICE_API_URL = "http://LEARNING-PATH-SERVICE/learning-path-service/api/learning-paths";

        public static final String PAYMENT_SERVICE_HOST = "http://PAYMENT-SERVICE/payment-service";
        public static final String PAYMENT_SERVICE_API_URL = "http://PAYMENT-SERVICE/payment-service/api/payments";

        public static final String AI_SERVICE_HOST = "http://AI-SERVICE/ai-service";
        public static final String AI_SERVICE_API_URL = "http://AI-SERVICE/ai-service/api/ai";
    }
}
