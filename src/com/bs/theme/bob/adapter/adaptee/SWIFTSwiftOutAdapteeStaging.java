package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMS_OUT;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SWIFT_OUT;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_SWIFT;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.bs.theme.bob.adapter.email.EmailAlertServiceFailureUtil;
import com.bs.theme.bob.adapter.sfms.SFMSOutwardMessageAdaptee;
import com.bs.theme.bob.adapter.sfms.SfmsAdviceHandler;
import com.bs.theme.bob.adapter.util.SWIFTMessageUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.listener.mq.MQMessageManager;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.swift.model.ThemeSwiftModel;
import com.bs.themebridge.swift.parser.ThemeSwiftParser;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.JAXBInstanceInitialiser;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.SWIFTSwiftOutXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.messages.SWOPF;
import com.misys.tiplus2.apps.ti.service.messages.SWOPF.Messages;

/**
 * End system communication implementation for Swift Out services is handled in
 * this class.
 * 
 * @author Bluescope
 * 
 */
public class SWIFTSwiftOutAdapteeStaging {

	private final static Logger logger = Logger.getLogger(SWIFTSwiftOutAdapteeStaging.class.getName());

	private String branch = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	// private String billDescMsg = "";
	private String msgFormtType = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String sequenceTotal = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private String service = SERVICE_SWIFT;
	private String billDescMsg = "No bill reference number available. ";

	public SWIFTSwiftOutAdapteeStaging() {
	}

