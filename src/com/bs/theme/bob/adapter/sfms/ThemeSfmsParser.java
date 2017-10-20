package com.bs.theme.bob.adapter.sfms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

//import org.apache.log4j.Logger;

import com.bs.themebridge.util.AlphaNumericSegregation;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.ThemeBridgeUtil;

public class ThemeSfmsParser {

	private final static Logger logger = Logger.getLogger(SFMSInwardMessageAdaptee.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			String msgLoc = "";
			// msgLoc = "D:\\_Prasath\\00_TASK\\SFMSAckNak\\SFMSACKMessage.txt";
			// msgLoc = "D:\\_Prasath\\00_TASK\\SFMSAckNak\\SFMSNAKMessage.txt";
			msgLoc = "D:\\_Prasath\\00_TASK\\SFMSAckNak\\SFMSNAKMessageType2short.txt";
			// msgLoc =
			// "D:\\_Prasath\\00_TASK\\SFMSAckNak\\SFMSInwardMessage.txt";
			String messageInward = ThemeBridgeUtil.readFile(msgLoc);

			// xml = "{FIMRT020021}";
			// xml = "{A:ILCF27O278680671KKBK0000958201709081634FBAPI000161XX}"
			// xml = "{A:ILCF27O245208742KKBK0000958201709081634PBAPI000000XX}"
			// xml =
			// "{A:ILCF01O707XXXKKBK0000958KKBK0000201DPN000TI23622276438063EILC2017082412512265330341XXXXXXXXX0958ILD17020074099}{4:";
			// xml =
			// "{A:ILCF01O707XXXKKBK0000958KKBK0000201112000TI236222764380632ILC2017082412512265330341XXXXXXXXX0958ILD17020074099}{4:\n"
			// + ":20:0958ILD170200740\n" + ":21:NONREF\n" + ":27:1/1\n" +
			// ":31C:20170502\n" + ":30:20170502\n"
			// + ":26E:01\n" + ":59:SONALI\n" + ":33B:INR1000,00\n" +
			// ":34B:INR19000,00\n" + "-}";

			messageInward = "{A:BGSF23O260125211KKBK0000958201709131606}";
			messageInward = "{A:ILCF22O213115461KKBK0000958201709131601}";

			messageInward = "{A:ILCF01O760COVKKBK0000958KKBK0000201DPN000TI23622276438063EILC2017082412512265330341XXXXXXXXX0958ILD17020074099}{4:";

			ThemeSfmsBean sfmsObj = sfmsParser(messageInward);
			logger.debug("IsValid SFMS: " + sfmsObj.getIsValidSFMS());
			logger.debug("IsAckNak : " + sfmsObj.getIsAckNak());
			logger.debug("ACK / NAK : " + sfmsObj.getAckOrNak());

			logger.debug(sfmsObj.getBlockStartIdentifier());
			logger.debug(sfmsObj.getApplicationIdentifier());
			logger.debug(sfmsObj.getMessageIdentifier());
			logger.debug(sfmsObj.getInOutIdentifier());
			logger.debug(sfmsObj.getMessageType());
			logger.debug(sfmsObj.getMessageSubType());
			logger.debug(sfmsObj.getSenderIfsc());
			logger.debug(sfmsObj.getReceiverIfsc());
			logger.debug(sfmsObj.getDeliveryNotification());
			logger.debug(sfmsObj.getOpenNotification());
			logger.debug(sfmsObj.getNondeliveryWarning());
			logger.debug(sfmsObj.getObsolescence());
			logger.debug(sfmsObj.getMurReference());
			logger.debug(sfmsObj.getDupEmissionFlag());
			logger.debug(sfmsObj.getServiceIdentifier());
			logger.debug(sfmsObj.getOrginatingDate());
			logger.debug(sfmsObj.getOrginatingTime());
			logger.debug(sfmsObj.getTestingFlag());
			logger.debug(sfmsObj.getSequenceNum());
			logger.debug(sfmsObj.getFiller());
			logger.debug(sfmsObj.getTransactionNum());
			logger.debug(sfmsObj.getPriorityFlag());
			logger.debug(sfmsObj.getBlockAendIdentifier());

			logger.debug(sfmsObj.getMessageIdentifier());
			logger.debug(sfmsObj.getMsgIdentifierDesc());
			logger.debug(sfmsObj.getErrorCode());
			logger.debug(sfmsObj.getErrorDesc());

			logger.debug("21 : " + sfmsObj.getOurRef21());
			logger.debug("20 : " + sfmsObj.getTheirRef20());
			logger.debug("27 : " + sfmsObj.getMessageSeq27());

			// logger.debug(getTheirReference(xml));

		} catch (Exception e) {
			logger.debug("Exceptions!! " + e.getMessage());
			e.printStackTrace();
		}

	}

	public static ThemeSfmsBean sfmsParser(String sfmsMessage) {

		ThemeSfmsBean sfmsObj = new ThemeSfmsBean();

		try {
			// sfmsObj
			if (sfmsMessage.startsWith("{A:")) {
				boolean ackNak = checkAckNak(sfmsMessage);
				logger.debug("SFMS isAckNak ? " + ackNak);

				if (ackNak) {
					logger.debug("******** SFMS ACK/NAK message *************");
					// logger.debug("SFMS Message : " + sfmsMessage);
					logger.debug("SFMS length : " + sfmsMessage.length());
					String blockStartIdentifier = sfmsMessage.substring(0, 3);// {A:
					String appIdentifier = sfmsMessage.substring(3, 6);// ILC
					String msgIdentifier = sfmsMessage.substring(6, 9);// F27
					String ioIdentifier = sfmsMessage.substring(9, 10);// O
					String sequence = sfmsMessage.substring(10, 19);// 245208742
					String senderOurIfsc = sfmsMessage.substring(19, 30);// KKBK0000958
					String ackNakDate = sfmsMessage.substring(30, 38);// 201709081634
					String ackNakTime = sfmsMessage.substring(38, 42);// 201709081634
					logger.debug("Milestone 01");
					String filler = "";
					String erorrCode = "";
					String successFailure = "";
					String blockEndIdentifier = "";
					// 43 or 56
					if (sfmsMessage.length() > 43) {
						erorrCode = sfmsMessage.substring(42, 53);// PBAPI000000
						successFailure = erorrCode.substring(0, 1); // P/F
						filler = sfmsMessage.substring(53, 55);// XX
						blockEndIdentifier = sfmsMessage.substring(55, 56);// }
					} else {
						blockEndIdentifier = sfmsMessage.substring(42, 43);// XX
					}

					logger.debug("Milestone 02");
					sfmsObj.setOurRef21(null);
					sfmsObj.setTheirRef20(null);
					sfmsObj.setMessageSeq27("1/1");
					sfmsObj.setIsValidSFMS("Y");
					sfmsObj.setIsAckNak("Y"); // N
					sfmsObj.setIfnmessageType("");
					sfmsObj.setMessageSubType("");

					if (successFailure.equals("P"))
						sfmsObj.setAckOrNak("ACK");
					else if (successFailure.equals("F"))
						sfmsObj.setAckOrNak("NAK");
					else
						sfmsObj.setAckOrNak("Notification");

					logger.debug("Milestone 03");
					sfmsObj.setErrorCode(erorrCode);
					if (!erorrCode.isEmpty()) {
						String statusRespDesc = getSFMSerrorDesc(erorrCode);
						sfmsObj.setErrorDesc(statusRespDesc);
					}
					sfmsObj.setBlockStartIdentifier(blockStartIdentifier);
					sfmsObj.setApplicationIdentifier(appIdentifier);
					sfmsObj.setMessageIdentifier(msgIdentifier);
					String msgIdentifierDesc = "";
					if (msgIdentifier.equals("F01"))
						msgIdentifierDesc = "User to User";
					if (msgIdentifier.equals("F20"))
						msgIdentifierDesc = "Acknowledgment Message";
					if (msgIdentifier.equals("F22"))
						msgIdentifierDesc = "Non-delivery Warning Message";
					if (msgIdentifier.equals("F23"))
						msgIdentifierDesc = "Delivery Notification Message";
					if (msgIdentifier.equals("F24"))
						msgIdentifierDesc = "Open Notification Message";
					if (msgIdentifier.equals("F25"))
						msgIdentifierDesc = "Negative Acknowledgment Message";
					if (msgIdentifier.equals("F26"))
						msgIdentifierDesc = "User Nack Message";
					if (msgIdentifier.equals("F27"))
						msgIdentifierDesc = "Bank API Response Message";
					// F01 - User to User,
					// F20 - Acknowledgment Message,
					// F22 - Non-delivery Warning Message,
					// F23 - Delivery Notification Message,
					// F24 - Open Notification Message,
					// F25 - Negative Acknowledgment Message,
					// F26 - User Nack Message,
					// F27 - Bank API Response Message,
					sfmsObj.setMsgIdentifierDesc(msgIdentifierDesc);
					sfmsObj.setInOutIdentifier(ioIdentifier);
					sfmsObj.setSequenceNum(sequence);
					sfmsObj.setSenderIfsc(senderOurIfsc);
					sfmsObj.setAckNakDate(ackNakDate);
					sfmsObj.setAckNakTime(ackNakTime);
					sfmsObj.setFiller(filler);
					sfmsObj.setBlockEndIdentifier(blockEndIdentifier);

					logger.debug(blockStartIdentifier + appIdentifier + msgIdentifier + ioIdentifier + sequence
							+ senderOurIfsc + ackNakDate + ackNakTime + erorrCode + filler + blockEndIdentifier);

				} else {
					logger.debug("******** SFMS Regular message *************");
					sfmsObj.setIsValidSFMS("Y");
					sfmsObj.setIsAckNak("N");
					sfmsObj.setAckOrNak("Regular");
					try {
						sfmsObj.setBlockStartIdentifier(sfmsMessage.substring(0, 3));// 3-{A:
						sfmsObj.setApplicationIdentifier(sfmsMessage.substring(3, 6));// 3-ILC
						sfmsObj.setMessageIdentifier(sfmsMessage.substring(6, 9));// 3-F01
						sfmsObj.setMsgIdentifierDesc("User to User");
						sfmsObj.setInOutIdentifier(sfmsMessage.substring(9, 10));// 1-I/O
						sfmsObj.setMessageType(sfmsMessage.substring(10, 13));// 3-760
						sfmsObj.setIfnmessageType("IFN" + sfmsMessage.substring(10, 13)); // Result-IFN700
						sfmsObj.setMessageSubType(sfmsMessage.substring(13, 16));// 3-COV/XXX
						sfmsObj.setSenderIfsc(sfmsMessage.substring(16, 27));// 11-CNRB0001210
						sfmsObj.setReceiverIfsc(sfmsMessage.substring(27, 38));// 11-KKBK00000958
						sfmsObj.setDeliveryNotification(sfmsMessage.substring(38, 39));// 1-1Y-2N
						sfmsObj.setOpenNotification(sfmsMessage.substring(39, 40));// 1-1Y-2N
						sfmsObj.setNondeliveryWarning(sfmsMessage.substring(40, 41));// 1-1Y-2N
						sfmsObj.setObsolescence(sfmsMessage.substring(41, 44));// 1-1
						sfmsObj.setMurReference(sfmsMessage.substring(44, 60));// 16-TI21459854613014
						sfmsObj.setDupEmissionFlag(sfmsMessage.substring(60, 61));// 1-1Y-2N
						sfmsObj.setServiceIdentifier(sfmsMessage.substring(61, 64));// 3-XXX
						sfmsObj.setOrginatingDate(sfmsMessage.substring(64, 72));// 8-YYYYMMDD
						sfmsObj.setOrginatingTime(sfmsMessage.substring(72, 76));// 4-HHMM
						sfmsObj.setTestingFlag(sfmsMessage.substring(76, 77));// 1-1Y-2N
						sfmsObj.setSequenceNum(sfmsMessage.substring(77, 86));// 9-123456789
						sfmsObj.setFiller(sfmsMessage.substring(86, 95));// 9-XXXXXXXXX
						sfmsObj.setTransactionNum(sfmsMessage.substring(95, 111));// 12-0958ILC170200123
						sfmsObj.setPriorityFlag(sfmsMessage.substring(111, 113));// 2-99
						sfmsObj.setBlockAendIdentifier(sfmsMessage.substring(113, 114));// 1-}

						//
						HashMap<String, String> hashMapList = getMessageFieldsMap(sfmsMessage);
						String ourRef21 = hashMapList.get("ourReference");// ourReference
						String theirRef20 = hashMapList.get("theirReference");// theirReference
						String messageSeq = hashMapList.get("messageSeq");

						sfmsObj.setOurRef21(ourRef21);
						sfmsObj.setTheirRef20(theirRef20);
						if (messageSeq == null)
							sfmsObj.setMessageSeq27("1/1");
						else
							sfmsObj.setMessageSeq27(messageSeq);

					} catch (Exception e) {
						sfmsObj.setErrorDesc("SFMS Message Format error");
						logger.error("Regular SFMS Message Format error!!!");
						logger.error("Exception!!! " + e.getMessage());
					}
				}

			} else if (sfmsMessage.startsWith("{F")) {
				// {FIMRT020021}
				logger.debug("******** SFMS NAK short message *************");
				String blockStartIdentifier = sfmsMessage.substring(0, 1); // 1-{
				String statusIdentifier = sfmsMessage.substring(1, 2); // 1-F
				String statusErorrCode = sfmsMessage.substring(2, 12); // 10-IMRT020021
				String blockEndIdentifier = sfmsMessage.substring(12, 13); // 1-}
				sfmsObj.setIsValidSFMS("Y");
				sfmsObj.setAckOrNak("NAK");
				sfmsObj.setIsAckNak("Y");
				sfmsObj.setOurRef21(null);
				sfmsObj.setTheirRef20(null);
				sfmsObj.setIfnmessageType("");
				sfmsObj.setMessageSubType("");
				sfmsObj.setMessageSeq27("1/1");
				sfmsObj.setErrorCode(statusErorrCode);
				String errorDesc = getSFMSerrorDesc(statusErorrCode);
				sfmsObj.setErrorDesc(errorDesc);
				// sfmsObj.setErorrDesc("SenderIFSC does not exist");
				logger.debug(blockStartIdentifier + statusIdentifier + statusErorrCode + blockEndIdentifier);

			} else {
				logger.debug("This message is not a valid SFMS message!!!");
				sfmsObj.setIsValidSFMS("N");
				sfmsObj.setIfnmessageType("");
				sfmsObj.setMessageSubType("");
			}

		} catch (Exception e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
		}
		return sfmsObj;
	}

	public static boolean checkAckNak(String sfmsMessage) {

		boolean isAckNak = false;
		try {
			String messageIdentifier = sfmsMessage.substring(6, 9);
			// logger.debug(messageIdentifier);

			// int ackNakMsgLength = sfmsMessage.length();
			// logger.debug("Msg length(43/56) : " + ackNakMsgLength);
			// if (ackNakMsgLength <= 58) {
			// }

			if (!messageIdentifier.equalsIgnoreCase("F01")) {
				// logger.debug("Message Identifier : " + messageIdentifier);
				isAckNak = true;
			}
			// F01 - User to User,
			// F20 - Acknowledgment Message,
			// F22 - Non-delivery Warning Message,
			// F23 - Delivery Notification Message,
			// F24 - Open Notification Message,
			// F25 - Negative Acknowledgment Message,
			// F26 - User Nack Message,
			// F27 - Bank API Response Message,

		} catch (Exception e) {
			logger.error("CheckAckNak Exception!!!  " + e.getMessage());
			e.printStackTrace();
		}

		return isAckNak;
	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static String getSFMSerrorDesc(String errorCode) {

		String statusDesc = null;
		ResultSet res = null;
		Connection con = null;
		PreparedStatement ps = null;

		String query = "SELECT ERRORCODE, DESCRIPTION FROM LOOKUP_SFMS_ERROR_CODE WHERE ERRORCODE = ? ";
		logger.debug("LOOKUP_SFMS_ERROR_CODE_Query : " + query);
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			ps = con.prepareStatement(query);
			ps.setString(1, errorCode);
			res = ps.executeQuery();
			while (res.next()) {
				statusDesc = res.getString("DESCRIPTION");
			}

		} catch (SQLException e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}
		// logger.debug("statusDesc : " + statusDesc);
		return statusDesc;
	}

	/**
	 * 
	 * @param sfmsMessage
	 * @return
	 */
	private static HashMap<String, String> getMessageFieldsMap(String sfmsMessage) {

		// String EBGmessageLines[] =
		// tiGwRequestXML.split(System.lineSeparator());
		String eBGmessageLines[] = sfmsMessage.split(System.getProperty("line.separator"));
		HashMap<String, String> mapList = new HashMap<String, String>();

		for (String lines : eBGmessageLines) {

			if (lines.contains(":20:"))
				mapList.put("theirReference", lines.replace(":20:", ""));

			else if (lines.contains(":21:"))
				mapList.put("ourReference", lines.replace(":21:", ""));

			else if (lines.contains(":27:"))
				mapList.put("messageSeq", lines.replace(":27:", ""));
		}
		return mapList;
	}

	/**
	 * Not in use. Get :20: reference number.
	 * 
	 * @param sfmsMessage
	 * @return
	 */
	public static String getTheirReference(String sfmsMessage) {

		logger.debug("Milestone getting TheirReference started..!");
		String result = "";
		try {
			int indexOf20 = sfmsMessage.lastIndexOf("20:");
			logger.debug("indexOf20.substring -->" + sfmsMessage.substring(indexOf20));

			String subMasterMsg = sfmsMessage.substring(indexOf20 + 3);
			// logger.debug(indexOf20);

			if (indexOf20 >= 0 && indexOf20 < sfmsMessage.length()) {
				int indexOf1 = subMasterMsg.indexOf(":");
				String masref = new StringBuffer(subMasterMsg).substring(0, indexOf1);
				logger.debug("GetTheirReference >>>: " + masref);
				// masref.replaceAll(System.getProperty("line.separator"), "");
				masref = AlphaNumericSegregation.getAlphabetsNumbers(masref);
				result = masref;
			}

		} catch (Exception e) {
			logger.error("Exceptions..! " + e.getMessage());
			e.printStackTrace();
			result = "";
		}

		// logger.debug("Master Reference Number --->" + result);
		return result;
	}

}
