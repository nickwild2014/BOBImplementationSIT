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
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.CustomisationFDLienMarkXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * 
 * @since 2016-09-07
 * @version 1.0.2
 * @author KXT51472, Prasath Ravichandran
 */
public class CustomizationFDLienEnquiryAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(CustomizationFDLienEnquiryAdaptee.class.getName());

	private String bankStatus = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public CustomizationFDLienEnquiryAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public CustomizationFDLienEnquiryAdaptee() {
		// TODO Auto-generated constructor stub
	}

	public String process(String requestXML) {

		logger.info("Enter into Customisation FDLienAddAdaptee process method...!");

		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.info("FDLienAdd TI Request--->" + tiRequest);

			tiResponse = getBankResponseFromTIRequest(tiRequest);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.info("FDLienAdd TI Response--->" + tiResponse);

		} catch (Exception e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();

		}

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
	private String getBankResponseFromTIRequest(String requestXML) {

		logger.info("Enter into FDLienAddAdaptee getBankRequest method...!");

		String result = "";
		String expression = "/ServiceRequest/fdlienadd/FDRow";
		StringBuilder tiResponseMessage = new StringBuilder();
		InputStream anInputStream = null;
		try {
			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FD_LIEN_ADD_BANK_REQUEST_TEMPLATE);

			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			int tagCount = XPathParsing.getMultiTagCount(requestXML, expression);
			String requestId = XPathParsing.getValue(requestXML, CustomisationFDLienMarkXpath.correlationIdXPath);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);

			for (int count = 1; count <= tagCount; count++) {

				expression = "/ServiceRequest/fdlienadd/FDRow[" + count + "]/";

				String accountID = XPathParsing.getValue(requestXML, expression + "AccountNumber");
				String amount = XPathParsing.getValue(requestXML, expression + "Amount");
				String remarks = XPathParsing.getValue(requestXML, expression + "Remarks");

				// String amount = "1,000.235 OMR";
				String amountNumbers = amount.replaceAll("[^0-9.]", "");
				// logger.info(amountNumbers);
				String amountCcy = amount.replaceAll("[^A-Z]", "");
				// logger.info(amountCcy);

				tokens.put("Remarks", remarks);
				tokens.put("ModuleType", "ULIEN");
				tokens.put("accountNumber", accountID);
				tokens.put("amountValue", AmountConversion.getAmountValues(amount));
				tokens.put("currencyCode", amountCcy);
				String reasonCode = ConfigurationUtil.getValueFromKey("FDLienReasonCode");
				// tokens.put("ReasonCode", "006");
				tokens.put("ReasonCode", reasonCode);
				// tokens.put("ReasonCode", XPathParsing.getValue(requestXML,
				// expression + "ReasonCode"));
				tokens.put("StartDt",
						XPathParsing.getValue(requestXML, expression + "LienStartDate") + "T00:00:00.000");
				tokens.put("EndDt", XPathParsing.getValue(requestXML, expression + "LienEndDate") + "T00:00:00.000");

				masterReference = XPathParsing.getValue(requestXML, expression + "MasterReference");
				eventReference = XPathParsing.getValue(requestXML, expression + "EventReference");

				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(requestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				result = reader.toString();
				reader.close();

				bankRequest = result;
				bankReqTime = DateTimeUtil.getSqlLocalDateTime();
				logger.info("FDLienAdd Bank Request--->" + bankRequest);

				bankResponse = getBankResponseFromBankRequest(bankRequest);

				bankResTime = DateTimeUtil.getSqlLocalDateTime();
				logger.info("FDLienAdd Bank Response--->" + bankResponse);

				tiResponse = getTIResponseFromBankResponse(accountID, bankResponse);
				tiResponseMessage.append(tiResponse);
				tiResponseMessage.append(",");
			}

		} catch (Exception e) {
			logger.error("FD Account Lien Add Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// NEW LOGGING
			boolean res = ServiceLogging.pushLogData(getRequestHeader().getService(), getRequestHeader().getOperation(),
					getRequestHeader().getSourceSystem(), "", getRequestHeader().getSourceSystem(),
					getRequestHeader().getTargetSystem(), masterReference, eventReference, bankStatus, tiRequest,
					tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "",
					"", false, "0", "");
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		logger.info("Return tiResponseMessage : "
				+ tiResponseMessage.toString().substring(0, tiResponseMessage.length() - 1));

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
		String lienId = "";// Not Applicable
		String remarks = "";
		String status = "";
		try {
			status = XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.StatusXPath);

			if (status.equalsIgnoreCase("FAILURE")) {
				bankStatus = ThemeBridgeStatusEnum.FAILED.toString();
				remarks = XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.ErrorDesc)
						+ XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.ErrorDescXPath);
				result = accountID + "~" + lienId + "~" + "MARK FAILED" + "~" + remarks;
				// TESTING
				// result = accountID + "~" + "KB2057753" + "~" + "SUCCEEDED" +
				// "~" + "Dum Lien Added";

			} else if (status.equalsIgnoreCase("SUCCESS")) {
				lienId = XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.LienIdXPath);
				bankStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				remarks = "FD Lien added";
				result = accountID + "~" + lienId + "~" + "MARK SUCCEEDED" + "~" + remarks;

			}

		} catch (Exception e) {
			logger.error("FDLien Exceptions while TIResponse! " + e.getMessage());
			return result = accountID + "~" + lienId + "~" + "FAILED" + "~" + remarks;
		}

		return result;
	}

	public static void main(String args[]) throws Exception {

		CustomizationFDLienEnquiryAdaptee anAdaptee = new CustomizationFDLienEnquiryAdaptee();
		logger.debug(anAdaptee.process(ThemeBridgeUtil
				.readFile("D:\\_Prasath\\Filezilla\\task\task LienRelease\\LienReleas_TIReq_success.xml")));

	}

}
