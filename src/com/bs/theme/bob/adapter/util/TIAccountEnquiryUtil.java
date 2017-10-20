package com.bs.theme.bob.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.AccountAvailBalXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * End system communication implementation for Account Balance Enquiry services
 * is handled in this class.
 * 
 * @author Bluescope
 */
public class TIAccountEnquiryUtil {

	private final static Logger logger = Logger.getLogger(TIAccountEnquiryUtil.class.getName());
	private String tiRequest = "NA";
	private String tiResponse = "NA";
	private String bankRequest = "";
	private String bankResponse = "";
	private String branch = "";
	private String eventReference = "N/A";
	private String masterReference = "N/A";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

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
	public Map<String, String> accountStatusCheck(String accountNumber) {

		logger.info(" ************ Account.AvailBal adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		tiResponse = "";
		Map<String, String> mapList = null;
		try {
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			tiRequest = accountNumber;

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			String bankRequestXML = getBankRequestFromTiRequest(accountNumber);
			logger.debug("Account.BalEnquiry Bank Response:\n" + bankRequest);
			bankRequest = bankRequestXML;

			String bankResponseXML = getBankResponseFromBankRequest(bankRequestXML);
			logger.debug("Account.BalEnquiry Bank Response:\n" + bankResponseXML);
			bankResponse = bankResponseXML;
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			mapList = getTIResponseFromBankResponse(bankResponseXML, accountNumber);
			tiResponse = mapList.toString();
			logger.debug("Account.BalEnquiry TI Response:\n" + tiResponse);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (XPathExpressionException e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (SAXException e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (IOException e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (FinacleServiceException e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			e.printStackTrace();

		} finally {
			ServiceLogging.pushLogData("Account", "Enquiry", "ZONE1", branch, "ZONE1", "BOB", masterReference,
					eventReference, status, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime,
					bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);
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
		return mapList;
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
	private String getBankRequestFromTiRequest(String accountNumber)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		InputStream anInputStream = null;
		try {
			anInputStream = TIAccountEnquiryUtil.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.ACCOUNT_AVAILBAL_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = ThemeBridgeUtil.randomCorrelationId();
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
			logger.error("Account.BalEnquiry Exceptions! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
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

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			e.getMessage();

		}
		return result;

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
	private Map<String, String> getTIResponseFromBankResponse(String bankResponse, String accountNumber) {

		String errorMsg = null;
		String errorCode = "";
		String bankRespClearBalance = null;
		Map<String, String> accountStatusList = null;
		String sanctionLimit = null;
		String drawingPower = null;
		String lien = null;

		try {
			// errorCode = XPathParsing.getValue(bankResponse,
			// AccountAvailBalXpath.FIBusinessExCodeXpath);
			// errorMsg = XPathParsing.getValue(bankResponse,
			// AccountAvailBalXpath.FIBusinessExMsgDescXpath);
			errorMsg = AccountAvailBalAdaptee.getBankResponseErrorMessage(bankResponse);
			//String currency = XPathParsing.getValue(bankResponse, AccountAvailBalXpath.BalanceCurrency);
			bankRespClearBalance = XPathParsing.getValue(bankResponse, AccountAvailBalXpath.EfectiveBalanceResp);
			sanctionLimit = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/BalInqResponse/BalInqRs/AcctBal[BalType[contains(.,'SANLIM')]]/BalAmt/amountValue");
			drawingPower = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/BalInqResponse/BalInqRs/AcctBal[BalType[contains(.,'DRWPWR')]]/BalAmt/amountValue");
			lien = XPathParsing.getValue(bankResponse,
					"/FIXML/Body/BalInqResponse/BalInqRs/AcctBal[BalType[contains(.,'LIEN')]]/BalAmt/amountValue");
			// String respAmount = getAmountWithPadding(bankRespClearBalance);
			String respAmount = bankRespClearBalance;

			String status = "";
			if (errorMsg == null || errorMsg.trim().isEmpty())
				status = "SUCCEEDED";
			else
				status = "FAILED";

			accountStatusList = new HashMap<String, String>();
			accountStatusList.put("accountNumber", accountNumber);
			accountStatusList.put("errorCode", errorCode);
			accountStatusList.put("errorMsg", errorMsg);
			accountStatusList.put("status", status);
			accountStatusList.put("bankRespEffectiveBalance", respAmount + "\t" + "");
			accountStatusList.put("currency", "");
			accountStatusList.put("sanctionLimit", sanctionLimit);
			accountStatusList.put("drawingPower", drawingPower);
			accountStatusList.put("lien", lien);

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

		logger.debug("accountStatusList : " + accountStatusList);
		return accountStatusList;

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

	public static void main(String a[]) throws Exception {

		TIAccountEnquiryUtil as = new TIAccountEnquiryUtil();
		as.accountStatusCheck("09582560005521");

		// logger.debug("AccountAvailBal : " + responseXML);
	}

}
