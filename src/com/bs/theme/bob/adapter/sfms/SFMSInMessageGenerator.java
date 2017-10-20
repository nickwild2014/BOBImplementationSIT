package com.bs.theme.bob.adapter.sfms;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMS_IN;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_TI;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.SWIFTSwiftInAdaptee;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AlphaNumericSegregation;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.misys.tiplus2.services.control.StatusEnum;
import com.prowidesoftware.swift.io.ConversionService;
import com.prowidesoftware.swift.model.SwiftMessage;

public class SFMSInMessageGenerator {

	private final static Logger logger = Logger.getLogger(SFMSInMessageGenerator.class);

	public static void main(String[] args) {

		try {
			String locationFile = "";
			locationFile = "D:\\_Prasath\\00_TASK\\SFMSCoverInward\\SWIFTInward.txt";
			String msg = ThemeBridgeUtil.readFile(locationFile);

			// new SFMSInMessageGenerator().processSFMSMessage(msg, "ILC");

			getTheirReference(msg);

			// new SFMSInMessageGenerator().insertSFMSHeaderDetails("MAS",
			// "KKBK0000958", "KKBKINBBXXX", "CNRB0000123",
			// "CNRBINNGXXX");

		} catch (Exception e) {
			logger.debug("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * 2.0
	 * 
	 * @param sfmsIncomingQueueMessage
	 * @return
	 */
	public String processSFMSMessage(String sfmsIncomingQueueMessage, String inQueueName) {

		String errorMsg = "";
		String tiRequest = "";
		String theirRef = "";
		String mtMsgType = "";
		String tiResponse = "";
		String statusEnum = "FAILED";
		String bankRequest = "";
		String formattedSwiftMsg = "";
		Timestamp tiResTime = null;
		Timestamp tiReqTime = DateTimeUtil.getSqlLocalDateTime();
		String operation = OPERATION_SFMS_IN; // "SFMSIn";
		try {
			/** 2.1 **/
			bankRequest = sfmsIncomingQueueMessage;
			// logger.debug("bankRequest : " + bankRequest);

			/** 2.2 Remove {UMAC tag in SWIFTin message **/
			sfmsIncomingQueueMessage = removeUMACTagProcess(sfmsIncomingQueueMessage);
			// logger.debug("removeUMACTageProcess :>" + smfsmsginqueue);

			/** 2.3 Getting Message Type **/
			mtMsgType = getSFMSInMsgType(sfmsIncomingQueueMessage);
			logger.debug("SFMS Message Type : " + mtMsgType);

			/** 2.4 Getting Their Reference **/ // MessageUtil
			theirRef = getTheirReference(sfmsIncomingQueueMessage);
			logger.debug("Their Master Reference : " + theirRef);

			/** 2.5 process messages **/
			formattedSwiftMsg = processSfmsIncomingMsg(sfmsIncomingQueueMessage, theirRef, mtMsgType);
			logger.debug("FormattedSwiftMsg :: FormattedSwiftMsg");

			/** 2.6 **/
			Map<String, String> swiftReqMap = null;
			swiftReqMap = pushSwiftMessage(formattedSwiftMsg);
			// swiftReqMap = pushSwiftMessage(formatterMsg, "");

			tiRequest = swiftReqMap.get("tiRequest");
			logger.debug("tiRequest " + tiRequest);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			tiResponse = swiftReqMap.get("tiResponse");
			logger.debug("tiResponse " + tiResponse);
			statusEnum = swiftReqMap.get("statusEnum");

		} catch (Exception e) {
			statusEnum = StatusEnum.FAILED.toString();
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// 2017-01-24
			ServiceLogging.pushLogData(SERVICE_TI, operation, SOURCE_SYSTEM, "", inQueueName, "TI", "IFN" + mtMsgType,
					"Regular", statusEnum, tiRequest, tiResponse, bankRequest, formattedSwiftMsg, tiReqTime, tiReqTime,
					tiResTime, tiResTime, theirRef, "", mtMsgType, "TIPLUS", false, "0", errorMsg);
		}

		return statusEnum;
	}

	/**
	 * 2.2
	 * 
	 * @param smfsmsginqueue
	 * @return
	 */
	private String removeUMACTagProcess(String sfmsIncomingQueueMessage) {

		// logger.debug("Milestone RemoveUMACTageProcess started..!");
		String formatterMsg = "";
		try {
			int indexCode = sfmsIncomingQueueMessage.indexOf("{UMAC:");
			// logger.debug("indexCode : " + indexCode);

			if (indexCode > 0) {
				String UMACvale = sfmsIncomingQueueMessage.substring(indexCode);
				int index1 = UMACvale.indexOf("}");
				UMACvale = sfmsIncomingQueueMessage.substring(0, indexCode);
				formatterMsg = UMACvale;
				logger.debug("UMAC digital signature removed");
			} else {
				formatterMsg = sfmsIncomingQueueMessage;
				logger.debug("UMAC digital signature not available");
			}

		} catch (Exception e) {
			logger.error("Exception..! " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("formatterMsg : " + formatterMsg);
		return formatterMsg;
	}

	/**
	 * 2.3
	 * 
	 * @param swiftinmsg
	 * @return
	 */
	public static String getSFMSInMsgType(String sfmsIncomingQueueMessage) {

		// logger.debug("Milestone getting SFMSInMsgType started..!");
		String ifnTypeCode = "";
		int indexCode = sfmsIncomingQueueMessage.indexOf("A:");
		if (indexCode >= 0 && indexCode < sfmsIncomingQueueMessage.length()) {
			ifnTypeCode = new StringBuffer(sfmsIncomingQueueMessage).substring(indexCode + 9, indexCode + 12);
			logger.debug("IFNMsg Type -> " + ifnTypeCode);
		}
		return ifnTypeCode;
	}

	/**
	 * 2.3
	 * 
	 * @param swiftinmsg
	 * @return
	 */
	public static String getSFMSInMsgTypes(String sfmsIncomingQueueMessage) {

		// logger.debug("Milestone getting SFMSInMsgType started..!");
		String ifnTypeCode = "";
		int indexCode = sfmsIncomingQueueMessage.indexOf("A:");
		if (indexCode >= 0 && indexCode < sfmsIncomingQueueMessage.length()) {
			ifnTypeCode = new StringBuffer(sfmsIncomingQueueMessage).substring(indexCode + 9, indexCode + 15);
			ifnTypeCode = "IFN" + ifnTypeCode;
			logger.debug("IFNMsgType : " + ifnTypeCode);
		}
		return ifnTypeCode;
	}

	// public String getTheirReference(String swiftMessage) {
	//
	// String tag20Value = "";
	//
	// // :21: receiver's reference
	// // :20: sender's reference
	//
	// if (swiftMessage.contains(":20:")) {
	// int indexOf20 = swiftMessage.lastIndexOf("20:");
	// logger.debug("index of 20 : " + swiftMessage.substring(indexOf20));
	//
	// if (indexOf20 >= 0 && indexOf20 < swiftMessage.length()) {
	// tag20Value = new StringBuffer(swiftMessage).substring(indexOf20 + 3,
	// indexOf20 + 19);
	// // tag21Value = tag21Value.replaceAll("/", "_");
	// }
	// logger.debug("Bill Reference Number************" + tag20Value);
	//
	// } else {
	// logger.info("Tag 21 is Not available");
	// }
	//
	// new SFMSInMessageGenerator().insertSFMSDetailsinTITable(tag20Value, "C1",
	// "C2", "C3", "C4");
	// return tag20Value;
	// }

	/**
	 * 2.4 OLD method and risk
	 * 
	 * @param swiftmsg
	 * @return
	 */
	public static String getTheirReference(String sfmsIncomingQueueMessage) {

		// logger.debug("Milestone getting TheirReference started..!");

		String result = "";
		try {
			int indexOf20 = sfmsIncomingQueueMessage.lastIndexOf("20:");
			// logger.debug("indexOf20.substring -->" +
			// swiftmsg.substring(indexOf20));

			String subMasterMsg = sfmsIncomingQueueMessage.substring(indexOf20 + 3);
			// logger.debug(indexOf20);

			if (indexOf20 >= 0 && indexOf20 < sfmsIncomingQueueMessage.length()) {
				int indexOf1 = subMasterMsg.indexOf(":");
				String masref = new StringBuffer(subMasterMsg).substring(0, indexOf1);
				// logger.debug("GetTheirReference >>>: " + masref);
				// masref = masref.replaceAll("\n", "").replaceAll("\r\n", "");
				// masref.replaceAll(System.getProperty("line.separator"), "");
				masref = AlphaNumericSegregation.getAlphabetsNumbers(masref);
				result = masref;
			}

		} catch (Exception e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();
			result = "";
		}

		logger.debug("Master Reference Number --->" + result);
		return result;
	}

	/**
	 * 2.5
	 * 
	 * @param swiftinmsg
	 * @param masterRef
	 * @param msgtype
	 * @return
	 */
	public String processSfmsIncomingMsg(String sfmsIncomingMessage, String masterRef, String mtMsgType) {

		// logger.debug("Milestone 01 ");
		String swiftInFinalMsg = "";
		String swiftinHeaderMsg = "";
		// 2.5.1
		swiftinHeaderMsg = getSfmsIncomingFormatter(sfmsIncomingMessage, masterRef, mtMsgType);
		logger.debug("swiftinHeaderMsg : " + swiftinHeaderMsg);

		int indexOf20 = sfmsIncomingMessage.lastIndexOf("{4:");
		String sfmsheadepart = sfmsIncomingMessage.substring(0, indexOf20);
		// logger.debug("Milestone 02");

		// 2.5.2
		sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage, sfmsheadepart,
				swiftinHeaderMsg);
				// logger.debug("swiftinmsg : " + sfmsIncomingMessage);

		// 2.5.3
		swiftInFinalMsg = updateSFMSBodyDateFormat(sfmsIncomingMessage);
		// logger.debug("updateSFMSBodyDateFormat : " + swiftInFinalMsg);

		// 2.5.4
		swiftInFinalMsg = updateSFMSBodyIfscBicFormat(swiftInFinalMsg);

		int index = swiftInFinalMsg.indexOf("}");
		// logger.debug("" + swiftInFinalMsg);

		return swiftInFinalMsg;
	}

	/**
	 * 2.5.1
	 * 
	 * @param swiftinmsg
	 * @param masterRef
	 * @param msgtype
	 * @return
	 */
	public String getSfmsIncomingFormatter(String sfmsinmsg, String masterRef, String msgtype) {

		String swiftinHeaderMsg = "";
		// logger.debug("getSfmsIncomingFormatter masterRef : " + masterRef);
		try {
			String narrative1 = "";
			String senderBIC = "";
			String receiverBIC = "";
			// logger.debug("Msgtype : " + msgtype);

			/** SENDER IFSC/BIC **/
			// String senderIFSC = sfmsinmsg.substring(16, 27);
			// String senderBank = senderIFSC.substring(0, 4);
			// senderBIC = getSwiftifscBicCode(senderBank); // 2.5.1.1
			// logger.debug("SenderIFSC :- " + senderIFSC + ", SenderBIC :- " +
			// senderBIC);

			/** RECEIVER IFSC/BIC **/
			// String receiverIFSC = sfmsinmsg.substring(27, 38);
			// String receiverBank = receiverIFSC.substring(0, 4);
			// receiverBIC = getSwiftifscBicCode(receiverBank); // 2.5.1.1
			// logger.debug("ReceiverIFSC :- " + receiverIFSC + ", ReceiverBIC
			// :- " + receiverBIC);

			/** New SENDER IFSC/BIC **/
			String senderIFSC = sfmsinmsg.substring(16, 27);
			String senderCountryCode = sfmsinmsg.substring(20, 22);
			// logger.debug("senderCountryCode " + senderCountryCode);
			if (senderCountryCode.equals("00"))
				senderBIC = getSfmsIfscBicMapping(senderIFSC);
			logger.debug("SenderIFSC :- " + senderIFSC + ", SenderBIC(IFSC Compatible) :- " + senderBIC);

			/** New RECEIVER IFSC/BIC **/
			String receiverIFSC = sfmsinmsg.substring(27, 38);
			String receiverCountryCode = sfmsinmsg.substring(31, 33);
			// logger.debug("receiverCountryCode " + receiverCountryCode);
			if (receiverCountryCode.equals("00"))
				receiverBIC = getSfmsIfscBicMapping(receiverIFSC);
			logger.debug("ReceiverIFSC :- " + receiverIFSC + ", ReceiverBIC(IFSC Compatible) :- " + receiverBIC);

			/**
			 * For Mapping Inward SFMS's both IFSC for TI transaction
			 */
			boolean b = insertSFMSHeaderDetails(masterRef, senderIFSC, senderBIC, receiverIFSC, receiverBIC);
			logger.debug("IncomingRetrieve: " + b);

			// 2.5.1.2
			// Inserting SFMS sender & receiver IFSC code, SWIFTBIC, Refer.no.
			// boolean insertStatus = sfmsincominglog(masterRef, senderIFSC,
			// senderBIC, receiverIFSC, receiverBIC, msgtype,
			// sfmsinmsg, narrative1);

			// 2.5.1.3
			swiftinHeaderMsg = SFMSInHeaderGenerator.getSWIFTHeader(senderBIC, receiverBIC, msgtype);

		} catch (Exception e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();
		}

		return swiftinHeaderMsg;
	}

	/**
	 * 
	 * @param productType
	 * @param subProductType
	 * @return
	 */
	public static String getSfmsIfscBicMapping(String reealifsc) {

		String realIfscCode = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		String sfmsIfscMappingQuery = "SELECT TIIFSC, REALIFSC FROM ETTIFSCMAP WHERE REALIFSC = ? ";
		logger.debug("SfmsIfsc-BicMappingQuery : " + sfmsIfscMappingQuery);

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(sfmsIfscMappingQuery);
			aPreparedStatement.setString(1, reealifsc);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				realIfscCode = aResultset.getString("TIIFSC");
			}

		} catch (Exception e) {
			logger.error("Exceptions! while getting ifscMap..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}

		return realIfscCode;
	}

	/**
	 * 2.5.1.1
	 * 
	 * @param ifsc
	 * @return
	 */
	public String getSwiftifscBicCode(String ifsc) {

		String swiftbic = "";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;

		try {
			String ifscQuery = "select trim(BICGLO) as ifsc from EXTSFMSIFSC where trim(IFSCBAN) ='" + ifsc + "'";
			// logger.debug("Get Swift BIC code Query : " + ifscQuery);

			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(ifscQuery);
			while (rs.next()) {
				swiftbic = rs.getString("ifsc");
			}

		} catch (SQLException e) {
			logger.error("SQLException e" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);

		}

		return swiftbic;
	}

	// 2.5.3
	public static String updateSFMSBodyDateFormat(String swiftMessage) {

		SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);

		String SwiftSFMSDateFormat = "31C|31D|44C|30|31E|32A|33A|34A";
		// logger.debug("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);

		try {
			String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");

			for (String tag : prodDateKeys) {
				// logger.debug("Date Tag -->" + tag);

				String value = m.getBlock4().getTagValue(tag);
				// logger.debug("Key-->" + tag + "\tvalue -->" + value);

				if (value != null) {
					if (!value.equals("")) {
						int taglen = value.length();

						// logger.debug("Tag Length : " + taglen);
						String tag2nd = "";
						if (taglen > 8) {
							tag2nd = value.substring(8, value.length());
							value = value.substring(0, 8);
						}

						String valueChange = DateTimeUtil.getDateTimeChangeFormat(value, "yyyyMMdd", "yyMMdd");
						// logger.debug("Key-->" + tag + "\t Old value -->" +
						// value + "\t New value -->" + valueChange);

						swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tag + ":" + value + tag2nd,
								tag + ":" + valueChange + tag2nd);
					}
					tag = "";
					value = "";
				}
			}
		} catch (Exception e) {
			logger.error("Exception !!!" + e.getMessage());
			e.printStackTrace();
		}

		return swiftMessage;
	}

	// 2.5.4
	public static String updateSFMSBodyIfscBicFormat(String sfmsIncomingMessage) {

		// logger.debug("Body IFSC Code to BIC Code changes initiated");

		try {
			String SwiftSFMSIfscFormat = "51A|41A|42A|53A|57A|52A|58A|54A|50A|56A|59A";
			SwiftMessage m = (new ConversionService()).getMessageFromFIN(sfmsIncomingMessage);
			String[] prodDateKeys = SwiftSFMSIfscFormat.split("\\|");

			for (String tag : prodDateKeys) {

				// logger.debug("Date Tag -->" + tag);
				String tagName = tag + ":";
				String value = m.getBlock4().getTagValue(tag);
				if (value != null && !value.isEmpty())
					logger.debug("Key-->" + tagName + "\tvalue -->" + value);

				if (value != null && !value.isEmpty()) {

					if (tag.equals("41A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value41AChange = getSfmsIfscBicMapping(valueIfsc);
						value41AChange = value41AChange + value.substring(11, value.length());
						logger.debug("value41AChange(new) : " + value41AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value41AChange);
						}
					}

					if (tag.equals("42A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value42AChange = getSfmsIfscBicMapping(valueIfsc);
						value42AChange = value42AChange + value.substring(11, value.length());
						logger.debug("value42AChange(new) : " + value42AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value42AChange);
						}
					}

					if (tag.equals("51A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value51AChange = getSfmsIfscBicMapping(valueIfsc);
						value51AChange = value51AChange + value.substring(11, value.length());
						logger.debug("value51AChange(new) : " + value51AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value51AChange);
						}
					}

					/** 52A **/
					if (tag.equals("52A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value52AChange = getSfmsIfscBicMapping(valueIfsc);
						value52AChange = value52AChange + value.substring(11, value.length());
						logger.debug("value52AChange(new) : " + value52AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value52AChange);
						}
					}

					/** 53A **/
					if (tag.equals("53A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value53AChange = getSfmsIfscBicMapping(valueIfsc);
						value53AChange = value53AChange + value.substring(11, value.length());
						logger.debug("value53AChange(new) : " + value53AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value53AChange);
						}
					}

					/** 54A **/
					if (tag.equals("54A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value54AChange = getSfmsIfscBicMapping(valueIfsc);
						value54AChange = value54AChange + value.substring(11, value.length());
						logger.debug("value54AChange(new) : " + value54AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value54AChange);
						}
					}

					/** 58A **/
					if (tag.equals("58A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value58AChange = getSfmsIfscBicMapping(valueIfsc);
						value58AChange = value58AChange + value.substring(11, value.length());
						logger.debug("value58AChange(new) : " + value58AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value58AChange);
						}
					}

					/********************************************/

					/** 50A **/
					if (tag.equals("50A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value50AChange = getSfmsIfscBicMapping(valueIfsc);
						value50AChange = value50AChange + value.substring(11, value.length());
						logger.debug("value50AChange(new) : " + value50AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value50AChange);
						}
					}

					/** 56A **/
					if (tag.equals("56A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value56AChange = getSfmsIfscBicMapping(valueIfsc);
						value56AChange = value56AChange + value.substring(11, value.length());
						logger.debug("value56AChange(new) : " + value56AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value56AChange);
						}
					}

					/** 57A **/
					if (tag.equals("57A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value57AChange = getSfmsIfscBicMapping(valueIfsc);
						value57AChange = value57AChange + value.substring(11, value.length());
						logger.debug("value57AChange(new) : " + value57AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value57AChange);
						}
					}

					/** 59A **/
					if (tag.equals("59A")) {
						String countryCode = value.substring(4, 6);
						String valueIfsc = value.substring(0, 11);
						String value59AChange = getSfmsIfscBicMapping(valueIfsc);
						value59AChange = value59AChange + value.substring(11, value.length());
						logger.debug("value59AChange(new) : " + value59AChange);
						if (countryCode.equals("00")) {
							sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage,
									tagName + value, tagName + value59AChange);
						}
					}

				}

				tagName = "";
				value = "";
			}

			logger.debug("Converted sfms-->> Swify : " + sfmsIncomingMessage);

		} catch (Exception e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();

		}

		return sfmsIncomingMessage;
	}

	/**
	 * 2.5.4 ORIGINAL
	 * 
	 * @param sfmsIncomingMessage
	 * @return
	 */
	public static String updateSFMSBodyIfscBicFormatOrig(String sfmsIncomingMessage) {

		// logger.debug("Body IFSC Code to BIC Code changes initiated");
		String SwiftSFMSIfscFormat = "51A|41A|42A|53A|57A|52A|58A|54A|50A|56A|59A";

		try {
			// logger.debug("swiftMessage -->" + sfmsIncomingMessage);
			// logger.debug("SwiftSFMSDateFormat -->" + SwiftSFMSIfscFormat);

			SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
			String[] prodDateKeys = SwiftSFMSIfscFormat.split("\\|");

			for (String string2 : prodDateKeys) {

				// logger.debug("Date Tag -->" + string2);
				string2 = string2 + ":";
				// 2.5.4.1
				String value = getSwiftBICKeyValue(sfmsIncomingMessage, string2);
				// logger.debug("Key-->" + string2 + "\tvalue -->" + value);

				if (!value.equals("")) {
					String InifscBank = value.substring(0, 4);
					// 2.5.1.1
					String valueChange = generator.getSwiftifscBicCode(InifscBank);
					logger.debug("IFSC valueChange :>" + valueChange);

					if (!ValidationsUtil.isValidString(valueChange)) {
						valueChange = "XXXXXXXXXXX";
					}

					logger.debug("Key-->" + string2 + "\t Old value -->" + value + "\t change value -->" + valueChange);

					// logger.info("\n <:::::::::::\tDate Replace CHanges
					// INput\t:::::::::::>"
					// + "\n <:::::::::::Searching string::::::::::::>" +
					// string2 + value
					// + "\n <::::::::::: Replace string:::::::::::>" + string2
					// + valueChange);

					sfmsIncomingMessage = ThemeBridgeUtil.stringReplaceCommonUtil(sfmsIncomingMessage, string2 + value,
							string2 + valueChange);

				}
				string2 = "";
				value = "";
			}

		} catch (Exception e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();

		}

		return sfmsIncomingMessage;
	}

	/**
	 * 2.5.4.1
	 * 
	 * @param swiftMessage
	 * @param key
	 * @return
	 */
	public static String getSwiftBICKeyValue(String sfmsIncomingMessage, String key) {

		String result = "";

		try {
			int indexOfMT = sfmsIncomingMessage.lastIndexOf(key);

			// logger.info("key -->" + key);
			// logger.info("indexOfKey -->" + indexOfMT + ":" + key.length());

			// :31C:150922
			// logger.info(indexOfMT);
			if (indexOfMT >= 0 && indexOfMT < sfmsIncomingMessage.length()) {
				result = new StringBuffer(sfmsIncomingMessage).substring(indexOfMT + key.length(),
						indexOfMT + key.length() + 11);
				// logger.info("result -->" + result);
				result = result.replaceAll("/", "_");
			}

		} catch (Exception e) {
			logger.error("Exception..!! " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 2.6
	 * 
	 * @param swiftInMessage
	 * @return
	 */
	public Map<String, String> pushSwiftMessage(String formattedSwiftMsg) {

		logger.debug("PushSwiftMessage initiated");

		int endIndex;
		int startIndex;
		boolean result = false;
		StatusEnum statusEnum = StatusEnum.UNAVAILABLE;
		Timestamp bankReqTime = DateTimeUtil.getSqlLocalDateTime();
		Map<String, String> swifinprocMap = new HashMap<String, String>();
		InputStream anInputStream = null;
		try {
			if (ValidationsUtil.isValidString(formattedSwiftMsg)) {
				String swiftInmsg = formattedSwiftMsg;
				/*** DON'T ENABLE BELOW COMMENTED LINES ***/
				// Remove Header from SWFT message before push into TI
				// startIndex = swiftInMessage.indexOf("{1:");
				// endIndex = swiftInMessage.indexOf("{451:0}}");
				// if (endIndex == -1) {
				// endIndex = swiftInMessage.indexOf("{451:1}}");
				// }
				// if (endIndex != -1) {
				// logger.debug("*** Processing to remove Acknowledgement Tag
				// ***");
				// String replacement = "";
				// String swiftMsg = swiftInMessage.substring(startIndex + 1,
				// endIndex);
				// swiftInmsg = swiftInMessage.replace(swiftMsg, replacement);
				// String ackMsg = getNString(swiftInmsg, 9);
				// if (ValidationsUtil.isValidString(swiftInmsg) &&
				// ackMsg.equals("{{451:0}}")) {
				// swiftInmsg = swiftInmsg.replace("{{451:0}}", "");
				// } else if (ValidationsUtil.isValidString(swiftInmsg) &&
				// ackMsg.equals("{{451:1}}")) {
				// swiftInmsg = swiftInmsg.replace("{{451:1}}", "");
				// }
				// } else {
				// logger.debug("*** Process to remove Acknowledgement Tag is
				// Skipped ***");
				// }

				anInputStream = SWIFTSwiftInAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.TI_SWIFTIN_REQUEST_TEMPLATE);
				String swiftInTiRequestTemplate = ThemeBridgeUtil.readFile(anInputStream);

				// logger.debug("Milestone 01");
				Map<String, String> tokens = new HashMap<String, String>();
				String correlationId = ThemeBridgeUtil.randomCorrelationId();
				tokens.put("correlationId", correlationId);
				tokens.put("name", ConfigurationUtil.getValueFromKey("SwiftInUser"));
				tokens.put("acknowledged", "true");
				tokens.put("message", swiftInmsg);
				logger.debug("Milestone 02");

				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(swiftInTiRequestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				String tiRequestXML = reader.toString();
				reader.close();

				swifinprocMap.put("tiRequest", tiRequestXML);
				logger.debug("SFMSSwiftIn TI Request to TI : \n" + tiRequestXML);

				try {
					String tiResponseXML = TIPlusEJBClient.process(tiRequestXML);
					logger.debug("SFMSSwiftIn TI Response : \n" + tiResponseXML);
					swifinprocMap.put("tiResponse", tiResponseXML);

					statusEnum = ResponseHeaderUtil.processEJBClientResponse(tiResponseXML);
					logger.debug("SFMSSwiftIn TI Response status : " + statusEnum.toString());

				} catch (Exception e) {
					logger.error("SFMSSwiftIn tireq XML gen exceptions! errorMsg " + e.getMessage());
					// swifinprocMap.put("statusEnum", statusEnum.toString());

				} finally {
					swifinprocMap.put("statusEnum", statusEnum.toString());
					logger.debug("SFMSSwiftIn processed successfully..!");
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("Error pushSFMSMessage : " + statusEnum.toString());

		} finally {
			// logger.debug("swifinprocMap " + swifinprocMap);
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		return swifinprocMap;
	}

	private boolean insertSFMSHeaderDetails(String masterRef, String senderIfsc, String senderBic, String receiverIfsc,
			String receiverBic) {
		boolean result = false;
		int excuteCount = 0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		PreparedStatement ps = null;
		try {
			String insertSfmsQuery = "INSERT INTO EXTSFMSCUSTMAP (MASTERREFERENCE, SENDERIFSC, RECEIVERIFSC, SENDERBICCODE, RECEIVERBICCODE) VALUES (?, ?, ?, ?, ?) ";
			// String insertSfmsQuery = "INSERT INTO ETTSFMSINWARDMAP
			// (THEIRREFERENCE, SENDERIFSC, RECEIVERIFSC, SENDERBIC,
			// RECEIVERBIC, PROCESSTIME) VALUES (?, ?, ?, ?, ?,
			// CURRENT_TIMESTAMP) ";
			logger.debug("InsertSFMSQuery : " + insertSfmsQuery);

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(insertSfmsQuery);
			ps.setString(1, masterRef);
			ps.setString(2, senderIfsc);
			ps.setString(3, receiverIfsc);
			ps.setString(4, senderBic);
			ps.setString(5, receiverBic);
			excuteCount = ps.executeUpdate();
			if (excuteCount > 0) {
				result = true;
				// logger.debug("SUCC");
			}

		} catch (SQLException e) {
			logger.error("insertSFMSDetailsinTITable SQLExceptions!!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}

		return result;
	}

	/**
	 * NOT IN USE 2.5.1.2
	 * 
	 * @param masterRef
	 * @param sender
	 * @param senderBic
	 * @param receiver
	 * @param receiverBic
	 * @param msgtype
	 */
	public boolean sfmsincominglog(String masterRef, String senderIfsc, String senderBic, String receiverIfsc,
			String receiverBic, String mtMsgType, String sfmsInMsg, String narrative1) {

		// logger.debug(masterRef);
		// logger.debug(senderIfsc + senderBic);
		// logger.debug(receiverIfsc + receiverBic);
		// logger.debug(mtMsgType);
		// logger.debug(sfmsInMsg);

		boolean result = false;
		int excuteCount = 0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		PreparedStatement ps = null;

		// logger.info(senderIfsc + " <> " + senderBic);
		// logger.info(receiverIfsc + " <> " + receiverBic);
		logger.debug("insertingSFMSInIsfcDetails : >>>" + masterRef + "<<<");

		try {
			String insertSfmsQuery = "INSERT INTO SFMSINCOMINGLOG (ID, SERVICE, OPERATION, MSGTYPE, SFMSINREFERENCE, RECEIVERIFSC, RECEIVERBIC, SENDERIFSC, SENDERBIC, INOMINGMESSAGE, NARRATIVE1, PROCESSTIME)"
					+ " VALUES (SFMSINCOMINGLOG_SEQ.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";

			// logger.debug("insertSfmsQuery : " + insertSfmsQuery);

			con = DatabaseUtility.getThemebridgeConnection();
			ps = con.prepareStatement(insertSfmsQuery);
			ps.setString(1, "SFMS");
			ps.setString(2, "SFMSIN");
			ps.setString(3, mtMsgType);
			ps.setString(4, masterRef);
			ps.setString(5, receiverIfsc);
			ps.setString(6, receiverBic);
			ps.setString(7, senderIfsc);
			ps.setString(8, senderBic);
			ps.setString(9, sfmsInMsg);
			ps.setString(10, narrative1);
			ps.setTimestamp(11, DateTimeUtil.getSqlLocalDateTime());

			excuteCount = ps.executeUpdate();
			// logger.debug("excuteCount " + excuteCount);

			if (excuteCount > 0) {
				result = true;
			}

		} catch (SQLException e) {
			logger.error("SQLExceptions " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}

		return result;
	}

	public void doServiceLogging(String tiRequest, String tiResponse, String bankRequest, String bankResponse,
			String status, String masterRef) {
		// NEW LOGGING
		boolean res = ServiceLogging.pushLogData("TI", "SFMSIN", "ZONE1", "", "ZONE1", "BOB", masterRef, "", status,
				tiRequest, tiResponse, bankRequest, bankResponse, null, null, null, null, "", "", "", "", false, "0",
				"");
	}

	/************************** Not in use **************************/

	// /**
	// *
	// * @param swiftMessage
	// * @param key
	// * @return
	// */
	// public static String getSwiftKeyValue(String swiftMessage, String key) {
	// String result = "";
	// int indexOfMT = swiftMessage.lastIndexOf(key);
	//
	// logger.info("key -->" + key);
	// logger.info("indexOfKey -->" + indexOfMT + ":" + key.length());
	//
	// // :31C:150922
	// logger.info(indexOfMT);
	// try {
	//
	// if (indexOfMT >= 0 && indexOfMT < swiftMessage.length()) {
	//
	// result = new StringBuffer(swiftMessage).substring(indexOfMT +
	// key.length(),
	// indexOfMT + key.length() + 8);
	// result = result.replaceAll("/", "_");
	//
	// }
	// } catch (Exception e) {
	// logger.error("Exceptions " + e.getMessage());
	// e.printStackTrace();
	//
	// }
	// return result;
	// }

	// /**
	// * Not in use KOTAK
	// *
	// * @param msg
	// * @return
	// */
	// public static String getSwiftMsgType(String msg) {
	// String swiftCode = "";
	// int indexCode = msg.indexOf("2:");
	// if (indexCode >= 0 && indexCode < msg.length()) {
	// swiftCode = msg.substring(indexCode + 3, indexCode + 6);
	// logger.debug("Msg Type -> " + swiftCode);
	// }
	// return swiftCode;
	// }

	/**
	 * sfms out
	 *
	 * @param masterRef
	 * @param BEVREFNO
	 * @param BEVREFNO_SERL
	 * @return
	 */
	public Map<String, String> getSwiftBodyBicifscCode(String masterRef, String BEVREFNO, String BEVREFNO_SERL) {

		String senderifsc = "", receifsc = "", senderRece = "";

		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;

		Map<String, String> map = new HashMap<String, String>();
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();

			String query = "select trim(EXPTY.PRTYBA) as tageName, trim(EXPTY.IFSCBANK) as tageValue, MAS.MASTER_REF, "
					+ " MAS.MASTER_REF, trim(BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL,3,0)) AS EventREFNO_PFIXSERL "
					+ " from BASEEVENT BEV, master mas,EXTEVENTPRT EXPTY " + " where MAS.KEY97 = BEV.MASTER_KEY"
					+ " and EXPTY.FK_EVENT = BEV.EXTFIELD" + " and MAS.MASTER_REF = '" + masterRef + "'"
					+ " and BEV.REFNO_PFIX = '" + BEVREFNO + "'" + " and BEV.REFNO_SERL= '" + BEVREFNO_SERL + "'";
			logger.info(query);

			rs = stmt.executeQuery(query);
			int i = 0;
			while (rs.next()) {

				String tagname = rs.getString("tageName");
				String tagvalue = rs.getString("tageValue");

				map.put(tagname, tagvalue);
				map.put("status", "1");
				i = 1;
			}

			if (i != 0) {
				map.put("status", "1");
			} else
				map.put("status", "0");

			senderRece = senderifsc + "|" + receifsc;
			// logger.info("------------->" + senderRece);

		} catch (SQLException e) {
			e.printStackTrace();
			senderRece = "|";
			map.put("status", "0");

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}

		return map;
	}

	// /**
	// *
	// * @param masterRef
	// * @param BEVREFNO
	// * @param BEVREFNO_SERL
	// * @return
	// */
	// public Map<String, String> getSwiftBodyBicifscCode(String masterRef,
	// String BEVREFNO, String BEVREFNO_SERL) {
	//
	// String receifsc = "";
	// String senderifsc = "";
	// String senderRece = "";
	//
	// ResultSet rs = null;
	// Connection con = null;
	// Statement stmt = null;
	// Map<String, String> map = new HashMap<String, String>();
	//
	// try {
	// con = DatabaseUtility.getTizoneConnection();
	// stmt = con.createStatement();
	//
	// String getSwiftBodyBicifscCodeQuery = "select trim(ev.senifsc)
	// SenderIFSC, trim(ev.recifsc) ReceiverIfsc from master mas,baseevent
	// bev,extevent ev where mas.key97=bev.master_key and bev.key97=ev.event and
	// trim(mas.master_ref)='"
	// + masterRef + "' and bev.REFNO_SERL='" + BEVREFNO_SERL + "' AND
	// BEV.REFNO_PFIX = '" + BEVREFNO
	// + "'";
	// logger.debug("getSwiftBodyBicifscCodeQuery : " +
	// getSwiftBodyBicifscCodeQuery);
	//
	// /*
	// * if 3rd party grid in TI, use below query
	// */
	// // String query = "select trim(EXPTY.PRTYBA) as tageName,
	// // trim(EXPTY.IFSCBANK) as tageValue ,MAS.MASTER_REF, "
	// // + " MAS.MASTER_REF,trim(BEV.REFNO_PFIX ||
	// // LPAD(BEV.REFNO_SERL,3,0)) AS EventREFNO_PFIXSERL "
	// // + " from BASEEVENT BEV, master mas,EXTEVENTPRT EXPTY "
	// // + " where MAS.KEY97 = BEV.MASTER_KEY"
	// // + " and EXPTY.FK_EVENT = BEV.EXTFIELD"
	// // + " and MAS.MASTER_REF = '"
	// // + masterRef
	// // + "'"
	// // + " and BEV.REFNO_PFIX = '"
	// // + BEVREFNO
	// // + "'"
	// // + " and BEV.REFNO_SERL= '" + BEVREFNO_SERL + "'";
	//
	// int i = 0;
	// rs = stmt.executeQuery(getSwiftBodyBicifscCodeQuery);
	// String SenderIFSC = "";
	// String ReceiverIfsc = "";
	//
	// while (rs.next()) {
	// SenderIFSC = rs.getString("SenderIFSC");
	// ReceiverIfsc = rs.getString("ReceiverIfsc");
	// map.put("status", "1");
	// i = 1;
	// }
	// map.put("SenderIFSC", SenderIFSC);
	// map.put("ReceiverIfsc", ReceiverIfsc);
	//
	// logger.info("SenderIFSC===> " + map.get("SenderIFSC"));
	// logger.info("ReceiverIfsc===> " + map.get("ReceiverIfsc"));
	//
	// if (i != 0) {
	// map.put("status", "1");
	// } else
	// map.put("status", "0");
	//
	// senderRece = senderifsc + "|" + receifsc;
	// logger.debug("senderifsc | receifsc : " + senderRece);
	//
	// } catch (SQLException e) {
	// senderRece = "|";
	// map.put("status", "0");
	// e.printStackTrace();
	//
	// } finally {
	// DatabaseUtility.surrenderConnection(con, stmt, rs);
	// }
	//
	// return map;
	// }

	/**
	 * 
	 * @param masterRef
	 * @param BEVREFNO
	 * @param BEVREFNO_SERL
	 * @return
	 */
	public static Map<String, String> getSenderReceiverIfscCode(String masterRef, String eventRef) {

		String senderIFSC = "";
		String receiverIFSC = "";

		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		Map<String, String> map = new HashMap<String, String>();

		try {
			String query = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) AS EVENT_REF, "
					+ " TRIM(SENIFSC) AS SENDERIFSC, TRIM(RECIFSC) AS RECEIVERIFSC FROM BASEEVENT BEV, MASTER MAS,EXTEVENT EXT  "
					+ " WHERE MAS.KEY97 = BEV.MASTER_KEY AND EXT.KEY29 = BEV.EXTFIELD AND TRIM(MAS.MASTER_REF) = ? "
					+ " AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) = ? ";

			logger.debug("Get Sender and Receiver IFSC : " + query);

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			ps.setString(1, masterRef);
			ps.setString(2, eventRef);
			rs = ps.executeQuery();
			while (rs.next()) {
				senderIFSC = rs.getString("SENDERIFSC");
				receiverIFSC = rs.getString("RECEIVERIFSC");
				map.put("senderIFSC", senderIFSC);
				map.put("receiverIFSC", receiverIFSC);
			}
			// logger.debug("Sender IFSC : " + senderIFSC);
			// logger.debug("Receiver IFSC : " + receiverIFSC);

		} catch (SQLException e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		return map;
	}

	/**
	 * SFMS MANY MANY
	 *
	 * @param masterRef
	 * @param BEVREFNO
	 * @param BEVREFNO_SERL
	 * @return
	 */
	public Map<String, String> getSwiftBicifscCode(String masterRef, String BEVREFNO, String BEVREFNO_SERL) {

		String senderifsc = "";
		String receifsc = "";
		String senderRece = "";

		int i = 0;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		Map<String, String> map = new HashMap<String, String>();

		try {
			String query = "select trim(MAS.MASTER_REF) as MASTER_REF, trim(BEV.REFNO_PFIX||lpad(BEV.REFNO_SERL, 3, 0)) AS EVENT_REF, "
					+ " trim(SENIFSC) as SENIFSC, trim(RECIFSC) as RECIFSC "
					+ " from BASEEVENT BEV, master mas,extevent EXT " + " where MAS.KEY97 = BEV.MASTER_KEY"
					+ " and EXT.KEY29 = BEV.EXTFIELD" + " and trim(MAS.MASTER_REF) = '" + masterRef + "'"
					+ " and trim(BEV.REFNO_PFIX||lpad(BEV.REFNO_SERL, 3, 000)) = '" + BEVREFNO + BEVREFNO_SERL + "' ";
			// + " BEV.REFNO_PFIX = '" + BEVREFNO + "'" + " and BEV.REFNO_SERL=
			// '" + BEVREFNO_SERL + "'";
			logger.debug("Get Sender and Receiver IFSC : " + query);

			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				senderifsc = rs.getString("SENIFSC");
				receifsc = rs.getString("RECIFSC");
				map.put("senderIFSC", senderifsc);
				map.put("receiverIFSC", receifsc);
				map.put("status", "1");
				i = 1;
			}

			if (i != 0) {
				map.put("status", "1");
			} else
				map.put("status", "0");

			senderRece = senderifsc + "|" + receifsc;
			// logger.debug("-------------> sendserIFSC | recIsfc " +
			// senderRece);

		} catch (SQLException e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();
			senderRece = "|";
			map.put("status", "0");

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}

		return map;
	}

	/**
	 * SFMS MANY MANY
	 *
	 * @param masterRef
	 * @param BEVREFNO
	 * @param BEVREFNO_SERL
	 * @return
	 */
	public static String getSwiftSenderIFSC(String masterRef) {

		String senderIFSC = "";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		// Map<String, String> map = new HashMap<String, String>();

		try {
			String query = "SELECT TRIM(MAS.BHALF_BRN) AS BEHALF_OF_BRANCH, TRIM(EXB.IFSC) AS SENDER_IFSC_CODE FROM MASTER MAS, EXTBRAMAS EXB "
					+ " WHERE TRIM(MAS.BHALF_BRN)=TRIM(EXB.BCODE) AND TRIM(MAS.MASTER_REF) = '" + masterRef + "'";
			logger.debug("Get Sender IFSC BY branch CODE : " + query);

			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				senderIFSC = rs.getString("SENDER_IFSC_CODE");
			}

		} catch (SQLException e) {
			logger.error("Get Sender IFSC Exceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}

		return senderIFSC;
	}

	// /**
	// * 2.6 To push the incoming swift messages to TI
	// *
	// * <p>
	// * <code>
	// * {@code swiftInMessage} is the message to be pushed
	// * </code>
	// * </p>
	// *
	// * @param swiftInMessage
	// * {@code allows } {@link String}
	// */
	// public Map<String, String> pushSwiftMessage(String swiftInMessage, String
	// test) {
	//
	// String masterRef = "";
	// String tiResponse = "";
	// StatusEnum statusEnum = StatusEnum.UNAVAILABLE;
	// RequestHeader requestHeader = new RequestHeader();
	// Map<String, String> swiftReqMap = new HashMap<String, String>();
	//
	// try {
	// if (ValidationsUtil.isValidString(swiftInMessage)) {
	// JAXBContext context = JAXBInstanceInitialiser.getSwiftInRequestContext();
	//
	// requestHeader =
	// RequestHeaderUtil.getRequestHeader(REQUEST_HEADER_CREDENTIAL_SUPERVISOR,
	// ThemeBridgeUtil.randomCorrelationId(), REQUEST_HEADER_SERVICE_TI,
	// REQUEST_HEADER_OPERATION_SWIFTIN);
	// // requestHeader.setCorrelationId(ThemeBridgeUtil
	// // .randomCorrelationId());
	//
	// ObjectFactory of = new ObjectFactory();
	// GatewaySwiftIn swiftIn = new GatewaySwiftIn();
	// JAXBElement<EnigmaBoolean> acknowledgeJAXB =
	// of.createGatewaySwiftInAcknowledged(EnigmaBoolean.TRUE);
	// swiftIn.setAcknowledged(acknowledgeJAXB);
	// swiftIn.setMessage(swiftInMessage);
	// ServiceRequest sRequest = new ServiceRequest();
	// sRequest.setRequestHeader(requestHeader);
	// JAXBElement<GatewaySwiftIn> swiftInJAXB = of.createSwiftIn(swiftIn);
	// List sReqList = sRequest.getRequest();
	// sReqList.add(swiftInJAXB);
	// String tiRequest = JAXBTransformUtil.doMarshalling(context, sRequest);
	//
	// logger.debug("Request to TI EJB :\n " + tiRequest);
	//
	// tiResponse = TIPlusEJBClient.process(tiRequest);
	// logger.debug("Response from TI EJB :\n " + tiResponse);
	//
	// statusEnum = ResponseHeaderUtil.processEJBClientResponse(tiResponse);
	//
	// swiftReqMap.put("tiRequest", tiRequest);
	// swiftReqMap.put("tiResponse", tiResponse);
	// swiftReqMap.put("statusEnum", statusEnum.toString());
	//
	// // String tiResponse = RestUtil.sendRequest(tiRequest);
	// logger.debug("Swift In processed successfully \n swiftReqMap:>" +
	// swiftReqMap);
	//
	// }
	// } catch (Exception e) {
	// logger.error(e.getMessage() + e.getMessage());
	// e.printStackTrace();
	//
	// }
	//
	// return swiftReqMap;
	// }

}
