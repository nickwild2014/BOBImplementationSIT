package com.bs.theme.bob.staticdata.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.incoming.util.StaticDataConstant;

import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.logging.StaticLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;

import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.StatusEnum;

public class CustomerDataServiceAdapter {

	private final static Logger logger = Logger.getLogger(CustomerDataServiceAdapter.class.getName());

	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String inputMessage = "";
	// private static String type = "StagingTable";

	public static void main(String ar[]) throws Exception {

		CustomerDataServiceAdapter aCustomerDataServiceAdapter = new CustomerDataServiceAdapter();
		// List<Map<String, String>> getCustomerDataList =
		// aCustomerDataServiceAdapter.getCustomerDataIdbList();
		// logger.debug(getCustomerDataList.size());

		String res = aCustomerDataServiceAdapter.pushCustomerDataService();
		// logger.debug(res);

		// getTIcustomerType("I");
		// updateCustomerDataStatus("6500074999", "S");

		// logger.debug(getCustomerDataIdbList());

		// logger.debug(aCustomerDataServiceAdapter.pushCustomerDataService());
	}

	/**
	 * 2016-OCT-31
	 * 
	 * @return
	 */
	public String pushCustomerDataService() {

		// logger.debug(" ************ Customer.Data adapter new process started
		// ************ ");
		String result = "";
		Timestamp reqReceivedTime = null;
		InputStream anInputStream = null;
		boolean customerDataPushStatus = true;
		try {
			List<Map<String, String>> mapList = getCustomerDataIdbList();
			logger.debug("CustomerDataList map size : " + mapList.size());

			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_CUSTOMER_DATA_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			if (mapList.size() > 0) {

				for (Map<String, String> map : mapList) {

					String status = "FAILED";
					String remarks = "";
					String fullName = "";
					String shortName = "";
					String customerCif = "";
					String tagRemovedXML = "";
					String errorMessage = "";

					try {
						customerCif = map.get("CUSTOMERNUMBER");
						if (map.get("FULLNAME").length() > 35)// max length
							fullName = map.get("FULLNAME").substring(0, 35);
						else
							fullName = map.get("FULLNAME");
						if (map.get("SHORTNAME").length() > 15)// max length
							shortName = map.get("SHORTNAME").substring(0, 15);
						else
							shortName = map.get("SHORTNAME");

						tagRemovedXML = getTirequestMsg(map, requestTemplate);
						tagRemovedXML = tagRemovedXML.replace("@@@", "\n");
						// logger.debug(tagRemovedXML);

						// String arr[] =
						// tagRemovedXML.split(System.lineSeparator());
						// for (String str : arr) {
						// logger.debug("hh " + str);
						// }

						reqReceivedTime = DateTimeUtil.getTimestamp();
						tiRequest = tagRemovedXML;
						logger.debug("CustomerData TI RequestXML :- " + tiRequest);

						/********************************************************/
						String tiResponseXML = TIPlusEJBClient.process(tagRemovedXML);
						tiResponse = tiResponseXML;
						logger.debug("CustomerData TI ResponseXML :- " + tiResponse);

						// StatusEnum statusEnum =
						// ResponseHeaderUtil.processEJBClientResponse(tiResponse);
						// status = statusEnum.toString();
						status = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Status");
						errorMessage = XPathParsing.getValue(tiResponse,
								"ServiceResponse/ResponseHeader/Details/Error");

						int updatedCount = updateCustomerDataStatus(customerCif, status, errorMessage);

					} catch (Exception e) {
						status = "FAILED";
						customerDataPushStatus = false;
						errorMessage = errorMessage + e.getMessage();
						logger.error("CustomerData Exceptionss! " + e.getMessage());
						e.printStackTrace();

					} finally {
						int updatedCount = updateCustomerDataStatus(customerCif, status, errorMessage);

						StaticLogging.pushLogData("TI", "CustomerData", "ZONE1", "ALL", "IDBEXT", "TIPlus", status,
								reqReceivedTime, inputMessage, tiRequest, tiResponse, customerCif, fullName, shortName,
								false, "0", errorMessage);
						try {
							anInputStream.close();
						} catch (IOException e) {
							logger.error("InputStream close " + e.getMessage());
						}
					}
				}

			} else {
				// logger.debug("CustomerData maplist is empty or zero");
			}

		} catch (Exception e) {
			logger.error("CustomerData Exception! " + e.getMessage());
			e.printStackTrace();
			return result = StatusEnum.SUCCEEDED.toString();

		}
		// logger.debug(" ************ Customer.Data adapter new process ended
		// ************ ");
		return result = StatusEnum.SUCCEEDED.toString();
	}

