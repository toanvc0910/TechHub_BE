package com.techhub.app.userservice.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class AppConstant {
	
	public static final String LOCAL_DATE_FORMAT = "dd-MM-yyyy";
	public static final String LOCAL_DATE_TIME_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";
	public static final String ZONED_DATE_TIME_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";
	public static final String INSTANT_FORMAT = "dd-MM-yyyy__HH:mm:ss:SSSSSS";

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public abstract class ROLECODE {
		public static final String ROLE_ADMIN = "ADM";
		public static final String ROLE_USER = "ROLE_USER";
		public static final String ROLE_MANAGER = "ROLE_MANAGER";
		public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
	}
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public abstract class DiscoveredDomainsApi {

		public static final String GET_ORG_TENANT_CODE = "http://TENANT-SERVICE/tenant-service/api/v1/tenant/getInforTenantOrg";
		public  static final String SYSTEM_SERVICE_HOST_FINDBYNAME = "http://SYSTEM-SERVICE/system-service/api/v1/config/findByName/";
		public static final String USER_SERVICE_HOST = "http://USER-SERVICE/user-service";
		public static final String USER_SERVICE_API_URL = "http://USER-SERVICE/user-service/api/users";

		public static final String TENANT_SERVICE_API_FIND_ID = "http://TENANT-SERVICE/tenant-service/api/v1/tenant";

		public static final String ORG_API_FIND_ID = "http://TENANT-SERVICE/tenant-service/api/v1/org";

		public static final String PRODUCT_SERVICE_HOST = "http://PRODUCT-SERVICE/product-service";
		public static final String PRODUCT_SERVICE_API_URL = "http://PRODUCT-SERVICE/product-service/api/products";
		
		public static final String ORDER_SERVICE_HOST = "http://ORDER-SERVICE/order-service";
		public static final String ORDER_SERVICE_API_URL = "http://ORDER-SERVICE/order-service/api/orders";
		
		public static final String FAVOURITE_SERVICE_HOST = "http://FAVOURITE-SERVICE/favourite-service";
		public static final String FAVOURITE_SERVICE_API_URL = "http://FAVOURITE-SERVICE/favourite-service/api/favourites";
		
		public static final String PAYMENT_SERVICE_HOST = "http://PAYMENT-SERVICE/payment-service";
		public static final String PAYMENT_SERVICE_API_URL = "http://PAYMENT-SERVICE/payment-service/api/payments";
		
		public static final String SHIPPING_SERVICE_HOST = "http://SHIPPING-SERVICE/shipping-service";
		public static final String SHIPPING_SERVICE_API_URL = "http://SHIPPING-SERVICE/shipping-service/api/shippings";

		public static final String GET_ORG_BY_ERP_ID =  "http://TENANT-SERVICE/tenant-service/api/v1/org/getByErpId";
		public static final String GET_POSTERMINAL_BY_ERP_ID =  "http://TENANT-SERVICE/tenant-service/api/v1/terminal/getByErpId";

		public static final String CHECK_POSTERMINALBYERP_ID =  "http://TENANT-SERVICE/tenant-service/api/v1/terminal/intSaveUser";
		public static final String GET_TOKEN =  "http://PROXY-CLIENT/app/api/v1/authenticate/internal";

		public static final String PRODUCT_SERVICE_GET_WAREHOUSE_BY_ERP_ID = "http://PRODUCT-SERVICE/product-service/api/v1/warehouses/getByErpId";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public abstract class NameSystemConfig {

		public static final String MDM_URL_SAVE_IMAGE = "MDM_URL_SAVE_IMAGE";

	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public abstract class SYS_VALUE{

		public static final String ID_USER_ADMIN = "ID_USER_ADMIN";
		public static final String ID_PASSWORD_ADMIN = "ID_PASSWORD_ADMIN";
		public static final String ID_DOMAIN_URL = "ID_DOMAIN_URL";
		public static final String ID_LOGIN_USRPASS_URL = "ID_LOGIN_USRPASS_URL";
		public static final String ID_LOGIN_SOCIAL_URL = "ID_LOGIN_SOCIAL_URL";
		public static final String ID_CREATE_USER_URL = "ID_CREATE_USER_URL";
		public static final String ID_UPDATE_USER_URL = "ID_UPDATE_USER_URL";
	}

	public static final class IntegrationTopicKafka {

		// Ngăn chặn khởi tạo class
		private IntegrationTopicKafka() {}

		public static final String GROUP_ID = "gr-sync-order";

		public static final String CUS_INTEGRATION = "integrate-customer";
		public static final String BGP_INTEGRATION = "integrate-business-partner-group";

	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public abstract static class ImportFileStatus {
		public static final String IN_PROGRESS = "INP";
		public static final String FAILED = "FAI";
		public static final String SUCCESS = "COM";
	}
	
}



