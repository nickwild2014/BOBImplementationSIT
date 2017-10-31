package com.bs.theme.bob.adapter.sfms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.util.SWIFTMessageUtil;
import com.bs.themebridge.listener.mq.MQMessageManager;
import com.bs.themebridge.swift.model.ThemeSwiftModel;
import com.bs.themebridge.swift.parser.ThemeSwiftParser;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.prowidesoftware.swift.io.ConversionService;
import com.prowidesoftware.swift.model.SwiftMessage;

public class SFMSOutwardMessageAdaptee {

	private final static Logger logger = Logger.getLogger(SFMSOutwardMessageAdaptee.class.getName());

	public static void main(String[] args) throws Exception {

		SFMSOutwardMessageAdaptee aMsg = new SFMSOutwardMessageAdaptee();
		String swiftOutMsg = ThemeBridgeUtil
				// .readFile("D:\\_Prasath\\00_TASK\\sfms printer
				// friendly\\01SWIFT-Outward.txt");//
				.readFile("D:\\_Prasath\\00_TASK\\sfms printer friendly\\Jira2917.swift.txt");

		aMsg.getSFMSOutMessage(swiftOutMsg, "0958TRF170200081", "ITX003", "720", "ELF");
		// logger.debug(getSfmsIfscMapping("CNRBXY04098"));
	}

