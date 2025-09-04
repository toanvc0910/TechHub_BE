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
		
		public static final String PRODUCT_SERVICE_HOST = "http://PRODUCT-SERVICE/product-service";
		public static final String PRODUCT_SERVICE_API_URL = "http://PRODUCT-SERVICE/product-service/api/products";

		public static final String BLOG_SERVICE_HOST = "http://BLOG-SERVICE/blog-service";
		public static final String BLOG_SERVICE_API_URL = "http://BLOG-SERVICE/blog-service/api/blogs";
	}
}









