package com.bs.theme.bob.template.util;

import com.bs.themebridge.token.util.ConfigurationUtil;

/**
 * ThemeConstant used to define the reusable global names ThemeBridge
 * 
 * @author PRASATH RAVICHANDRAN
 * 
 */
public class KotakConstant {

	/** Kotak Bank Finacle constants **/
	public static final String BANKID = "01";
	public static final String CHANNELID = "COR";
	public static final String ZONE_NAME = "ZONE";
	public static final String SERVICEREQUESTVERSION = "10.2";

	public static final String OPERATION_ACCOUNT_AVAILBAL_FINACLE = "BalInq";
	public static final String OPERATION_LIMIT_RESERVATIONS_FINACLE = "AcctLienAdd";
	public static final String OPERATION_LIMIT_RESERVATIONS_REVERSAL_FINACLE = "AcctLienMod";
	public static final String OPERATION_LIMIT_FACILITIES_FINACLE = "inquireCustomerLimitDetails";

	public static final String SERVICE_TI = "TI";//
	public static final String SERVICE_LIMIT = "Limit";//
	public static final String SERVICE_SFMS = "SFMS"; // Not
	public static final String SERVICE_SWIFT = "SWIFT";//
	public static final String SERVICE_EMAIL = "EMAIL";//
	public static final String SERVICE_ACCOUNT = "Account";//
	public static final String SERVICE_GATEWAY = "GATEWAY";//
	public static final String SERVICE_CUSTOMER = "Customer";
	public static final String SERVICE_BACKOFFICE = "BackOffice";//
	public static final String OPERATION_NOTIFICATION = "Notifications";//
	public static final String SERVICE_CUSTOMIZATION = "Customization";//
	public static final String SERVICE_BUSINESSSUPPORT = "BusinessSupport";//

	/** Misys OPERATION NAMES **/
	public static final String OPERATION_BATCH = "Batch";//
	public static final String OPERATION_VERIFY = "Verify";//
	public static final String OPERATION_AVAILBAL = "AvailBal";//

	public static final String OPERATION_ADVICE = "Advice";//
	public static final String OPERATION_TRACER = "Tracer";// Not required
	public static final String OPERATION_WORKFLOW = "WorkFlow";//
	public static final String OPERATION_HIGHVALUE = "HighValue";//
	public static final String OPERATION_SFMSADVICE = "SFMSAdvice";//
	public static final String OPERATION_LOCALISATION = "Localisation";
	public static final String OPERATION_SERVICEFAILURE = "ServiceFailure";//

	public static final String OPERATION_FAILURE_EMAIL_SMS = "SMS";//
	public static final String OPERATION_FAILURE_EMAIL_RESUBMIT = "Resubmit";//
	public static final String OPERATION_FAILURE_EMAIL_RTGSNEFT = "RTGSNEFT";//

	public static final String OPERATION_SFMS_OUT = "SFMSOut";//
	public static final String OPERATION_SFMS_IN = "SFMSIn";//
	public static final String OPERATION_SFMS_IN_ACK = "SFMSInAck";//
	public static final String OPERATION_SFMS_IN_NAK = "SFMSInNak";//

	public static final String OPERATION_SWIFT_OUT = "SwiftOut";//
	public static final String OPERATION_SWIFT_IN = "SwiftIn";//
	public static final String OPERATION_SWIFT_IN_ACK = "SwiftInAck";//
	public static final String OPERATION_SWIFT_IN_NAK = "SwiftInNak";//

	public static final String OPERATION_SFMSIN_298RES = "EBGIFN298SDPIn";//
	public static final String OPERATION_SFMSIN_760COV = "EBGIFN760COVIn";//
	public static final String OPERATION_SFMSIN_767COV = "EBGIFN767COVIn";//
	public static final String OPERATION_SFMSOUT_298REQ = "EBGIFN298SDROut";//
	public static final String OPERATION_SFMSOUT_760COV = "EBGIFN760COVOut";//
	public static final String OPERATION_SFMSOUT_767COV = "EBGIFN767COVOut";//

	public static final String OPERATION_FDLIEN_MARK = "FDLienMark";//
	public static final String OPERATION_FDLIEN_ENQ = "FDLienEnquiry";//
	public static final String OPERATION_FDLIEN_ACCENQ = "CustAccEnquiry";
	public static final String OPERATION_FDLIEN_REMOVAL = "FDLienRemoval";//

