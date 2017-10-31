package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_AVAILBAL;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_ACCOUNT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.AccountAvailBalXpath;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.common.EnigmaBoolean;
import com.test.NumberFormatting;

/**
 * End system communication implementation for Account Balance Enquiry services
 * is handled in this class.
 * 
 * @author Bluescope
 */
public class AccountAvailBalAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(AccountAvailBalAdaptee.class.getName());
	private String branch = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String accountType = "";
	private String bankRequest = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String bankResponse = "";
	private String accountNumber = "";
	private String postingCurrency = "";
	private String eventReference = "N/A";
	private String masterReference = "N/A";
	private String tiRequestPostingAmount = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public AccountAvailBalAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public AccountAvailBalAdaptee() {
	}

	/**
	 * <p>
	 * Process the incoming Account available balance Service XML from the TI
	 * </p>
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@link String}
	 * 
	 */
	public String process(String tirequestXML) {

		logger.info(" ************ Account.AvailBal adaptee process started ************ ");

		String errorMsg = "";
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("\n\nAvailBal TI Request:\n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = getBankRequestFromTiRequest(tirequestXML);
			System.out.println(bankRequest);
			// logger.debug("\n\nAvailBal Bank Request:\n" + bankRequest);

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			System.out.println("bankResponse "+bankResponse);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("\n\nAvailBal Bank Response:\n" + bankResponse);
			logger.debug("AvailBal Bank Response API: >>>>" + bankResponse + "<<<<");

			if (!bankResponse.isEmpty()) {
				logger.debug("BankResponse : " + bankResponse + "<<<");
				tiResponse = getTIResponseFromBankResponse(bankResponse);
			} else {
				logger.debug("Host Unavailable!!!");
				tiResponse = getDefaultErrorResponse("FI Host Unavailable [IM]");
			}
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("\n\nAvailBal TI Response:\n" + tiResponse);

			status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			// string zone = tirequestXML

		} catch (XPathExpressionException e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			e.printStackTrace();

		} catch (SAXException e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			e.printStackTrace();

		} catch (IOException e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			e.printStackTrace();

		} catch (FinacleServiceException e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			e.printStackTrace();

		} catch (Exception e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			e.printStackTrace();

		} finally {
			ServiceLogging.pushLogData(SERVICE_ACCOUNT, OPERATION_AVAILBAL, sourceSystem, branch, sourceSystem,
					targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,
					bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);

			// ServiceLogging.pushLogData(getRequestHeader().getService(),
			// getRequestHeader().getOperation(),
			// getRequestHeader().getSourceSystem(), branch,
			// getRequestHeader().getSourceSystem(),
			// getRequestHeader().getTargetSystem(), masterReference,
			// eventReference, status, tiRequest,
			// tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime,
			// bankResTime, tiResTime, "", "", "",
			// "", false, "0", errorMsg);
		}

		logger.info(" ************ Account.AvailBal adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param requestXML
	 *            {@code allows }{@link String}
	 * @return
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 */
	private String getBankRequestFromTiRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
		targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
		postingCurrency = XPathParsing.getValue(requestXML, AccountAvailBalXpath.PostingCurrency);
		accountNumber = ThemeBridgeUtil.getValue(requestXML, AccountAvailBalXpath.BackOfficeAccount);
		tiRequestPostingAmount = ThemeBridgeUtil.getValue(requestXML, AccountAvailBalXpath.PostingAmount);
		InputStream anInputStream = null;
		try {
			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_AVAILBAL_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = ThemeBridgeUtil.randomCorrelationId();
			logger.debug("Finacle RequestUUID: " + requestId);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("accountNumber", accountNumber);
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("channelId", KotakConstant.CHANNELID);
			tokens.put("bankId", KotakConstant.BANKID);
			tokens.put("serviceReqVersion", KotakConstant.SERVICEREQUESTVERSION);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("AvilBal getBankReq Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				System.out.println("InputStram close exception");
			}
		}
		return result;
	}

	/**
	 * 
	 * @param bankRequest
	 *            {@code allows }{@link String}
	 * @return
	 * @throws FinacleServiceException
	 * @throws IOException
	 * @throws HttpException
	 */
	private String getBankResponseFromBankRequest(String bankRequest)
			throws HttpException, IOException, FinacleServiceException {

		try {
			/******* Finacle http client call *******/
			bankResponse = FinacleHttpClient.postXML(bankRequest,"");

		} catch (Exception e) {
			logger.error("AvailBal FI exceptions! " + e.getMessage());
			bankResponse = "";
			// logger.debug("Bank response : " + bankResponse);
		}
		return bankResponse;
	}

	/**
	 * 
	 * @param bankResponse
	 *            {@code allows object is }{@link String}
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws XPathExpressionException
	 * @throws Exception
	 */
	private String getTIResponseFromBankResponse(String bankResponse)
			throws XPathExpressionException, SAXException, IOException {

		logger.debug("getTIResponseFromBankResponse(bankResp)");
		String result = "";
		if (ValidationsUtil.isValidString(bankResponse)) {
			Map<String, String> tokens = new HashMap<String, String>();
			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_AVAILBAL_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String errorMessages = "";
			String bankRespCurrency = "";
			String bankRespClearBalance = "";

			errorMessages = getBankResponseErrorMessage(bankResponse);
			logger.debug("AvailBal BankResp error : " + errorMessages);

			bankRespClearBalance = XPathParsing.getValue(bankResponse, AccountAvailBalXpath.EfectiveBalanceResp);
			//bankRespCurrency = XPathParsing.getValue(bankResponse, AccountAvailBalXpath.BalanceCurrency);

			BigDecimal reqAmount = new BigDecimal("0");
			BigDecimal respAmount = new BigDecimal("0");
			if (ValidationsUtil.isValidString(tiRequestPostingAmount)) {
				reqAmount = new BigDecimal(tiRequestPostingAmount);
			}
			if (ValidationsUtil.isValidString(bankRespClearBalance)) {
				respAmount = new BigDecimal(bankRespClearBalance);
			}

			logger.debug("Account Number   : " + accountNumber);
			logger.debug("TI Request(D)    : " + postingCurrency + ", Amount : " + reqAmount);
			logger.debug("Bank Response(D) : " + bankRespCurrency + ", Amount : " + respAmount);

			// TODO currency check validations
			// HashMap<String, String> accountDetailMap = accountDetails("INR",
			// "12882199416101");
			HashMap<String, String> accountDetailMap = accountDetails(postingCurrency, accountNumber);
			String internalAccType = "";
			if (accountDetailMap != null) {
				internalAccType = accountDetailMap.get("INTRNAL");
				accountType = accountDetailMap.get("ACC_TYPE");
			}
			//if (accountDetailMap != null && (accountType.equals("CN") || accountType.startsWith("R"))
				//	&& !accountType.equals("R1") && !accountType.equals("R11") && !accountType.equals("RTGS"))
			if (accountDetailMap != null && (accountType.equals("CN")))
			{
				logger.debug("Account Type(CN / R) : " + accountType + ", Suppressed Account Avail Balance enquiry");
				tokens.put("blocked", "N");
				tokens.put("negative", "N");
				tokens.put("errorMessage", "");// always
				tokens.put("balance", "999999999999999");
				tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																			// warning
				tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());

			} else if (accountDetailMap != null && internalAccType.equals("Y")) {
				logger.debug("InternalAcc (Y / N): " + internalAccType + ", Suppressed Account Avail Balance enquiry");
				tokens.put("blocked", "N");
				tokens.put("negative", "N");
				tokens.put("errorMessage", "");// always
				tokens.put("balance", "999999999999999");
				tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																			// warning
				tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());

			} 
			else if (tiRequestPostingAmount.equals("0") || tiRequestPostingAmount.equals("0.0")
					|| tiRequestPostingAmount.equals("0.00") || tiRequestPostingAmount.equals("0.000")) {
				logger.debug("Ms 01 c");
				logger.debug("TI Request amount is(ZERO) " + reqAmount + ", Suppressed Account Avail Balance enquiry");
				tokens.put("blocked", "N");
				tokens.put("negative", "N");
				tokens.put("errorMessage", "");// always
				tokens.put("balance", "000000000000000");
				tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																			// warning
				tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());

			} else {
				if (ValidationsUtil.isValidString(bankRespClearBalance)) {
					int balanceCompare = reqAmount.compareTo(respAmount);
					// logger.debug("balanceCompare : " + balanceCompare);

//					if (!postingCurrency.equals(bankRespCurrency)) {
//						logger.debug("Currency not matched");
//						tokens.put("blocked", "N");
//						tokens.put("negative", "N");
//						tokens.put("balance", "000000000000000");
//						tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());
//						// TODO
//						// tokens.put("errorMessage", "");
//						// tokens.put("errorOrWarning",
//						// EnigmaBoolean.Y.toString());
//						tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
//																					// error
//						tokens.put("errorMessage",
//								"AVAILBAL: Currency not match ( " + accountNumber + ", " + bankRespCurrency + ") [IM]");// always
//
//					} else
					if (balanceCompare == -1 || balanceCompare == 0) {
						/*** Successive balance amount ***/
						tokens.put("blocked", "N");
						tokens.put("negative", "N");
						tokens.put("errorMessage", ""); // always
						tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																					// warning
						tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());
						tokens.put("balance", NumberFormatting.getAmountWithPadding(bankRespClearBalance));

					} else if (balanceCompare == 1) {
						/*** Insufficient balance ***/
						String errMsg = "AVAILBAL: INSUFFICIENT BALANCE ";
						tokens.put("blocked", "N");
						tokens.put("negative", "N");
						tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());
						tokens.put("balance", NumberFormatting.getAmountWithPadding(bankRespClearBalance));
						// TODO Testing
						// tokens.put("errorMessage", "");
						// tokens.put("errorOrWarning",
						// EnigmaBoolean.N.toString());
						// Actual
						tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																					// error
						tokens.put("errorMessage", errMsg + " " + errorMessages + " (" + accountNumber + ", "
								+ respAmount + ", " + bankRespCurrency + ") [IM]");// always
					}

				} else {
					logger.debug("Bank Response error");
					tokens.put("blocked", "N");
					tokens.put("negative", "N");
					tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());
					tokens.put("balance", NumberFormatting.getAmountWithPadding(bankRespClearBalance));
					// TODO Testing
					// tokens.put("errorMessage", "");
					// tokens.put("errorOrWarning", EnigmaBoolean.N.toString());
					tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
																				// error
					tokens.put("errorMessage", "AVAILBAL: " + errorMessages + " (" + accountNumber + ", " + respAmount
							+ "," + bankRespCurrency + ") [IM]");// always
				}

			}
			anInputStream.close();
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();
			// TODO Test
			result = CSVToMapping.RemoveEmptyTagXML(result);
			// logger.debug("WO Removed empty tag : " + result);

		} else {
			try {
				result = generateTiErrorResponse("AVAILBAL: HTTP 404 - Finacle host unavailable");

			} catch (Exception e) {
				logger.error("AVAILBAL exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println(result);
		return result;
	}

	/**
	 * @ @param
	 *       errorMsg {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getDefaultErrorResponse(String errorMsg) {

		// logger.debug("***** Account.AvailBal error response initiated
		// *****");
		logger.debug("getTIResponseFromBankResponse(errorMsg)");
		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			tokens.put("status", ThemeBridgeStatusEnum.SUCCEEDED.toString());
			tokens.put("blocked", "N");
			tokens.put("balance", "000000000000000");
			tokens.put("negative", "N");
			tokens.put("error", "Y");
			tokens.put("errorMessage", errorMsg);
			tokens.put("errorOrWarning", EnigmaBoolean.N.toString());// always
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			String responseTemplate;
			responseTemplate = ThemeBridgeUtil
					.readPropertiesFile(RequestResponseTemplate.ACCOUNT_AVAILBAL_TI_RESPONSE_TEMPLATE);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			response = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("AvilBal default exception! " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("AvailBal TIRsponse : " + response);
		return response;
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
					AccountAvailBalXpath.FIScriptResponseExCode);
			String executeFinacleScriptExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					AccountAvailBalXpath.FIScriptResponseExDesc);

			String executeFinacleBusinessExcepCode = XPathParsing.getValue(bankResponseXml,
					AccountAvailBalXpath.FIBusinessErrCode);
			String executeFinacleBusinessExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					AccountAvailBalXpath.FIBusinessErrMsgDesc);

			String executeFinacleSystemExcepCode = XPathParsing.getValue(bankResponseXml,
					AccountAvailBalXpath.FISystemExCode);
			String executeFinacleSystemExcepErrorDesc = XPathParsing.getValue(bankResponseXml,
					AccountAvailBalXpath.FISystemExErrorMsgDesc);

			allerrorMessages = executeFinacleScriptExcepCode + " " + executeFinacleScriptExcepErrorDesc + " "
					+ executeFinacleBusinessExcepCode + " " + executeFinacleBusinessExcepErrorDesc
					+ executeFinacleSystemExcepCode + " " + executeFinacleSystemExcepErrorDesc;

			logger.debug("AvailBal BankResponse error : " + allerrorMessages);

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
	 * <i> To check the balance </i>
	 * 
	 * @param amount
	 *            {@code allows }{@link String}
	 * @return {@code amount }
	 */
	public static String getAmountWithPadding(String amount) {
		String result = "";
		if (ValidationsUtil.isValidString(amount)) {
			amount = amount.replace(".", "");
			amount = amount.replace("-", "");
			if (amount.length() > 15) {
				result = amount.substring(0, 15);
			} else {
				String format = "%015d";
				result = String.format(format, new BigInteger(amount));
			}
		}
		return result;
	}

	/**
	 * 
	 * @param errorMsg
	 *            {@code allows object is }{@link String}
	 * @return
	 * @throws Exception
	 */
	public String generateTiErrorResponse(String errorMsg) throws Exception {

		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("blocked", "N");
		tokens.put("balance", "000000000000000");
		tokens.put("negative", "N");
		tokens.put("error", "N");// tokens.put("error", "Y");
		tokens.put("errorMessage", errorMsg + " [IM]");

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		String responseTemplate = ThemeBridgeUtil
				.readPropertiesFile(RequestResponseTemplate.ACCOUNT_AVAILBAL_TI_RESPONSE_TEMPLATE);
		Reader fileValue = new StringReader(responseTemplate);
		Reader reader = new TokenReplacingReader(fileValue, resolver);
		response = reader.toString();
		reader.close();
		// logger.info("AvailBal error response : \n" + response);

		return response;
	}

	/**
	 * 
	 * @param currency
	 * @param accountNumber
	 * @return
	 */
//	public static String getAccountType(String currency, String accountNumber) {
//
//		String accType = "";
//		ResultSet aResultset = null;
//		Statement aStatement = null;
//		Connection aConnection = null;
//
//		String accountTypeQuery = "SELECT TRIM(ACC_TYPE) AS ACC_TYPE, CUS_MNM, BRCH_MNM, CURRENCY FROM ACCOUNT WHERE TRIM(CURRENCY) = '"
//				+ currency + "' AND TRIM(BO_ACCTNO) = '" + accountNumber + "'";
//		// logger.debug("AccountTypeQuery : " + accountTypeQuery);
//
//		try {
//			aConnection = DatabaseUtility.getTizoneConnection();
//			aStatement = aConnection.createStatement();
//			aResultset = aStatement.executeQuery(accountTypeQuery);
//			while (aResultset.next()) {
//				accType = aResultset.getString("ACC_TYPE");
//			}
//
//		} catch (Exception e) {
//			logger.error(e.getMessage());
//			e.printStackTrace();
//
//		} finally {
//			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
//		}
//
//		return accType;
//	}

	/**
	 * 
	 * @param currency
	 * @param accountNumber
	 * @return
	 */
	public static HashMap<String, String> accountDetails(String currency, String accountNumber) {

		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		HashMap<String, String> mapList = null;

		String accountDetailsQuery = "SELECT TRIM(BO_ACCTNO) AS BO_ACCTNO, TRIM(ACC_TYPE) AS ACC_TYPE, CUS_MNM, BRCH_MNM, CURRENCY, TRIM(INTRNAL) AS INTRNAL FROM ACCOUNT WHERE CURRENCY = '"
				+ currency + "' AND BO_ACCTNO = '" + accountNumber + "'";
		// logger.debug("AccountDetailsQuery : " + accountDetailsQuery);

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(accountDetailsQuery);
			while (aResultset.next()) {
				mapList = new HashMap<String, String>();
				mapList.put("INTRNAL", aResultset.getString("INTRNAL"));
				mapList.put("ACC_TYPE", aResultset.getString("ACC_TYPE"));
				// internalAcc = aResultset.getString("INTRNAL");
			}

		} catch (Exception e) {
			logger.error("Exceptions getting account details! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}
		// logger.debug("mapList : " + mapList);
		return mapList;
	}

	public static void main(String a[]) throws Exception {
		AccountAvailBalAdaptee as = new AccountAvailBalAdaptee();
		//String requestXML = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\bob documents\\04_TIPlus2.7_API_XMLs\\Account.AvailBal-REQUEST.xml");
		String requestXML =ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\AvailableBalRequest2.xml");
		System.out.println(as.process(requestXML));

	}

}
