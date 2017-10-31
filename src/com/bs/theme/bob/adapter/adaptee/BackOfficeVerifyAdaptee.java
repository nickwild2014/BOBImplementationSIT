package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_VERIFY;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_BACKOFFICE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.util.FXDealUtilization;
import com.bs.theme.bob.adapter.util.StepStatusUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.theme.bob.template.util.StepNameConstants;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.JAXBTransformUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ThemeConstant;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.BackOfficeVerifyXpath;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.ServiceResponse;
import com.misys.tiplus2.services.control.ServiceResponse.ResponseHeader;
import com.misys.tiplus2.services.control.ServiceResponse.ResponseHeader.Details;
import com.misys.tiplus2.services.control.StatusEnum;

/**
 * End system communication implementation for BackOffice verify services is
 * handled in this class.
 * 
 * @author Bluescope
 * 
 */
public class BackOfficeVerifyAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(BackOfficeVerifyAdaptee.class.getName());

	private String stepID = "";
	private String branch = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private String serviceStatus = "SUCCEEDED";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public BackOfficeVerifyAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public BackOfficeVerifyAdaptee() {
	}

	public static void main(String[] args) {
		BackOfficeVerifyAdaptee bv = new BackOfficeVerifyAdaptee();
//		String tiReq;
//		try {
//			tiReq = ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\BackOfficeVerify\\PostinfExposureFXdeal.xml");
//			bv.process(tiReq);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		System.out.println(bv.getErrorOrWarning());

	}

	@Override
	public String process(String tiRequestXML) throws Exception {
		logger.info(" ************ Backoffice.Verify adaptee process started ************ ");
		String tiResponseXML = "";
		String errorDescription = "";
		try {
			tiRequest = tiRequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			sourceSystem = XPathParsing.getValue(tiRequestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(tiRequestXML, RequestHeaderXpath.TARGETSYSTEM);
			tiResponseXML = processTiRequest(tiRequestXML);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("BackOffice.Verify TI Response :\n" + tiResponseXML);
			tiResponse = tiResponseXML;
		} catch (Exception e) {
			errorDescription = e.getMessage();
			logger.error("BO Verify exceptions..! " + errorDescription);
			tiResponse = getErrorResponse(errorDescription);
		} finally {
			if (!bankRequest.isEmpty() && !bankResponse.isEmpty())
				ServiceLogging.pushLogData(SERVICE_BACKOFFICE, OPERATION_VERIFY, sourceSystem, branch, sourceSystem,
						targetSystem, masterReference, eventReference, serviceStatus, tiRequestXML, tiResponseXML,
						bankRequest, bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", stepID, "",
						false, "0", errorDescription);
		}
		logger.info(" ************ Backoffice.Verify adaptee process finished ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param tiRequestXML
	 * @return
	 */
	private String processTiRequest(String tiRequestXML) {

		String eventRef = "";
		String masterRef = "";
		String tiresponseXML = "";
		String headerStatus = "SUCCEEDED";// always
		String postingErrWarnMsg = "";
		String postingStatus = "SUCCEEDED";// initial
		String exposureErrWarnMsg = "";
		String exposureStatus = "SUCCEEDED";// initial
		String fxdealErrWarnMsg = "";
		String fxdealStatus = "SUCCEEDED";// initial
		try {
			int postingLegsCount = XPathParsing.getMultiTagCount(tiRequestXML,
					"/ServiceRequest/VerifyRequest/ServiceRequest/Posting");
			logger.debug("PostingLegsCount : " + postingLegsCount);
			int exposureLegsCount = XPathParsing.getMultiTagCount(tiRequestXML,
					"/ServiceRequest/VerifyRequest/ServiceRequest/Exposure");
			logger.debug("ExposureLegsCount >>: " + exposureLegsCount);
			int fxDealLegsCount = XPathParsing.getMultiTagCount(tiRequestXML,
					"/ServiceRequest/VerifyRequest/ServiceRequest/FXDeal");
			logger.debug("FXDealsLegsCount >>: " + fxDealLegsCount);

			logger.debug("Milestone 01 BackOffice.Verify");

			if (postingLegsCount > 0) {
				List<HashMap<String, String>> postingLegsMapList = getPostingLegsMapList(tiRequestXML);
				if (postingLegsMapList != null && postingLegsMapList.size() > 0) {
					postingErrWarnMsg = getPostingMessages(postingLegsMapList);
					if (!postingErrWarnMsg.isEmpty()) {
						postingStatus = "FAILED";
					}
					logger.debug("Verify(Posting) Status & ErrorWarn : " + postingStatus + postingErrWarnMsg);
				}
			} else if (exposureLegsCount > 0) {
				logger.debug("Nothing to do for exposure");
				exposureStatus = "SUCCEEDED"; // always
				exposureErrWarnMsg = ""; // always
				masterRef = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[1]/Exposure/MasterReference");
				masterReference = masterRef;
				eventRef = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[1]/Exposure/EventReference");
				eventReference = eventRef;
				logger.debug("Verify(Exposure) Reference : " + masterRef + eventRef);
				logger.debug("Verify(Exposure) Status & ErrorWarn : " + exposureStatus + exposureErrWarnMsg);

			} else if (fxDealLegsCount > 0) {
				logger.debug("Check for fxdeals!");
				List<Map<String, String>> reponseList = verifyFxDeal(masterReference, eventReference);
				logger.debug("Verify(FXDeal) ReponseListSize : " + reponseList.size());
				for (Map<String, String> reponse : reponseList) {
					if (reponse.get("STATUS").equalsIgnoreCase("FAILED")) {
						fxdealStatus = reponse.get("STATUS");
						fxdealErrWarnMsg = reponse.get("REPONSEMSG");
					}
				}
				logger.debug("Verify(FXDeal) Status & ErrorWarn : " + fxdealStatus + fxdealErrWarnMsg);
			}
			logger.debug("Milestone 02 BackOffice.Verify");

			String headerErrorMsg = "";
			String headerWarningMsg = "";
			if (postingStatus.equalsIgnoreCase("FAILED") || exposureStatus.equalsIgnoreCase("FAILED")
					|| fxdealStatus.equalsIgnoreCase("FAILED")) {
				headerStatus = "FAILED";
				logger.debug("Milestone 03 BackOffice.Verify");
				String errorOrWarning = getErrorOrWarning();

				String isResponseHasError = "";
	
				if (errorOrWarning.contains("E")) {
					headerErrorMsg = "Verify: " + postingErrWarnMsg + exposureErrWarnMsg + fxdealErrWarnMsg + " [IM]";
				} else {
					headerWarningMsg = "Verify: " + postingErrWarnMsg + exposureErrWarnMsg + fxdealErrWarnMsg + " [IM]";
				}
			}
			logger.debug("Header Status & Error >>" + headerStatus + " : " + headerErrorMsg + headerWarningMsg + "<<");
			logger.debug("Milestone 04 BackOffice.Verify");

			tiresponseXML = getTIResponseFromBankResponseXML(masterReference, eventReference, headerStatus,
					headerErrorMsg, headerWarningMsg, postingStatus, postingErrWarnMsg, exposureStatus,
					exposureErrWarnMsg, fxdealStatus, fxdealErrWarnMsg);

			logger.debug("Milestone 05 BackOffice.Verify");

		} catch (JAXBException e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();
			tiresponseXML = getErrorResponse(e.getMessage());

		} catch (Exception e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();
			tiresponseXML = getErrorResponse(e.getMessage());

		} finally {

		}
		return tiresponseXML;
	}

	private String getErrorOrWarning() {

		String errorOrWarning = "";
		ResultSet rs = null;
		Connection connection = null;
		PreparedStatement pst = null;
		try {
			connection = DatabaseUtility.getThemebridgeConnection();
			pst = connection.prepareStatement("select trim(value) value from BRIDGEPROPERTIES where key ='BackOffice.Verify.WarnError'");
			rs = pst.executeQuery();
			if (rs.next()) {
				errorOrWarning = rs.getString("value");
			}
		} catch (SQLException e) {
			logger.error("SQLException! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(connection, pst, rs);
		}

		return errorOrWarning;
	}

	/**
	 * New Posting Leg
	 * 
	 * @param tiRequestXML
	 * @return
	 */
	private List<HashMap<String, String>> getPostingLegsMapList(String tiRequestXML) {

		List<HashMap<String, String>> postingLegsMapList = new ArrayList<HashMap<String, String>>();

		try {
			int postingLegsCount = XPathParsing.getMultiTagCount(tiRequestXML,
					"/ServiceRequest/VerifyRequest/ServiceRequest/Posting");
			// logger.debug("PostingLegsCount : " + postingLegsCount);

			for (int postingIterator = 1; postingIterator <= postingLegsCount; postingIterator++) {
				HashMap<String, String> postingMap = new HashMap<String, String>();
				// logger.debug("\nTEST" + postingIterator);

				if (branch.isEmpty()) {
					branch = XPathParsing.getValue(tiRequestXML, "/ServiceRequest/VerifyRequest/ServiceRequest["
							+ postingIterator + "]/Posting/PostingBranch");
					logger.debug("behalfOfBranch : " + branch);
				}

				String transactionId = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/TransactionId");
				postingMap.put("transactionId", transactionId);
				// logger.debug("TransactionId : " + transactionId);

				String masterRef = XPathParsing.getValue(tiRequestXML, "/ServiceRequest/VerifyRequest/ServiceRequest["
						+ postingIterator + "]/Posting/MasterReference");
				postingMap.put("masterReference", masterRef);
				masterReference = masterRef;
				// logger.debug("MasterReference : " + masterReference);

				String eventRef = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/EventReference");
				postingMap.put("eventReference", eventRef);
				eventReference = eventRef;
				// logger.debug("EventReference : " + eventReference);

				String accountType = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/AccountType");
				postingMap.put("accountType", accountType);
				// logger.debug("AccountType : " + accountType);

				String debitCreditFlag = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator
								+ "]/Posting/DebitCreditFlag");
				postingMap.put("debitCreditFlag", debitCreditFlag);
				// logger.debug("DebitCreditFlag : " + debitCreditFlag);

				String postingCcy = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/PostingCcy");
				postingMap.put("postingCcy", postingCcy);
				// logger.debug("PostingCcy : " + postingCcy);

				String postingBranch = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/PostingBranch");
				postingMap.put("postingBranch", postingBranch);
				// logger.debug("PostingBranch : " + postingBranch);

				String postingAmount = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/PostingAmount");
				postingMap.put("postingAmount", postingAmount);
				// logger.debug("PostingAmount : " + postingAmount);

				String backOfficeAccountNo = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator
								+ "]/Posting/BackOfficeAccountNo");
				postingMap.put("backOfficeAccountNo", backOfficeAccountNo);
				// logger.debug("BackOfficeAccountNo : " + backOfficeAccountNo);

				String productReference = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator
								+ "]/Posting/ProductReference");
				postingMap.put("productReference", productReference);
				// logger.debug("ProductReference : " + productReference);

				String valueDate = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator + "]/Posting/ValueDate");
				postingMap.put("valueDate", valueDate);
				// logger.debug("valueDate : " + valueDate);

				String forceDebitFlag = XPathParsing.getValue(tiRequestXML,
						"/ServiceRequest/VerifyRequest/ServiceRequest[" + postingIterator
								+ "]/Posting/ExtraData/FORCDBT");
				if (forceDebitFlag.equals("Y")) // 2017-06-16
					postingMap.put("forceDebitFlag", "F");
				else
					postingMap.put("forceDebitFlag", "N");
				// logger.debug("ProductReference : " + forceDebitFlag);

				postingLegsMapList.add(postingMap);
				// logger.debug(postingMap);

			}
			// logger.debug("\n\n\nBackOffice.Verify TI tiResTime: " + tiReqTime
			// + "\n\n\n");

		} catch (Exception e) {
			logger.error("BackOffice Verify Exceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// logger.debug("Finally");
		}
		// logger.debug("\n\n\n\n>>>>>>>>>>>>>>" + postingLegsMapList +
		// "\n\n\n\n");
		return postingLegsMapList;
	}

	/**
	 * 
	 * @param postingLegsMapList
	 * @return
	 * @throws Exception
	 */
	public String getPostingMessages(List<HashMap<String, String>> postingLegsMapList) throws Exception {

		String bankReq = "";
		String errorMsg = "";
		try {
			String partTrnRec = getPartTrnRecDetails(postingLegsMapList);
			bankReq = generateVerifyBankRequest(partTrnRec);
			// bankReq = generateVerifyBankRequest(partTrnRec,
			// postingLegsMapList);
			logger.debug("BackOffice.Verify BankRequest : " + bankReq);
			bankRequest = bankReq;

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("BackOffice.Verify BankReqTime : " + bankReqTime);
			String bankResp = getBankResponseFromBankRequest(bankReq);                      ///////////////////////
			// String bankResp = ThemeBridgeUtil
			// .readFile("D:\\_Prasath\\00_TASK\\BackOfficeVerify\\BankRespPostingFailure.xml");

			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("BackOffice.Verify BankResTime: " + bankResTime);
			logger.debug("BackOffice.Verify BankResponse: " + bankResp);
			bankResponse = bankResp;

			if (!bankResp.isEmpty()) {
				errorMsg = getBankResponseErrorMessage(bankResp);
			} else {
				errorMsg = "Finacle host unavailable!";
			}

		} catch (Exception e) {
			logger.error("Backoffice Verify Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}
		return errorMsg;
	}

	/**
	 * 
	 * @param anPostingsList
	 * @return
	 * @throws Exception
	 */
	public String getPartTrnRecDetails(List<HashMap<String, String>> postingLegsMapList) throws Exception {

		int reccount = 1;
		String partTrnRecRequest = null;
		StringBuilder partTrnRec = new StringBuilder();

		try {
			correlationId = postingLegsMapList.get(0).get("transactionId");
			// logger.debug("correlationId : " + correlationId );

			for (int i = 0; i < postingLegsMapList.size(); i++) {
				if (i == 0) {
					// partTrnRec.append("\n<VALUE_DATE>" +
					// postingLegsMapList.get(i).get("valueDate") + "T"
					// + DateTimeUtil.getStringLocalTimeFi() + "</VALUE_DATE>");

					String valueDate = postingLegsMapList.get(i).get("valueDate").trim();
					SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
					String changedValueDate = sdf2.format(sdf1.parse(valueDate));

					//partTrnRec.append("\n<VALUE_DATE>" + changedValueDate + "</VALUE_DATE>");

//					partTrnRec.append(
//							"\n<FORCE_DEBIT>" + postingLegsMapList.get(i).get("forceDebitFlag") + "</FORCE_DEBIT>");
				}
				String accountNumber = "";
				String debitCredit = postingLegsMapList.get(i).get("debitCreditFlag");
				String accountType = postingLegsMapList.get(i).get("accountType");
				String postingCcy = postingLegsMapList.get(i).get("postingCcy");
				String postingBranch = postingLegsMapList.get(i).get("postingBranch");
				String productCode = postingLegsMapList.get(i).get("productReference");
				String postingAmount = postingLegsMapList.get(i).get("postingAmount");
				String boaccountNumber = postingLegsMapList.get(i).get("backOfficeAccountNo");
				// 2017-04-11
				postingAmount = AmountConversion.getTransactionAmount(postingAmount, postingCcy);

				partTrnRec.append("\n<partTran ismultirec=\"Y\">");
				/** UAT **/
				// if ((accountType.startsWith("R") ||
				// accountType.startsWith("L")) && !accountType.equals("R1")
				// && !accountType.equals("R11") &&
				// !accountType.startsWith("RTGS")
				// && !accountType.startsWith("RAA")) {
				// String glNumber =
				// BackofficeBatchUtil.getGLAccount(accountType, debitCredit,
				// productCode);
				// accountNumber = postingBranch +
				// BackofficeBatchUtil.getCcyCode(postingCcy) + glNumber;
				// partTrnRec.append("<AcctId>" + accountNumber + "</AcctId>");
				//
				// } else {
				// partTrnRec.append("\n\t<AcctId>" + boaccountNumber +
				// "</AcctId>");
				// }
				
				
				/** SIT **/
				partTrnRec.append("\n\t<acctNo>" + boaccountNumber + "</acctNo>");
				partTrnRec.append("\n\t<drCrInd>" + debitCredit + "</drCrInd>");
				partTrnRec.append("\n\t\t<tranAmt>" + postingAmount + "</tranAmt>");
				partTrnRec.append("<tranParti>" + "" + "</tranParti>");
				//partTrnRec.append("<tranRmks>" + postingLeg.get("lobcode") + "</tranRmks>"); // lob
				partTrnRec.append("<tranRmks>" + "" + "</tranRmks>"); // lob
				partTrnRec.append("\n</partTran>");
				reccount++;
			}
		} catch (Exception e) {
			logger.error("Exceptions " + e.getMessage());
			e.printStackTrace();

		} finally {

		}
		partTrnRecRequest = partTrnRec.toString();
		// logger.debug(partTrnRec);

		return partTrnRecRequest;
	}

	private String getBankResponseErrorMessage(String bankResponseXML) {

		String postingErrorMessage = "";
		try {
			String hostStatus = XPathParsing.getValue(bankResponseXML, BackOfficeVerifyXpath.HostStataus);
			// logger.debug("hostStatus : " + hostStatus);
			String serviceStataus = XPathParsing.getValue(bankResponseXML, BackOfficeVerifyXpath.ServiceStataus);
			logger.debug("serviceStataus : " + serviceStataus);
			// bankStatus = hostStatus;

			if (serviceStataus.equalsIgnoreCase("FAILURE") || serviceStataus.isEmpty() || serviceStataus == null) {
				String errorCode = XPathParsing.getValue(bankResponseXML, BackOfficeVerifyXpath.FIScriptResponseExCode);
				String errorMessage = XPathParsing.getValue(bankResponseXML,
						BackOfficeVerifyXpath.FIScriptResponseExMsgDesc);
				String FISystemExCode = XPathParsing.getValue(bankResponseXML, BackOfficeVerifyXpath.FISystemExCode);
				String FISystemExErrorMsgDesc = XPathParsing.getValue(bankResponseXML,
						BackOfficeVerifyXpath.FISystemExErrorMsgDesc);
				String FIBusinessExCode = XPathParsing.getValue(bankResponseXML,
						BackOfficeVerifyXpath.FIBusinessExCode);
				String FIBusinessExMsgDesc = XPathParsing.getValue(bankResponseXML,
						BackOfficeVerifyXpath.FIBusinessExMsgDesc);

				postingErrorMessage = errorMessage + FISystemExErrorMsgDesc + FIBusinessExMsgDesc;
			} else {
				String successMessage = XPathParsing.getValue(bankResponseXML,
						BackOfficeVerifyXpath.SuccessMessageDesc);
				logger.debug("serviceStataus : " + serviceStataus + " " + successMessage);
			}

		} catch (Exception e) {
			logger.debug("BackOffice Verify Exceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {

		}
		// logger.debug(resultMsg);
		return postingErrorMessage;
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

		// logger.debug("TEST BANKRESPONSE");
		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.error("BackOffice.Verify FI exceptions! " + e.getMessage());
			e.printStackTrace();
			result = "";
		}

		// logger.debug("TEST BANKRESPONSE" + result);
		return result;
	}

	/**
	 * 
	 * 
	 * @param getPartTrnRecDetails
	 * @return
	 */
	private String generateVerifyBankRequest(String getPartTrnRecDetails) {

		String request = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			InputStream anInputStream = BackOfficeVerifyAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_VERIFY_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			tokens.put("requestId", correlationId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("dateTime", dateTime);

			// tokens.put("VALUE_DATE", postingLeg.get("valueDate") + "T" +
			// DateTimeUtil.getStringLocalTimeFi());
			// tokens.put("FORCE_DEBIT", "");
			tokens.put("PartTrnRec", getPartTrnRecDetails);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			request = reader.toString();
			reader.close();
			anInputStream.close();

		} catch (Exception e) {
			logger.error("BackOffice.Verify exception! " + e.getMessage());
			e.printStackTrace();
		}

		return request;
	}

	/**
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @param headerStatus
	 * @param headerErrorMsg
	 * @param headerWarningMsg
	 * @param exposureStatus
	 * @param exposureErrorMsg
	 * @param postingStatus
	 * @param postingErrorMessage
	 * @param dealStatus
	 * @param dealErrorMsg
	 * @return
	 */
	private String getTIResponseFromBankResponseXML(String masterRef, String eventRef, String headerStatus,
			String headerErrorMsg, String headerWarningMsg, String postingStatus, String postingErrorMessage,
			String exposureStatus, String exposureErrorMsg, String dealStatus, String dealErrorMsg) {

		String tiresponseXML = "";
		String tokenReplacedXML = "";
		try {
			InputStream anInputStream = BackOfficeVerifyAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_VERIFY_TI_RESPONSE_TEMPLATE);
			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", "SUCCEEDED"); // always
			/** step id **/
			String stepType = "";
			String stepName = "";
			HashMap<String, String> StepNameStatusMap = StepStatusUtil.getStepNameAndStatus(masterRef, eventRef);
			stepType = StepNameStatusMap.get("stepType");
			stepName = StepNameStatusMap.get("stepName");
			if (stepType == null)
				stepType = "";
			if (stepName == null)
				stepName = "";
			stepID = stepName;
			logger.debug("StepId: " + stepType + " / StepName: " + stepName);

			/**
			 * <p>
			 * If CSM step should be warning message
			 * </p>
			 * <p>
			 * If CBS step should be error message
			 * </p>
			 **/
			if ((!headerErrorMsg.isEmpty() || !headerWarningMsg.isEmpty())) {

//				if ((!stepName.isEmpty() && !stepType.equals("a1"))
//						&& (!stepName.isEmpty() && !stepName.equalsIgnoreCase(StepNameConstants.CSM_STEP)))
				if(headerErrorMsg!=null && !headerErrorMsg.isEmpty())
				{
					logger.debug("Verify:ErrorAndWarning");
					tokens.put("HeaderError", headerErrorMsg + headerWarningMsg);
					tokens.put("HeaderWarning", "");
					tokens.put("PostingError", postingErrorMessage);
					tokens.put("ExposureError", exposureStatus);
					tokens.put("FXError", dealErrorMsg);
					tokens.put("PostingWarning", "");
					tokens.put("ExposureWarning", "");
					tokens.put("FXWarning", "");
				} else {
					logger.debug("Verify:WarningOnly");
					tokens.put("HeaderWarning", headerErrorMsg + headerWarningMsg);// 2017-10-05
					tokens.put("HeaderError", "");
					tokens.put("PostingWarning", postingErrorMessage);
					tokens.put("ExposureWarning", exposureStatus);
					tokens.put("FXWarning", dealErrorMsg);
					tokens.put("PostingError", "");
					tokens.put("ExposureError", "");
					tokens.put("FXError", "");
				}
			} else {
				logger.debug("Verify:Empty2");
				tokens.put("HeaderError", "");
				tokens.put("HeaderWarning", "");
				tokens.put("PostingWarning", "");
				tokens.put("ExposureWarning", "");
				tokens.put("FXWarning", "");
				tokens.put("PostingError", "");
				tokens.put("ExposureError", "");
				tokens.put("FXError", "");
			}
			tokens.put("CorrelationId", correlationId);
			tokens.put("HeaderInfo", "");
			tokens.put("ExposureInfo", "");
			tokens.put("PostingInfo", "");
			tokens.put("FXInfo", "");
			tokens.put("PostingStatus", postingStatus);
			tokens.put("ExposureStatus", exposureStatus);
			tokens.put("FXStatus", dealStatus);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();
			anInputStream.close();

			tiresponseXML = tokenReplacedXML;
			tiresponseXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" +
			// tiresponseXML);

		} catch (Exception e) {
			logger.error("BackOfficeVerify TIResponse Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return tiresponseXML;
	}

	/************************************************** **************************************************/
	/************************************************** **************************************************/

	private List<Map<String, String>> verifyFxDeal(String masterReference, String eventRef) {

		// NotificationsEventStepAdaptee eventStepAdaptee = new
		// NotificationsEventStepAdaptee();
		List<Map<String, String>> returnmaplist = FXDealUtilization.fxdealprocess(masterReference, eventRef);
		// logger.info("Not " + returnmaplist.size());

		List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();
		List<String> list_contrctref = new ArrayList<String>();
		String contrct_ref = "", dealcustomer = "", ti_sysdate = "", customer = "", buysell = "", saleamount = "",
				purchaseamount = "";
		for (Map<String, String> map : returnmaplist) {
			contrct_ref = map.get("DEALREF");
			ti_sysdate = map.get("TI_SYSDATE");
			customer = map.get("CUSTOMER");
			buysell = map.get("BUYSELL");
			// saleamount = convertAmountFromTIPlus(map.get("SALEAMOUNT"),
			// map.get("SALECURRENCY"));
			// purchaseamount =
			// convertAmountFromTIPlus(map.get("PURCHASEAMOUNT"),
			// map.get("PURCHASECURRENCY"));
			list_contrctref.add(contrct_ref);
		}

		List<Map<String, String>> reponseList = checkingmasterdealAuth(returnmaplist, customer, masterReference,
				eventRef);
		// logger.info("*********** verify deal auth or not ********* stage - 1-
		// end");
		return reponseList;

	}

	private List<Map<String, String>> checkingmasterdealAuth(List<Map<String, String>> mastercontractlist,
			String customer, String masterReference, String eventRef) {

		List<Map<String, String>> responseList = new ArrayList<Map<String, String>>();
		// logger.info("----- Master each Deal check is authorized or not
		// --Stage - 3 + " + mastercontractlist.size());

		for (Map<String, String> dealDetails : mastercontractlist) {

			Map<String, String> response = new HashMap<String, String>();
			String puramount = "", saleamount = "", buy_sell = "";
			// saleamount =
			// convertAmountFromTIPlus(dealDetails.get("SALEAMOUNT"),
			// dealDetails.get("SALECURRENCY"));
			// puramount =
			// convertAmountFromTIPlus(dealDetails.get("PURCHASEAMOUNT"),
			// dealDetails.get("PURCHASECURRENCY"));

			puramount = dealDetails.get("PURCHASEAMOUNT");
			saleamount = dealDetails.get("SALEAMOUNT");
			buy_sell = dealDetails.get("BUYSELL");

			// contrct_ref = map.get("DEALREF");
			// ti_sysdate = map.get("TI_SYSDATE");
			// customer = map.get("CUSTOMER");
			// buysell = map.get("BUYSELL");
			// saleamount = map.get("SALEAMOUNT");
			// purchaseamount = map.get("PURCHASEAMOUNT");
			// list_contrctref.add(contrct_ref);

			String totalutlizedamt = getFXAutorizedDealDetails(dealDetails.get("CUSTOMER"), dealDetails.get("DEALREF"));
			logger.debug("totalutlizedamt : " + totalutlizedamt);
			String status = "", reponsemsg = "";
			int recordcount = 0;

			// if (ValidationsUtil.isValidString(totalutlizedamt)) {
			// status = "SUCCEEDED";
			// }

			// logger.info(totalutlizedamt);
			String dealid = dealDetails.get("DEALREF");
			if (ValidationsUtil.isValidString(totalutlizedamt)) {

				BigDecimal tanxdealUtamtbg = new BigDecimal("0");
				logger.debug("BigDecimal tanxdealUtamtbg " + tanxdealUtamtbg);

				BigDecimal bd = new BigDecimal(totalutlizedamt);
				logger.debug("BigDecimal bd " + bd);

				tanxdealUtamtbg = tanxdealUtamtbg.add(bd);
				logger.debug("BigDecimal tanxdealUtamtbg 1 " + tanxdealUtamtbg);

				/*
				 * for (Map<String, String> contctMap : returnmaplist) {
				 * BigDecimal bd = new BigDecimal(contctMap.get("REF_AMT"));
				 * logger.info( "BigDecimal 1-> " + bg.toString()); logger.info(
				 * "BigDecimal 2-> " + bd.toString()); bg = bg.add(bd); }
				 */
				// logger.info("Total Authorized Amount -> " +
				// tanxdealUtamtbg.toString());
				BigDecimal trxnUt = new BigDecimal("0");
				logger.debug("buy_sell : " + buy_sell);
				logger.debug("puramount : " + puramount);
				logger.debug("saleamount : " + saleamount);
				if (ValidationsUtil.isValidString(buy_sell) && buy_sell.equalsIgnoreCase("P")) {
					// tanxdealUtamtbg = tanxdealUtamtbg.add(new
					// BigDecimal(puramount));
					trxnUt = trxnUt.add(new BigDecimal(puramount));

				} else if (ValidationsUtil.isValidString(buy_sell) && buy_sell.equalsIgnoreCase("S")) {
					// tanxdealUtamtbg = tanxdealUtamtbg.add(new
					// BigDecimal(saleamount));
					trxnUt = trxnUt.add(new BigDecimal(puramount));
				}
				logger.info("Total Amount Authorised and Transaction DealAmount-> 2 " + tanxdealUtamtbg.toString());

				String retAmount = "";
				List<Map<String, String>> retMapListDetails = getFXRETDealDetails(dealDetails.get("CUSTOMER"),
						dealDetails.get("DEALREF"));
				for (Map<String, String> map : retMapListDetails) {
					retAmount = map.get("REF_AMT");
				}
				logger.debug("Ret Amount-> " + retAmount);
				logger.debug("tanxDealUt Amount-> " + tanxdealUtamtbg);

				BigDecimal retamtbg = new BigDecimal(retAmount);
				int res = retamtbg.compareTo(tanxdealUtamtbg); // compare bg1
				// logger.info("compare res -> " + res);

				String str1 = "Both values are equal ";
				String str2 = "First Value is greater ";
				String str3 = "Second value is greater ";

				if (res == 0) {
					logger.debug(retAmount + ", " + tanxdealUtamtbg + ", " + str1);
					status = "SUCCEEDED";
					reponsemsg = "";

				} else if (res == 1) {
					logger.debug(retAmount + ", " + tanxdealUtamtbg + ", " + str2);
					status = "SUCCEEDED";
					reponsemsg = "";

				} else if (res == -1) {
					logger.debug(retAmount + ", " + tanxdealUtamtbg + ", " + str3);
					status = "FAILED";
					reponsemsg = "Insufficient amount of " + retAmount + " for the Deal " + dealid;
				}

				// if (ValidationsUtil.isValidString(retAmount)) {
				// status = "FAILED";
				// reponsemsg = "Insufficient Amount for the Deal";
				// } else {
				// status = "SUCCEEDED";
				// reponsemsg = "";
				// }

			} else {
				status = "SUCCEEDED";
			}

			response.put("STATUS", status);
			response.put("REPONSEMSG", reponsemsg);

			// logger.info(response);
			// logger.info(
			// "--------------------- Master each Deal check is authorized or
			// not ---------------------Stage - 3 - End");
			responseList.add(response);
		}

		return responseList;
	}

	public static String getFXAutorizedDealDetails(String customer, String fxContractReference) {

		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		String totalutlizedamt = "";
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {
				StringBuffer fxOptionSearchQuery = new StringBuffer();
				// 2016-12-03 Prasath
				// fxOptionSearchQuery.append(
				// "SELECT sum(REF_AMT) TOTALUTLIZEDAMT FROM
				// FTRT_DETAILS_UTILIZE WHERE
				// APPLICATION_ID = 'TRDE' and CIF_ID = ? AND TR_REF_NUM = ? ");
				// 2017-03-01
				fxOptionSearchQuery.append(
						"SELECT sum(REF_AMT) TOTALUTLIZEDAMT FROM FTRT_DETAILS_UTILIZE WHERE APPLICATION_ID = 'TIP' and CIF_ID = ? AND TR_REF_NUM = ? and TI_CONTCT_STATUS in ( 'A' )");
				ps = con.prepareStatement(fxOptionSearchQuery.toString());

				ps.setString(1, customer);
				ps.setString(2, fxContractReference);
				logger.debug("FxOptionSearchQuery : " + fxOptionSearchQuery);
				rs = ps.executeQuery();
				ResultSetMetaData rsmd = ps.getMetaData();
				int columncount = rsmd.getColumnCount();
				while (rs.next()) {
					totalutlizedamt = rs.getString("TOTALUTLIZEDAMT");
				}
			}
		} catch (SQLException ex) {
			logger.error("Exception -->" + ex.getMessage(), ex);

		} catch (Exception ex) {
			logger.error("Exception -->" + ex.getMessage(), ex);

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		// logger.debug("********* Backoffice FXContract drawdown
		// request/fxutilization ended *********");
		return totalutlizedamt;
	}

	public static List<Map<String, String>> getFXRETDealDetails(String customer, String fxContractReference) {

		// logger.debug("********* Backoffice FXContract drawdown
		// request/fxutilization started *********");
		// logger.info("Customer : " + customer);
		// logger.info("fxContractRef : " + fxContractReference);

		List<Map<String, String>> returnmaplist = new ArrayList<Map<String, String>>();
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {
				StringBuffer fxOptionSearchQuery = new StringBuffer();
				// and TI_CONTCT_STATUS = 'I'
				fxOptionSearchQuery.append(
						"SELECT * FROM FTRT_DETAILS_UTILIZE WHERE APPLICATION_ID = 'RET' and CIF_ID = ? AND TR_REF_NUM = ? ");
				logger.debug("FxOptionSearchQuery : " + fxOptionSearchQuery);

				ps = con.prepareStatement(fxOptionSearchQuery.toString());
				ps.setString(1, customer);
				ps.setString(2, fxContractReference);

				rs = ps.executeQuery();

				ResultSetMetaData rsmd = ps.getMetaData();
				int columncount = rsmd.getColumnCount();
				while (rs.next()) {
					// logger.debug("whilelooping FxOptions");
					Map<String, String> maplist = new HashMap<String, String>();
					for (int i = 1; i < columncount + 1; i++) {
						// logger.debug("Forlooping FxOptions");
						String key = rsmd.getColumnLabel(i);
						String value = ValidationsUtil.checkIsNull(rs.getString(key));
						// logger.debug("The key:" + key + " & the value: "
						// + value);
						maplist.put(key, value);
					}
					returnmaplist.add(maplist);
					logger.debug("FxOptions list added");
				}
			}
		} catch (SQLException ex) {
			logger.error("Exception -->" + ex.getMessage(), ex);

		} catch (Exception ex) {
			logger.error("Exception -->" + ex.getMessage(), ex);

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		// logger.debug("********* Backoffice FXContract drawdown
		// request/fxutilization ended *********");
		return returnmaplist;
	}

	/**
	 * 
	 * @param errorMessage
	 * @return
	 */
	public String getErrorResponse(String errorMessage) {

		String resultXML = "";
		try {
			ServiceResponse serviceResponse = new ServiceResponse();
			ResponseHeader respHeader = ResponseHeaderUtil.getResponseHeader(ThemeConstant.SERVICE_BACKOFFICE,
					ThemeConstant.OPERATION_VERIFY);
			respHeader.setStatus(StatusEnum.FAILED);
			Details details = new Details();
			// logger.error("errorMessageerrorMessage " + errorMessage);

			details.getError().add("Verify: " + errorMessage + " [IM]");
			details.getWarning().add("Verify: " + errorMessage + " [IM]");

			respHeader.setDetails(details);
			serviceResponse.setResponseHeader(respHeader);
			resultXML = JAXBTransformUtil.doMarshalling(serviceResponse);

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return resultXML;
	}

	// /**
	// *
	// * @param amount
	// * @param currency
	// * @return
	// */
	// public String getTransactionAmount(String amount, String currency) {
	//
	// String result = "";
	// BigDecimal transAmount = new BigDecimal(amount);
	//
	// if (currency.equals("OMR") || currency.equals("BHD") ||
	// currency.equals("KWD") || currency.equals("JOD")) {
	// result = transAmount.divide(new BigDecimal(1000), 3,
	// RoundingMode.CEILING).toString();
	// } else if (currency.equals("JPY")) {
	// result = amount;
	// } else {
	// result = transAmount.divide(new BigDecimal(100), 2,
	// RoundingMode.CEILING).toString();
	// }
	//
	// return result.trim();
	// }
	//
	// /**
	// *
	// * @param amount
	// * @param ccy
	// * @return
	// */
	// public static String convertAmountFromTIPlus(String amount, String ccy) {
	//
	// BigDecimal bg = new BigDecimal("0");
	// if (ValidationsUtil.isValidString(amount) &&
	// ValidationsUtil.isValidString(ccy)
	// && (ccy.equals("OMR") || ccy.equals("BHD") || ccy.equals("KWD"))) {
	// bg = new BigDecimal(amount);
	// bg = bg.divide(new BigDecimal(1000));
	// } else if (ValidationsUtil.isValidString(amount) &&
	// ValidationsUtil.isValidString(ccy) && ccy.equals("JPY")) {
	// bg = new BigDecimal(amount);
	// } else if (ValidationsUtil.isValidString(amount) &&
	// ValidationsUtil.isValidString(ccy)) {
	// bg = new BigDecimal(amount);
	// bg = bg.divide(new BigDecimal(100));
	// }
	// // logger.info("convertAmountFromTIPlus :\t" + bg.toString());
	// return bg.toString();
	//
	// }

}
