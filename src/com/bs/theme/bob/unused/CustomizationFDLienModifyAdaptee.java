package com.bs.theme.bob.unused;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.CustomisationFDLienModifyXpath;
import com.bs.themebridge.xpath.CustomisationFDLienRemovalXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * NOT Implemented
 * 
 * @author KXT51472
 *
 */
public class CustomizationFDLienModifyAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(CustomizationFDLienModifyAdaptee.class.getName());

	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String eventReference = "";
	private String masterReference = "";
	private String bankStatus = "";

	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public CustomizationFDLienModifyAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public CustomizationFDLienModifyAdaptee() {
		// TODO Auto-generated constructor stub
	}

	public String process(String requestXML) {

		logger.info("Enter into KMFDLienModifyAdaptee process method...!");

		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.info("FDLienAdd TI Request--->" + tiRequest);

			// String bankRequest = getBankRequestFromTIRequest(requestXML);
			// logger.info("Bank Request--->" + bankRequest);
			//
			// String bankResponse =
			// getBankResponseFromBankRequest(bankRequest);
			// logger.info("Bank Response--->" + bankResponse);

			tiResponse = getBankResponseFromTIRequest(requestXML);

		} catch (Exception e) {
			logger.error("Exceptions " + e.getMessage());
			e.printStackTrace();

		}
		return tiResponse;
	}

	private String getBankResponseFromTIRequest(String requestXML) {

		logger.info("Enter into KMFDLienModifyAdaptee getBankRequest method...!");

		String result = "";
		String expression = "/ServiceRequest/fdlienmodify/FDRow";
		StringBuilder tiResponseMessage = new StringBuilder();
		logger.info("Milestone 01 ");
		logger.debug(" >> " + requestXML);

		InputStream anInputStream = null;
		try {
			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FD_LIEN_MODIFY_BANK_REQUEST_TEMPLATE);

			logger.info("Milestone 02 ");
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			logger.info("Milestone 03 A");
			int tagCount = XPathParsing.getMultiTagCount(requestXML, expression);
			logger.info("Milestone 03 B");
			String requestId = XPathParsing.getValue(requestXML, CustomisationFDLienRemovalXpath.correlationIdXPath);

			logger.info("Milestone 03 ");

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);

			logger.info("Milestone 04 ");

			for (int count = 1; count <= tagCount; count++) {

				expression = "/ServiceRequest/fdlienmodify/FDRow[" + count + "]/";

				String lienId = XPathParsing.getValue(requestXML, expression + "LienId");
				String amount = XPathParsing.getValue(requestXML, expression + "Amount");
				String remarks = XPathParsing.getValue(requestXML, expression + "Remarks");
				// String reasonCode = XPathParsing.getValue(requestXML,
				// expression + "ReasonCode");
				String accountID = XPathParsing.getValue(requestXML, expression + "AccountNumber");

				// String amount = "1,000.235 OMR";
				String amountNumbers = amount.replaceAll("[^0-9.]", "");
				// logger.info(amountNumbers);
				String amountCcy = amount.replaceAll("[^A-Z]", "");
				// logger.info(amountCcy);

				tokens.put("LienId", lienId);
				tokens.put("Remarks", "remarks");// Hard code
				tokens.put("ModuleType", "ULIEN");
				String reasonCode = ConfigurationUtil.getValueFromKey("FDLienReasonCode");
				tokens.put("ReasonCode", reasonCode);
				// tokens.put("ReasonCode", "006");// DEFLT// Hard code
				tokens.put("accountNumber", accountID);
				tokens.put("amountValue", AmountConversion.getAmountValues(amount));
				tokens.put("currencyCode", amountCcy);
				logger.debug("Start Date & End Date"); // 2017-01-17
				String startDate = XPathParsing.getValue(requestXML, expression + "LienStartDate");
				logger.debug("startDate : " + startDate);
				if (ValidationsUtil.isValidString(startDate))
					tokens.put("StartDt", startDate + "T00:00:00.000");
				String endDate = XPathParsing.getValue(requestXML, expression + "LienEndDate");
				logger.debug("endDate : " + endDate);
				// Neeraj advice
				tokens.put("EndDt", "2099-12-31" + "T00:00:00.000");

				masterReference = XPathParsing.getValue(requestXML, expression + "MasterReference");
				eventReference = XPathParsing.getValue(requestXML, expression + "EventReference");

				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(requestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				result = reader.toString();
				reader.close();

				bankRequest = result;
				bankReqTime = DateTimeUtil.getSqlLocalDateTime();
				logger.info("FDLienModify Bank Request--->" + bankRequest);

				bankResponse = getBankResponseFromBankRequest(bankRequest);

				bankResTime = DateTimeUtil.getSqlLocalDateTime();
				logger.info("FDLienModify Bank Response--->" + bankResponse);

				tiResponse = getTIResponseFromBankResponse(accountID, bankResponse);
				tiResponseMessage.append(tiResponse);
				tiResponseMessage.append(",");
				// logger.debug("tiResponseMessage : " +
				// tiResponseMessage);

				// ServiceLogging.pushLogData(getRequestHeader().getService(),
				// getRequestHeader().getOperation(),
				// getRequestHeader().getSourceSystem(), "",
				// getRequestHeader().getSourceSystem(),
				// getRequestHeader().getTargetSystem(), tiRequest, tiResponse,
				// bankRequest, bankResponse,
				// bankStatus, masterReference, eventReference, false, "0", "",
				// tiReqTime, bankReqTime,
				// bankResTime, tiResTime);

				// NEW LOGGING
				boolean res = ServiceLogging.pushLogData(getRequestHeader().getService(),
						getRequestHeader().getOperation(), getRequestHeader().getSourceSystem(), "",
						getRequestHeader().getSourceSystem(), getRequestHeader().getTargetSystem(), masterReference,
						eventReference, bankStatus, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime,
						bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", "");

			}

		} catch (Exception e) {
			logger.error("FD Account Lien FDLienModify Exceptions! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		logger.info("Return tiResponseMessage : "
				+ tiResponseMessage.toString().substring(0, tiResponseMessage.length() - 1));
		// logger.debug("Return tiResponseMessage w/o trim: " +
		// tiResponseMessage.toString());
		// return tiResponseMessage.toString().substring(0, expression.length()
		// - 1);
		return tiResponseMessage.toString().substring(0, tiResponseMessage.length() - 1);
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
	private String getBankResponseFromBankRequest(String bankRequest) {

		String bankResponse = "";
		try {
			bankResponse = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			e.getMessage();
			return null;
		}
		return bankResponse;
	}

	/**
	 * 
	 * @param bankResponse
	 *            {@code allows }{@link String}
	 * @return
	 */
	public String getTIResponseFromBankResponse(String accountID, String bankResponseXML) {

		String result = "";
		String lienId = "Not Appicable";
		String remarks = "";
		String status = "";
		try {
			status = XPathParsing.getValue(bankResponseXML, CustomisationFDLienRemovalXpath.LienStatusXPath);
			logger.debug("Status : " + status);

			if (status.equalsIgnoreCase("FAILURE")) {
				logger.debug("Lien FAILURE");
				bankStatus = ThemeBridgeStatusEnum.FAILED.toString();
				remarks = XPathParsing.getValue(bankResponseXML,
						CustomisationFDLienRemovalXpath.ErrorDescBusinessExXPath);
				result = accountID + "~" + lienId + "~" + "REVERSAL FAILED" + "~" + remarks;

			} else if (status.equalsIgnoreCase("SUCCESS")) {
				logger.debug("Lien SUCCESS");
				lienId = XPathParsing.getValue(bankResponseXML,
						CustomisationFDLienRemovalXpath.ErrorCodeBusinessExXPath);
				bankStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				remarks = "FD Lien removed";
				result = accountID + "~" + lienId + "~" + "REVERSAL SUCCEEDED" + "~" + remarks;

			}

		} catch (Exception e) {
			logger.error("FDLien Exceptions while TIResponse! " + e.getMessage());
			return result = accountID + "~" + lienId + "~" + "FAILED" + "~" + remarks;
		}

		return result;
	}

	public static void main(String args[]) {
		CustomizationFDLienModifyAdaptee anAdaptee = new CustomizationFDLienModifyAdaptee();
		try {
			// logger.debug(anAdaptee.process(ThemeBridgeUtil
			// .readFile("D:\\_Prasath\\Filezilla\\task\\task
			// LienRelease\\LienReleas_TIReq_success.xml")));

			logger.debug(anAdaptee.getTIResponseFromBankResponse("1234",
					ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\newReverse.xml")));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