	public String pushSFMSMessage(String tiswiftMessage, String masterRef, String eventRef, String swMsgType,
			String prdType, String billRefNumber) {

		String sfmsOutMsg = "";
		try {
			sfmsOutMsg = getSFMSOutMessage(tiswiftMessage, masterRef, eventRef, swMsgType, prdType);

			/** Replace Bill reference **/
			// sfmsOutMsg = sfmsOutMsg.replaceAll(masterRef, billRefNumber);

			/** Pushing MQ **/
			// String sfmsOutMQName =
			// ConfigurationUtil.getValueFromKey("SfmsOutMQName");
			// String sfmsOutMQJNDIName =
			// ConfigurationUtil.getValueFromKey("SfmsOutMQJndiName");
			// boolean mqueueStatus =
			// MQMessageManager.pushMqMessage(sfmsOutMQJNDIName, sfmsOutMQName,
			// sfmsOutMsg);
			// logger.debug("Swiftout(SFMS) QueuePostingStatus : " +
			// mqueueStatus);
			// logger.debug("Swiftout(SFMS) SfmsOutMsg : " + sfmsOutMsg);

			/** Advice copy **/
			// boolean sfmsAdviceMail =
			// SfmsAdviceHandler.adviceHandler(sfmsOutMsg, swMsgType, masterRef,
			// eventRef);

		} catch (Exception e) {
			logger.error("" + e.getMessage());
			e.printStackTrace();
		}

		return sfmsOutMsg;
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
	public String getSFMSOutMessage(String tiswiftMessage, String masterRef, String eventRef, String messageType,
			String prdType) {

		// event_refno_pfix, event_refno_serl
		// logger.debug(" masterRef, refno_pfix, refno_serl, messageType" +
		// masterRef + eventRef + messageType);
		String senderIFSC = "";
		String receiverIFSC = "";

		/** MiSYS new patch **/
		ThemeSwiftParser tsp = new ThemeSwiftParser();
		ThemeSwiftModel tsmBeanObj = tsp.parseSwiftMessage(tiswiftMessage);

		// IFSC without terminator - 11
		String swiftsenderBIC = tsmBeanObj.getSenderBICCode();
		logger.debug("swiftsender(BIC) : " + swiftsenderBIC);

		// IFSC without terminator - 11
		String swiftreceiverIFSC = tsmBeanObj.getReceiverBICCode();
		// Get IFSC with master
		senderIFSC = SFMSInMessageGenerator.getSwiftSenderIFSC(masterRef);
		logger.debug("SwiftCompatible ( senderIFSC w/o terminator ) :- " + senderIFSC
				+ ", ( receiverIFSC w/o terminator ) :- " + swiftreceiverIFSC);

		// String bankCode = swiftreceiverIFSC.substring(0, 4);
		// String countryCode = "00"; // always hard coded
		// String branchCode = swiftreceiverIFSC.substring(6, 11);
		// receiverIFSC = bankCode + countryCode + branchCode;
		// logger.debug("ReceiverIFSC w/o terminator : " + bankCode +
		// countryCode + branchCode);
		/** MiSYS new patch **/

		/** **/
		// 2017-Aug-02
		String receivedCountryCode = swiftreceiverIFSC.substring(4, 6);
		if (receivedCountryCode.equals("XY") || receivedCountryCode.equals("XZ"))
			receiverIFSC = getSfmsBicIfscMapping(swiftreceiverIFSC);
		logger.debug("ReceiverIFSC(new) w/o terminator : " + receiverIFSC);

		/** Value from Customization Pane **/
		// SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
		// Map<String, String> senderRece =
		// generator.getSwiftBicifscCode(masterRef, event_refno_pfix,
		// event_refno_serl);
		// receiverIFSC = senderRece.get("receiverIFSC");
		// senderIFSC = senderRece.get("senderIFSC");
		// logger.debug("Customized field ( senderIFSC ) : " + senderIFSC + ", (
		// receiverIFSC ) : " + receiverIFSC);
		/** Value from Customization Pane **/

		String sFMSMessage = "";
		try {
			// II
			String SFMSHeader = getSFMSHeader(masterRef, eventRef, messageType, prdType, senderIFSC, receiverIFSC);
			logger.debug("SFMSHeader :- " + SFMSHeader);

			// III
			String SFMSBody = getSFMSBody(tiswiftMessage, messageType);
			// logger.debug("SFMSBody " + SFMSBody);

			sFMSMessage = SFMSHeader + SFMSBody;
			// logger.debug("SFMSMessage " + SFMSMessage);

			// IV
			sFMSMessage = updateSfmsBodyBicIntoIfsc(sFMSMessage, messageType, masterRef, eventRef, senderIFSC,
					receiverIFSC, swiftsenderBIC);
			logger.debug("Final SFMSMessage:- " + sFMSMessage);

			// V ? COV message ?
			/** DIGITAL SIGNATURE **/
			//String isSignatureRequired = ConfigurationUtil.getValueFromKey("DigitalSignature");
			//logger.debug("isSignatureRequired : " + isSignatureRequired);
//			if (isSignatureRequired.equalsIgnoreCase("YES")) {
//				logger.debug("YESYES");
//				// sFMSMessage =
//				// DigitalSignature.signSFMSMessage(sFMSMessage);
//				sFMSMessage = SignarSignature.signSFMSMessage(sFMSMessage);
//			}
			// sFMSMessage = SignarSignature.signSFMSMessage(sFMSMessage);

		} catch (Exception e) {
			logger.error("SFMS Message generation exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		return sFMSMessage;
	}

	/**
	 * II . SECOND
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventpfx
	 *            {@code allows }{@link String}
	 * @param eventserl
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSFMSHeader(String masterRef, String eventRef, String messageType, String prdType,
			String senderIFSC, String receiverIFSC) {

		// String eventpfx, String eventserl,

		// Sender Bank application identifier -- (APP) //It's not constant, may
		// be it will change.
		// - Doc OK
		StringBuffer stringBuf = new StringBuffer("{A:");

		try {
			/** Commented for new approach **/
			// String messageType = "";
			// SwiftMessage m = (new
			// ConversionService()).getMessageFromFIN(swiftMessage);
			// messageType = m.getType();

			// get receiver IFSC Code from message
			// String receiverIFSC = m.getReceiver();

			// String ServiceIdentifier = "";
			// try {
			// Map<String, String> anPropertiesMap =
			// SWIFTMessageUtil.getSFMSPropertiesValue(messageType);
			// ServiceIdentifier = anPropertiesMap.get("serviceIdentifier");
			// logger.debug("ServiceIdentifier : " + ServiceIdentifier);
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// logger.debug("ServiceIdentifierExceptions! " + e.getMessage());
			// }

			/** Prodcut confirmation **/
			// stringBuf.append(ServiceIdentifier);

			/** Prodcut confirmation **/
			String product = "";
			if (prdType.equals("IGT") || prdType.equals("EGT") || prdType.equals("SHG")) {
				product = "BGS";
			} else {
				product = "ILC";
			}
			stringBuf.append(product);

			// - Doc OK
			String msgIdent = "F01";
			stringBuf.append(msgIdent);

			// Input/output Identifier (either I or O) - Doc OK
			stringBuf.append("O");

			// Message type - Doc OK
			// SwiftMessage m = (new
			// ConversionService()).getMessageFromFIN(swiftMessage);
			// messageType = m.getType();
			stringBuf.append(messageType);

			// Sub Message Type ( For IFN 298C01, this field should be C01, for
			// IFN100 message, this field should be XXX) - Doc OK
			String subMT = "XXX";
			stringBuf.append(subMT);

			// below code written by subhash

			/******************************************/
			/** Commented for new approach **/
			// String senderIFSC = "";
			// String receiverIFSC = "";
			// SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
			// Map<String, String> senderRece =
			// generator.getSwiftBicifscCode(masterRef, eventpfx, eventserl);
			// receiverIFSC = senderRece.get("receiverIFSC");
			// senderIFSC = senderRece.get("senderIFSC");
			logger.debug("Real SenderIFSC  :- " + senderIFSC);
			logger.debug("Real ReceiverIFSC  :- " + receiverIFSC);

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
			/******************************************/

			// Delivery Monitoring flag YES-1, NO-2 - Doc OK
			stringBuf.append("1");

			// Open Notification flag - Doc OK
			stringBuf.append("1");

			// TODO Doubt - Not required as per document
			// Non-delivery Warning flag
			stringBuf.append("2");

			// Obsolescence Period - Doc OK
			stringBuf.append("000");

			// Message User Reference (MUR) - Doc OK
			// long MUR = 0;
			// try {
			// MUR = ThemeBridgeUtil.generateRandom(16);
			// } catch (Exception e) {
			// }
			// stringBuf.append(String.valueOf(MUR) + "");

			// Message User Reference (MUR) - Doc OK
			String murPrefix = "TI";
			long murSuffix = ThemeBridgeUtil.generateRandom(14);
			stringBuf.append(murPrefix + String.valueOf(murSuffix));
			// logger.debug("MUR : " + murPrefix + String.valueOf(murSuffix));

			// Possible Duplicate flag - Doc OK
			stringBuf.append("2");

			// Service Identifier 3 digit - Doc OK
			// stringBuf.append(ServiceIdentifier);
			stringBuf.append(product);

			// Originating date YYYYMMDD - Doc OK
			String Originatingdate = SWIFTMessageUtil.getSFMSDate();
			stringBuf.append(Originatingdate);

			// Originating time HHMM - Doc OK
			String hourMins = SWIFTMessageUtil.getHourMins();
			stringBuf.append(hourMins);

			// Authorization flag -Testing and training flag - Doc OK
			stringBuf.append("2");

			// Testing and training flag - Doc OK
			stringBuf.append("2");// 1 Testing, 2 Product

			// Sequence Number - Doc OK
			stringBuf.append(ThemeBridgeUtil.generateRandom(8));

			// Filler
			String Filler = "XXXXXXXXX";
			stringBuf.append(Filler);

			// Unique Transaction Reference.masterRef
			// String TransRef = "XXXXXXXXXXXXXXXX";
			// stringBuf.append(TransRef);
			// TransRef = getReferenceNo(swiftMessage);
			stringBuf.append(masterRef);

			// Priority Flag - Urgent, High, Normal, Low
			// 00, 99
			stringBuf.append("99");

			// Final SFMS Header tag close
			stringBuf.append("}");

		} catch (Exception e) {
			logger.error("Get SFMS Header Exception!! " + e.getMessage());
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
	public static String getSFMSBody(String tiswiftMessage, String messageType) {

		// logger.error("GetSFMSBody started: " + tiswiftMessage);
		String SFMSBody = "";
		try {
			Map<String, String> propertiesMap = SWIFTMessageUtil.getSFMSPropertiesValue(messageType);
			String SFMSDateTag = propertiesMap.get("DateTag");
			String[] prodDateKeys = SFMSDateTag.split("\\|");
			tiswiftMessage = SWIFTMessageUtil.changeSWIFTDate(prodDateKeys, tiswiftMessage);
			// logger.info("swiftMessage " + tiswiftMessage);
			try {
				SFMSBody = tiswiftMessage.substring(tiswiftMessage.indexOf("{4"), tiswiftMessage.length());

			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("GetSFMSBody Exceptions! " + e.getMessage());
		}

		// logger.error("GetSFMSBody return " + SFMSBody);
		return SFMSBody;
	}

	/**
	 * IV . FOURTH - SUBHASH
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @param MsgType
	 *            {@code allows }{@link String}
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventPriFix
	 *            {@code allows }{@link String}
	 * @param serl
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String updateSfmsBodyBicIntoIfsc(String swiftMessage, String MsgType, String masterRef,
			String eventRef, String senderIFSC, String receiverIFSC, String swiftsenderBIC) {
		// String eventPriFix, String serl,
		try {
			SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
			String SwiftSFMSDateFormat = "";
			SwiftSFMSDateFormat = "41A|42A|50A|51A|52A|53A|54A|56A|57A|58A|59A";
			logger.debug("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);

			// SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
			String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");

			/** Removed - created by Subhash **/
			// Map<String, String> map =
			// generator.getSwiftBodyBicifscCode(masterRef, eventPriFix, serl);
			/** New added by Prasath **/
			// Map<String, String> map =
			// generator.getSwiftBicifscCode(masterRef, eventPriFix, serl);

			for (String tag : prodDateKeys) {

				// logger.debug("Date Tag -->" + tag);
				String tagName = tag + ":";
				String value = m.getBlock4().getTagValue(tag);
				if (value != null && !value.isEmpty())
					logger.debug("Key-->" + tagName + "\tvalue -->" + value);

				if (value != null && !value.equals("")) {
					/** Below two line are commented for NEW approach **/
					// String senderIFSC = map.get("senderIFSC");
					// String receiverIFSC = map.get("receiverIFSC");

					/** Existing **/
					// logger.debug("SFMS ReceiverIfsc :>" + receiverIFSC);
					// String valueChange = "XXXXXXXXXXX";
					// if (ValidationsUtil.isValidString(receiverIFSC)) {
					// valueChange = receiverIFSC;
					// } else {
					// valueChange = "XXXXXXXXXXX";
					// }
					// /** 41A Receiver IFSC code **/
					// if (tag.equals("41A")) {
					// valueChange = valueChange + value.substring(11,
					// value.length());
					// logger.debug("41A valueChange " + valueChange);
					// }
					// swiftMessage =
					// ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage,
					// tagName + value,
					// tagName + valueChange);

					if (tag.equals("41A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						// logger.debug("countryCodeDup : " + countryCodeDup);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value41AChange = bankCode + countryCode + branchCode;
						value41AChange = value41AChange + value.substring(11, value.length());
						logger.debug("41A w/o terminator : " + bankCode + countryCode + branchCode);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value41AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("41A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					if (tag.equals("42A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value42AChange = bankCode + countryCode + branchCode;
						value42AChange = value42AChange + value.substring(11, value.length());
						logger.debug("42A w/o terminator : " + bankCode + countryCode + branchCode);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value42AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("42A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 51A **/
					if (tag.equals("51A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value51AChange = bankCode + countryCode + branchCode;
						value51AChange = value51AChange + value.substring(11, value.length());
						logger.debug("51A w/o terminator : " + value51AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value51AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("51A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 52A **/
					if (tag.equals("52A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value52AChange = bankCode + countryCode + branchCode;
						value52AChange = value52AChange + value.substring(11, value.length());
						// logger.debug("52A existing : " + tagName + value);
						logger.debug("52A w/o terminator : " + value52AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value52AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("52A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 53A **/
					if (tag.equals("53A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value53AChange = bankCode + countryCode + branchCode;
						value53AChange = value53AChange + value.substring(11, value.length());
						// logger.debug("53A existing : " + tagName + value);
						logger.debug("53A w/o terminator : " + value53AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value53AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("53A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 54A **/
					if (tag.equals("54A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value54AChange = bankCode + countryCode + branchCode;
						value54AChange = value54AChange + value.substring(11, value.length());
						// logger.debug("54A existing : " + tagName + value);
						logger.debug("54A w/o terminator : " + value54AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value54AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("54A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 58A **/
					if (tag.equals("58A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value58AChange = bankCode + countryCode + branchCode;
						value58AChange = value58AChange + value.substring(11, value.length());
						// logger.debug("58A existing : " + tagName + value);
						logger.debug("58A w/o terminator : " + value58AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value58AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("58A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/********************************************/

					/** 50A **/
					if (tag.equals("50A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value50AChange = bankCode + countryCode + branchCode;
						value50AChange = value50AChange + value.substring(11, value.length());
						// logger.debug("50A existing : " + tagName + value);
						logger.debug("50A w/o terminator : " + value50AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value50AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("50A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 56A **/
					if (tag.equals("56A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value56AChange = bankCode + countryCode + branchCode;
						value56AChange = value56AChange + value.substring(11, value.length());
						// logger.debug("56A existing : " + tagName + value);
						logger.debug("56A w/o terminator : " + value56AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value56AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("56A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 57A **/
					if (tag.equals("57A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value57AChange = bankCode + countryCode + branchCode;
						value57AChange = value57AChange + value.substring(11, value.length());
						// logger.debug("57A existing : " + tagName + value);
						logger.debug("57A w/o terminator : " + value57AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value57AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("57A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

					/** 59A **/
					if (tag.equals("59A")) {
						String bankCode = value.substring(0, 4);
						String countryCodeDup = value.substring(4, 6);
						String countryCode = "00"; // always hard coded
						String branchCode = value.substring(6, 11);
						String value59AChange = bankCode + countryCode + branchCode;
						value59AChange = value59AChange + value.substring(11, value.length());
						// logger.debug("59A existing : " + tagName + value);
						logger.debug("59A w/o terminator : " + value59AChange);
						if (countryCodeDup.equals("XY") || countryCodeDup.equals("XZ")) {
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + value59AChange);
						} else { // if (swiftsenderBIC.equals(value))
							logger.debug("59A {" + value + " / " + senderIFSC + "}");
							senderIFSC = senderIFSC + value.substring(11, value.length());
							swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + value,
									tagName + senderIFSC);
						}
					}

				}
				tagName = "";
				value = "";
			}

		} catch (Exception e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("updateSFMSBodyDateFormat swiftMessage -->" +
		// swiftMessage);
		return swiftMessage;

	}

	/**
	 * 
	 * @param prdType
	 *            {@code allows }{@link String}
	 * @param eventcode
	 *            {@code allows }{@link String}
	 * @param subType
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String checkinSFMSFlag(String prdType, String eventcode, String subType) {

		int count = 0;
		String flag = "";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;

		try {
			String checkinSFMSFlagQuery = "Select Count(*) as SFMSFLAG  from EXTSFMSPRMA where PROTY = '" + prdType
					+ "'and PRSUB = '" + subType + "'  and EVENT = '" + eventcode + "' ";
			logger.debug("CheckinSFMSFlag : " + checkinSFMSFlagQuery);

			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(checkinSFMSFlagQuery);

			if (rs.next()) {
				count = rs.getInt("SFMSFLAG");
			}
			if (count > 0) {
				flag = "YES";
			} else {
				flag = "NO";
			}

		} catch (SQLException e) {
			logger.error("SQLException!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, null, rs);
		}

		logger.debug("SFMS Flag >>-->>" + flag);
		return flag;
	}

	/**
	 * swift message is swift or not
	 * 
	 * @param prdType
	 *            {@code allows }{@link String}
	 * @param eventcode
	 *            {@code allows }{@link String}
	 * @param subType
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */

	public String getSWFIT(String swiftMessage) {

		logger.debug("getSWFIT()");
		String swiftInMsg = "";
		try {
			String swiftHeader = getSWIFTHeader(swiftMessage);
			String swiftBody = getSWIFTBody(swiftMessage);
			swiftInMsg = swiftHeader + swiftBody;
			// logger.info("swiftInMsg >>>" + swiftInMsg);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return swiftInMsg;
	}

	/**
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSWIFTHeader(String swiftMessage) {

		String serviceID = "";
		String messageType = "";
		String applicationID = "";
		String senderIFSC = "";
		String receiverIFSC = "";
		try {
			messageType = SWIFTMessageUtil.getSwiftInMsgType(swiftMessage);
			senderIFSC = SWIFTMessageUtil.getsenderIFSC(swiftMessage);
			receiverIFSC = SWIFTMessageUtil.getreceiverIFSC(swiftMessage);
			applicationID = SWIFTMessageUtil.getapplicationID(swiftMessage);
			serviceID = SWIFTMessageUtil.getserviceID(swiftMessage);

		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("Exceptions! " + e.getMessage());
		}

		logger.debug("senderIFSC :>" + senderIFSC);
		logger.debug("receiverIFSC :>" + receiverIFSC);
		// block 1
		StringBuffer SWIFTHeader = new StringBuffer("{1:");
		// Application ID
		SWIFTHeader.append(applicationID);
		// Service ID
		SWIFTHeader.append(serviceID);
		// Sender IFSC Code
		SWIFTHeader.append(senderIFSC);
		// Session number. It is generated by the user's computer and is padded
		// with zeros
		SWIFTHeader.append("0");
		SWIFTHeader.append(SWIFTMessageUtil.generateRandom(3));
		// Sequence number that is generated by the user's computer. It is
		// padded with zeros.
		SWIFTHeader.append(SWIFTMessageUtil.generateRandom(6) + "}");

		// block 2
		SWIFTHeader.append("{2:O" + messageType);
		String date = SWIFTMessageUtil.getCurrentDate();// SWIFTMessageUtil.getCurrentDateofTISystem();
		String hhmmStr = SWIFTMessageUtil.getCurentformatTime("HHMM");
		String tempdate = hhmmStr + date;
		// Date and Time
		SWIFTHeader.append(tempdate);
		// Receiver IFSC Code
		SWIFTHeader.append(receiverIFSC);
		// random 10 digit
		SWIFTHeader.append(SWIFTMessageUtil.generateRandom(10));
		// date = date + hhmmStr;
		date = date + hhmmStr;
		SWIFTHeader.append(date + "N}");

		return SWIFTHeader.toString();
	}

	/**
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getSWIFTBody(String swiftMessage) {

		String SWIFTDateTag = "";
		try {
			boolean UMACflag = SWIFTMessageUtil.isUMACContains(swiftMessage);
			if (UMACflag) {
				try {
					swiftMessage = SWIFTMessageUtil.removeUMAC(swiftMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Map<String, String> propertiesMap = SWIFTMessageUtil.getSFMSPropertiesValue("");
			SWIFTDateTag = propertiesMap.get("DateTag");
			String[] prodDateKeys = SWIFTDateTag.split("\\|");
			swiftMessage = SWIFTMessageUtil.chageSFMSDate(prodDateKeys, swiftMessage);
			try {
				if (swiftMessage.contains("{4:")) {
					swiftMessage = swiftMessage.substring(swiftMessage.indexOf("{4:"), swiftMessage.length());
				}
				// logger.info("swiftBody >>>" + swiftMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return swiftMessage;
	}

	/**
	 * 
	 * @param productType
	 * @param subProductType
	 * @return
	 */
	public static String getSfmsBicIfscMapping(String receiverifsc) {

		String realIfscCode = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		String sfmsIfscMappingQuery = "SELECT TIIFSC, REALIFSC FROM ETTIFSCMAP WHERE TIIFSC = ? ";
		logger.debug(sfmsIfscMappingQuery + " " + receiverifsc);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(sfmsIfscMappingQuery);
			aPreparedStatement.setString(1, receiverifsc);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				realIfscCode = aResultset.getString("REALIFSC");
			}

		} catch (Exception e) {
			logger.error("Exceptions! while getting ifscMap..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}

		return realIfscCode;
	}

}