	/**
	 * <p>
	 * Process the Swift out Service XML from the TI
	 * </p>
	 * 
	 * @throws Exception
	 */
	public String process(String requestXML) throws Exception {

		logger.info(" ************ SWIFT.SwiftOut/SFMSOut/IFN staging adaptee process started ************ ");

		String errorMsg = "";
		// String billDescMsg = "";
		String status = "FAILED";

		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
			logger.debug("SwiftOut TI Request :\n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankResponse = getSwiftOutMessage(requestXML);
			logger.debug("SwiftOut Bank Response :\n" + bankResponse);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			tiResponse = getTIResponse(bankResponse);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("SwiftOut TI Response :\n" + tiResponse);

			status = bankResponse;

		} catch (Exception e) {
			status = "FAILED";
			errorMsg = e.getMessage();
			tiResponse = getTIResponse("FAILED");
			logger.error("Swiftout / SFMSOut Exceptions..!! " + errorMsg);
			e.printStackTrace();

		} finally {
			boolean res = ServiceLogging.pushLogData(service, operation, sourceSystem, branch, sourceSystem,
					targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,
					bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", msgFormtType, sequenceTotal,
					false, "0", billDescMsg + errorMsg);

			if (status.equals("FAILED"))
				EmailAlertServiceFailureUtil.sendFailureAlertMail(service, operation, masterReference, eventReference,
						sourceSystem, targetSystem);
		}
		logger.info(" ************ SWIFT.SwiftOut/SFMSOut/IFN staging adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param inputXML
	 * @return
	 */
	public String getSwiftOutMessage(String inputXML) {

		NodeList nodeList;
		Messages bsrList = null;
		Unmarshaller unmarshaller;
		String response = "FAILED";
		JAXBContext jaxbContext = JAXBInstanceInitialiser.getSwiftOutRequestContext();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document document = null;
			dbf.setNamespaceAware(true);
			unmarshaller = jaxbContext.createUnmarshaller();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(inputXML));
			document = builder.parse(inputSource);
			nodeList = document.getElementsByTagNameNS("*", "SwiftOut");
			JAXBElement<SWOPF> bulkServiceJAXB = (JAXBElement<SWOPF>) unmarshaller.unmarshal(nodeList.item(0),
					SWOPF.class);
			SWOPF swiftSourceMsg = bulkServiceJAXB.getValue();
			int swiftMessageSize = swiftSourceMsg.getMessages().getMessage().size();
			logger.debug("SwiftOut message size : " + swiftMessageSize);

			bsrList = swiftSourceMsg.getMessages();
			List<String> swiftMsglist = bsrList.getMessage();
			// reqHeader = getRequestHeader();
			correlationId = ThemeBridgeUtil.getValue(inputXML, SWIFTSwiftOutXpath.CORRELATION);
			// branch = ThemeBridgeUtil.getValue(inputXML,
			// SWIFTSwiftOutXpath.Branch);

			String bankResponseValues = processSwiftMessage(swiftMsglist);
			// logger.debug("response : " + response);

			bankResponseValues = bankResponseValues.replace("|", " ").trim();
			boolean b = bankResponseValues.contains("FAILED");
			if (b)
				response = "FAILED";
			else
				response = "TRANSMITTED";

		} catch (Exception e) {
			logger.error("Get SwiftAdapteeStaging <Messages> exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		return response;
	}

	/**
	 * 
	 * @param swiftMsglist
	 * @return
	 */
	public String processSwiftMessage(List<String> swiftMsglist) {

		String sfmsOutMsg = "";
		String mqResponseStatus = "";
		String swiftAllMessages = "";
		boolean mqueueStatus = false;
		try {
			for (String tiswiftMessage : swiftMsglist) {

				ThemeSwiftParser tsp = new ThemeSwiftParser();
				ThemeSwiftModel tsmBeanObj = tsp.parseSwiftMessage(tiswiftMessage);

				String masterReferenceNum = tsmBeanObj.getTag20Reference();
				masterReference = masterReferenceNum;
				logger.debug("SwiftMasterReference : " + masterReferenceNum);

				String swMsgType = tsmBeanObj.getMessageType();
				logger.debug("SwiftMessageType : " + swMsgType);
				msgFormtType = swMsgType;

				String eventReferenceNum = getEventReferenceNumber(correlationId);
				eventReference = eventReferenceNum;
				logger.debug("SwiftEventReferenceNum : " + eventReferenceNum);

				String sequenceNum = tsmBeanObj.getSequenceOfTotal();
				logger.debug("SequenceOfTotal : " + sequenceNum);
				sequenceTotal = sequenceNum;

				// String senderBIC = tsmBeanObj.getSenderBICCode(); // BIC
				// logger.debug("Sender BIC : " +
				// tsmBeanObj.getSenderBICCode());
				// String receiverBICorIFSC = tsmBeanObj.getReceiverBICCode();
				// logger.debug("Receiver BIC / IFSC : " +
				// tsmBeanObj.getReceiverBICCode());

				/** 2017-08-30 **/
				// String masterReference =
				// getMasterReferenceNumber(tiswiftMessage); // FixedLength
				// logger.debug("masterReferenceNum : " + masterReference);

				Map<String, String> swiftOutMapList = new HashMap<String, String>();
				swiftOutMapList = getswiftMSGType(masterReference, correlationId);
				logger.debug("SwiftOutMapList: " + swiftOutMapList);

				String prdType = swiftOutMapList.get("ProdType");
				String subType = swiftOutMapList.get("subType");
				String subTypeDesc = swiftOutMapList.get("subTYPEDesc");
				String eventcode = swiftOutMapList.get("eventcode");
				// TFLC Main
				String masterRef = swiftOutMapList.get("MASTER_REF");
				String eventRef = swiftOutMapList.get("eventRef");
				String tranCurrency = swiftOutMapList.get("tranCurrency");
				// String refno_pfix = "";
				// String refno_serl = "";
				// if (eventRef != null && !eventRef.isEmpty()) {
				// refno_pfix = eventRef.substring(0, 3);
				// refno_serl = eventRef.substring(3, 6);
				// }

				String billRefNumber = "";
				billRefNumber = getBillRefNumber(masterReferenceNum, eventReferenceNum);
				String sfmsFlag = SWIFTMessageUtil.checkinSFMSFlag(prdType, subType, eventcode);
				logger.debug(
						"SFMSFlag: (" + masterReferenceNum + "(" + masterRef + ")-" + eventRef + ") :- " + sfmsFlag);

				if (sfmsFlag.equals("YES") && tranCurrency.equalsIgnoreCase("INR")) {
					logger.debug(" ************ SWIFT.SFMS process started ************ ");
					operation = OPERATION_SFMS_OUT; // SFMSOut
					SFMSOutwardMessageAdaptee aSFMSAdapteeObj = new SFMSOutwardMessageAdaptee();
					sfmsOutMsg = aSFMSAdapteeObj.pushSFMSMessage(tiswiftMessage, masterReferenceNum, eventRef,
							swMsgType, prdType, billRefNumber); // masterRef
					/** Replace Bill Ref Num **/
					if (billRefNumber != null && !billRefNumber.trim().isEmpty()) {
						if (!prdType.equals("IDC") && !prdType.equals("ODC")) {
							sfmsOutMsg = sfmsOutMsg.replaceAll(masterReferenceNum, billRefNumber);
							billDescMsg = "Tag20 BillRef " + billRefNumber + " replaced on MasterRef "
									+ masterReferenceNum;
						}
					}
					String sfmsOutMQName = ConfigurationUtil.getValueFromKey("SfmsOutMQName");
					String sfmsOutMQJNDIName = ConfigurationUtil.getValueFromKey("SfmsOutMQJndiName");
					mqueueStatus = MQMessageManager.pushMqMessage(sfmsOutMQJNDIName, sfmsOutMQName, sfmsOutMsg);
					logger.debug("Swiftout(SFMS) QueuePushingStatus : " + mqueueStatus);

					/** Advice copy **/
					// SfmsAdviceHandler.adviceHandler(sfmsOutMsg,
					// loggingMsgType, masterRef, eventRef);
					boolean sfmsAdviceMail = SfmsAdviceHandler.adviceHandler(sfmsOutMsg, swMsgType, masterReferenceNum,
							eventRef, sourceSystem, targetSystem);// masterRef

					/** Logging **/
					if (swiftAllMessages.isEmpty())
						swiftAllMessages = sfmsOutMsg;
					else
						swiftAllMessages = swiftAllMessages + "\n$\n" + sfmsOutMsg;
					logger.debug(" ************ SWIFT.SFMS process ended ************ ");

				} else {
					logger.debug(" ************ SWIFT.SwiftOut process started ************ ");
					operation = OPERATION_SWIFT_OUT; // SwiftOut
					String formatSwiftMsg = MQMessageManager.formatSwiftMsg(tiswiftMessage);
					/** Replace Bill Ref Num **/
					if (billRefNumber != null && !billRefNumber.trim().isEmpty()) {
						if (!prdType.equals("IDC") && !prdType.equals("ODC")) {
							formatSwiftMsg = formatSwiftMsg.replaceAll(masterReferenceNum, billRefNumber);
							billDescMsg = "Tag20 BillRef " + billRefNumber + " replaced on MasterRef "
									+ masterReferenceNum;
						}
					}
					String swiftOutMQName = ConfigurationUtil.getValueFromKey("SwiftOutMQName");
					String swiftOutMQJndiName = ConfigurationUtil.getValueFromKey("SwiftOutMQJndiName");
					/** SwiftOut encryption on 04-03-2016 **/
					// String encryptSwiftMsg =
					// SwiftOutEncryption.getSwiftEncryptedMsg(formatSwiftMsg);
					// encryptSwiftMsg);
					mqueueStatus = MQMessageManager.pushMqMessage(swiftOutMQJndiName, swiftOutMQName, formatSwiftMsg);
					logger.debug("Swiftout(SWIFT) QueuePushingStatus : " + mqueueStatus);
					if (swiftAllMessages.isEmpty())
						swiftAllMessages = formatSwiftMsg;
					else
						swiftAllMessages = swiftAllMessages + "\n$\n" + formatSwiftMsg;
					logger.debug(" ************ SWIFT.SwiftOut process ended ************ ");
				}

				if (mqueueStatus)
					mqResponseStatus = mqResponseStatus + " | " + "SUCCEEDED";
				else
					mqResponseStatus = mqResponseStatus + " | " + "FAILED";
			}

			bankRequest = swiftAllMessages;
			// logger.debug("BankRequest " + bankRequest);

		} catch (Exception e) {
			logger.error("Exception while processing SWIFT / SFMS OUT..!!! " + e.getMessage());
			return mqResponseStatus = "FAILED";
		}

		logger.debug("mqResponseStatus : " + mqResponseStatus);
		return mqResponseStatus;
	}

	/**
	 * To get Event reference number
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getEventReferenceNumber(String correlationId) {

		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		String eventRef = "";
		String eventRefQuery = "SELECT A.REFNO_PFIX AS EVENTCODE, A.REFNO_SERL AS EVENTSEQ, (TRIM(A.REFNO_PFIX)||LPAD(TRIM(A.REFNO_SERL),3,'0')) AS EVENTREF FROM BASEEVENT A, RELITEM B, DOCRELITEM C, SWOPF D WHERE A.KEY97 = B.EVENT_KEY AND B.KEY97 = C.KEY97 AND C.KEY97 = D.OWNER AND D.SWORNO = "
				+ correlationId + "";
		logger.debug("SwiftEventRefQuery : " + eventRefQuery);
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(eventRefQuery);
			while (res.next()) {
				eventRef = res.getString("EVENTREF");
			}

		} catch (SQLException e) {
			logger.error("Swift event ref exception! Check the logs for details" + e.getMessage());

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return eventRef;
	}

	/**
	 * To get Event reference number
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static Map<String, String> getEventRefSerialNumber(String correlationId) {
		Map<String, String> eventRefMap = new HashMap<String, String>();
		// eventRefMap.get("Code"), eventRefMap.get("Serial")
		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		String eventCode = "";
		String eventSerial = "";
		String eventRefQuery = "SELECT A.REFNO_PFIX AS EVENTCODE, A.REFNO_SERL AS EVENTSEQ, (TRIM(A.REFNO_PFIX)||LPAD(TRIM(A.REFNO_SERL),3,'0')) AS EVENTREF FROM BASEEVENT A, RELITEM B, DOCRELITEM C, SWOPF D WHERE A.KEY97 = B.EVENT_KEY AND B.KEY97 = C.KEY97 AND C.KEY97 = D.OWNER AND D.SWORNO = "
				+ correlationId + "";
		logger.debug("SwiftEventRefQuery : " + eventRefQuery);
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(eventRefQuery);
			while (res.next()) {
				eventCode = res.getString("EVENTCODE").trim();
				eventSerial = res.getString("EVENTSEQ").trim();
			}
			eventRefMap.put("Code", eventCode);
			eventRefMap.put("Serial", eventSerial);

		} catch (SQLException e) {
			logger.error("Swift event ref exception! Check the logs for details" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return eventRefMap;
	}

	/**
	 * 
	 * @param masterRef
	 * @param sworno
	 * @return
	 */
	public static Map<String, String> getswiftMSGType(String masterRef, String sworno) {

		// logger.debug("Getting Sender & recevier IFSC code started");

		String subType = "";
		Statement stmt = null;
		ResultSet resultSet = null;
		Connection connection = null;
		Map<String, String> ifscResp = new HashMap<String, String>();
		// 1. prd.CODE79 = product
		// 2. pad.role =
		// 3. bev.refno_pfix = event ref no
		// 4. mas.master_ref= master ref
		// 5. sw.SWORNO = corrlation id
		String query = "select distinct trim(mas.ccy) as TRANCURRENCY, trim(p.code) as subType, trim(p.descrip) as subTYPEDesc, "
				+ " trim(prd.code79) as ProdType, trim(MAS.MASTER_REF) as MASTER_REF, trim(BEV.REFNO_PFIX) AS eventcode, "
				+ " trim(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EventREFNO_PFIXSERL "
				+ " from master mas, tidataitem tid, exempl30 prd, prodtype p, baseevent bev, swopf sw, docrelitem dri, relitem rel "
				+ " where mas.exemplar = prd.key97 and mas.key97 = tid.master_key "
				+ "  and mas.key97 = bev.master_key "
				+ " and sw.owner = dri.key97  and dri.key97 = rel.key97  and rel.event_key = bev.key97 "
				+ " and bev.master_key = mas.key97 and mas.prodtype = p.key97 and sw.sworno = '" + sworno + "' ";
		try {
			logger.debug("GetswiftMSGDetails Query-->" + query);

			int i = 0;
			connection = DatabaseUtility.getTizoneConnection();
			stmt = connection.createStatement();
			resultSet = stmt.executeQuery(query);
			while (resultSet.next()) {

				// logger.debug("Count : " + i);
				subType = resultSet.getString("subType");
				ifscResp.put("Status", "SUCCESS");

				ifscResp.put("subType", subType);
				String ProdType = resultSet.getString("ProdType");
				ifscResp.put("ProdType", ProdType);

				String subTYPEDesc = resultSet.getString("subTYPEDesc");
				ifscResp.put("subTYPEDesc", subTYPEDesc);

				String eventcode = resultSet.getString("eventcode");
				ifscResp.put("eventcode", eventcode);

				String MASTER_REF = resultSet.getString("MASTER_REF");
				ifscResp.put("MASTER_REF", MASTER_REF);

				String eventRef = resultSet.getString("EventREFNO_PFIXSERL");
				ifscResp.put("eventRef", eventRef);

				String tranCurrency = resultSet.getString("TRANCURRENCY");
				ifscResp.put("tranCurrency", tranCurrency);

				i = i + 1;
			}

			if (i == 0) {
				logger.debug("No Process in master refno");
				ifscResp.put("subType", "");
				ifscResp.put("subTYPEDesc", "");
				ifscResp.put("ProdType", "");
			}

		} catch (SQLException e) {
			logger.error("SQL Exception..!! " + e.getMessage());
			ifscResp.put("subTYPEDesc", "");
			ifscResp.put("Status", "FILED");
			ifscResp.put("subType", "");
			ifscResp.put("ProdType", "");
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(connection, stmt, resultSet);
		}

		return ifscResp;
	}

	/**
	 * 2017-08-28
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @return
	 */
	public static String getBillRefNumber(String masterRef, String eventRef) {

		ResultSet res = null;
		Connection con = null;
		Statement stmt = null;
		String billRefNumber = null;
		String billRefQuery = "select TRIM(MAS.MASTER_REF) as MASTERREF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) as EVENTREF, TRIM(BLLREFNO) as BLLREFNO "
				+ " from MASTER MAS, BASEEVENT BEV, EXTEVENT EXT where MAS.KEY97 = BEV.MASTER_KEY and BEV.KEY97 = EXT.EVENT "
				+ " and MAS.MASTER_REF = '" + masterRef + "' " + " and BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0) = '"
				+ eventRef + "' ";
		logger.debug("BillRefNumQuery : " + billRefQuery);
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(billRefQuery);
			while (res.next()) {
				billRefNumber = res.getString("BLLREFNO");
			}

		} catch (SQLException e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return billRefNumber;
	}

	/**
	 * To get Bill reference number, OLD but GOOD!!
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getBillRefNumber(String masterRef, String eventCode, String eventSerial) {

		ResultSet res = null;
		Connection con = null;
		Statement stmt = null;
		String billRefNumber = null;
		String eventRefQuery = "select bllrefno from master mas, baseevent bev, extevent ext "
				+ "where mas.key97 = bev.master_key AND bev.key97 = ext.event " + "AND mas.master_ref='" + masterRef
				+ "' " + "AND bev.refno_pfix='" + eventCode + "' AND bev.refno_serl='" + eventSerial + "'";
		logger.debug("GetBillRefNumQuery : " + eventRefQuery);
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(eventRefQuery);
			while (res.next()) {
				billRefNumber = res.getString("bllrefno");
			}

		} catch (SQLException e) {
			logger.error("Exception! Check the logs for details");
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return billRefNumber;
	}

	/**
	 * To get swift message type number, DO NOT USE. But Superb!
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getMessagetype(String swiftMessage) {

		String result = "";
		int indexOfMT = swiftMessage.lastIndexOf("{2:");
		// logger.debug("indexOfMT: " + indexOfMT);
		if (indexOfMT >= 0 && indexOfMT < swiftMessage.length()) {
			result = new StringBuffer(swiftMessage).substring(indexOfMT + 4, indexOfMT + 7);
			result = result.replaceAll("/", "_");
		}
		return result;
	}

	/**
	 * To get Master reference number, DO NOT USE. But Superb! If master
	 * reference length is 16 character only this method will work.
	 * 
	 * @param swiftOutmsg
	 * @return
	 */
	public static String getMasterReferenceNumber(String swiftOutmsg) {

		String result = "";
		// logger.debug("swiftOutmsg : " + swiftOutmsg);
		try {
			int indexOf20 = swiftOutmsg.lastIndexOf("20:");
			// logger.debug("index of -->" + swiftOutmsg.substring(indexOf20));
			// logger.debug("indexOf20 : " + indexOf20);
			if (indexOf20 >= 0 && indexOf20 < swiftOutmsg.length()) {
				result = new StringBuffer(swiftOutmsg).substring(indexOf20 + 3, indexOf20 + 19); // Prasath
				result = result.replaceAll("/", "_");
			}
		} catch (Exception e) {
			logger.error("Exception..! " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("Master Reference Number --->" + result);
		return result;
	}

	/**
	 * To get Master reference number, DO NOT USE. But Superb!. If master
	 * reference length is 16 character only this method will work.
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public String getReferenceNo(String swiftMessage) {

		String tag20Value = "";
		int indexOf20 = swiftMessage.lastIndexOf("20:");
		// logger.debug("index of 20 >>>" + swiftMessage.substring(indexOf20));

		if (indexOf20 >= 0 && indexOf20 < swiftMessage.length()) {
			tag20Value = new StringBuffer(swiftMessage).substring(indexOf20 + 3, indexOf20 + 19);// prasath
			tag20Value = tag20Value.replaceAll("/", "_");
		}

		logger.debug("Reference Number************" + tag20Value);
		return tag20Value;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getTIResponse(String status) {

		String result = "";
		try {
			InputStream anInputStream = SWIFTSwiftOutAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.SWIFT_SWIFTOUT_TI_RESPONSE_TEMPLATE);
			String swiftTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Map<String, String> tokens = new HashMap<String, String>();

			tokens.put("status", status);
			tokens.put("correlationId", correlationId);
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(swiftTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("Exception SWIFT tiresponse!! " + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String a[]) throws Exception {

		// ThemeSwiftParser tsp = new ThemeSwiftParser();
		// ThemeSwiftModel tsmBeanObj = tsp.parseSwiftMessage("");

		SWIFTSwiftOutAdapteeStaging swiftObj = new SWIFTSwiftOutAdapteeStaging();
		// logger.debug(swiftObj.getTIResponse(" | FAILED"));

		// String SfmsOutMsg =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\SFMSMESSAGE.txt");
		// C:\Users\KXT51472\Desktop\SFMSMESSAGE.txt
		// getswiftMSGType("", "16017");

		// String SfmsOutMs = DigitalSignature.signSFMSMessage(SfmsOutMsg);
		// logger.debug("ss \n" + SfmsOutMs);

		// Map<String, String> eventRefMap = getEventRefSerialNumber("27254");

		// logger.debug(eventRefMap.get("Code"));
		// logger.debug(eventRefMap.get("Serial"));

		// logger.debug(getBillRefNumber("0463OCF170200061","CRE",
		// "001"));

		// String swiftTiReq =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\Garbage\\sfms-321\\sfmsout-1.xml");
		// String swiftTiReq = ThemeBridgeUtil
		// .readFile("C:\\Users\\KXT51472\\Downloads\\SFMS\\SFMSOUT
		// message20161226.txt");

		// String swiftTiReq =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\SFMSCODESWIFT\\tiresquest-modified.xml");
		// String resp = swiftObj.process(swiftTiReq);
		// String tag20 = swiftObj.getReferenceNo(swiftTiReq);
		// swiftTiReq=swiftTiReq.replaceAll(tag20, "123****");
		// logger.debug(swiftTiReq);
		// String swiftTiReq =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\tasksfms\\SFMS\\SFMSOUT-UAT1.xml");

		String prdType = "IDC";
		if (!prdType.equals("IDC") && !prdType.equals("ODC")) {
			System.out.println("if " + prdType);
		} else {
			System.out.println("else " + prdType);
		}

		// String swiftTiReq = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\00_TASK\\task
		// swift-ifsc\\Swift-IFSC-Code-tirequest.xml");

		// getswiftMSGType("","15654");

		// String swiftTiReq =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\new277.xml");
		// String resp = swiftObj.process(swiftTiReq);

	}
}
