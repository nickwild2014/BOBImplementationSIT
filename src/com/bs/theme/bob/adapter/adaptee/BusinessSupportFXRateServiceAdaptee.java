package com.bs.theme.bob.adapter.adaptee;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

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

public class BusinessSupportFXRateServiceAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(BusinessSupportFXRateServiceAdaptee.class.getName());

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
	private String dealTicketId = "";

	String Date1 = "";
	String TxnAmount1 = "";
	String FromCrncy1 = "";
	String ToCrncy = "";
	String RateCode = "";

	public static void main(String args[]) throws Exception {
		// String request =
		// ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\bobdocuments\\04_TIPlus2.7_API_XMLs\\BusinessSupport.FXRateService-REQUEST.xml");
		// String DealTicketId = XPathParsing.getValue(request,
		// "/ServiceRequest/FXRateServiceRequest/DealTicketId");
		// System.out.println("deal ticket == "+DealTicketId.isEmpty());
	}

	@Override
	public String process(String tirequestXML) throws Exception {

		dealTicketId = XPathParsing.getValue(tirequestXML, "/ServiceRequest/FXRateServiceRequest/DealTicketId");

		if (dealTicketId != null && !dealTicketId.isEmpty()) {
			tiResponse = new BusinessSupportFXRateServiceInternal().process(tirequestXML);
		} else {
			System.out.println(" ************ FXRateService adaptee process started ************ ");
			String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			String errorMsg = "";
			try {
				tiRequest = tirequestXML;
				tiReqTime = DateTimeUtil.getSqlLocalDateTime();
				System.out.println("FXRateService TI Request : \n" + tiRequest);

				bankReqTime = DateTimeUtil.getSqlLocalDateTime();
				bankRequest = getBankRequestFromTiRequest(tirequestXML);
				System.out.println("FXRateService BankRequest : \n" + bankRequest);

				bankResTime = DateTimeUtil.getSqlLocalDateTime();

				bankResponse = getBankResponseFromBankRequest(bankRequest);
				System.out.println("FXRateService TI Response : \n" + bankResponse);

				if (bankResponse != null && !bankResponse.isEmpty()) {
					tiResponse = getTIResponseFromBankResponse(bankResponse);
					System.out.println(tiResponse);
				} else {
					tiResponse = getDefaultErrorResponse("FXRateService: No record found [IM]");
				}

				tiResTime = DateTimeUtil.getSqlLocalDateTime();

			} catch (Exception e) {
				status = ThemeBridgeStatusEnum.FAILED.toString();
				errorMsg = e.getMessage();
				tiResponse = getDefaultErrorResponse("FXRateService: No record found [IM]");

			} finally {
				tiResTime = DateTimeUtil.getSqlLocalDateTime();
				processtime = DateTimeUtil.getSqlLocalDateTime();

				ServiceLogging.pushLogData("BusinessSupport", "FXRateService", sourceSystem, branch, sourceSystem,
						targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,
						bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
						errorMsg);

				System.out.println("finally block completed..!!");
			}

		}

		return tiResponse;
	}

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
			tokens.put("dealTicketId", dealTicketId);
			tokens.put("RateCode", RateCode);

			tokens.put("TxnAmount1", "");
			tokens.put("FromCrncy1", "");
			tokens.put("ToCrncy", "");

			tokens.put("Reciprocal", "");
			tokens.put("ExchangeRate", "");

			tokens.put("DealDirection", "");
			tokens.put("FXRateType", "");
			tokens.put("BuyCurrency", "");
			tokens.put("BuyCurrencyQuotationUnit", "");
			tokens.put("BuyRateToBase", "");
			tokens.put("SellCurrency", "");
			tokens.put("SellCurrencyQuotationUnit", "");
			tokens.put("SellRateToBase", "");
			tokens.put("FxRateDate", "");
			tokens.put("FxRateTime", "");
			tokens.put("BaseCurrency", "");
			tokens.put("OrderId", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			String responseTemplate;
			InputStream anInputStream = AccountAccountSearchAdapteeInternal.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FX_RATE_TI_RESPONSE_TEMPLATE);
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

	public String getBankRequestFromTiRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		String bankRequestt = "";
		HashMap<String, String> tiReqValues = new HashMap<String, String>();
		try {
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
			correlationId = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/CorrelationId");
			ToCrncy = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/Currency2");
			RateCode = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/FXRateCode");
			TxnAmount1 = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/Amount1/Amount");
			FromCrncy1 = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/Amount1/Currency");

			Date1 = XPathParsing.getValue(requestXML, "/ServiceRequest/FXRateServiceRequest/Date1");

			System.out.println("ToCrncy " + ToCrncy);
			System.out.println("RateCode " + RateCode);
			System.out.println("TxnAmount " + TxnAmount1);
			System.out.println("FromCrncy " + FromCrncy1);

			tiReqValues.put("ToCrncy", ToCrncy);
			tiReqValues.put("RateCode", RateCode);
			tiReqValues.put("TxnAmount", TxnAmount1);
			tiReqValues.put("FromCrncy", FromCrncy1);

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

	private String generateBankRequest(Map<String, String> tiReqValues, String correlationId) {

		// logger.info("Enter into generateBankRequest message");
		// logger.debug("BankReqXML Milestone 01");
		String bankRequestXML = null;
		try {
			InputStream anInputStream = BusinessSupportFXRateServiceAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FX_RATE_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", correlationId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("ACCTNO", tiReqValues.get("accountNumber"));
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
			InputStream anInputStream = BusinessSupportFXRateServiceAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FX_RATE_TEMPLATE);
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

	private String getTIResponseFromBankResponse(String bankResponse)
			throws XPathExpressionException, SAXException, IOException {
		HashMap<String, String> bankReqValues = new HashMap<String, String>();
		String tiResponseXML = null;
		try {

			// String STATUS = XPathParsing.getValue(bankResponse,
			// "/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/STATUS/");

			String ExchangeRate = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/FXRateServiceResponse/ExchangeRate");

			String Reciprocal = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/executeFinacleScriptResponse/executeFinacleScript_CustomData/FXRateServiceResponse/Reciprocal");
			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.FX_RATE_TI_RESPONSE_TEMPLATE);
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
				tokens.put("dealTicketId", dealTicketId);
				tokens.put("RateCode", RateCode);

				tokens.put("TxnAmount1", TxnAmount1);
				tokens.put("FromCrncy1", FromCrncy1);
				tokens.put("ToCrncy", ToCrncy);

				tokens.put("Reciprocal", Reciprocal);
				tokens.put("ExchangeRate", ExchangeRate);

				tokens.put("DealDirection", "");
				tokens.put("FXRateType", "");
				tokens.put("BuyCurrency", "");
				tokens.put("BuyCurrencyQuotationUnit", "");
				tokens.put("BuyRateToBase", "");
				tokens.put("SellCurrency", "");
				tokens.put("SellCurrencyQuotationUnit", "");
				tokens.put("SellRateToBase", "");
				tokens.put("FxRateDate", Date1);
				tokens.put("FxRateTime", "");
				tokens.put("BaseCurrency", "INR");
				tokens.put("OrderId", "");

				// if(STATUS.equalsIgnoreCase("SUCCESS"))
				tokens.put("Status", "SUCCEEDED");
				// else tokens.put("Status", "FAILED");
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

}
