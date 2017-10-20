package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_FAILURE_EMAIL_RESUBMIT;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_GATEWAY;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.email.EmailAlertServiceFailureUtil;
import com.bs.theme.bob.adapter.util.BackofficeBatchUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.entity.adapter.TransactionloghistoryAdapter;
import com.bs.themebridge.entity.model.Transactionloghistory;
import com.bs.themebridge.logging.TransactionLogging;
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
import com.bs.themebridge.xpath.BackOfficeBatchXpath;
import com.bs.themebridge.xpath.LimitReservationsReversalXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.messages.BulkServiceRequest;
import com.misys.tiplus2.apps.ti.service.messages.Exposure;
import com.misys.tiplus2.apps.ti.service.messages.Posting;
import com.misys.tiplus2.services.control.ServiceRequest;

/**
 * End system communication implementation for BackOffice Batch services is
 * handled in this class.
 * 
 * @author Raghu M & Prasath R
 */
public class BackOfficeBatchResubmit extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(BackOfficeBatchResubmit.class.getName());

	private String branch = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankStatus = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String correlationId = "";
	private String bankResptranId = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private String exposureReversalSlogStatus = "";
	private String exposureReservationSlogStatus = "";

	public BackOfficeBatchResubmit(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	List<Map<String, String>> postingLegsList = new ArrayList<Map<String, String>>();

	public BackOfficeBatchResubmit() {
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 */
	public boolean doResubmitPosting(String id) {

		logger.debug(" ************ Posting <<<resubmit>>> adaptee started ************ ");
		boolean result = true;
		try {
			// logger.debug("Resbmit id list : " + id);
			// Get Array List of id
			String[] idsList = null;
			idsList = id.split(",");
			logger.debug("BackOffice.Posting.Resumit List : >> " + idsList.length);

			for (String sid : idsList) {
				// logger.debug("Transaction Log ID : " + sid);
				Transactionloghistory transactionlog = BackofficeBatchUtil.getTransactioLog(sid);
				String tiRequestXML = transactionlog.getTirequest();
				// logger.debug("tiRequestXML : " + tiRequestXML);
				short count = transactionlog.getResubmittedcount();
				// logger.debug("count : " + count++);
				transactionlog.setResubmittedcount(new Short(count));

				String exposureOperation = "ExposureReservation";
				exposureReservationSlogStatus = getServiceLogExposureStatus(transactionlog.getMasterreference(),
						transactionlog.getEventreference(), exposureOperation);

				exposureOperation = "ExposureReversal";
				exposureReversalSlogStatus = getServiceLogExposureStatus(transactionlog.getMasterreference(),
						transactionlog.getEventreference(), exposureOperation);
				logger.debug("Exposure(Reservation / Reversal) Servicelog Status : " + exposureReversalSlogStatus);

				// ADD THE TRANSACTIONLOG INTO TRANSACTIONLOGHISTORY
				TransactionloghistoryAdapter thAdapter = new TransactionloghistoryAdapter();
				boolean historyStatus = thAdapter.addProperty(transactionlog);
				// logger.debug("Transactionlog History status : " + result);

				// Delete the recode from TRANSACTIONLOG
				if (historyStatus) {
					BackofficeBatchUtil.deleteTransactionLog(sid);
				}

				/// BackOfficeBatchResubmit batchObj = new
				/// BackOfficeBatchResubmit();
				// String tiResponseXML = batchObj.process(tiRequestXML);
				String tiResponseXML = process(tiRequestXML);
				logger.debug(sid + " response : " + tiResponseXML);
			}

		} catch (Exception e) {
			logger.debug("BackOfficeBatch Resubmit..!! " + e.getMessage());
			e.printStackTrace();
			result = false;
		}

		logger.debug(" ************ Posting <<<resubmit>>> adaptee ended ************ ");
		return result;
	}

	/**
	 * <p>
	 * Process the incoming Back office batch service XML from the TI
	 * </p>
	 * 
	 * @since 2017-FEB-23
	 * @throws JAXBException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String process(String tirequestXML) throws Exception {

		logger.info(" ************ Backoffice.Batch adaptee process started ************ ");

		String errorMsg = "";
		boolean isEOD = false;
		String serviceStatus = "SUCCEEDED";
		initialize(tirequestXML);
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("Backoffice.Batch.Resubmit TI Request: " + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			// TODO EOD
			int isPosting = XPathParsing.getMultiTagCount(tirequestXML, BackOfficeBatchXpath.PostingXPath);
			String iseodMasRef = XPathParsing.getValue(tirequestXML, BackOfficeBatchXpath.MasterReferenceXPath);
			String iseodEvent = XPathParsing.getValue(tirequestXML, BackOfficeBatchXpath.EventReferenceXPath);
			String iseodEventCode = "";
			if (iseodEvent != null && !iseodEvent.isEmpty()) {
				iseodEventCode = iseodEvent.substring(0, 3);
			}
			if ((iseodEventCode.isEmpty() || iseodEventCode == null) && (isPosting > 0)) {
				logger.debug("Entering into EOD posting handler TOP");
				isEOD = true;
				boolean result = BackofficeBatchUtil.eodpostingLogging(tirequestXML, iseodMasRef, iseodEvent);

			} else {
				isEOD = false;
				bankRequest = getBankRequestFromTIRequest(tirequestXML);
				logger.debug("Backoffice.Batch.Resubmit Bank Request: " + bankRequest);

				if (!bankRequest.isEmpty()) {
					bankResponse = getBankResponseFromBankRequest(bankRequest);
					bankResTime = DateTimeUtil.getSqlLocalDateTime();
					logger.debug("Backoffice.Batch.Resubmit Bank Response: " + bankResponse);

					if (!bankRequest.isEmpty() && !bankResponse.isEmpty()) {
						tiResponse = getTIResponseFromBankResponse(bankResponse);
						serviceStatus = XPathParsing.getValue(tiResponse, "/ServiceResponse/ResponseHeader/Status");

					} else if (!bankRequest.isEmpty() && bankResponse.isEmpty()) {
						errorMsg = "HTTP 404 - Finacle host unavailable [IM]";
						tiResponse = generateTIErorrResponse(errorMsg);
						serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
					}

				} else {
					logger.debug("bankRequest is empty..!");
					if (bankRequest.isEmpty()) {
						errorMsg = "Exception occurred while parsing request [IM]";
						tiResponse = generateTIErorrResponse(errorMsg);
						serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
					}
				}
				tiResTime = DateTimeUtil.getSqlLocalDateTime();
				logger.debug("Backoffice.Batch.Resubmit TI Response: " + tiResponse);
			}

		} catch (Exception e) {
			errorMsg = e.getMessage();
			serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
			tiResponse = generateTIErorrResponse(
					"Unexpected Exception occurred while process request. " + errorMsg + " [IM]");

		} finally {
			if (!isEOD) {
				serviceStatus = XPathParsing.getValue(tiResponse, "/ServiceResponse/ResponseHeader/Status");
				logger.debug("Bacloffice.Posting.Resubmit ststus : " + serviceStatus);

				TransactionLogging.pushLogData(getRequestHeader().getService(), getRequestHeader().getOperation(),
						getRequestHeader().getSourceSystem(), branch, getRequestHeader().getSourceSystem(),
						getRequestHeader().getTargetSystem(), masterReference, eventReference, serviceStatus, tiRequest,
						tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "",
						bankResptranId, "", false, "0", errorMsg);

				if (serviceStatus.equals("FAILED"))
					EmailAlertServiceFailureUtil.sendFailureAlertMail(SERVICE_GATEWAY, OPERATION_FAILURE_EMAIL_RESUBMIT,
							masterReference, eventReference, getRequestHeader().getSourceSystem(),
							getRequestHeader().getTargetSystem());
			}
		}
		logger.info(" ************ Backoffice.Batch adaptee process finished ************ ");
		return tiResponse;
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
			// logger.debug("Bank response : " + bankResponse);
		}
		return result;
	}

	/**
	 * 
	 * @param error
	 *            {@code allows } {@link String}
	 * @param customerLimitResponse
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String generateTIErorrResponse(String errorMessage) {

		String responseXML = "";
		try {
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", "FAILED");// PRASATH
			tokens.put("Error", errorMessage);
			tokens.put("Warning", "");
			tokens.put("Info", "");
			tokens.put("ResponseHeader", "");
			tokens.put("CorrelationId", correlationId);

			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_BATCH_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			responseXML = reader.toString();

			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			reader.close();

		} catch (Exception e) {
			logger.error("BackOffice postings exceptions! " + e.getMessage());
			return e.getMessage();
		}
		return responseXML;
	}

	/**
	 * 
	 * @param responseXml
	 *            {@code allows } {@link String}
	 * @return
	 */
	private String getTIResponseFromBankResponse(String bankResponseXML) {

		bankResptranId = "";
		String result = "";
		String errorMessages = "";
		String tokenReplacedXML = "";
		String bankPostingStatus = "";
		try {
			if (!bankResponseXML.isEmpty())
				bankPostingStatus = XPathParsing.getValue(bankResponseXML, BackOfficeBatchXpath.StatusXPath);
			// logger.debug("Bank Response Status : " + bankStatus);

			if (bankPostingStatus.equalsIgnoreCase("FAILURE")) {
				bankStatus = "FAILED";
			} else if (bankPostingStatus.equalsIgnoreCase("SUCCESS")) {
				bankStatus = "SUCCEEDED";
				bankResptranId = XPathParsing.getValue(bankResponseXML, BackOfficeBatchXpath.TranIdXPath);
				if (ValidationsUtil.isValidString(bankResptranId))
					bankResptranId.trim();

			} else if (bankPostingStatus == null || bankPostingStatus.isEmpty()) {
				bankStatus = "SUCCEEDED";
			}

			errorMessages = getBankResponseErrorMessage(bankResponseXML);

			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_BATCH_TI_RESPONSE_TEMPLATE);
			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", bankStatus);
			tokens.put("Error", errorMessages);
			tokens.put("Warning", "");
			tokens.put("Info", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("ResponseHeader", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();

			result = tokenReplacedXML;
			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeBatch TIResponse Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 */
	private String getBankRequestFromTIRequest(String requestXML) {

		// logger.info("Enter into getBankRequestFromTIRequest method...");
		String limitResult = null;
		boolean fxUtilResult = false;
		try {
			correlationId = XPathParsing.getValue(requestXML, BackOfficeBatchXpath.correlationIdXPath);
			JAXBContext jaxbContext = JAXBContext.newInstance("com.misys.tiplus2.apps.ti.service.messages");
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			NodeList nodeList = getDocument().getElementsByTagNameNS("*", "BatchRequest");
			JAXBElement<BulkServiceRequest> bulkServiceJAXB = (JAXBElement<BulkServiceRequest>) unmarshaller
					.unmarshal(nodeList.item(0), BulkServiceRequest.class);
			BulkServiceRequest bsr = bulkServiceJAXB.getValue();
			List<ServiceRequest> bsrList = bsr.getServiceRequest();
			List<Exposure> anExposureList = new ArrayList<Exposure>();
			// List<FxContractDrawdownAPI> fxContractsList = new
			// ArrayList<FxContractDrawdownAPI>();

			for (ServiceRequest sr : bsrList) {
				List<JAXBElement<?>> serviceList = sr.getRequest();
				JAXBElement<?> postingJAXB = (JAXBElement<?>) serviceList.get(0);

				/******* Posting leg *******/
				if (postingJAXB.getValue() instanceof Posting) {
					Posting aPosting = (Posting) postingJAXB.getValue();

					String eventCode = "";
					if (!aPosting.getProductReference().equalsIgnoreCase("FTI"))
						masterReference = aPosting.getMasterReference();
					eventReference = aPosting.getEventReference();

					if (aPosting.getEventReference() != null) {
						eventCode = aPosting.getEventReference().substring(0, 3);
					}

					if (eventCode != null && !eventCode.isEmpty()) {
						postingLegsList.add(getPostingLegMap(aPosting, eventCode));

					} else {
						// Neither EOD nor Online Posting
						logger.debug("Neither EOD nor Online Posting");
					}

				} else if (postingJAXB.getValue() instanceof Exposure) {
					if (exposureReservationSlogStatus.equalsIgnoreCase("FAILED")
							|| exposureReversalSlogStatus.equalsIgnoreCase("FAILED")) {
						logger.debug("Exposure FAILED");
						// Exposure anExposure = (Exposure)
						// postingJAXB.getValue();
						// anExposureList.add(anExposure);
					}
				}
			}

			if (postingLegsList != null && postingLegsList.size() > 0) {
				// logger.debug("Online posting generating bank Request");
				bankRequest = generateBankRequest(postingLegsList);
			}


		} catch (Exception exp) {
			logger.error("Resubmit Posting Exceptions! " + exp.getMessage(), exp);
			exp.printStackTrace();
		}

		return bankRequest;
	}

	/**
	 * 
	 * @param aPosting
	 * @param eventCode
	 * @return
	 */
	private Map<String, String> getPostingLegMap(Posting aPosting, String eventCode) {

		// logger.info("Enter into getPostingLegMap method with param Posting
		// and eventCode");
		Map<String, String> postingLegs = new HashMap<String, String>();

		try {
			postingLegs.put("eventCode", eventCode);
			postingLegs.put("currency", aPosting.getPostingCcy());
			postingLegs.put("accountNumber", aPosting.getBackOfficeAccountNo());
			postingLegs.put("debitcreditFlag", aPosting.getDebitCreditFlag());
			postingLegs.put("postingAmount",
					getTransactionAmount(aPosting.getPostingAmount().toString(), aPosting.getPostingCcy()));
			postingLegs.put("transactionCode", aPosting.getTransactionCode());
			postingLegs.put("accountType", aPosting.getAccountType());
			postingLegs.put("customerID", aPosting.getCustomerMnemonic());
			postingLegs.put("branch", aPosting.getPostingBranch());
			postingLegs.put("valueDate", aPosting.getValueDate().toString());
			postingLegs.put("masterReference", aPosting.getMasterReference());
			postingLegs.put("eventReference", aPosting.getEventReference());
			postingLegs.put("productReference", aPosting.getProductReference());
			postingLegs.put("forceDebitCredit", aPosting.getExtraData().getFORCDBT());
			postingLegs.put("lobcode", aPosting.getExtraData().getLOBCOD());
			// get Particulars
			String transactionParticulars = BackofficeBatchUtil
					.getTransChargeParticulars(aPosting.getSPSKCategoryCode());
			postingLegs.put("transactionParticulars", transactionParticulars);
			// get Posting narrative
			String postingNarrative1 = "";
			postingNarrative1 = aPosting.getPostingNarrative1();
			postingLegs.put("postingNarrative1", postingNarrative1);

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
			e.printStackTrace();
		}

		// logger.debug("Final Result of getPostingLegMap method:" +
		// postingLegs);
		return postingLegs;

	}

	/**
	 * 
	 * @param postingLegList
	 * @return
	 */
	private String generateBankRequest(List<Map<String, String>> postingLegList) {

		// logger.info("Enter into generateBankRequest message");
		// logger.debug("BankReqXML Milestone 01");
		String bankRequestXML = null;
		try {
			StringBuilder xferTrnDetail = new StringBuilder();
			StringBuilder xferTrnAddCustomData = new StringBuilder();

			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACK_OFFICE_POSTING_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = ThemeBridgeUtil.randomCorrelationId();
			correlationId = requestId;
			logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("TrnType", "T");// TODO
			tokens.put("TrnSubType", "BI");// TODO
			String billRefNum = BackofficeBatchUtil.getBillReferenceNum(masterReference, eventReference);
			logger.debug("BankReqXML Milestone 03");
			int legCount = 1;
			for (Map<String, String> postingLeg : postingLegList) {
				xferTrnDetail.append(generateXferTrnDetailXML(postingLeg, legCount));
				xferTrnAddCustomData.append(generateXferTrnAddCustomDataXML(legCount, billRefNum));
				legCount++;
			}
			String forceDebitCredit = "N";
			String forceDrCrFlag = postingLegList.get(0).get("forceDebitCredit");
			logger.debug("XML Force DrCr Flag : " + forceDrCrFlag);
			// TODO
			forceDrCrFlag = getForceDebit(masterReference, eventReference);
			logger.debug("DB Force DrCr Flag : " + forceDrCrFlag);
			if (!forceDrCrFlag.isEmpty() && forceDrCrFlag != null && forceDrCrFlag.equalsIgnoreCase("Y")) {
				forceDebitCredit = "F";
			}
			logger.debug("BankReqXML Milestone 04");
			xferTrnAddCustomData.append("<Debit_Mode_Flg>" + forceDebitCredit + "</Debit_Mode_Flg>");
			// xferTrnAddCustomData.append("<Debit_Mode_Flg>N</Debit_Mode_Flg>");
			tokens.put("XferTrnDetail", xferTrnDetail.toString());
			tokens.put("XferTrnAdd_CustomData", xferTrnAddCustomData.toString());

			logger.debug("BankReqXML Milestone 05 XXX");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			bankRequestXML = reader.toString();
			reader.close();
			logger.debug("BankReqXML Milestone 06 YYY");

			bankRequestXML = bankRequestXML.replace("&", "&amp;");
			logger.debug("BankReqXML Milestone 06 ZZZ");

		} catch (Exception e) {
			logger.error("Bank Request XML generate exceptions! " + e.getMessage());
			e.printStackTrace();
			logger.debug("BankReqXML Milestone 07 ERER");
			return e.getMessage();
		}
		// bankRequestXML = ThemeBridgeUtil.formatXml(bankRequestXML);
		logger.info("Final bank requestXML message-->" + bankRequestXML);
		return bankRequestXML;
	}

	/**
	 * 
	 * @param legCount
	 * @return
	 */
	private String generateXferTrnAddCustomDataXML(int legCount, String billRefNum) {

		// logger.info("Enter into generateXferTrnAddCustomDataXML method...");

		String postingRefNumber = "";// 1030
		if (!billRefNum.isEmpty() && billRefNum != null) {
			postingRefNumber = billRefNum;
		}
		StringBuilder xferTrnAddCustomData = new StringBuilder();
		xferTrnAddCustomData.append("<PTRANREC isMultiRec='Y'>");
		xferTrnAddCustomData.append("<RECNUM>" + legCount + "</RECNUM>");
		xferTrnAddCustomData.append("<PLACEHOLDER/>");
		xferTrnAddCustomData.append("<SOLID/>");
		xferTrnAddCustomData.append("<CRNCY/>");
		xferTrnAddCustomData.append("<REFNUM>" + postingRefNumber + "</REFNUM>");
		xferTrnAddCustomData.append("</PTRANREC>");

		return xferTrnAddCustomData.toString();
	}

	/**
	 * 
	 * @param postingLeg
	 * @param legCount
	 * @return
	 */
	private String generateXferTrnDetailXML(Map<String, String> postingLeg, int legCount) {

		// logger.info("Enter into generateXferTrnDetailXML method...");
		String accountNumber = "";
		StringBuilder xferTrnDetail = new StringBuilder();
		try {
			String accountType = postingLeg.get("accountType");
			String debitCredit = postingLeg.get("debitcreditFlag");
			String productCode = postingLeg.get("productReference");
			String postingCcy = postingLeg.get("currency");
			// logger.debug("productCode : " + productCode);

			xferTrnDetail.append("<PartTrnRec>");
			xferTrnDetail.append("<AcctId>");
			/** UAT **/
			// if ((accountType.startsWith("R") || accountType.startsWith("L"))
			// && !accountType.equals("R1")
			// && !accountType.equals("R11") && !accountType.startsWith("RTGS"))
			// {
			// String glNumber = BackofficeBatchUtil.getGLAccount(accountType,
			// debitCredit, productCode);
			// logger.debug("accountType / glNumber : " + accountType + "\t" +
			// glNumber);
			// accountNumber = postingLeg.get("branch") +
			// BackofficeBatchUtil.getCcyCode(postingCcy) + glNumber;
			// xferTrnDetail.append("<AcctId>" + accountNumber + "</AcctId>");
			// } else {
			// xferTrnDetail.append("<AcctId>" + postingLeg.get("accountNumber")
			// + "</AcctId>");
			// }
			/** SIT **/
			xferTrnDetail.append("<AcctId>" + postingLeg.get("accountNumber") + "</AcctId>");

			xferTrnDetail.append("</AcctId>");
			xferTrnDetail.append("<CreditDebitFlg>" + postingLeg.get("debitcreditFlag") + "</CreditDebitFlg>");
			xferTrnDetail.append("<TrnAmt>");
			xferTrnDetail.append("<amountValue>" + postingLeg.get("postingAmount") + "</amountValue>");
			xferTrnDetail.append("<currencyCode>" + postingCcy + "</currencyCode>");
			xferTrnDetail.append("</TrnAmt>");

			String postingNarrative1 = "";
			if (postingLeg.get("postingNarrative1") != null && !postingLeg.get("postingNarrative1").isEmpty()) {
				postingNarrative1 = postingLeg.get("postingNarrative1");
			}
			xferTrnDetail.append("<TrnParticulars>" + postingNarrative1 + "</TrnParticulars>");
			xferTrnDetail.append("<PartTrnRmks>" + postingLeg.get("lobcode") + "</PartTrnRmks>"); // lob
			xferTrnDetail.append("<ValueDt>" + postingLeg.get("valueDate") + "T" + DateTimeUtil.getStringLocalTimeFi()
					+ "</ValueDt>");
			xferTrnDetail.append("<SerialNum>" + legCount + "</SerialNum>");
			xferTrnDetail.append("</PartTrnRec>");

		} catch (Exception e) {
			logger.error("Transaction Particluars...! " + e.getMessage());
			e.printStackTrace();

		}
		return xferTrnDetail.toString();
	}

	/**
	 * 
	 * @param amount
	 * @param currency
	 * @return
	 */
	public String getTransactionAmount(String amount, String currency) {

		String result = "";
		BigDecimal transAmount = new BigDecimal(amount);

		if (currency.equals("OMR") || currency.equals("BHD") || currency.equals("KWD") || currency.equals("JOD")) {
			result = transAmount.divide(new BigDecimal(1000), 3, RoundingMode.CEILING).toString();
		} else if (currency.equals("JPY")) {
			result = amount;
		} else {
			result = transAmount.divide(new BigDecimal(100), 2, RoundingMode.CEILING).toString();
		}

		return result.trim();
	}

	/**
	 * 
	 * @param masterReference
	 * @param eventReference
	 * @return
	 */
	public static String getForceDebit(String masterReference, String eventReference) {

		String forceDrNo = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"SELECT TRIM(EXT.FORCDEBT) AS FORCDEBIT FROM EXTEVENT EXT JOIN BASEEVENT BEV ON BEV.KEY97 = EXT.EVENT JOIN MASTER MAS ON MAS.KEY97=BEV.MASTER_KEY WHERE trim(MAS.MASTER_REF) = ? AND trim((BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 0)) ) = ? ");
			aPreparedStatement.setString(1, masterReference);
			aPreparedStatement.setString(2, eventReference);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				forceDrNo = aResultset.getString("FORCDEBIT");
			}

		} catch (Exception e) {
			logger.debug("Force Debit Credit Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return forceDrNo;
	}

	public static void main(String a[]) throws Exception {

		// ResubmitBatchAdaptee bb = new ResubmitBatchAdaptee();

		String requestXML = "";
		requestXML = ThemeBridgeUtil
				.readFile("D:\\_Prasath\\00_TASK\\task Exposure only\\DoubleExposure2017-02-28.xml");

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
			String scriptResponseException = XPathParsing.getValue(bankResponseXml,
					LimitReservationsReversalXpath.FIScriptResponseExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml,
							LimitReservationsReversalXpath.FIScriptResponseExMsgDescXpath);

			String fiBusinessException = XPathParsing.getValue(bankResponseXml,
					LimitReservationsReversalXpath.FIBusinessExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml, LimitReservationsReversalXpath.FIBusinessExMsgDescXpath);

			String fiSystemException = XPathParsing.getValue(bankResponseXml,
					LimitReservationsReversalXpath.FISystemExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml,
							LimitReservationsReversalXpath.FISystemExErrorMsgDescXpath);

			allerrorMessages = scriptResponseException + fiBusinessException + fiSystemException;
			if (ValidationsUtil.isValidString(allerrorMessages))
				allerrorMessages = allerrorMessages + " [IM]";
			logger.debug("BackOffice Batch-Posting BankResponse error : " + allerrorMessages);

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

	private String getServiceLogExposureStatus(String SlogMasterreference, String slogEventreference,
			String exposureOperation) {

		String slogStatus = "FAILED";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"select TRIM(STATUS) SLOGSTATUS from SERVICELOG WHERE masterreference=? AND eventreference=? AND OPERATION=?");
			aPreparedStatement.setString(1, SlogMasterreference);
			aPreparedStatement.setString(2, slogEventreference);
			aPreparedStatement.setString(3, exposureOperation);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				slogStatus = aResultset.getString("SLOGSTATUS");
			}
			logger.debug("ServiceLog ExposureStatus : " + slogStatus);

		} catch (Exception e) {
			logger.error("ServiceLog ExposureStatus Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}

		return slogStatus;
	}
}
