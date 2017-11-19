package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_BATCH;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_BACKOFFICE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import com.bs.themebridge.entity.model.Postingstaging;
import com.bs.themebridge.logging.TransactionLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.BackOfficeBatchXpath;
import com.bs.themebridge.xpath.LimitReservationsReversalXpath;
import com.bs.themebridge.xpath.RequestHeaderXpath;
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
public class BackOfficeBatchAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(BackOfficeBatchAdaptee.class.getName());
	public static final String POSTING_STATUS_QUERY = "SELECT COUNT(*) AS FAILED FROM TRANSACTIONLOG WHERE MASTERREFERENCE=? AND EVENTREFERENCE=? AND SERVICE='BackOffice' AND OPERATION='Batch' AND STATUS='FAILED' AND PROCESSTIME >= SYSDATE-1";

	private String branch = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankStatus = "";
	private String bankPostingRequest = "";
	private String bankPostingResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private String eventReference = "";
	private String bankResptranId = "";
	private String masterReference = "";
	private String fxStatus = "";
	private String exposureStatus = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	List<Map<String, String>> postingLegsList = new ArrayList<Map<String, String>>();
	String serviceStatus = "";

	public BackOfficeBatchAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public BackOfficeBatchAdaptee() {
	}

	public static void main(String a[]) throws Exception {
		BackOfficeBatchAdaptee bb = new BackOfficeBatchAdaptee();
		//String req = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Downloads\\TIREQUSTBATCH.txt");
		String req = ThemeBridgeUtil.readFile("P:\\pending\\Batch with limit exposure.txt");
		bb.process(req);
		//bb.getTIResponseFromBankResponse(resp, "SUCCEEDED", "SUCCEEDED");
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
		serviceStatus = "SUCCEEDED";
		initialize(tirequestXML);
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			sourceSystem = XPathParsing.getValue(tirequestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(tirequestXML, RequestHeaderXpath.TARGETSYSTEM);
			logger.debug("Backoffice.Batch TI Request: " + tiRequest);

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
				bankPostingRequest = getBankRequestFromTIRequest(tirequestXML);
				logger.debug("Backoffice.Batch Bank Request: " + bankPostingRequest);

				if (!bankPostingRequest.isEmpty()) {
					bankPostingResponse = getBankResponseFromBankRequest(bankPostingRequest);
					bankResTime = DateTimeUtil.getSqlLocalDateTime();
					logger.debug("Backoffice.Batch Bank Response: " + bankPostingResponse);

					if (!bankPostingRequest.isEmpty() && !bankPostingResponse.isEmpty()) {
						tiResponse = getTIResponseFromBankResponse(bankPostingResponse, exposureStatus, fxStatus);
						serviceStatus = XPathParsing.getValue(tiResponse, "/ServiceResponse/ResponseHeader/Status");

					} else if (!bankPostingRequest.isEmpty() && bankPostingResponse.isEmpty()) {
						errorMsg = "Batch: HTTP 404 - Finacle host unavailable [IM]";
						tiResponse = generateTIErorrResponse(errorMsg, exposureStatus, fxStatus);
						serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
					}

				} else {
					logger.debug("bankRequest is empty..!");
					if (bankPostingRequest.isEmpty()) {
						errorMsg = "";
						tiResponse = generateTIErorrResponse(errorMsg, exposureStatus, fxStatus);
						serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
					}
				}
				tiResTime = DateTimeUtil.getSqlLocalDateTime();
				logger.debug("Backoffice.Batch TI Response: " + tiResponse);
			}

		} catch (Exception e) {
			errorMsg = e.getMessage();
			serviceStatus = ThemeBridgeStatusEnum.FAILED.toString();
			tiResponse = generateTIErorrResponse("Unexpected Exception occurred " + errorMsg + " [IM]", exposureStatus,
					fxStatus);

		} finally {
			if (!isEOD) {
				serviceStatus = XPathParsing.getValue(tiResponse, "/ServiceResponse/ResponseHeader/Status");
				// serviceStatus="SUCCEEDED";
				logger.debug("Bacloffice.Posting ststus : " + serviceStatus);

				TransactionLogging.pushLogData(SERVICE_BACKOFFICE, OPERATION_BATCH, sourceSystem, branch, sourceSystem,
						targetSystem, masterReference, eventReference, serviceStatus, tiRequest, tiResponse,
						bankPostingRequest, bankPostingResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "",
						bankResptranId, "", false, "0", errorMsg);

				Thread alertMailThread = new Thread() {
					public void run() {
						if (serviceStatus.equals("FAILED"))
							EmailAlertServiceFailureUtil.sendFailureAlertMail("BackOffice", "Posting", masterReference,
									eventReference, sourceSystem, targetSystem);
					}
				};
				alertMailThread.setName("alertMailThread");
				alertMailThread.start();

			}
		}
		logger.info(" ************ Backoffice.Batch adaptee process finished ************ ");
		return tiResponse;
	}

	private static boolean checkTransactionPostingStatus(String masterRef, String eventRef) {

		// logger.info("Enter into checkSwiftTransactionStatus method..!");
		ResultSet rs = null;
		Connection dbConnection = null;
		boolean swiftTransResult = false;
		PreparedStatement preparedStatement = null;
		String queryDetails = POSTING_STATUS_QUERY;
		// logger.info("Posting Staging : " + queryDetails);

		try {
			dbConnection = DatabaseUtility.getThemebridgeConnection();
			if (ValidationsUtil.isValidObject(dbConnection)) {
				preparedStatement = dbConnection.prepareStatement(queryDetails);
				// logger.debug("MASTERREFERENCE >>" + masterRef + "<<");
				// logger.debug("EVENTREFERENCE >>" + eventRef + "<<");
				preparedStatement.setString(1, masterRef);
				preparedStatement.setString(2, eventRef);
				rs = preparedStatement.executeQuery();
				while (rs.next()) {
					// logger.debug("Posting status : " + rs.getInt(1));
					if (rs.getInt(1) == 0)
						swiftTransResult = true;
				}
			}
		} catch (SQLException e) {
			logger.error("Posting staging exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(dbConnection, preparedStatement, rs);

		}
		// logger.debug("checkTransactionPostingStatus " + swiftTransResult);
		return swiftTransResult;
	}

	private static List<Postingstaging> getStagingQueueDetails(String mastRef, String eventRef) {

		ResultSet rs = null;
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		String queryDetails = "SELECT * FROM POSTINGSTAGING WHERE masterreference like '%" + mastRef
				+ "%' and eventreference like '%" + eventRef + "%' and status='QUEUED'";
		List<Postingstaging> stagingQueueList = new ArrayList<Postingstaging>();
		// logger.info("Get QUEUED List from Posting Staging : " +
		// queryDetails);

		try {
			dbConnection = DatabaseUtility.getThemebridgeConnection();
			if (ValidationsUtil.isValidObject(dbConnection)) {
				preparedStatement = dbConnection.prepareStatement(queryDetails);
				rs = preparedStatement.executeQuery();
				while (rs.next()) {
					Postingstaging aPostingstaging = new Postingstaging();
					// System.out.println("TR");
					if (ValidationsUtil.isValidString(rs.getString("MASTERREFERENCE"))
							&& ValidationsUtil.isValidString(rs.getString("EVENTREFERENCE"))
							&& ValidationsUtil.isValidString(rs.getString("TIREQUEST"))) {

						aPostingstaging.setId(rs.getBigDecimal("ID"));
						aPostingstaging.setService(rs.getString("SERVICE"));
						aPostingstaging.setOperation(rs.getString("OPERATION"));
						aPostingstaging.setMasterreference(rs.getString("MASTERREFERENCE"));
						aPostingstaging.setEventreference(rs.getString("EVENTREFERENCE"));
						aPostingstaging.setStatus(rs.getString("STATUS"));
						aPostingstaging.setProcesstime(rs.getDate("PROCESSTIME"));
						aPostingstaging.setTirequest(rs.getString("TIREQUEST"));
						// PRASATH RAVICHANDRAN
						aPostingstaging.setTiresponse(rs.getString("TIRESPONSE"));
						aPostingstaging.setTireqtime(rs.getTimestamp("TIREQTIME"));
						aPostingstaging.setTirestime(rs.getTimestamp("TIRESTIME"));
						stagingQueueList.add(aPostingstaging);
					}
				}
			}
		}

		catch (SQLException e) {
			logger.error("Posting staging get queued List exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(dbConnection, preparedStatement, rs);

		}
		return stagingQueueList;
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 */
	private String getBankRequestFromTIRequest(String requestXML) {

		// logger.info("Enter into getBankRequestFromTIRequest method...");
		// String limitResult = null;
		// boolean fxUtilResult = false;
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
					// logger.debug("aPosting.getProductReference() >>" +
					// aPosting.getProductReference() + "<<");
					if (!aPosting.getProductReference().equalsIgnoreCase("FTI"))
						masterReference = aPosting.getMasterReference();
					eventReference = aPosting.getEventReference();

					if (aPosting.getEventReference() != null) {
						eventCode = aPosting.getEventReference().substring(0, 3);
					}
					if (eventCode != null && !eventCode.isEmpty()) {
						// logger.debug("Entering into ONLINE posting handler");
						// logger.debug("postingJAXB >>-->>Milestone
						// 001<<--<<");
						postingLegsList.add(getPostingLegMap(aPosting, eventCode));

					} else {
						// Neither EOD nor Online Posting
						logger.debug("Neither EOD nor Online Posting");
					}
					// logger.debug("postingJAXB >>-->>Milestone 002<<--<<");
				} else if (postingJAXB.getValue() instanceof Exposure) {
					/******* Eposure Leg *******/
					Exposure anExposure = (Exposure) postingJAXB.getValue();
					anExposureList.add(anExposure);
				}
			}
			// logger.debug("postingJAXB >>-->>Milestone 003<<--<<");
			if (postingLegsList != null && postingLegsList.size() > 0) {
				// logger.debug("Online posting generating bank Request");
				bankPostingRequest = generateBankRequest(postingLegsList);
			}
			
			if (anExposureList != null && anExposureList.size() > 0) {
				// TODO Exposure adaptee class
				logger.debug(" ************ Backoffice.Batch exposure calling ************ ");
				LimitExposureAdaptee anLimitExposureObj = new LimitExposureAdaptee();
				exposureStatus = anLimitExposureObj.processBankRequestDetails(anExposureList, tiRequest,
						masterReference, eventReference);
				logger.debug(" ************ Backoffice.Batch exposure calling ended ************ ");
			}
			
			logger.debug("postingJAXB >>-->>Milestone 005<<--<<");
		} catch (Exception exp) {
			logger.error("Exceptions! " + exp.getMessage(), exp);
			exp.printStackTrace();
		}
		// logger.debug("return bankRequest : >>" + bankRequest + "<<");
		return bankPostingRequest;
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
			logger.debug("Exception..! " + e.getMessage());
			e.printStackTrace();

		}
		return result;
	}

	/**
	 * 
	 * @param aPosting
	 * @param eventCode
	 * @return
	 */
	public Map<String, String> getPostingLegMap(Posting aPosting, String eventCode) {

		// logger.info("Enter into getPostingLegMap method with param Posting
		// and eventCode");
		Map<String, String> postingLegs = new HashMap<String, String>();

		try {
			postingLegs.put("eventCode", eventCode);
			postingLegs.put("currency", aPosting.getPostingCcy());
			postingLegs.put("accountNumber", aPosting.getBackOfficeAccountNo());
			postingLegs.put("debitcreditFlag", aPosting.getDebitCreditFlag());
			// 2017-04-11
			postingLegs.put("postingAmount", AmountConversion
					.getTransactionAmount(aPosting.getPostingAmount().toString(), aPosting.getPostingCcy()));
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
			//String transactionParticulars = BackofficeBatchUtil
				//	.getTransChargeParticulars(aPosting.getSPSKCategoryCode());
			String transactionParticulars = masterReference+" "+eventReference;
			postingLegs.put("transactionParticulars", transactionParticulars);
			String postingNarrative1 = "";
			postingNarrative1 = aPosting.getPostingNarrative1();
			postingLegs.put("postingNarrative1", postingNarrative1);
			String postingNarrative2 = "";
			postingNarrative2 = aPosting.getPostingNarrative2();
			postingLegs.put("postingNarrative2", postingNarrative2);

		} catch (Exception e) {
			logger.error("Exception..! " + e.getMessage());
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

			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACK_OFFICE_POSTING_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = "COR-" + ThemeBridgeUtil.randomCorrelationId();
			correlationId = requestId;
			// logger.debug("BankReqXML Milestone 02");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("TrnType", "T");// TODO
			tokens.put("TrnSubType", "CI");// TODO
			String billRefNum = BackofficeBatchUtil.getBillReferenceNum(masterReference, eventReference);
			// logger.debug("BankReqXML Milestone 03");
			int legCount = 1;
			for (Map<String, String> postingLeg : postingLegList) {
				xferTrnDetail.append(generateXferTrnDetailXML(postingLeg, legCount));
				//xferTrnAddCustomData.append(generateXferTrnAddCustomDataXML(legCount, billRefNum));
				legCount++;
			}
			//String forceDebitCredit = "N";
//			String forceDrCrFlag = postingLegList.get(0).get("forceDebitCredit");
//			logger.debug("XML Force DrCr Flag : " + forceDrCrFlag);
//			// TODO
//			forceDrCrFlag = getForceDebit(masterReference, eventReference);
//			logger.debug("DB Force DrCr Flag : " + forceDrCrFlag);
//			if (!forceDrCrFlag.isEmpty() && forceDrCrFlag != null && forceDrCrFlag.equalsIgnoreCase("Y")) {
//				forceDebitCredit = "F";
//			}
			// logger.debug("BankReqXML Milestone 04");
			//xferTrnAddCustomData.append("<Debit_Mode_Flg>" + forceDebitCredit + "</Debit_Mode_Flg>");
			// xferTrnAddCustomData.append("<Debit_Mode_Flg>N</Debit_Mode_Flg>");
			tokens.put("XferTrnDetail", xferTrnDetail.toString());
			//tokens.put("XferTrnAdd_CustomData", xferTrnAddCustomData.toString());

			// logger.debug("BankReqXML Milestone 05 XXX");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			bankRequestXML = reader.toString();
			reader.close();
			
			// TODO
			bankRequestXML = bankRequestXML.replace("&", "&amp;");
			// logger.debug("BankReqXML Milestone 06 ZZZ");

		} catch (Exception e) {
			logger.error("Bank Request XML generate exceptions! " + e.getMessage());
			e.printStackTrace();
			// logger.debug("BankReqXML Milestone 07 ERER");
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

			xferTrnDetail.append("<partTran ismultirec=\"Y\">");
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
			xferTrnDetail.append("<acctNo>" + postingLeg.get("accountNumber") + "</acctNo>");

			xferTrnDetail.append("<drCrInd>" + postingLeg.get("debitcreditFlag") + "</drCrInd	>");
			xferTrnDetail.append("<tranAmt>" + postingLeg.get("postingAmount") + "</tranAmt>");
			//xferTrnDetail.append("<currencyCode>" + postingCcy + "</currencyCode>");

			String postingNarrative1 = "";
			if (postingLeg.get("postingNarrative1") != null && !postingLeg.get("postingNarrative1").isEmpty()) {
				postingNarrative1 = postingLeg.get("postingNarrative1");
			}
			logger.info("postingNarrative1 " + postingNarrative1);

			String postingNarrative2 = "";
			if (postingLeg.get("postingNarrative2") != null && !postingLeg.get("postingNarrative2").isEmpty()) {
				postingNarrative2 = postingLeg.get("postingNarrative2");
			}
			logger.info("postingNarrative2 " + postingNarrative2);

			if (postingNarrative2.length() >= 26) {
				postingNarrative2 = postingNarrative2.substring(0, 26);
			}
			if (postingNarrative2.length() <= 25) {
				postingNarrative2 = postingNarrative2.substring(0, postingNarrative2.length());
			}

			postingNarrative1 = postingNarrative1.trim() + " " + postingNarrative2;

			if (postingNarrative1.length() > 50) {
				postingNarrative1 = postingNarrative1.substring(0, 50);
			}
			String remarks= "nill";
			xferTrnDetail.append("<tranParti>" + masterReference+" "+eventReference  + "</tranParti>");
			xferTrnDetail.append("<tranRmks>" + remarks + "</tranRmks>"); // lob
//			xferTrnDetail.append("<ValueDt>" + postingLeg.get("valueDate") + "T" + DateTimeUtil.getStringLocalTimeFi()
//					+ "</ValueDt>");
//			xferTrnDetail.append("<SerialNum>" + legCount + "</SerialNum>");
			xferTrnDetail.append("</partTran>");

		} catch (Exception e) {
			logger.error("Transaction Particluars...! " + e.getMessage());
			e.printStackTrace();

		}
		return xferTrnDetail.toString();
	}

	/**
	 * 
	 * @param masterReference
	 * @param eventReference
	 * @return
	 */