	public static final String OPERATION_FXCONTRACTS = "FXContracts";// Notrequired
	public static final String OPERATION_FXOPTIONSSEARCH = "FXOptionsSearch";//

	public static final String OPERATION_TREEVIEW = "TreeView";//
	public static final String OPERATION_FACILITIES = "Facilities";//
	public static final String OPERATION_RESERVATIONS = "Reservations";//
	public static final String OPERATION_EXPOSUREREVERSAL = "ExposureReversal";//
	public static final String OPERATION_EXPOSURERESERVATION = "ExposureReservation";//
	public static final String OPERATION_EXPOSUREREVERSALLCBD = "ExposureReversalLCBD";//
	public static final String OPERATION_RESERVATIONSREVERSAL = "ReservationsReversal";//
	public static final String OPERATION_RESERVATIONSREVERSALR = "ReservationsReversalR";//

	public static final String SERVICE_HEALTH = "Health";
	public static final String SERVICE_WATCHLIST = "Watch";// Not required
	public static final String OPERATION_TFEOD = "TFEOD";// Not required
	public static final String OPERATION_CHECK = "Check";// Not required
	public static final String OPERATION_FXDEAL = "FXDeal";// Not required
	public static final String OPERATION_POSTING = "Posting";// Not required
	public static final String OPERATION_EXPOSURE = "Exposure";// Not required
	public static final String OPERATION_FXCONTRACTDRAWDOWN = "FXContractDrawdown"; // Notrequired

	/****** LISTENER ******/
	// public final static String LISTNER_TYPE_RATE = "RateListener";
	// public final static String LISTNER_TYPE_SFMSIN = "SFMSListener";
	// public final static String LISTNER_TYPE_SWIFTIN = "SwiftOutListener";
	// public final static String LISTNER_TYPE_STATICDATA = "StaticListener";

	/*** Misys SERVICE NAMES ***/
	public static final String ZONE = "ZONE1"; // TODO
	public static final String SOURCE_SYSTEM = "ZONE1";// TODO
	public static final String TARGET_SYSTEM = "BOB";// TODO

	/** ZONE ID **/
	public final static String TIZONE_ID = ConfigurationUtil.getValueFromKey("TIZONEID");
	public final static String RPTZONE_ID = ConfigurationUtil.getValueFromKey("RPTZONEID");

	/** SCHEMA NAME **/
	public final static String TIZONE_SCHEMA_NAME = ConfigurationUtil.getValueFromKey("TIZoneSchemaName");
	public final static String RPTZONE_SCHEMA_NAME = ConfigurationUtil.getValueFromKey("RPTZoneSchemaName");
	public final static String TIGLOBAL_SCHEMA_NAME = ConfigurationUtil.getValueFromKey("TIGlobalSchemaName");
	public final static String THEMEBRIDGE_SCHEMA_NAME = ConfigurationUtil.getValueFromKey("ThemeBridgeSchemaName");

	/** LOGGER LEVEL */
	public final static String LOGGER_LEVEL_INFO = "INFO";
	public final static String LOGGER_LEVEL_WARN = "WARN";
	public final static String LOGGER_LEVEL_DEBUG = "DEBUG";
	public final static String LOGGER_LEVEL_ERROR = "ERROR";
	public final static String LOGGER_LEVEL_FATAL = "FATAL";

	/** THEMEBRIDGE **/
	public static final String THEMEBRIDGE_WEBSERVICE_USERNAME = "BridgeUser";
	public static final String THEMEBRIDGE_WEBSERVICE_PASSWORD = "BridgePassword";

