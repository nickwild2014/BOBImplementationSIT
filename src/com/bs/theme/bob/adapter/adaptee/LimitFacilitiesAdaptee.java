package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_RESERVATIONS;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_LIMIT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bs.theme.bob.adapter.util.LimitServicesUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ThemeConstant;
import com.bs.themebridge.xpath.LimitFacilitiesXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.bob.client.finacle.FinacleHttpClient;

/**
 * End system communication implementation for Limit Facilities Enquiry services
 * is handled in this class.
 * 
 * @since 2016-DEC-16
 * @version v.1.0.1
 * @author <b><i><font color=blue>Prasath Ravichandran</font></i></b>,
 *         <font color=green>Software Analyst, </font>
 *         <font color=violet>Bluescope</font>
 */
public class LimitFacilitiesAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(LimitFacilitiesAdaptee.class.getName());

	private String branch = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String correlationId = "";
	private String relatedParty = "";
	private String postingAmount = "";
	private String postingProduct = "";
	private String eventReference = "";
	private String masterReference = "";
	private String postingCurrency = "";
	private String postingSubProduct = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private Date productExpiryDate = null;
	String customerID = "";

	public LimitFacilitiesAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public LimitFacilitiesAdaptee() {
	}

	/**
	 * Process the Limit facility service from the TI to Corebank
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@link String}
	 */
	public String process(String requestXML) throws Exception {

		logger.info(" ************ Limit.Facilities adaptee process started ************ ");

		String errorMsg = "";
		String serviceStatus = "";
		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("Limit Facilities TI Request : \n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = getBankRequestFromTiRequest(requestXML);
			logger.debug("Limit Facilities BankRequest : \n" + bankRequest);

			/******* Finacle http client call *******/
			bankResponse = getBankResponseFromBankRequest(bankRequest);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("Limit Facilities BankResponse : \n" + bankResponse);

			if (!bankRequest.isEmpty() && !bankResponse.isEmpty()) {
				// tiResponse = getTiResponseFromBankResponse(bankResponse);
				tiResponse = getTiResponseFromBankResponseCRN(bankResponse);
				logger.info("Facility TI response "+tiResponse);
				serviceStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				// facilityStataus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				// logger.debug("Loop 1 completed");

			} else if (bankRequest.isEmpty() && !bankResponse.isEmpty()) {
				errorMsg = "LIMIT FACILITIES: Exception occurred while parsing tirequest [IM]";
				tiResponse = generateBanKCustomerLimitResponse(errorMsg, "");
				serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
				// facilityStataus = ThemeBridgeStatusEnum.FAILED.toString();
				// logger.debug("Loop 2 completed");

			} else if (!bankRequest.isEmpty() && bankResponse.isEmpty()) {
				errorMsg = "LIMIT FACILITIES: HTTP 404 - Finacle host unavailable [IM]";
				tiResponse = generateBanKCustomerLimitResponse(errorMsg, "");
				serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
				// facilityStataus = ThemeBridgeStatusEnum.FAILED.toString();
				// logger.debug("Loop 3 completed");

			} else if (bankRequest.isEmpty() && bankResponse.isEmpty()) {
				errorMsg = "LIMIT FACILITIES: Exception occurred while parsing request [IM]";
				tiResponse = generateBanKCustomerLimitResponse(errorMsg, "");
				serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
				// facilityStataus = ThemeBridgeStatusEnum.FAILED.toString();
				// logger.debug("Loop 4 completed");
			}
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("Limit Facilities TI Response : \n" + tiResponse);
			// logger.debug("Loop 5 completed");

		} catch (Exception e) {
			logger.error("Limit facilities Catch Block ");
			tiResponse = generateBanKCustomerLimitResponse(
					"LIMIT FACILITIES: HTTP 404 - Finacle host unavailable. [IM]", "");
			serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();

		} finally {
			// logger.debug("finally block arrived..!!");
			boolean res = ServiceLogging.pushLogData(getRequestHeader().getService(), getRequestHeader().getOperation(),
					getRequestHeader().getSourceSystem(), branch, getRequestHeader().getSourceSystem(),
					getRequestHeader().getTargetSystem(), masterReference, eventReference, serviceStatus, tiRequest,
					tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "",
					relatedParty, "", false, "0", errorMsg);
			logger.debug("finally block completed..!!");
		}
		logger.info(" ************ Limit.Facilities adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String getBankResponseFromBankRequest(String bankRequest) {

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.error("Limit ReservationsReversal Finacle exceptions! " + e.getMessage());
			result = "";
		}
		return result;
	}

	/**
	 * 
	 * @param requestXML
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String getBankRequestFromTiRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		try {
			InputStream anInputStream = LimitFacilitiesAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.LIMIT_FACILITIES_CRN_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			relatedParty = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.RelatedParty);
			postingProduct = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.Product);
			postingSubProduct = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.ProductSubType);
			postingAmount = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.Amount);
			postingCurrency = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.Currency);

			branch = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.Branch);
			masterReference = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.MasterReference);
			eventReference = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.EventReference);
			// TODO
			// productExpiryDate = getProductExpiryDate(masterReference);
			// logger.debug("Product Expiry Date : " + productExpiryDate);

			Map<String, String> tokens = new HashMap<String, String>();

			correlationId = XPathParsing.getValue(requestXML, LimitFacilitiesXpath.correlationIdXPath);
			if (correlationId != null && !correlationId.isEmpty())
				tokens.put("RequestUUID", correlationId);
			else
				tokens.put("RequestUUID", ThemeBridgeUtil.randomCorrelationId());

			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceRequestVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("MessageDateTime", DateTimeUtil.getDateAsEndSystemFormat());
			// logger.debug("Milestone 00");

			String limitType = "C";

			/** UAT **/
			// customerMneonic = XPathParsing.getValue(requestXML,
			// LimitFacilitiesXpath.Customer);
			// String customerType = XPathParsing.getValue(requestXML,
			// LimitFacilitiesXpath.CustomerType);
			/** SIT **/
			// customerMneonic = XPathParsing.getValue(requestXML,
			// LimitFacilitiesXpath.RelatedParty);
			customerID = XPathParsing.getValue(requestXML,
					"/ServiceRequest/FacilitiesRequest/FacilityRequestDetails/Customer");
			// String customerType = getCustomerType(customerMneonic);

			logger.debug("Milestone 02 a");
			tokens.put("customerID", customerID);
			// String sharedLimits = "Y"; // always
			// tokens.put("SHARED_LIMITS", sharedLimits);
			// logger.debug("Milestone 02");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();

		}
		return result;

	}

	/**
	 * 
	 * @param bankResponseXML
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	public String getTiResponseFromBankResponseCRN(String bankResponseXML) {

		String responseXML = "";
		String errorMessages = "";
		String facilitydeatils = "";

		try {
			List<HashMap<String, String>> bankrespMapList = getBankResponseNodeDetails(bankResponseXML);
			logger.debug("TIResponse Facilities size : " + bankrespMapList.size());
			if (bankrespMapList.size() > 0) {
				facilitydeatils = getFacilityDetails(bankrespMapList);
			} else {
				errorMessages = getBankResponseErrorMessage(bankResponseXML);
			}
			// logger.debug("Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", "SUCCEEDED");// PRASATH
			tokens.put("Error", errorMessages);

			// logger.debug("Milestone 03");
			/****** validate *****/
			// if (ValidationsUtil.isValidString(errorMessages)) {
			// tokens.put("Error", errorMessages);
			// } else {
			// tokens.put("Error", "Finacle exception Limit not Found [IM]");
			// }
			tokens.put("Warning", "");
			tokens.put("Info", "");
			tokens.put("FacilityDetailss", facilitydeatils);
			// logger.debug("Milestone 04");

			InputStream anInputStream = LimitFacilitiesAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.LIMIT_FACILITIES_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);
			// logger.debug("Milestone 05");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			responseXML = reader.toString();

			// logger.debug("Milestone 06");
			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			reader.close();

			// logger.debug("Milestone 07");

			// logger.debug("Limit responseXML : " + responseXML);
			return responseXML;

		} catch (Exception e) {
			logger.error("Exception while buil tiresponse XML. " + e.getMessage());
		}
		return responseXML;
	}

	/**
	 * 
	 * @param bankResponseXML
	 *            {@code allows } {@link String}
	 * @return {@code allows List<HashMap<String, String>> } {@link HashMap}
	 */
	public List<HashMap<String, String>> getBankResponseNodeDetails(String bankResponseXML) {

		boolean status = false;
		String bankstatus = null;
		String errorMessages = "";
		List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();

		try {
			int tagcount = XPathParsing.getMultiTagCount(bankResponseXML,
					"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/customerLimitDetails");
			logger.debug("BankResponse count : " + tagcount);

			for (int i = 1; i <= tagcount; i++) {
				HashMap<String, String> hashmap = new HashMap<String, String>();
				String amountValue = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/limitHeaderDetails/totalNodeLimit/amountValue");
				System.out.println("===== i "+i);
				String ammountt = amountValue;
				if (ammountt.contains("E")) {
					ammountt = ammountt.replace("E", "");
				}
				double amount = Double.parseDouble(ammountt);
				amount = Math.floor(amount * 100) / 100;

				hashmap.put("amountValue", String.valueOf(amount));
				String currencyCode = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/limitHeaderDetails/totalNodeLimit/currencyCode");
				hashmap.put("currencyCode", currencyCode);
				String totalLimit = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/limitHeaderDetails/totalLimit/amountValue");
				hashmap.put("totalLimit", totalLimit);
				hashmap.put("customerID", customerID);
				String entityId = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/customerLimitDetails["
								+ i + "]/entityId");
				String sanctionDate = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/customerLimitDetails["
								+ i + "]/sanctionDate");
				String expiryDate = XPathParsing.getValue(bankResponseXML,
						"/FIXML/Body/inquireCustomerLimitDetailsResponse/CustomerLimitDetailsOutputVO/customerLimitDetails["
								+ i + "]/expiryDate");

				hashmap.put("entityId", entityId);
				hashmap.put("sanctionDate", sanctionDate);
				hashmap.put("expiryDate", expiryDate);
				hashMapList.add(hashmap);

			}
			for (HashMap<String, String> hashMap : hashMapList) {
				System.out.println("  ==  " + hashMap);
			}
			// logger.debug(DateTimeUtil.getSqlLocalDateTime());

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
		}

		// logger.debug("hashMapList : " + hashMapList);
		return hashMapList;

	}

	/**
	 * 
	 * @param hashMapList
	 *            {@code allows List<HashMap<String, String>> } {@link String}
	 * @return
	 */
	public String getFacilityDetails(List<HashMap<String, String>> hashMap) {

		// logger.debug(hashMapList);
		StringBuilder facilityDetails = new StringBuilder();

		try {
			int sequenceId = 1;
			for (HashMap<String, String> hashMapList : hashMap) {

				String amount = "";
				if(hashMapList.get("amountValue")!=null)
				amount = hashMapList.get("amountValue");
				amount.replace(".", "");
				String availableamountStr = getTransactionAmount(amount, "INR");
				// logger.debug("AvailableStrAmount : " + availableamountStr);

//				String facilityidentifier = hashMapList.get("limitId");
//				// logger.debug("facilityidentifier : " + facilityidentifier);
//				String nodePrefix = "";
//				String nodeSuffix = "";
//				if (!facilityidentifier.isEmpty() && facilityidentifier != null) {
//					String[] facilityArray = facilityidentifier.split("/");
//					nodePrefix = facilityArray[0];
//					nodeSuffix = facilityArray[1];
//				}
				String facilitycurrency = hashMapList.get("crncyCode");
				//long exposurelongAmount = LimitServicesUtil.exposureLongAmount(postingAmount, postingCurrency);
				facilityDetails.append("\n<m:FacilityDetails>");
				facilityDetails.append("\n<m:Identifier>" + hashMapList.get("entityId") + "</m:Identifier>");
				facilityDetails.append("\n<m:SequenceNumber>" + sequenceId + "</m:SequenceNumber>");
				facilityDetails.append("\n<m:FacilityCode>" + "" + "</m:FacilityCode>");
				facilityDetails.append("\n<m:Description>" + "" + "</m:Description>");
				facilityDetails.append("\n<m:Customer>" + "" + "</m:Customer>");
				facilityDetails.append("\n<m:StartDate>" +hashMapList.get("ssanctionDate")+ "</m:StartDate>");
				facilityDetails.append("\n<m:ExpiryDate>" + hashMapList.get("expiryDate") + "</m:ExpiryDate>");
				facilityDetails.append("\n<m:Currency>" + hashMapList.get("currencyCode") + "</m:Currency>");
				facilityDetails.append("\n<m:LimitAmount>" + "" + "</m:LimitAmount>");
				facilityDetails.append("\n<m:ExposureAmount>" + "" + "</m:ExposureAmount>");
				facilityDetails.append("<m:AvailableAmount>"+hashMapList.get("amountValue")+"</m:AvailableAmount>");
				
				facilityDetails.append("\n<m:LiabilityCurrency>" + "" + "</m:LiabilityCurrency>");
				facilityDetails.append("\n<m:MultiCurrency>" + "" + "</m:MultiCurrency>");
				facilityDetails.append("\n<m:AllowableCurrencies>" + "" + "</m:AllowableCurrencies>");
				facilityDetails.append("\n<m:RelatedParty>" + "" + "</m:RelatedParty>");
				facilityDetails.append("\n<m:RelatedPartyIdentifier>" + "" + "</m:RelatedPartyIdentifier>");
				facilityDetails.append("\n<m:DisplayField1>" + hashMapList.get("customerID") + "</m:DisplayField1>");
				facilityDetails.append("\n<m:DisplayField2>" + hashMapList.get("entityId") + "</m:DisplayField2>");
				facilityDetails.append("\n<m:DisplayField3>" + hashMapList.get("amountValue") + "</m:DisplayField3>");
				facilityDetails.append("\n<m:DisplayField4>" + hashMapList.get("currencyCode") + "</m:DisplayField4>");
				facilityDetails.append("\n<m:DisplayField5>" + hashMapList.get("sanctionDate") + "</m:DisplayField5>");
				facilityDetails.append("\n<m:DisplayField6>" + hashMapList.get("expiryDate") + "</m:DisplayField6>");
				facilityDetails.append("\n<m:DisplayField7>" + "" + "</m:DisplayField7>");
				facilityDetails.append("\n</m:FacilityDetails>");
				facilityDetails.append("\n");

				sequenceId++;
			}

		} catch (Exception e) {
			logger.error("Exceptions while Parsing bankResponseXML..! " + e.getMessage());
			e.printStackTrace();
		}

		return facilityDetails.toString();
	}

	/**
	 * 
	 * @param amount
	 *            {@code allows }{@link String}
	 * @param currency
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getTransactionAmount(String amount, String currency) {

		String result = "";
		BigDecimal transAmount = new BigDecimal(amount);
		if (currency.equals("OMR") || currency.equals("BHD") || currency.equals("KWD") || currency.equals("JOD")) {
			result = transAmount.divide(new BigDecimal(1000), 3, RoundingMode.CEILING).toString();
		} else if (currency.equals("JPY")) {
			result = amount;
		} else {
			result = transAmount.divide(new BigDecimal(100), 2, RoundingMode.CEILING).toString();
		}

		return result.trim();
	}

	/**
	 * 
	 * @param bankResponseXml
	 *            {@code allows }{@link String}
	 * @return {@code returns }{@link String}
	 */
	public static String getBankResponseErrorMessage(String bankResponseXml) {

		String allerrorMessages = "";

		try {
			String executeFinacleScriptExcepCode = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FIScriptResponseExCode);
			String executeFinacleScriptExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FIScriptResponseExDesc);

			String executeFinacleBusinessExcepCode = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FIBusinessErrCode);
			String executeFinacleBusinessExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FIBusinessErrMsgDesc);

			String executeFinacleSystemExcepCode = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FISystemExCode);
			String executeFinacleSystemExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					LimitFacilitiesXpath.FISystemExErrorMsgDesc);

			allerrorMessages = executeFinacleScriptExcepCode + " " + executeFinacleScriptExcepErrorDesc + " "
					+ executeFinacleBusinessExcepCode + " " + executeFinacleBusinessExcepErrorDesc
					+ executeFinacleSystemExcepCode + " " + executeFinacleSystemExcepErrorDesc;

			logger.debug("Limit Facilities BankResponse error : " + allerrorMessages);

		} catch (XPathExpressionException e) {
			logger.error("XPathExpressionException! " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.error("SAXException! " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error("IOException! " + e.getMessage());
			e.printStackTrace();
		}

		return allerrorMessages;
	}

	/**
	 * 
	 * @param masterRef
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	public static Date getProductExpiryDate(String masterRef) {

		// logger.debug("Limit get expired log !");
		Date result = null;
		ResultSet aResultset = null;
		Statement bStatement = null;
		Connection bConnection = null;
		try {
			String expiryDateQuery = "SELECT EXPIRY_DAT, MASTER_REF FROM MASTER WHERE TRIM(MASTER_REF) = '" + masterRef
					+ "'";
			logger.debug("ExpiryDateQuery : " + expiryDateQuery);

			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			aResultset = bStatement.executeQuery(expiryDateQuery);
			while (aResultset.next()) {
				result = aResultset.getDate("EXPIRY_DAT");
				logger.debug("EXPIRY_DAT : " + result);
			}

		} catch (Exception e) {
			logger.error("Prod expiry exception! " + e.getMessage());
			e.printStackTrace();
			return result;

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, null);
		}
		return result;
	}

	public static String getExistingFacilityIdentifier(String masterRef) {

		// logger.debug("Limit get expired log !");
		String result = "";
		ResultSet aResultset = null;
		Statement bStatement = null;
		Connection bConnection = null;
		try {
			String expiryDateQuery = "SELECT FACILITYID FROM CUSTOMERLIMITDETAILS WHERE TRIM(MASTERREFERENCE) = '"
					+ masterRef + "' ";
			logger.debug("ExistingFacilityId : " + expiryDateQuery);

			bConnection = DatabaseUtility.getThemebridgeConnection();
			bStatement = bConnection.createStatement();
			aResultset = bStatement.executeQuery(expiryDateQuery);
			while (aResultset.next()) {
				result = aResultset.getString("FACILITYID");
			}

		} catch (Exception e) {
			logger.error("Existing FacilityId exception! " + e.getMessage());
			e.printStackTrace();
			return result;

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, null);
		}
		return result;
	}

	public static String getCustomerType(String customer) {

		String customerType = "";
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		ResultSet aResultset = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection
					.prepareStatement("SELECT trim(GFCTP1) as GFCTP1 FROM GFPF WHERE GFCUS1 = '" + customer + "'");
			// aPreparedStatement.setString(1, customer);
			aResultset = aPreparedStatement.executeQuery();

			if (aResultset.next()) {
				customerType = aResultset.getString("GFCTP1");
			}

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		logger.debug("customerType : " + customerType);
		return customerType;
	}

	public static void main(String args[]) throws Exception {

//		String ammountt = "5.04E78";
//		if (ammountt.contains("E")) {
//			ammountt = ammountt.replace("E", "");
//		}
//		double amount = Double.parseDouble(ammountt);
//		amount = Math.floor(amount * 100) / 100;
//		System.out.println(amount);


		LimitFacilitiesAdaptee test = new LimitFacilitiesAdaptee();
		String requestXML = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\bobdocuments\\04_TIPlus2.7_API_XMLs\\Limit.Facilities-REQUEST.xml");
		test.process(requestXML);

	}

	
	/**
	 *
	 * @param error
	 *            {@code allows } {@link String}
	 * @param customerLimitResponse
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String generateBanKCustomerLimitResponse(String errorMessage, String customerLimitResponse) {

		try {
			Map<String, String> tokens = new HashMap<String, String>();

			// tokens.put("Status", "FAILED");// RAGHU
			tokens.put("Status", "SUCCEEDED");// PRASATH
			tokens.put("Error", errorMessage);
			tokens.put("Warning", "");
			tokens.put("Info", "");
			tokens.put("FacilityDetailss", customerLimitResponse);

			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.LIMIT_FACILITIES_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			String responseXML = reader.toString();

			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			reader.close();

			// logger.debug(responseXML);
			return responseXML;

		} catch (Exception e) {
			logger.error("Limit Facilities exceptions! " + e.getMessage());
			return e.getMessage();
		}
	}

}
