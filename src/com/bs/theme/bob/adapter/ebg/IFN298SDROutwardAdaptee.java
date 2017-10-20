package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;
import static com.bs.theme.bob.template.util.KotakConstant.TARGET_SYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.GatewayRtgsNeftAdapteeStaging;
import com.bs.theme.bob.adapter.sfms.SFMSInMessageGenerator;
import com.bs.theme.bob.adapter.util.SWIFTMessageUtil;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.listener.mq.MQMessageManager;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.IFNSFMSGatewayXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class IFN298SDROutwardAdaptee {

	private final static Logger logger = Logger.getLogger(IFN298SDROutwardAdaptee.class.getName());

	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private static String branch = "";
	private static String service = "";
	private static String operation = "";
	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String bankRequest = "";
	private static String bankResponse = "";
	private static String eventReference = "";
	private static String correlationId = "";
	private static String masterReference = "";

	/**
	 * 
	 * @param tiRequestXML
	 * @return
	 */
	public String processIFN298SDP(String tiGwRequestXML, String service, String operation) {

		String eventRef = "";
		String masterRef = "";
		String errorMessage = "";
		String ifnMsgStaus = "FAILED";
		String ifn298RequestMQMessage = "";
		try {
			tiRequest = tiGwRequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.e-BG IFN298SDP TI Request : \n" + tiRequest);

			// service = XPathParsing.getValue(tiGwRequestXML,
			// IFNSFMSGatewayXpath.SERVICE);
			// operation = XPathParsing.getValue(tiGwRequestXML,
			// IFNSFMSGatewayXpath.OPERATION);
			correlationId = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.CORRELATIONID);

			masterRef = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.MASTER_REFERENCE);
			eventRef = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.EVENT_REFERENCE);
			branch = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.BEHALF_OF_BRANCH);
			String eventRefnoPfix = "";
			String eventRefnoSerl = "";
			if (!eventRef.isEmpty() && eventRef.length() > 5) {
				eventRefnoPfix = eventRef.substring(0, 3);
				eventRefnoSerl = eventRef.substring(3, 6);
			}
			eventReference = eventRef;
			masterReference = masterRef;
			logger.debug("IFN298SDP Request Reference : " + masterRef + "-" + eventRef + "\t" + eventRefnoPfix
					+ eventRefnoSerl);

			// Get IFN Request message
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			ifn298RequestMQMessage = getIFN298SDPSFMSMessage(masterRef, eventRef, tiGwRequestXML);
			bankRequest = ifn298RequestMQMessage;
			logger.debug("GATEWAY.e-BG IFN298SDP Bank Request : \n" + bankRequest);

			// Push to MQ
			String ifnOutMQName = ConfigurationUtil.getValueFromKey("IFN298OUTMQName");// SfmsOutMQName
			String ifnOutMQJNDIName = ConfigurationUtil.getValueFromKey("IFN298OUTMQJndiName");// SfmsOutMQJndiName
			// MQMessageManager mqmanagerObj = new MQMessageManager();
			boolean ifn298OutQueuePostingStatus = MQMessageManager.pushMqMessage(ifnOutMQJNDIName, ifnOutMQName,
					ifn298RequestMQMessage);
			if (ifn298OutQueuePostingStatus) {
				ifnMsgStaus = ThemeBridgeStatusEnum.TRANSMITTED.toString();
			} else {
				ifnMsgStaus = ThemeBridgeStatusEnum.FAILED.toString();
			}
			bankResponse = ifnMsgStaus;
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.e-BG IFN298SDP Bank Response : " + bankResponse);

			tiResponse = getTIResponse(ifnMsgStaus);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.e-BG IFN298SDP TI Response : " + tiResponse);

		} catch (Exception e) {
			ifnMsgStaus = "FAILED";
			errorMessage = e.getMessage();
			logger.error("IFN298SDP Processing Exceptions..! " + errorMessage);
			tiResponse = getTIResponse(ifnMsgStaus);

		} finally {
			// service logging, "SFMSOut_EBGIFN298R", "SWIFT", "EBGIFN298R"
			ServiceLogging.pushLogData(service, operation, SOURCE_SYSTEM, branch, SOURCE_SYSTEM, TARGET_SYSTEM,
					masterRef, eventRef, ifnMsgStaus, tiRequest, tiResponse, bankRequest, "MQ status : " + bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "Cover", "298SDR", "1/1", false, "0",
					errorMessage);
		}

		return tiResponse;
	}

	/**
	 * I . FIRST
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @param refno_pfix
	 *            {@code allows }{@link String}
	 * @param refno_serl
	 *            {@code allows }{@link String}
	 * @param masterReferenceNum
	 *            {@code allows }{@link String}
	 * @param messageType
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getIFN298SDPSFMSMessage(String masterRef, String eventRef, String tiGwRequestXML) {

		// logger.debug("refno_pfix, refno_serl, masterRef" + eventRefnoPfix +
		// eventRefnoSerl + masterRef);

		String IFN298SFMSMessage = "";
		try {
			// SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
			// Map<String, String> senderRece =
			// generator.getSwiftBicifscCode(masterRef, eventRefPfix,
			// eventRefSerl);
			Map<String, String> senderRece = SFMSInMessageGenerator.getSenderReceiverIfscCode(masterRef, eventRef);
			String senderIFSC = senderRece.get("senderIFSC");
			String receiverIFSC = senderRece.get("receiverIFSC");

			// II
			String sfmsIfn298Header = getSFMSIFN298SDPRequestHeader(masterRef, eventRef, senderIFSC, receiverIFSC);
			// logger.debug("SFMSIFN298 Header " + sfmsIfn298Header);

			// III
			String sfmsIfn298Message = getSFMSIFN298SDPRequestBody(sfmsIfn298Header, masterRef, eventRef, senderIFSC,
					tiGwRequestXML);
					// logger.debug("SFMSIFN298 Message " + sfmsIfn298Message);

			// IV
			try {
				// SFMSMessage =
				// DigitalSignature.signSFMSMessage(sfmsIfn298Message);

			} catch (Exception e) {
				e.printStackTrace();
				logger.error("DigitalSignature signSFMSMessage exceptions..!");
			}

			// V
			IFN298SFMSMessage = sfmsIfn298Message;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("IFN 298 SDP SFMS Message generation exceptions..! " + e.getMessage());
		}

		// logger.debug("IFN298 SDP OUT : " + IFN298SFMSMessage);
		return IFN298SFMSMessage;
	}

	/**
	 * II . SECOND
	 * 
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventpfx
	 *            {@code allows }{@link String}
	 * @param eventserl
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSFMSIFN298SDPRequestHeader(String masterRef, String eventRef, String senderIFSC,
			String receiverIFSC) {

		// Sender Bank application identifier -- (APP) //It's not constant, may
		// be it will change.
		StringBuffer stringBuf = new StringBuffer("{A:");

		try {
			// get receiver IFSC Code from message
			// String receiverIFSC = m.getReceiver();

			// Prodcut confirmation
			stringBuf.append("BGS"); // always

			// - Doc OK
			String msgIdent = "F01";
			stringBuf.append(msgIdent);// always

			// Input/output Identifier (either I or O) - Doc OK
			stringBuf.append("O"); // always

			// Message type - Doc OK
			stringBuf.append("298"); // always

			// Sub Message Type ( For IFN 298C01, this field should be C01, for
			// IFN100 message, this field should be XXX) - Doc OK
			String subMT = "SDP";
			stringBuf.append(subMT);

			// below code written by subhash

			/*******************************************************/
			// logger.debug("stringBuf " + stringBuf.toString());
			if (senderIFSC != null && !senderIFSC.isEmpty()) {
				stringBuf.append(senderIFSC);
			} else {
				stringBuf.append("XXXXXXXXXXX");
			}
			// logger.debug("stringBuf " + stringBuf.toString());

			if (receiverIFSC != null && !receiverIFSC.isEmpty()) {
				stringBuf.append(receiverIFSC);
			} else {
				stringBuf.append("XXXXXXXXXXX");
			}
			// logger.debug("stringBuf " + stringBuf.toString());
			/*******************************************************/

			// Delivery Monitoring flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Open Notification flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Non-delivery Warning flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Obsolescence Period - Doc OK
			// If Non-delivery warning flag is 2, then this value should be set
			// to ‘000’.
			// If Non-delivery warning flag is 1, then this value should be set
			// to ‘002’.
			stringBuf.append("000");

			// Message User Reference (MUR) - Doc OK
			String murPrefix = "KKBK";
			long murSuffix = ThemeBridgeUtil.generateRandom(12);
			stringBuf.append(murPrefix + String.valueOf(murSuffix));
			// logger.debug("MUR : " + murPrefix + String.valueOf(murSuffix));

			// Possible Duplicate flag - Doc OK
			stringBuf.append("2");

			// Service Identifier 3 digit - Doc OK
			stringBuf.append("BGS"); // always

			// Originating date YYYYMMDD - Doc OK
			String Originatingdate = SWIFTMessageUtil.getSFMSDate();
			stringBuf.append(Originatingdate);// alway current date YYYYMMDD

			// Originating time HHMM - Doc OK
			String hourMins = SWIFTMessageUtil.getHourMins();
			stringBuf.append(hourMins); // always curreny Time HHMM

			// Authorization flag - Doc OK
			stringBuf.append("2"); // 1

			// Testing and training flag - Doc OK
			stringBuf.append("2"); // 1

			// Sequence Number - Doc OK
			stringBuf.append(ThemeBridgeUtil.generateRandom(8));

			// Filler for future use and default
			String Filler = "XXXXXXXXX";
			stringBuf.append(Filler); // always

			// Unique Transaction Reference.masterRef
			// String TransRef = "XXXXXXXXXXXXXXXX";
			stringBuf.append(masterRef);

			// Priority Flag - Urgent, High, Normal, Low
			// 00, 99
			stringBuf.append("99"); // always

			// Final SFMS Header tag close
			stringBuf.append("}");

			// Body
			stringBuf.append("{4:");

		} catch (Exception e) {
			logger.error("Get IFN-298 SFMS Header Exception!! ");
			e.printStackTrace();
		}

		return stringBuf.toString();
	}

	/**
	 * III . THIRD
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSFMSIFN298SDPRequestBody(String swiftMessage, String masterReference, String eventRef,
			String senderIFSC, String tiGwRequestXML) {

		// logger.error("GetSFMSBody " + swiftMessage);
		String fin298Body = "";

		try {
			StringBuilder ifnmsgBody = new StringBuilder();

			String stateCode = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.STATE_CODE);
			String dateOfPayment = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.DATEOF_PAYMENT);
			String amountCCY = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.AMOUNT_CCY);
			// String amount = XPathParsing.getValue(tiGwRequestXML,
			// IFNSFMSGatewayXpath.AMOUNT);
			// String amount = XPathParsing.getValue(tiGwRequestXML,
			// IFNSFMSGatewayXpath.AMOUNT);
			// amount = amount.replaceAll("[^0-9.]", ""); // <<ORA,v,a>>
			String currency = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.CURRENCY);
			String sendingParty = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.SENDING_PARTYNAME);
			String receivingParty = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.RECEIVING_PARTYNAME);
			String stampDutyPaidBy = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.STAMPDUTY_PAIDBY);
			String amountPaid = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.AMOUNT_PAID);
			amountPaid = amountPaid.replaceAll("[^0-9.]", "");
			String articleNumber = XPathParsing.getValue(tiGwRequestXML, IFNSFMSGatewayXpath.ARTICLE_NUMBER);
			// String senderIFSC = XPathParsing.getValue(tiGwRequestXML,
			// IFNSFMSGatewayXpath.SENDER_IFSC);

			ifnmsgBody.append(swiftMessage);

			if (ValidationsUtil.isValidString(masterReference)) {
				ifnmsgBody.append("\r\n" + ":7020:" + masterReference);
			}
			if (ValidationsUtil.isValidString(masterReference)) {
				ifnmsgBody.append("\r\n" + ":7021:" + eventRef);
			}
			if (ValidationsUtil.isValidString(stateCode)) {
				ifnmsgBody.append("\r\n" + ":7044:" + stateCode);
			}
			if (ValidationsUtil.isValidString(dateOfPayment)) {
				ifnmsgBody.append("\r\n" + ":7046:" + dateOfPayment); // YYYYMMDD
			}
			if (ValidationsUtil.isValidString(senderIFSC)) {
				ifnmsgBody.append("\r\n" + ":7031:" + senderIFSC); // IFSC
			}
			if (ValidationsUtil.isValidString(amountCCY)) {
				// ifnmsgBody.append("\r\n" + ":7025:" + currency + amount);
				ifnmsgBody.append("\r\n" + ":7025:" + amountCCY); // USD123.32
			}
			// ifnmsgBody.append("\r\n" + ":7050:" + sendingParty);
			if (ValidationsUtil.isValidString(sendingParty)) {
				ifnmsgBody.append("\r\n" + ":7051:" + sendingParty);
			}
			if (ValidationsUtil.isValidString(receivingParty)) {
				ifnmsgBody.append("\r\n" + ":7052:" + receivingParty);
			}
			if (ValidationsUtil.isValidString(stampDutyPaidBy)) {
				ifnmsgBody.append("\r\n" + ":7053:" + stampDutyPaidBy);
			}
			if (ValidationsUtil.isValidString(amountPaid)) {
				ifnmsgBody.append("\r\n" + ":7043:" + amountPaid);// 12356.23
			}
			if (ValidationsUtil.isValidString(articleNumber)) {
				ifnmsgBody.append("\r\n" + ":7045:" + articleNumber);
			}

			ifnmsgBody.append("\r\n" + "-}");

			// logger.debug("ifnmsgBody : " + ifnmsgBody.toString());

			fin298Body = ifnmsgBody.toString();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("GetSFMSBody Exceptions! " + e.getMessage());
		}

		// logger.error("GetSFMSBody return " + fin298Body);
		return fin298Body;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getTIResponse(String status) {

		String result = "";
		InputStream anInputStream = null;
		try {
			anInputStream = GatewayRtgsNeftAdapteeStaging.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.GATEWAY_DOCUMENTS_TI_RESPONSE_TEMPLATE);

			String swiftTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("operation", operation);
			tokens.put("status", status);
			tokens.put("correlationId", correlationId);
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(swiftTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();

			reader.close();

		} catch (IOException e) {
			logger.error("IOException..! " + e.getMessage());
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

	public static void main(String[] args) throws Exception {

		IFN298SDROutwardAdaptee ifAn = new IFN298SDROutwardAdaptee();

		// String tiGwRequestXML =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\task-sfms-e-BG\\EBGISSUE.xml");
		// String tiGwRequestXML = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task-sfms-e-BG\\e-BG760Test2.xml");
		String tiGwRequestXML = ThemeBridgeUtil
				.readFile("D:\\_Prasath\\Filezilla\\task\\task-sfms-e-BG\\eBgIssue.ti.request.xml");

		String get = ifAn.processIFN298SDP(tiGwRequestXML, "", "");
		logger.debug("IFNMESSAGE >>> \n" + get);
	}
}
