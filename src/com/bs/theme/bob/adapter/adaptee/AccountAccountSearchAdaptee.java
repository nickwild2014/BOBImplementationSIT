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

public class AccountAccountSearchAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(AccountAccountSearchAdaptee.class.getName());

	public AccountAccountSearchAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public AccountAccountSearchAdaptee() {
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

		AccountAccountSearchAdaptee accountAccountSearchAdaptee = new AccountAccountSearchAdaptee();
		try {
			String testxml = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\bob documents\\04_TIPlus2.7_API_XMLs\\Account.AccountDetails-REQUEST.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\Account.AccountSearch-REQUEST.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\SerachREQ.xml");

			String resp = accountAccountSearchAdaptee.process(testxml);
			System.out.println(resp);
			System.out.println("000000000" + resp);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String tirequestXML) {
		String SearchType1 ="";
		try {
		 SearchType1 = XPathParsing.getValue(tirequestXML,"/ServiceRequest/AccountSearchRequest/SearchType");
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(SearchType1!=null && !SearchType1.isEmpty())
		{
			if(SearchType1.equalsIgnoreCase("internal"))
			{
			tiResponse = new AccountAccountSearchAdapteeInternal().process(tirequestXML);
			 return tiResponse;
			}
		}

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
		HashMap<String, String> bankReqValues = new HashMap<String, String>();
		String tiResponseXML = null;
		try {
			
			

			//String STATUS = XPathParsing.getValue(bankResponse,
				//	"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/STATUS/");
			
			String MaintainedInBackOffice = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/MaintainedInBackOffice");
            String AccountNumber = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/AccountNumber");
            String Branch = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/SolId");
            String customer = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/CustId");
            String SchmType = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/SchmType");
            String Currency = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/AcctCrncy");
            String IBAN_Number = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/IBAN_Number");
            String Short_Name = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/Short_Name");
            String Country = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/Country");
            String ContingentAccount = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/ContingentAccount");
            String InternalAccount = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/InternalAccount");
            String AcctOpenDate = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/AcctOpenDate");
            String DateMaintained = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/DateMaintained");
            String AcctCloseDate = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/AccountDetail/AcctCloseDate");
			
            bankReqValues.put("AccountNumber", AccountNumber);
            bankReqValues.put("Short_Name", Short_Name);
			bankReqValues.put("SolId", Branch);
			bankReqValues.put("BranchNumber", "");
			bankReqValues.put("CustId", customer);
			bankReqValues.put("CustomerNumber", "");
			bankReqValues.put("CategoryCode", "");
			bankReqValues.put("SchmType", SchmType);
			bankReqValues.put("AcctCrncy", Currency);
			bankReqValues.put("CurrencyNumber", "");
			bankReqValues.put("OtherCurrency", "");
			bankReqValues.put("OtherCurrencyNumber", "");
			bankReqValues.put("ExternalAccount", "");
			bankReqValues.put("IBAN_Number", IBAN_Number);
			bankReqValues.put("ContingentAccount", ContingentAccount);
			bankReqValues.put("InternalAccount", InternalAccount);
			bankReqValues.put("AcctOpenDate", AcctOpenDate);
			bankReqValues.put("DateMaintained", DateMaintained);
			bankReqValues.put("AcctCloseDate", AcctCloseDate);
			bankReqValues.put("Description1", "");
			bankReqValues.put("Description2", "");
			
            
			System.out.println("bankReqValues ====> " + bankReqValues);
			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.ACCOUNT_SEARCH_TI_RESPONSE_TEMPLATE);
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


				tokens.put("AccountNumber",bankReqValues.get("AccountNumber"));
				tokens.put("SolId",bankReqValues.get("SolId"));
				tokens.put("BranchNumber",bankReqValues.get("BranchNumber"));
				tokens.put("CustId",bankReqValues.get("CustId"));
				tokens.put("CustomerNumber",bankReqValues.get("CustomerNumber"));
				tokens.put("CategoryCode",bankReqValues.get("CategoryCode"));
				tokens.put("SchmType",bankReqValues.get("SchmType"));
				tokens.put("AcctCrncy",bankReqValues.get("AcctCrncy"));
				tokens.put("CurrencyNumber",bankReqValues.get("CurrencyNumber"));
				tokens.put("OtherCurrency",bankReqValues.get("OtherCurrency"));
				tokens.put("OtherCurrencyNumber",bankReqValues.get("OtherCurrencyNumber"));
				tokens.put("ExternalAccount",bankReqValues.get("ExternalAccount"));
				tokens.put("IBAN_Number",bankReqValues.get("IBAN_Number"));
				tokens.put("Short_Name",bankReqValues.get("Short_Name"));
				tokens.put("ContingentAccount",bankReqValues.get("ContingentAccount"));
				tokens.put("InternalAccount",bankReqValues.get("InternalAccount"));
				tokens.put("AcctOpenDate",bankReqValues.get("AcctOpenDate"));
				tokens.put("DateMaintained",bankReqValues.get("DateMaintained"));
				tokens.put("AcctCloseDate",bankReqValues.get("AcctCloseDate"));
				tokens.put("Description1",bankReqValues.get("Description1"));
				tokens.put("Description2",bankReqValues.get("Description2"));
				
				tokens.put("SystemParameter","");
				tokens.put("ChargeCode","");
				tokens.put("AdditionalResults","");
				
				//if(STATUS.equalsIgnoreCase("SUCCESS"))
				tokens.put("Status", "SUCCEEDED");
				//else tokens.put("Status", "FAILED");
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
			String BackOfficeAccount = XPathParsing.getValue(requestXML,"/ServiceRequest/AccountSearchRequest/BackOfficeAccount");
			//String accountNumber = XPathParsing.getValue(requestXML,"/ServiceRequest/AccountDetailsRequest/AccountNumber");
			System.out.println("BackOfficeAccount "+BackOfficeAccount);
			//tiReqValues.put("accountFormat", accountFormat);
			tiReqValues.put("accountNumber", BackOfficeAccount);

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
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_DETAILS_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", correlationId);
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