//	public static String getForceDebit(String masterReference, String eventReference) {
//
//		String forceDrNo = "";
//		ResultSet aResultset = null;
//		Connection aConnection = null;
//		PreparedStatement aPreparedStatement = null;
//		try {
//			aConnection = DatabaseUtility.getTizoneConnection();
//			aPreparedStatement = aConnection.prepareStatement(
//					"SELECT TRIM(EXT.FORCDEBT) AS FORCDEBIT FROM EXTEVENT EXT JOIN BASEEVENT BEV ON BEV.KEY97 = EXT.EVENT "
//							+ " JOIN MASTER MAS ON MAS.KEY97=BEV.MASTER_KEY WHERE trim(MAS.MASTER_REF) = ? AND trim((BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 0)) ) = ? ");
//			aPreparedStatement.setString(1, masterReference);
//			aPreparedStatement.setString(2, eventReference);
//			aResultset = aPreparedStatement.executeQuery();
//			while (aResultset.next()) {
//				forceDrNo = aResultset.getString("FORCDEBIT");
//			}
//
//		} catch (Exception e) {
//			logger.debug("Force Debit Credit Exceptions! " + e.getMessage());
//			e.printStackTrace();
//
//		} finally {
//			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
//
//		}
//		return forceDrNo;
//	}

	
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
			// logger.debug("BackOffice Batch-Posting BankResponse error : " +
			// allerrorMessages);

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
	 * 
	 * @param error
	 *            {@code allows } {@link String}
	 * @param customerLimitResponse
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String generateTIErorrResponse(String errorMessage, String exposureStatus, String fxStatus) {

		String responseXML = "";
		try {
			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_BATCH_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", "FAILED");// PRASATH
			tokens.put("Error", errorMessage);
			tokens.put("Warning", "");
			tokens.put("Info", "");
			// tokens.put("ResponseHeader", "");
			tokens.put("CorrelationId", correlationId);

			/** Exposure **/
			if (!exposureStatus.isEmpty())
				tokens.put("ExposureStatus", exposureStatus);
			else
				tokens.put("ExposureStatus", "");
			tokens.put("ExposureError", "");
			tokens.put("ExposureWarning", "");// always
			tokens.put("ExposureInfo", "");// always
			// tokens.put("ExposureResponse", "");

			/** Posting **/
			tokens.put("PostingStatus", bankStatus);
			tokens.put("PostingError", errorMessage);
			tokens.put("PostingWarning", "");// always
			tokens.put("PostingInfo", "");// always
			// tokens.put("PostingResponse", "");

			/** FXContractDrawdown **/
			if (!fxStatus.isEmpty())
				tokens.put("FXStatus", fxStatus);
			else
				tokens.put("FXStatus", "");
			tokens.put("FXError", "");
			tokens.put("FXWarning", "");// always
			tokens.put("FXInfo", "");// always
			// tokens.put("FXResponse", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			responseXML = reader.toString();

			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			reader.close();
			anInputStream.close();

		} catch (Exception e) {
			logger.error("BackOffice postings exceptions! " + e.getMessage());
			e.printStackTrace();
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
	private String getTIResponseFromBankResponse(String bankResponseXML, String exposureStatus, String fxStatus) {

		// System.out.println("TEST");
		bankResptranId = "";
		String result = "";
		String postingError = "";
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

			postingError = getBankResponseErrorMessage(bankResponseXML);

			InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_BATCH_TI_RESPONSE_TEMPLATE);
			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", bankStatus);
			tokens.put("Error", postingError);
			tokens.put("Warning", "");// always
			tokens.put("Info", "");// always
			tokens.put("CorrelationId", correlationId);

			/** Exposure **/
			if (!exposureStatus.isEmpty())
				tokens.put("ExposureStatus", exposureStatus);
			else
				tokens.put("ExposureStatus", "");
			tokens.put("ExposureError", "");
			tokens.put("ExposureWarning", "");// always
			tokens.put("ExposureInfo", "");// always
			// tokens.put("ExposureResponse", "");

			/** Posting **/
			tokens.put("PostingStatus", bankStatus);
			tokens.put("PostingError", postingError);
			tokens.put("PostingWarning", "");// always
			tokens.put("PostingInfo", "");// always
			// tokens.put("PostingResponse", "");

			/** FXContractDrawdown **/
			if (!fxStatus.isEmpty())
				tokens.put("FXStatus", fxStatus);
			else
				tokens.put("FXStatus", "");
			tokens.put("FXError", "");
			tokens.put("FXWarning", "");// always
			tokens.put("FXInfo", "");// always
			// tokens.put("FXResponse", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();
			anInputStream.close();

			result = tokenReplacedXML;
			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeBatch TIResponse Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

}
