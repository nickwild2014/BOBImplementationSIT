package com.bs.theme.bob.adapter.adaptee;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class CustomerCustomerSearchAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(CustomerCustomerSearchAdaptee.class.getName());

	public CustomerCustomerSearchAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public CustomerCustomerSearchAdaptee() {
	}

	private String sourceSystem = "";
	private String targetSystem = "";

	private static String branch = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private Timestamp processtime = null;
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";

	public static void main(String[] args) {

		CustomerCustomerSearchAdaptee customerCustomerSearchAdaptee = new CustomerCustomerSearchAdaptee();
		try {
			String testxml = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\Customer.CustomerDetails-REQUEST");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\Account.AccountSearch-REQUEST.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\SerachREQ.xml");

			String resp = customerCustomerSearchAdaptee.process(testxml);
			System.out.println("000000000" + resp);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String tirequestXML) {

		// return "bob AccountSearch implementaion called";

		System.out.println(" ************ Customer Search adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			System.out.println("Customer Search TI Request : \n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = getBankRequestFromTiRequest(tirequestXML);
			System.out.println("Customer Search BankRequest : \n" + bankRequest);

			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			System.out.println("Customer Search TI Response : \n" + bankResponse);

			if (bankResponse != null && !bankResponse.isEmpty()) {
				tiResponse = getTIResponseFromBankResponse(bankResponse);
			} else {
				tiResponse = getDefaultErrorResponse("Customer Search: No record found [IM]");
			}

			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			tiResponse = getDefaultErrorResponse("Customer Search: No record found [IM]");

		} finally {
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			processtime = DateTimeUtil.getSqlLocalDateTime();

			ServiceLogging.pushLogData("Account", "AccountSearch", sourceSystem, branch, sourceSystem, targetSystem,
					masterReference, eventReference, status, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);

			System.out.println("finally block completed..!!");
		}
		System.out.println(" ************ Customer Search adaptee process ended ************ ");
		return tiResponse;

	}

	private String getTIResponseFromBankResponse(String bankResponse)
			throws XPathExpressionException, SAXException, IOException {
		HashMap<String, String> bankReqValues = new HashMap<String, String>();
		String tiResponseXML = null;
		try {

			
			String Mnemonic = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Mnemonic");
			String FullName = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/FullName");
			String CustomerNumber = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CustomerNumber");
			String Blocked = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Blocked");
			String AccountOfficer = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AccountOfficer");
			String ResidenceCountry = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ResidenceCountry");
			String Group = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Group");
			bankReqValues.put("CustomerMnemonic", Mnemonic); 
			bankReqValues.put("FullName", FullName); 
			bankReqValues.put("CustomerNumber", CustomerNumber); 
			bankReqValues.put("Blocked", Blocked);  
			bankReqValues.put("AccountOfficer", AccountOfficer);  
			bankReqValues.put("CountryOfResidence", ResidenceCountry); 
			bankReqValues.put("Group", Group);    
			System.out.println("bankReqValues ====> " + bankReqValues);
			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.CUSTOMER_SEARCH_TI_RESPONSE_TEMPLATE);
				String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
				String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
				String requestId = "Req_" + ThemeBridgeUtil.randomCorrelationId();
				correlationId = requestId;
				// logger.debug("BankReqXML Milestone 02");
				Map<String, String> tokens = new HashMap<String, String>();
				tokens.put("dateTime", dateTime);
				tokens.put("requestId", requestId);
				tokens.put("ChannelId", KotakConstant.CHANNELID);
				tokens.put("BankId", KotakConstant.BANKID);
				tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
				tokens.put("CorrelationId", correlationId);
				tokens.put("CustomerMnemonic",bankReqValues.get("CustomerMnemonic"));
				tokens.put("FullName", bankReqValues.get("FullName"));
				tokens.put("CustomerNumber", bankReqValues.get("CustomerNumber"));
				tokens.put("Blocked", bankReqValues.get("Blocked"));
				tokens.put("AccountOfficer", bankReqValues.get("AccountOfficer"));
				tokens.put("CountryOfResidence", bankReqValues.get("CountryOfResidence"));   
				tokens.put("Group", bankReqValues.get("Group"));
				tokens.put("AdditionalResults", "");
				
				tokens.put("Status", "SUCCEEDED");
				tokens.put("Error", "");
				tokens.put("Warning", "");
				tokens.put("Info", "");

				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(requestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				tiResponseXML = reader.toString();
				reader.close();
			} catch (Exception e) {
				logger.error("Bank Request XML generate exceptions! " + e.getMessage());
				e.printStackTrace();
				return e.getMessage();
			}
			logger.info("Final tiResponseXML message-->" + tiResponseXML);

		} catch (Exception e) {
			System.out.println("tiResponseXML Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		return tiResponseXML;
	}

	private String getBankResponseFromBankRequest(String bankRequest)
			throws HttpException, IOException, FinacleServiceException {

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.debug("Exception..! " + e.getMessage());
			e.printStackTrace();

		}
		return result;
	}

	/**
	 * @ @param
	 *       errorMsg {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getDefaultErrorResponse(String errorMsg) {

		System.out.println("***** Customer Search error response initiated *****");

		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			tokens.put("Status", "FAILED");
			tokens.put("Info", "");
			tokens.put("Error", errorMsg);
			tokens.put("Warning", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("AccountSearchResult", "");
			tokens.put("AdditionalResults", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			String responseTemplate;
			InputStream anInputStream = AccountAccountSearchAdapteeInternal.class.getClassLoader()
					.getResourceAsStream("Account.SearchResponseTemplate.xml");
			responseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			String responseXML = reader.toString();
			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			// System.out.println("Removed empty <tag> responseXML : " +
			// responseXML);
			response = responseXML;
			reader.close();

		} catch (Exception e) {
			System.out.println("Default exception! " + e.getMessage());
			e.printStackTrace();
		}
		return response;
	}

	public String getBankRequestFromTiRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		String bankRequestt = "";
		HashMap<String, String> tiReqValues = new HashMap<String, String>();
		try {
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);

			correlationId = XPathParsing.getValue(requestXML, "/ServiceRequest/RequestHeader/CorrelationId");
			String customerId = XPathParsing.getValue(requestXML, "/ServiceRequest/CustomerDetailsRequest/CustomerId");
			String primeType = XPathParsing.getValue(requestXML, "/ServiceRequest/CustomerDetailsRequest/PrimeType");
			String swiftType = XPathParsing.getValue(requestXML, "/ServiceRequest/CustomerDetailsRequest/SwiftType");
			tiReqValues.put("customerId", customerId);
			tiReqValues.put("primeType", primeType);
			tiReqValues.put("swiftType", swiftType);

			bankRequestt = generateBankRequest(tiReqValues);

		} catch (Exception e) {
			System.out.println("TiValues Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		return bankRequestt;
	}

	private String generateBankRequest(Map<String, String> postingLegList) {

		// logger.info("Enter into generateBankRequest message");
		// logger.debug("BankReqXML Milestone 01");
		String bankRequestXML = null;
		try {
			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.CUSTOMER_DETAILS_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = "Req_" + ThemeBridgeUtil.randomCorrelationId();
			correlationId = requestId;
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("CustID", postingLegList.get("customerId"));

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			bankRequestXML = reader.toString();
			reader.close();
		} catch (Exception e) {
			logger.error("Bank Request XML generate exceptions! " + e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}
		logger.info("Final bank requestXML message-->" + bankRequestXML);
		return bankRequestXML;
	}

}



