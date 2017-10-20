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

public class CustomerCustomerDetailsAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(CustomerCustomerDetailsAdaptee.class.getName());

	public CustomerCustomerDetailsAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public CustomerCustomerDetailsAdaptee() {
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

		CustomerCustomerDetailsAdaptee customerCustomerDetailsAdaptee = new CustomerCustomerDetailsAdaptee();
		try {
			String testxml = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\Customer.CustomerDetails-REQUEST");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\Account.AccountSearch-REQUEST.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\SerachREQ.xml");

			String resp = customerCustomerDetailsAdaptee.process(testxml);
			System.out.println("000000000" + resp);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String tirequestXML) {

		// return "bob AccountSearch implementaion called";

		System.out.println(" ************ Customer Details adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			System.out.println("Customer Details TI Request : \n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = getBankRequestFromTiRequest(tirequestXML);
			System.out.println("Customer Details BankRequest : \n" + bankRequest);

			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			System.out.println("Customer Details TI Response : \n" + bankResponse);

			if (bankResponse != null && !bankResponse.isEmpty()) {
				tiResponse = getTIResponseFromBankResponse(bankResponse);
			} else {
				tiResponse = getDefaultErrorResponse("Customer Details: No record found [IM]");
			}

			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			tiResponse = getDefaultErrorResponse("Customer Details: No record found [IM]");

		} finally {
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			processtime = DateTimeUtil.getSqlLocalDateTime();

			ServiceLogging.pushLogData("Account", "AccountSearch", sourceSystem, branch, sourceSystem, targetSystem,
					masterReference, eventReference, status, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);

			System.out.println("finally block completed..!!");
		}
		System.out.println(" ************ Customer Details adaptee process ended ************ ");
		return tiResponse;

	}

	private String getTIResponseFromBankResponse(String bankResponse)
			throws XPathExpressionException, SAXException, IOException {
		HashMap<String, String> bankReqValues = new HashMap<String, String>();
		String tiResponseXML = null;
		try {

			String FullName = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/FullName");
			String CustomerNumber = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CustomerNumber");
			String ShortName = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ShortName");
			String CustomerType = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CustomerType");
			String Blocked = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Blocked");
			String Closed = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Closed");
			String Deceased = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Deceased");
			String Inactive = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Inactive");
			String AccountOfficer = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AccountOfficer");
			String Reference = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Reference");
			String Language = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Language");
			String ParentCountry = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ParentCountry");
			String RiskCountry = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/RiskCountry");
			String ResidenceCountry = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ResidenceCountry");
			String MailToBranch = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/MailToBranch");
			String AnalysisCode = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AnalysisCode");
			String BankCode1 = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/BankCode1");
			String BankCode2 = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/BankCode2");
			String BankCode3 = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/BankCode3");
			String BankCode4 = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/BankCode4");
			String Group = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Group");
			String GroupDescription = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/GroupDescription");
			String DateMaintained = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/DateMaintained");
			String MidasFacilityAllow = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/MidasFacilityAllow");
			String ClearingId = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ClearingId");
			String AddressType = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AddressType");
			String AddressId = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AddressId");
			String Salutation = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Salutation");
			String NameAndAddress = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/NameAndAddress");
			String ZipCode = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ZipCode");
			String Phone = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Phone");
			String Fax = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Fax");
			String Telex = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Telex");
			String TelexAnswerBack = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/TelexAnswerBack");
			String Email = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Email");
			String SwiftBIC = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/SwiftBIC");
			String TransferMethod = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/TransferMethod");
			String AddresseeCustomer = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AddresseeCustomer");
			String NumberOfCopies = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/NumberOfCopies");
			String NumberOfOriginals = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/NumberOfOriginals");
			String Severity = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Severity");
			String Code = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Code");
			String Details = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Details");
			String Style = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Style");
			String Emphasis = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Emphasis");
			String AllowMT103C = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AllowMT103C");
			String Amount = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CutoffAmount/Amount");
			String Currency = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CutoffAmount/Currency");
			String SWIFTAckRequired = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/SWIFTAckRequired");
			String TransliterateSWIFT = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/TransliterateSWIFT");
			String Team = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Team");
			String CorporateAccess = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/CorporateAccess");
			String PrincipalFxRateCode = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/PrincipalFxRateCode");
			String ChargeFxRateCode = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/ChargeFxRateCode");
			String AllowTaxExemptions = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/AllowTaxExemptions");
			String Suspended = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Suspended");
			String SwiftAddress = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/SwiftAddress");
			String Authenticated = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/Authenticated");
			String TransliterationRequired = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/CustomerDetail/TransliterationRequired");

			bankReqValues.put("FullName", FullName);
			bankReqValues.put("CustomerNumber", CustomerNumber);
			bankReqValues.put("ShortName", ShortName);
			bankReqValues.put("CustomerType", CustomerType);
			bankReqValues.put("Blocked", Blocked);
			bankReqValues.put("Closed", Closed);
			bankReqValues.put("Deceased", Deceased);
			bankReqValues.put("Inactive", Inactive);
			bankReqValues.put("AccountOfficer", AccountOfficer);
			bankReqValues.put("Reference", Reference);
			bankReqValues.put("Language", Language);
			bankReqValues.put("ParentCountry", ParentCountry);
			bankReqValues.put("RiskCountry", RiskCountry);
			bankReqValues.put("ResidenceCountry", ResidenceCountry);
			bankReqValues.put("MailToBranch", MailToBranch);
			bankReqValues.put("AnalysisCode", AnalysisCode);
			bankReqValues.put("BankCode1", BankCode1);
			bankReqValues.put("BankCode2", BankCode2);
			bankReqValues.put("BankCode3", BankCode3);
			bankReqValues.put("BankCode4", BankCode4);
			bankReqValues.put("Group", Group);
			bankReqValues.put("GroupDescription", GroupDescription);
			bankReqValues.put("DateMaintained", DateMaintained);
			bankReqValues.put("MidasFacilityAllow", MidasFacilityAllow);
			bankReqValues.put("ClearingId", ClearingId);
			bankReqValues.put("AddressType", AddressType);
			bankReqValues.put("AddressId", AddressId);
			bankReqValues.put("Salutation", Salutation);
			bankReqValues.put("NameAndAddress", NameAndAddress);
			bankReqValues.put("ZipCode", ZipCode);
			bankReqValues.put("Phone", Phone);
			bankReqValues.put("Fax", Fax);
			bankReqValues.put("Telex", Telex);
			bankReqValues.put("TelexAnswerBack", TelexAnswerBack);
			bankReqValues.put("Email", Email);
			bankReqValues.put("SwiftBIC", SwiftBIC);
			bankReqValues.put("TransferMethod", TransferMethod);
			bankReqValues.put("AddresseeCustomer", AddresseeCustomer);
			bankReqValues.put("NumberOfCopies", NumberOfCopies);
			bankReqValues.put("NumberOfOriginals", NumberOfOriginals);
			bankReqValues.put("Severity", Severity);
			bankReqValues.put("Code", Code);
			bankReqValues.put("Details", Details);
			bankReqValues.put("Style", Style);
			bankReqValues.put("Emphasis", Emphasis);
			bankReqValues.put("AllowMT103C", AllowMT103C);
			bankReqValues.put("Amount", Amount);
			bankReqValues.put("Currency", Currency);
			bankReqValues.put("SWIFTAckRequired", SWIFTAckRequired);
			bankReqValues.put("TransliterateSWIFT", TransliterateSWIFT);
			bankReqValues.put("Team", Team);
			bankReqValues.put("CorporateAccess", CorporateAccess);
			bankReqValues.put("PrincipalFxRateCode", PrincipalFxRateCode);
			bankReqValues.put("ChargeFxRateCode", ChargeFxRateCode);
			bankReqValues.put("AllowTaxExemptions", AllowTaxExemptions);
			bankReqValues.put("Suspended", Suspended);
			bankReqValues.put("SwiftAddress", SwiftAddress);
			bankReqValues.put("Authenticated", Authenticated);
			bankReqValues.put("TransliterationRequired", TransliterationRequired);

			System.out.println("bankReqValues ====> " + bankReqValues);

			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.CUSTOMER_DETAILS_TI_RESPONSE_TEMPLATE);
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

				tokens.put("FullName", bankReqValues.get("FullName"));
				tokens.put("CustomerNumber", bankReqValues.get("CustomerNumber"));
				tokens.put("ShortName", bankReqValues.get("ShortName"));
				tokens.put("CustomerType", bankReqValues.get("CustomerType"));
				tokens.put("Blocked", bankReqValues.get("Blocked"));
				tokens.put("Closed", bankReqValues.get("Closed"));
				tokens.put("Deceased", bankReqValues.get("Deceased"));
				tokens.put("Inactive", bankReqValues.get("Inactive"));
				tokens.put("AccountOfficer", bankReqValues.get("AccountOfficer"));
				tokens.put("Reference", bankReqValues.get("Reference"));
				tokens.put("Language", bankReqValues.get("Language"));
				tokens.put("ParentCountry", bankReqValues.get("ParentCountry"));
				tokens.put("RiskCountry", bankReqValues.get("RiskCountry"));
				tokens.put("ResidenceCountry", bankReqValues.get("ResidenceCountry"));
				tokens.put("MailToBranch", bankReqValues.get("MailToBranch"));
				tokens.put("AnalysisCode", bankReqValues.get("AnalysisCode"));
				tokens.put("BankCode1", bankReqValues.get("BankCode1"));
				tokens.put("BankCode2", bankReqValues.get("BankCode2"));
				tokens.put("BankCode3", bankReqValues.get("BankCode3"));
				tokens.put("BankCode4", bankReqValues.get("BankCode4"));
				tokens.put("Group", bankReqValues.get("Group"));
				tokens.put("GroupDescription", bankReqValues.get("GroupDescription"));
				tokens.put("DateMaintained", bankReqValues.get("DateMaintained"));
				tokens.put("MidasFacilityAllow", bankReqValues.get("MidasFacilityAllow"));
				tokens.put("ClearingId", bankReqValues.get("ClearingId"));
				tokens.put("AddressType", bankReqValues.get("AddressType"));
				tokens.put("AddressId", bankReqValues.get("AddressId"));
				tokens.put("Salutation", bankReqValues.get("Salutation"));
				tokens.put("NameAndAddress", bankReqValues.get("NameAndAddress"));
				tokens.put("ZipCode", bankReqValues.get("ZipCode"));
				tokens.put("Phone", bankReqValues.get("Phone"));
				tokens.put("Fax", bankReqValues.get("Fax"));
				tokens.put("Telex", bankReqValues.get("Telex"));
				tokens.put("TelexAnswerBack", bankReqValues.get("TelexAnswerBack"));
				tokens.put("Email", bankReqValues.get("Email"));
				tokens.put("SwiftBIC", bankReqValues.get("SwiftBIC"));
				tokens.put("TransferMethod", bankReqValues.get("TransferMethod"));
				tokens.put("AddresseeCustomer", bankReqValues.get("AddresseeCustomer"));
				tokens.put("NumberOfCopies", bankReqValues.get("NumberOfCopies"));
				tokens.put("NumberOfOriginals", bankReqValues.get("NumberOfOriginals"));
				tokens.put("Severity", bankReqValues.get("Severity"));
				tokens.put("Code", bankReqValues.get("Code"));
				tokens.put("Details", bankReqValues.get("Details"));
				tokens.put("Style", bankReqValues.get("Style"));
				tokens.put("Emphasis", bankReqValues.get("Emphasis"));
				tokens.put("AllowMT103C", bankReqValues.get("AllowMT103C"));
				tokens.put("Amount", bankReqValues.get("Amount"));
				tokens.put("Currency", bankReqValues.get("Currency"));
				tokens.put("SWIFTAckRequired", bankReqValues.get("SWIFTAckRequired"));
				tokens.put("TransliterateSWIFT", bankReqValues.get("TransliterateSWIFT"));
				tokens.put("Team", bankReqValues.get("Team"));
				tokens.put("CorporateAccess", bankReqValues.get("CorporateAccess"));
				tokens.put("PrincipalFxRateCode", bankReqValues.get("PrincipalFxRateCode"));
				tokens.put("ChargeFxRateCode", bankReqValues.get("ChargeFxRateCode"));
				tokens.put("AllowTaxExemptions", bankReqValues.get("AllowTaxExemptions"));
				tokens.put("Suspended", bankReqValues.get("Suspended"));
				tokens.put("SwiftAddress", bankReqValues.get("SwiftAddress"));
				tokens.put("Authenticated", bankReqValues.get("Authenticated"));
				tokens.put("TransliterationRequired", bankReqValues.get("TransliterationRequired"));

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

		System.out.println("***** Customer Details error response initiated *****");

		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			tokens.put("Status", "SUCCEEDED");
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
