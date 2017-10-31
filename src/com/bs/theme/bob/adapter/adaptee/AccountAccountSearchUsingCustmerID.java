package com.bs.theme.bob.adapter.adaptee;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class AccountAccountSearchUsingCustmerID extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(AccountAccountSearchUsingCustmerID.class.getName());

	public AccountAccountSearchUsingCustmerID(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public AccountAccountSearchUsingCustmerID() {
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

		AccountAccountSearchUsingCustmerID accountAccountSearchAdaptee = new AccountAccountSearchUsingCustmerID();
		try {
//			String bankResponse = ThemeBridgeUtil.readFile(
//					"C:\\Users\\subhash\\Desktop\\new 11.txt");
//			String tiResponse = accountAccountSearchAdaptee.getTIResponseFromBankResponse(bankResponse);
//			System.out.println(tiResponse);
			String tirequestXML = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\bob documents\\04_TIPlus2.7_API_XMLs\\Account.AccountSearch-REQUEST.xml");
			String bankRequest = accountAccountSearchAdaptee.getBankRequestFromTiRequest(tirequestXML);
			System.out.println("Account Search BankRequest : \n" + bankRequest);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String tirequestXML)  {
		System.out.println(" ************ Account Search adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			System.out.println("Account Search TI Request : \n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = getBankRequestFromTiRequest(tirequestXML);
			System.out.println("Account Search BankRequest : \n" + bankRequest);

			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			System.out.println("Account Search TI Response : \n" + bankResponse);

			if (bankResponse != null && !bankResponse.isEmpty()) {
				tiResponse = getTIResponseFromBankResponse(bankResponse);
				System.out.println(tiResponse);
			} else {
				tiResponse = getDefaultErrorResponse("Account Search: No record found [IM]");
			}

			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			tiResponse = getDefaultErrorResponse("Account Search: No record found [IM]");

		} finally {
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			processtime = DateTimeUtil.getSqlLocalDateTime();

			ServiceLogging.pushLogData("Account", "AccountDetails", sourceSystem, branch, sourceSystem, targetSystem,
					masterReference, eventReference, status, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);

			System.out.println("finally block completed..!!");
		}
		System.out.println(" ************ Account Search adaptee process ended ************ ");
		return tiResponse;

	}


	private String getTIResponseFromBankResponse(String bankResponse)
			throws XPathExpressionException, SAXException, IOException {
		String tiResponseXML = null;
		
		int tagcount = XPathParsing.getMultiTagCount(bankResponse,
				"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account");
		System.out.println("tagcount "+tagcount);
		
		List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
		
		for (int i = 1; i <= tagcount; i++) {
			HashMap<String, String> hashmap = new HashMap<String, String>();
			String Branch = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/Branch");
			String Customer = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/Customer");
			String AccountType = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/AccountType");
			String Currency = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/Currency");
			String OtherCurrency = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/OtherCurrency");
			String BackOfficeAccount = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/BackOfficeAccount");
			String ShortName = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerAccountDetails/Account["+i+"]/ShortName");
			hashmap.put("Branch", Branch);
			hashmap.put("Customer", Customer);
			hashmap.put("AccountType", AccountType);
			hashmap.put("Currency", Currency);
			hashmap.put("OtherCurrency", OtherCurrency);
			hashmap.put("BackOfficeAccount", BackOfficeAccount);
			hashmap.put("ShortName", ShortName);
			hashMapList.add(hashmap);
		}
		StringBuilder customerAccounts = new StringBuilder();
		for (HashMap<String, String> hashMap : hashMapList) {
			
			customerAccounts.append("\n<m:AccountSearchResult>");
			customerAccounts.append("\n<m:Branch>"+hashMap.get("Branch")+"</m:Branch>");
			customerAccounts.append("\n<m:BranchNumber></m:BranchNumber>");
			customerAccounts.append("\n<m:Customer>"+hashMap.get("Customer")+"</m:Customer>");
			customerAccounts.append("\n<m:CustomerNumber></m:CustomerNumber>");
			customerAccounts.append("\n<m:SystemParameter></m:SystemParameter>");
			customerAccounts.append("\n<m:ChargeCode></m:ChargeCode>");
			customerAccounts.append("\n<m:CategoryCode></m:CategoryCode>");
			customerAccounts.append("\n<m:AccountType>"+hashMap.get("AccountType")+"</m:AccountType>");
			customerAccounts.append("\n<m:Currency>"+hashMap.get("Currency")+"</m:Currency>");
			customerAccounts.append("\n<m:OtherCurrency>"+hashMap.get("OtherCurrency")+"</m:OtherCurrency>");
			customerAccounts.append("\n<m:BackOfficeAccount>"+hashMap.get("BackOfficeAccount")+"</m:BackOfficeAccount>");
			customerAccounts.append("\n<m:ShortName>"+hashMap.get("ShortName")+"</m:ShortName>");
			customerAccounts.append("\n<m:ExternalAccount></m:ExternalAccount>");
			customerAccounts.append("\n<m:IBAN></m:IBAN>");
			
			customerAccounts.append("\n</m:AccountSearchResult>");
		
		}
		
		try {

			// String STATUS = XPathParsing.getValue(bankResponse,
			// "/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/STATUS/");

			
			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.ACCOUNT_SEARCH_USING_CUSTID_TI_RESPONSE_TEMPLATE);
				String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
				String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
				String requestId = "Req_" + correlationId;
				Map<String, String> tokens = new HashMap<String, String>();
				tokens.put("dateTime", dateTime);
				tokens.put("requestId", requestId);
				tokens.put("ChannelId", KotakConstant.CHANNELID);
				tokens.put("BankId", KotakConstant.BANKID);
				tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
				tokens.put("CorrelationId", correlationId);
				// if(STATUS.equalsIgnoreCase("SUCCESS"))
				tokens.put("Status", "SUCCEEDED");
				// else tokens.put("Status", "FAILED");
				tokens.put("Error", "");
				tokens.put("Warning", "");
				tokens.put("Info", "");
				tokens.put("customerAccounts", customerAccounts.toString());
				tokens.put("AdditionalResults", "");
				
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

		System.out.println("***** Account Search error response initiated *****");

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
			String CustID = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/Customer");
			// String accountNumber =
			// XPathParsing.getValue(requestXML,"/ServiceRequest/AccountDetailsRequest/AccountNumber");
			System.out.println("CustID " + CustID);
			// tiReqValues.put("accountFormat", accountFormat);
			tiReqValues.put("CustID", CustID);

			if (correlationId != null && !correlationId.isEmpty()) {
				bankRequestt = generateBankRequest(tiReqValues, correlationId);
			} else {
				bankRequestt = generateBankRequest(tiReqValues);
			}

		} catch (Exception e) {
			System.out.println("TiValues Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		return bankRequestt;
	}

	private String generateBankRequest(Map<String, String> postingLegList, String correlationId) {

		// logger.info("Enter into generateBankRequest message");
		// logger.debug("BankReqXML Milestone 01");
		String bankRequestXML = null;
		try {
			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_DETAILS_USING_CUSTID_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", correlationId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("CUSTID", postingLegList.get("CustID"));
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

	private String generateBankRequest(Map<String, String> postingLegList) {

		// logger.info("Enter into generateBankRequest message");
		// logger.debug("BankReqXML Milestone 01");
		String bankRequestXML = null;
		try {
			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_DETAILS_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = "Req_" + ThemeBridgeUtil.randomCorrelationId();
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("ACCTNO", postingLegList.get("accountNumber"));
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