	/**
	 * 2016-10-29
	 * 
	 * @return
	 */
	public static List<Map<String, String>> getCustomerDataIdbList() {

		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		List<Map<String, String>> returnMaplist = new ArrayList();
		try {
			aConnection = DatabaseUtility.getIdbTiplusConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();
				// String query = "SELECT MAINTTYPE, SOURCEBANKINGBUSINESS,
				// CUSTOMERNUMBER, CUSTOMERTYPE, FULLNAME, SHORTNAME,
				// PROCESSED_FLAG, PROCESSED_DATE, PROCCESSED_REMARKS, LOB,
				// EMAIL_ADDRESS_1, EMAIL_ADDRESS_2, EMAIL_ADDRESS_3, CRM1,
				// CRM2, CRM3, PAN_NUMBER, CIF_NUMBER, CLIENT_TYPE,
				// CLIENT_SOLID, MESSAGE_ID FROM CUSTOMER WHERE PROCESSED_FLAG
				// IS null OR PROCESSED_FLAG IN ('N','U') ";

				/** 2017-02-22 **/
				// String query = " SELECT MAINTTYPE, SOURCEBANKINGBUSINESS,
				// CUSTOMERNUMBER, CUSTOMERTYPE, FULLNAME, SHORTNAME,
				// PROCESSED_FLAG, PROCESSED_DATE, PROCCESSED_REMARKS, LOB,
				// EMAIL_ADDRESS_1, EMAIL_ADDRESS_2, EMAIL_ADDRESS_3, CRM1,
				// CRM2, CRM3, PAN_NUMBER, CIF_NUMBER, CLIENT_TYPE,
				// CLIENT_SOLID, MESSAGE_ID, COMPANY_REG_NO, "
				// + " ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3, CITY,
				// PIN_CODE, STATE, COUNTRY, MOBILE_NOS, TELEPHONE_NOS,
				// TELEPHONE_AREA_CODE, "
				// + " FAX_NOS, BORROWERS_LEGAL_CONSTITUTN, INDUSTRY_TYPE, DOB,
				// DATE_OF_INCORPORATION, GENDER, INDIVIDUAL_NAME_PREFIX,
				// DRIVER_LIC_NO, PASSPORT_NO, "
				// + " UIN_NO, VOTER_ID, CREDIT_RATING, CREDIT_RATING_AS_ON FROM
				// CUSTOMER WHERE PROCESSED_FLAG IS null OR PROCESSED_FLAG IN
				// ('N','U','Y')";

				String customerFlag = "N";
				String flag = ConfigurationUtil.getValueFromKey("CustomerDataFlag");
				// logger.debug("Config StaticQuery : " + flag);
				if (!customerFlag.isEmpty() || customerFlag != null) {
					flag = flag.trim();
					customerFlag = flag;
				}
				// logger.debug("CUSTOMER StaticQuery : \n" + customerFlag);

				/** 2017-02-22 **/
				String query = "SELECT TRIM(MAINTTYPE) AS MAINTTYPE, TRIM(SOURCEBANKINGBUSINESS) AS SOURCEBANKINGBUSINESS, TRIM(CUSTOMERNUMBER) AS CUSTOMERNUMBER, TRIM(CUSTOMERTYPE) AS CUSTOMERTYPE, TRIM(FULLNAME) AS FULLNAME, TRIM(SHORTNAME) AS SHORTNAME, TRIM(PROCESSED_FLAG) AS PROCESSED_FLAG, TRIM(PROCESSED_DATE) AS PROCESSED_DATE, TRIM(PROCCESSED_REMARKS) AS PROCCESSED_REMARKS, TRIM(LOB) AS LOB, TRIM(EMAIL_ADDRESS_1) AS EMAIL_ADDRESS_1, TRIM(EMAIL_ADDRESS_2) AS EMAIL_ADDRESS_2, TRIM(EMAIL_ADDRESS_3) AS EMAIL_ADDRESS_3, TRIM(CRM1) AS CRM1, TRIM(CRM2) AS CRM2, TRIM(CRM3) AS CRM3, TRIM(PAN_NUMBER) AS PAN_NUMBER, TRIM(CIF_NUMBER) As CIF_NUMBER, TRIM(CLIENT_TYPE) AS  CLIENT_TYPE, TRIM(CLIENT_SOLID) AS CLIENT_SOLID, TRIM(MESSAGE_ID) AS MESSAGE_ID, TRIM(COMPANY_REG_NO) AS COMPANY_REG_NO, TRIM(ADDRESS_LINE_1) AS ADDRESS_LINE_1, TRIM(ADDRESS_LINE_2) AS ADDRESS_LINE_2, TRIM(ADDRESS_LINE_3) AS ADDRESS_LINE_3, TRIM(CITY) AS CITY, TRIM(PIN_CODE) AS PIN_CODE, TRIM(STATE) AS STATE, TRIM(COUNTRY) AS COUNTRY, TRIM(MOBILE_NOS) AS MOBILE_NOS, TRIM(TELEPHONE_NOS) AS TELEPHONE_NOS, TRIM(TELEPHONE_AREA_CODE) AS TELEPHONE_AREA_CODE, TRIM(FAX_NOS) AS FAX_NOS, TRIM(BORROWERS_LEGAL_CONSTITUTN) AS BORROWERS_LEGAL_CONSTITUTN, TRIM(INDUSTRY_TYPE) AS INDUSTRY_TYPE, TRIM(to_char(DOB, 'yyyy-mm-dd')) AS DOB, TRIM(to_char(DATE_OF_INCORPORATION, 'yyyy-mm-dd')) AS DATE_OF_INCORPORATION, TRIM(GENDER) AS GENDER, TRIM(INDIVIDUAL_NAME_PREFIX) AS INDIVIDUAL_NAME_PREFIX, TRIM(DRIVER_LIC_NO) AS DRIVER_LIC_NO, TRIM(PASSPORT_NO) AS PASSPORT_NO, TRIM(UIN_NO) AS UIN_NO, TRIM(VOTER_ID) AS VOTER_ID, TRIM(CREDIT_RATING) AS CREDIT_RATING, TRIM(to_char(CREDIT_RATING_AS_ON, 'yyyy-mm-dd')) AS CREDIT_RATING_AS_ON "
						+ " FROM CUSTOMER WHERE PROCESSED_FLAG IN ('" + customerFlag + "','U') ";
						// logger.debug("CUSTOMER StaticQuery : \n" + query);

				// String query = "SELECT TRIM(MAINTTYPE) AS MAINTTYPE,
				// TRIM(SOURCEBANKINGBUSINESS) AS SOURCEBANKINGBUSINESS,
				// TRIM(CUSTOMERNUMBER) AS CUSTOMERNUMBER, TRIM(CUSTOMERTYPE) AS
				// CUSTOMERTYPE, TRIM(FULLNAME) AS FULLNAME, TRIM(SHORTNAME) AS
				// SHORTNAME, TRIM(PROCESSED_FLAG) AS PROCESSED_FLAG,
				// TRIM(PROCESSED_DATE) AS PROCESSED_DATE,
				// TRIM(PROCCESSED_REMARKS) AS PROCCESSED_REMARKS, TRIM(LOB) AS
				// LOB, TRIM(EMAIL_ADDRESS_1) AS EMAIL_ADDRESS_1,
				// TRIM(EMAIL_ADDRESS_2) AS EMAIL_ADDRESS_2,
				// TRIM(EMAIL_ADDRESS_3) AS EMAIL_ADDRESS_3, TRIM(CRM1) AS CRM1,
				// TRIM(CRM2) AS CRM2, TRIM(CRM3) AS CRM3, TRIM(PAN_NUMBER) AS
				// PAN_NUMBER, TRIM(CIF_NUMBER) As CIF_NUMBER, TRIM(CLIENT_TYPE)
				// AS CLIENT_TYPE, TRIM(CLIENT_SOLID) AS CLIENT_SOLID,
				// TRIM(MESSAGE_ID) AS MESSAGE_ID, TRIM(COMPANY_REG_NO) AS
				// COMPANY_REG_NO, TRIM(ADDRESS_LINE_1) AS ADDRESS_LINE_1,
				// TRIM(ADDRESS_LINE_2) AS ADDRESS_LINE_2, TRIM(ADDRESS_LINE_3)
				// AS ADDRESS_LINE_3, TRIM(CITY) AS CITY, TRIM(PIN_CODE) AS
				// PIN_CODE, TRIM(STATE) AS STATE, TRIM(COUNTRY) AS COUNTRY,
				// TRIM(MOBILE_NOS) AS MOBILE_NOS, TRIM(TELEPHONE_NOS) AS
				// TELEPHONE_NOS, TRIM(TELEPHONE_AREA_CODE) AS
				// TELEPHONE_AREA_CODE, TRIM(FAX_NOS) AS FAX_NOS,
				// TRIM(BORROWERS_LEGAL_CONSTITUTN) AS
				// BORROWERS_LEGAL_CONSTITUTN, TRIM(INDUSTRY_TYPE) AS
				// INDUSTRY_TYPE, TRIM(to_char(DOB, 'yyyy-mm-dd')) AS DOB,
				// TRIM(to_char(DATE_OF_INCORPORATION, 'yyyy-mm-dd')) AS
				// DATE_OF_INCORPORATION, TRIM(GENDER) AS GENDER,
				// TRIM(INDIVIDUAL_NAME_PREFIX) AS INDIVIDUAL_NAME_PREFIX,
				// TRIM(DRIVER_LIC_NO) AS DRIVER_LIC_NO, TRIM(PASSPORT_NO) AS
				// PASSPORT_NO, TRIM(UIN_NO) AS UIN_NO, TRIM(VOTER_ID) AS
				// VOTER_ID, TRIM(CREDIT_RATING) AS CREDIT_RATING,
				// TRIM(to_char(CREDIT_RATING_AS_ON, 'yyyy-mm-dd')) AS
				// CREDIT_RATING_AS_ON FROM CUSTOMER where
				// CUSTOMERNUMBER='8728117' ";

				aResultset = aStatement.executeQuery(query);
				ResultSetMetaData rsmd = aResultset.getMetaData();
				int columnCount = rsmd.getColumnCount();

				while (aResultset.next()) {

					Map maplist = new HashMap();
					for (int i = 1; i < columnCount + 1; i++) {
						String key = rsmd.getColumnLabel(i);
						String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
						// logger.debug(key + "\t" + value);
						maplist.put(key, value);
					}
					returnMaplist.add(maplist);
				}
			}
		} catch (Exception ex) {
			logger.error("CustomerData Exceptions! " + ex.getMessage());
			ex.getMessage();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		return returnMaplist;
	}