	/** SERVICE+OPERATION **/
	public final static String REQUEST_TYPE_TI_EODPROCESS = "TITFEOD";
	public final static String REQUEST_TYPE_TI_GLCMSPROCESS = "TIGLCMS";
	public final static String REQUEST_TYPE_SWIFT_OUT = "SWIFTSwiftOut";
	public final static String REQUEST_TYPE_AVAIL_BAL = "AccountAvailBal";
	public final static String REQUEST_TYPE_FAX_CLIENT = "FaxNotification";
	public final static String REQUEST_TYPE_GATEWAY_EXTDMS = "GATEWAYEXTDMS";
	public final static String REQUEST_TYPE_HOST_SEARCH = "BrowserHostSearch";
	public final static String REQUEST_TYPE_LIMIT_FACILITY = "LimitFacilities";
	public final static String REQUEST_TYPE_ACCOUNT_OPEN = "AccountAccountOpen";
	public final static String REQUEST_TYPE_BACKOFFICE_BATCH = "BackOfficeBatch";
	public final static String REQUEST_TYPE_BACKOFFICE_VERIFY = "BackOfficeVerify";
	public final static String REQUEST_TYPE_TI_FXUTILIZATION = "TIFXUtilization";
	public final static String PROPERTY_SFMS_DATE_FORMAT = "SwiftSFMSDateFormat";
	public final static String REQUEST_TYPE_GATEWAY_FD_CCSFDPS = "GATEWAYCCSFDPS";
	public final static String PROPERTY_SWIFT_DATE_FORMAT = "SwiftSFMSDateFormat";
	public final static String REQUEST_TYPE_WATCHLIST_CHECKER = "GatewayWatchList";
	public final static String PROPERTY_SFMS_BIC_IFSC_FORMAT = "SFMSBicIfscFormat";
	public final static String PROPERTY_SFMS_IFSC_BIC_FORMAT = "SFMSBicIfscFormat";
	public final static String REQUEST_TYPE_LIMIT_RESERVATION = "LimitReservations";
	public final static String REQUEST_TYPE_BACKOFFICE_FX_DEAL = "BackOfficeFXDeal";
	public final static String REQUEST_TYPE_GATEWAY_FD_CCSFDPS1 = "GATEWAYCCSFDPS1";
	public final static String REQUEST_TYPE_GATEWAY_FD_CCSFDPS2 = "GATEWAYCCSFDPS2";
	public final static String REQUEST_TYPE_GATEWAY_ATTICKET_RMDOC = "GATEWAYRMDOC";
	public final static String REQUEST_TYPE_GATEWAY_ATTICKET_RMDOC1 = "GATEWAYRMDOC1";
	public final static String REQUEST_TYPE_GATEWAY_ATTICKET_RMDOC2 = "GATEWAYRMDOC2";
	public final static String REQUEST_TYPE_GATEWAY_ATTICKET_RMDOC3 = "GATEWAYRMDOC3";
	public final static String REQUEST_TYPE_ACCOUNT_DETAILS = "AccountAccountDetails";
	public final static String REQUEST_TYPE_CUSTOMER_DETAILS = "CustomerCustomerDetails";
	public final static String REQUEST_TYPE_GATEWAY_INITIATEREFERRAL_CCSRM = "GATEWAYCCSRM";
	public final static String REQUEST_TYPE_GATEWAY_INITIATEREFERRAL_CCSRM1 = "GATEWAYCCSRM1";
	public final static String REQUEST_TYPE_GATEWAY_INITIATEREFERRAL_CCSRM2 = "GATEWAYCCSRM2";
	public final static String REQUEST_TYPE_GATEWAY_INITIATEREFERRAL_CCSRM3 = "GATEWAYCCSRM3";
	public final static String REQUEST_TYPE_NOTIFICATION_EVENT_STEP = "Notificationsevent-step";
	public final static String REQUEST_TYPE_NOTIFICATION_ASYNC_STEP = "Notificationsasync-step";
	public final static String REQUEST_TYPE_LIMIT_RESERVATION_REVERSAL = "LimitReservationsReversal";
	public final static String REQUEST_TYPE_BUSINESS_SUPPORT_TI_BASE_RT = "BusinessSupportTIBaseRate";
	public final static String REQUEST_TYPE_BUSINESS_SUPPORT_FX_CONTRACTS = "BusinessSupportFXContracts";
	public final static String REQUEST_TYPE_BUSINESS_SUPPORT_TI_FX_SPOT_RT = "BusinessSupportTIFXSpotRt";
	public final static String REQUEST_TYPE_BACKOFFICE_FX_CONTRACT_DRAWDOWN = "BackOfficeFXContractDrawdown";

	/** ENCRYPTION UTIL **/
	public final static String ENCRYPTION_UTIL_EKEY = "9D51E3DF2067B983EAEC3DC7F10B9D3DA44CA4D591528951";

}