	/**
	 * 
	 * @return
	 */
	public static String getTirequestMsg(Map<String, String> map, String requestTemplate) {

		String result = "";
		String remarks = "";
		String tokenReplacedXML = "";
		String customerCifNumber = "";
		try {
			customerCifNumber = map.get("CUSTOMERNUMBER");
			logger.debug("customerCifNumber : " + customerCifNumber);
			// logger.debug("message 01a");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Name", StaticDataConstant.CredentialName);
			tokens.put("Password", "");
			tokens.put("Certificate", "");
			tokens.put("Digest", "");
			tokens.put("CorrelationId", UUID.randomUUID().toString());
			tokens.put("MaintType", "F");// Always
			tokens.put("MaintainedInBackOffice", "N");
			tokens.put("SourceBankingBusiness", StaticDataConstant.SourceBankingBusiness);
			tokens.put("Mnemonic", customerCifNumber);
			tokens.put("CustomerNumber", map.get("CUSTOMERNUMBER"));

			// logger.debug("message 01b");
			String fullName = "";
			if (map.get("FULLNAME").length() > 35) {// max length
				fullName = map.get("FULLNAME").substring(0, 35);
				tokens.put("FullName", fullName);
			} else {
				fullName = map.get("FULLNAME");
				tokens.put("FullName", fullName);
			}
			String shortName = "";
			if (map.get("SHORTNAME").length() > 15) {// max length
				shortName = map.get("SHORTNAME").substring(0, 15);
				tokens.put("ShortName", shortName);
			} else {
				shortName = map.get("SHORTNAME");
				tokens.put("ShortName", shortName);
			}
			// logger.debug("message 01");
			// Extradata
			// TRADE, MANUFACTURE - from EXTLOB
			String lob = map.get("LOB");
			if (ValidationsUtil.isValidString(lob)) {
				tokens.put("Lob", lob.toUpperCase());
			} else {
				tokens.put("Lob", lob);
			}

			tokens.put("Email_address_1", map.get("EMAIL_ADDRESS_1"));
			tokens.put("Email_address_2", map.get("EMAIL_ADDRESS_2"));
			tokens.put("Email_address_3", map.get("EMAIL_ADDRESS_3"));
			tokens.put("CRM1", map.get("CRM1"));
			tokens.put("CRM2", map.get("CRM2"));
			tokens.put("CRM3", map.get("CRM3"));
			tokens.put("Pan_number", map.get("PAN_NUMBER"));
			tokens.put("Cif_number", map.get("CIF_NUMBER"));
			tokens.put("Client_type", map.get("CLIENT_TYPE"));
			tokens.put("Client_solid", map.get("CLIENT_SOLID"));
			tokens.put("Message_id", "");

			// New Extra data
			tokens.put("Company_reg_no", map.get("COMPANY_REG_NO"));
			tokens.put("City", map.get("CITY"));
			tokens.put("State", map.get("STATE"));
			tokens.put("Country", map.get("COUNTRY"));
			tokens.put("Mobile_nos", map.get("MOBILE_NOS"));// 20 Max
			tokens.put("Telephone_area_code", map.get("TELEPHONE_AREA_CODE"));
			String dob = "";
			if (ValidationsUtil.isValidString(map.get("DOB"))) {
				dob = map.get("DOB").substring(0, 10);
			}
			String date_incorporation = "";
			if (ValidationsUtil.isValidString(map.get("DATE_OF_INCORPORATION"))) {
				date_incorporation = map.get("DATE_OF_INCORPORATION").substring(0, 10);
			}
			String credit_rating_on = "";
			if (ValidationsUtil.isValidString(map.get("CREDIT_RATING_AS_ON"))) {
				credit_rating_on = map.get("CREDIT_RATING_AS_ON").substring(0, 10);
			}
			tokens.put("DOB", dob);
			tokens.put("Date_of_incorporation", date_incorporation);
			tokens.put("Gender", map.get("GENDER"));
			tokens.put("Individual_name_prefix", map.get("INDIVIDUAL_NAME_PREFIX"));
			tokens.put("Driver_lic_no", map.get("DRIVER_LIC_NO"));
			tokens.put("Passport_no", map.get("PASSPORT_NO"));
			tokens.put("UINNO", map.get("UIN_NO"));
			tokens.put("Voter_id", map.get("VOTER_ID"));
			tokens.put("Credit_rating", map.get("CREDIT_RATING"));
			tokens.put("Credit_rating_as_on", credit_rating_on);
			tokens.put("Industry_type", map.get("INDUSTRY_TYPE"));

			String borrowers_legal_cons = map.get("BORROWERS_LEGAL_CONSTITUTN");
			tokens.put("Borrowers_legal_constitutn", borrowers_legal_cons);
			// TODO 2017-09-11
			if (borrowers_legal_cons.contains("Bank") || borrowers_legal_cons.contains("bank")
					|| borrowers_legal_cons.contains("BANK")) {
				tokens.put("CustomerType", "BB"); // TODO
				tokens.put("AddressDetailsTransferMethod", "SW");// SW-Bank
			} else {
				String tiCustType = getTIcustomerType(map.get("CUSTOMERTYPE"));
				tokens.put("CustomerType", tiCustType);
				tokens.put("AddressDetailsTransferMethod", "AC");// AC-non-banking
			}

			// Extradata
			tokens.put("Reference", "");
			tokens.put("MailToBranch", "");
			tokens.put("Group", "");
			tokens.put("GroupDescription", "");
			tokens.put("AccountOfficer", "");
			tokens.put("ResidenceCountry", "IN");// Always
			tokens.put("ParentCountry", "IN");// Always
			tokens.put("RiskCountry", "");
			tokens.put("AnalysisCode", "");
			tokens.put("Language", "GB");// Always
			tokens.put("Closed", "");
			tokens.put("Blocked", "");
			tokens.put("Deceased", "");
			tokens.put("Inactive", "");
			tokens.put("MidasFacilityAllow", "");
			tokens.put("BehalfOfBranch", "");
			tokens.put("BankCode2", "");
			tokens.put("BankCode3", "");
			tokens.put("BankCode4", "");
			tokens.put("ClearingId", "");
			tokens.put("AddressDetailsAddressType", "P");// Staticdatamaintenance
			tokens.put("AddressDetailsAddressId", "");
			tokens.put("AddressDetailsSalutation", "");

			logger.debug("message");
			StringBuffer addressName = new StringBuffer();
			// if (shortName != null || !shortName.isEmpty()) {
			// addressName.append(shortName);
			// addressName.append("@@@");
			// }
			if (fullName != null && !fullName.trim().isEmpty()) {
				addressName.append(fullName);
				addressName.append("@@@");
			}
			if (map != null && !map.get("ADDRESS_LINE_1").trim().isEmpty()) {
				addressName.append(map.get("ADDRESS_LINE_1"));
				addressName.append("@@@");
			}
			if (map != null && !map.get("ADDRESS_LINE_2").trim().isEmpty()) {
				addressName.append(map.get("ADDRESS_LINE_2"));
				addressName.append("@@@");
			}
			if (map != null && !map.get("ADDRESS_LINE_3").trim().isEmpty()) {
				addressName.append(map.get("ADDRESS_LINE_3"));
				addressName.append("@@@");
			}
			// if (map != null && !map.get("CITY").trim().isEmpty()) {
			// addressName.append(map.get("CITY"));
			// addressName.append("@@@");
			// }
			// if (map != null && !map.get("PIN_CODE").trim().isEmpty()) {
			// addressName.append(map.get("PIN_CODE"));
			// addressName.append("@@@");
			// }
			// if (map != null && !map.get("STATE").trim().isEmpty()) {
			// addressName.append(map.get("STATE"));
			// addressName.append("@@@");
			// }
			// if (map != null && !map.get("COUNTRY").trim().isEmpty()) {
			// addressName.append(map.get("COUNTRY"));
			// }
			logger.debug("AddressDetailsNameAndAddress : " + addressName.toString());
			tokens.put("AddressDetailsNameAndAddress", addressName.toString());

			tokens.put("AddressDetailsZipCode", map.get("PIN_CODE"));
			tokens.put("AddressDetailsLanguage", "");

			String telephone = "";
			if (map.get("TELEPHONE_NOS").length() > 20)// max length
				telephone = map.get("TELEPHONE_NOS").substring(0, 20);// 20
			else
				telephone = map.get("TELEPHONE_NOS");
			tokens.put("AddressDetailsPhone", telephone);

			String faxNo = "";
			if (map.get("FAX_NOS").length() > 20)
				faxNo = map.get("FAX_NOS").substring(0, 20);// 20
			else
				faxNo = map.get("FAX_NOS");
			tokens.put("AddressDetailsFax", faxNo);

			tokens.put("AddressDetailsTelex", "");
			tokens.put("AddressDetailsTelexAnswerBack", "");
			tokens.put("AddressDetailsEmail", map.get("EMAIL_ADDRESS_1")); // map.get("EMAIL_ADDRESS_1")
			tokens.put("AddressDetailsSwiftBIC", "");

			tokens.put("AddressDetailsAddresseeCustomerSBB", "");
			tokens.put("AddressDetailsAddresseeCustomer", "");
			tokens.put("AddressDetailsNumberOfCopies", "");
			tokens.put("AddressDetailsNumberOfOriginals", "");
			tokens.put("SpecialInstructionDetailsSeverity", "");
			tokens.put("SpecialInstructionDetailsCode", "");
			tokens.put("SpecialInstructionDetailsDetails", "");
			tokens.put("SpecialInstructionDetailsStyle", "");
			tokens.put("SpecialInstructionDetailsEmphasis", "");
			tokens.put("OtherDetailsAllowMT103C", "");
			tokens.put("OtherDetailsCutoffAmountAmount", "");
			tokens.put("OtherDetailsCutoffAmountCurrency", "");
			tokens.put("OtherDetailsSWIFTAckRequired", "Y");// default
			tokens.put("OtherDetailsTransliterateSWIFT", "");
			tokens.put("OtherDetailsTeam", "");
			tokens.put("OtherDetailsCorporateAccess", "GW");// gateway customer
			tokens.put("OtherDetailsPrincipalFxRateCode", "");
			tokens.put("OtherDetailsChargeFxRateCode", "");
			tokens.put("OtherDetailsAllowTaxExemptions", "");
			tokens.put("OtherDetailsSuspended", "");
			tokens.put("SwiftDetailsMainBankingEntity", "");
			tokens.put("SwiftDetailsSwiftAddress", "");
			tokens.put("SwiftDetailsAuthenticated", "");
			tokens.put("SwiftDetailsBlocked", "");
			tokens.put("SwiftDetailsClosed", "");
			tokens.put("SwiftDetailsTransliterationRequired", "");
			tokens.put("TICustomerExtraData", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			// logger.debug("tokenReplacedXML : " + tokenReplacedXML);
			reader.close();

			tokenReplacedXML = tokenReplacedXML.replace("&", "&amp;");
			// logger.debug("& >>-->> " + tokenReplacedXML);

			String tagRemovedXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			result = tagRemovedXML;
			// logger.debug("RequestXML to TIPlus :\n" + tagRemovedXML);

		} catch (IOException e2) {
			logger.error("IOException " + e2.getMessage(), e2);
			e2.printStackTrace();

		} catch (Exception e) {
			logger.error("Exceptions " + e.getMessage(), e);
			e.printStackTrace();

		}

		return result;
	}

	// /**
	// * NOT IMPLEMENTED IN KOTAK
	// *
	// * @param finacleAccountType
	// * @return
	// */
	// public static String geTitAccountType(String finacleCustType) {
	//
	// // logger.debug("finacleAccountType : " + finacleAccountType);
	//
	// String tiCustType = "";
	// ResultSet aResultset = null;
	// Connection aConnection = null;
	// PreparedStatement aPreparedStatement = null;
	//
	// try {
	// aConnection = DatabaseUtility.getThemebridgeConnection();
	// aPreparedStatement = aConnection
	// .prepareStatement("SELECT FICUSTOMERTYPE FROM LOOKUP_CUSTOMER_TYPE WHERE
	// FICUSTOMERTYPE = ? ");
	// aPreparedStatement.setString(1, finacleCustType);
	// aResultset = aPreparedStatement.executeQuery();
	//
	// if (aResultset.next()) {
	// tiCustType = aResultset.getString("FICUSTOMERTYPE");
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	//
	// } finally {
	// DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement,
	// aResultset);
	//
	// }
	// // logger.debug("tiAccType : " + tiAccType);
	// return tiCustType;
	// }

	public static String getTIcustomerType(String finacleCustomerType) {

		// logger.debug("FI customerType : " + finacleCustomerType);
		String tiCustType = "";
		ResultSet aRes = null;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {
				String custTypeQuery = "SELECT TICUSTOMERTYPE FROM LOOKUP_CUSTOMER_TYPE WHERE FICUSTOMERTYPE = ? ";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug("CUSTOMER update query is :\n" + custTypeQuery);
				aPreParedStatement = aConnection.prepareStatement(custTypeQuery);
				aPreParedStatement.setString(1, finacleCustomerType);

				aRes = aPreParedStatement.executeQuery();
				while (aRes.next()) {
					tiCustType = aRes.getString("TICUSTOMERTYPE");
				}

				// logger.debug("CUSTOMER Type >>> " + tiCustType);
			}
		} catch (Exception ex) {
			logger.error("CUSTOMER exception is " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return tiCustType;
	}

	/**
	 * 
	 * @param customerMnemonic
	 * @param extnlAccNum
	 * @param status
	 * @return
	 */
	public static int updateCustomerDataStatus(String customerMnemonic, String status, String errorMessage) {

		logger.debug("customerMnemonic : " + customerMnemonic);
		logger.debug("status : " + status);

		if (status.equals("SUCCEEDED"))
			status = "S";
		else if (status.equals("FAILED"))
			status = "F";
		else if (status.equals("UNAVAILABLE"))
			status = "U";
		else if (status.isEmpty())
			status = "U";

		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbTiplusConnection();
			if (aConnection != null) {
				String updateQuery = "UPDATE CUSTOMER SET PROCESSED_FLAG = ?, PROCESSED_DATE = CURRENT_DATE, PROCCESSED_REMARKS = ? WHERE CUSTOMERNUMBER = ? ";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug("CUSTOMER update query is :\n" + updateQuery);
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, status);
				// aPreParedStatement.setDate(2, );
				String datetime = DateTimeUtil.getCurrentTimeStamp();
				String errorMsg = datetime + errorMessage;
				if (errorMsg.length() > 255)
					errorMsg = errorMsg.substring(0, 255);
				aPreParedStatement.setString(2, errorMsg);
				aPreParedStatement.setString(3, customerMnemonic);

				updatedRowCount = aPreParedStatement.executeUpdate();
				// logger.debug("CUSTOMER UpdatedRowCount >>> " +
				// updatedRowCount);
			}
		} catch (Exception ex) {
			logger.error("CUSTOMER exception is " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

}
