package com.bs.theme.bob.adapter.sfms;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMS_IN;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMS_IN_ACK;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMS_IN_NAK;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_TI;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.ebg.GatewayIFNMessageRouter;
import com.bs.theme.bob.template.util.StaticDataListenerConstant;
import com.bs.themebridge.entity.model.Servicelog;
import com.bs.themebridge.incoming.util.IncomingServiceProcessor;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;

/**
 * 
 * @author KXT51472
 *
 */
public class SFMSInwardMessageAdaptee extends IncomingServiceProcessor {

	private final static Logger logger = Logger.getLogger(SFMSInwardMessageAdaptee.class);

	private String errorMsg = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private String eventReference = "N/A";
	private String masterReference = "N/A";

	public SFMSInwardMessageAdaptee() throws Exception {
		super(StaticDataListenerConstant.REQUEST_HEADER_NAME, "", StaticDataListenerConstant.REQUEST_HEADER_SERVICE_TI,
				StaticDataListenerConstant.REQUEST_HEADER_OPERATION_SWIFTIN);
	}

	public static void main(String[] args) throws Exception {

		SFMSInwardMessageAdaptee bob = new SFMSInwardMessageAdaptee();
		try {
			String msg = "";
			String location = "D:\\_Prasath\\00_TASK\\SFMSInw\\999.txt";
			msg = ThemeBridgeUtil.readFile(location);

			// msg = "{A:ILCF27O213115461KKBK0000958201709131601PBAPI000000XX}";
			// msg = "{A:ILCF27O213115461KKBK0000958201709131601FBAPI000161XX}";
			msg = "{A:ILCF27O294881316KKBK0000958201709181147FBAPI000161XX}";
			// msg = "{A:ILCF22O213115461KKBK0000958201709131601}";
			// msg = "{A:ILCF23O213115461KKBK0000958201709131601}";
			// msg = "{A:ILCF24O213115461KKBK0000958201709131601}";
			// msg = "{A:ILCF25O213115461KKBK0000958201709131601}";
			// msg = "{A:ILCF26O213115461KKBK0000958201709131601}";

			// msg = "{A:BGSF27O223724517KKBK0000958201709161324FBAPI000161XX}";
			bob.processSFMSInMessages(msg, "$", "BGS");

			// updateSFMSOutward("ACK", "21562659612", "KKBK0000958");

			// System.out.println(getMasterRefByBillRef("ILD170200651C001"));

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param swiftInMessage
	 * @param messageSeparator
	 * @return
	 */
	public boolean processSFMSInMessages(String sfmsInQueueMessage, String messageSeparator, String inQueueName) {

		logger.info(" ************ SWIFT.SFMS adaptee process started ************ ");

		String our21Ref = "";
		String their20Ref = "";
		String msgSequence = "";
		String messageSubType = "";
		String ifnMessageType = "";
		boolean isValidLogging = false;
		String processStatus = "FAILED";
		String pushingTowards = "TIPLUS";
		inQueueName = inQueueName + "_MQ";
		String operation = OPERATION_SFMS_IN; // "SFMSIn";
		SFMSInMessageGenerator messagerGenerator = new SFMSInMessageGenerator();
		try {
			if (ValidationsUtil.isValidString(sfmsInQueueMessage)) {
				// 2.5.0.1
				List<String> aMessage = separateTextFromFile(sfmsInQueueMessage, messageSeparator);
				// logger.debug("MqSwiftInMessage : " + swiftInMessage);

				bankRequest = sfmsInQueueMessage;
				for (String sfmsIn : aMessage) {
					sfmsIn = sfmsIn.trim();

					ThemeSfmsBean sfmsObj = ThemeSfmsParser.sfmsParser(sfmsIn);
					String isAckNak = sfmsObj.getIsAckNak();
					logger.debug("isAckNak(SFMS) ? >>-->>" + isAckNak + "<<--<<");

					msgSequence = sfmsObj.getMessageSeq27();
					// logger.debug("MessageSequence27 >>-->>" + msgSequence +
					// "<<--<<");

					ifnMessageType = sfmsObj.getIfnmessageType();
					logger.debug("IFNMessageType(IFN) >>-->>" + ifnMessageType + "<<--<<");

					messageSubType = sfmsObj.getMessageSubType();
					logger.debug("messageSubType(COV / SDP)>>-->>" + messageSubType + "<<--<<");

					their20Ref = sfmsObj.getTheirRef20();
					// logger.debug("TheirRef20 >>-->>" + their20Ref +
					// "<<--<<");

					our21Ref = sfmsObj.getOurRef21();
					// logger.debug("OurRef21 >>-->>" + our21Ref + "<<--<<");

					/** 2.4 Getting Their Reference **/ // MessageUtil
					// their20Ref =
					// SFMSInMessageGenerator.getTheirReference(sfmsIn);
					// logger.debug("Their Master Reference(OLD) : " +
					// their20Ref);

					// ifnMessageType =
					// SFMSInMessageGenerator.getSFMSInMsgTypes(sfmsIn);
					// logger.debug("SFMS Incoming type(OLD) : " +
					// ifnMessageType);

					String msgRouter = ifnMessageType + messageSubType;
					logger.debug("Message Router : " + msgRouter);
					if (msgRouter.contains("IFN298SDP") || msgRouter.contains("IFN760COV")
							|| msgRouter.contains("IFN767COV")) {
						GatewayIFNMessageRouter router = new GatewayIFNMessageRouter();
						processStatus = router.processInwardMessage(sfmsIn, msgRouter, inQueueName);

					} else {
						if (isAckNak.equals("N")) {
							logger.debug("******** SFMS inward Regular ********");
							/*--------------- Swift Bill Ref No Change start ---------------*/
							// String billRef = getBillReferenceNo(sfmsIn);
							String billRef = our21Ref; // their20Ref
							if (ValidationsUtil.isValidString(billRef)) {
								// logger.debug("Valid billRef/OurRef");
								String origMasterRef = getMasterRefByBillRef(billRef);
								// logger.debug("Valid origMasterRef");
								if (ValidationsUtil.isValidString(origMasterRef)) {
									// sfmsIn = sfmsIn.replace("\n", "\r\n");
									// sfmsIn = sfmsIn.replaceAll(":20:" +
									// billRef, ":20:" + masterRef);
									sfmsIn = sfmsIn.replaceAll(":21:" + billRef, ":21:" + origMasterRef);
									// logger.debug("origMasterRef : " +
									// sfmsIn);
								}
							}
							/*--------------- End of Swift Bill Ref No Change ---------------*/
							processStatus = messagerGenerator.processSFMSMessage(sfmsIn, inQueueName);

						} else if (isAckNak.equals("Y")) {
							logger.debug("******** SFMS inward ACK/NAK ********");
							processStatus = processSFMSAckNakMessage(sfmsIn, inQueueName, sfmsObj);
						}
					}

					pushingTowards = "TIPLUS";
					isValidLogging = false;
					// }
				}
			} else {
				logger.debug("SFMS message is empty or null!");
				processStatus = "FAILED";
				isValidLogging = true;
			}

		} catch (IOException e) {
			processStatus = "FAILED";
			errorMsg = e.getMessage();
			logger.error("IOException -> " + errorMsg);
			e.printStackTrace();
			isValidLogging = true;

		} catch (Exception e) {
			processStatus = "FAILED";
			errorMsg = e.getMessage();
			logger.error("Exception -> " + errorMsg);
			e.printStackTrace();
			isValidLogging = true;

		} finally {
			if (isValidLogging) {
				boolean res = ServiceLogging.pushLogData(SERVICE_TI, operation, SOURCE_SYSTEM, "", inQueueName, "TI",
						ifnMessageType, "Regular", processStatus, tiRequest, tiResponse, bankRequest, bankResponse,
						tiReqTime, bankReqTime, bankResTime, tiResTime, their20Ref, pushingTowards, ifnMessageType,
						msgSequence, false, "0", errorMsg);
			}
			// logger.debug("SwiftIn pushed successfully");
		}

		logger.info(" ************ SWIFT.SFMS adaptee process ended ************ ");

		return isValidLogging;
	}

	private String processSFMSAckNakMessage(String sfmsInAckNakMsg, String inQueueName, ThemeSfmsBean sfmsObj) {

		tiRequest = "";
		tiResponse = "";
		bankRequest = sfmsInAckNakMsg;
		bankResponse = "";
		String event = "NA";
		String master = "NA";
		String ackOrNak = "";
		String descMsg = "";
		String msgSequence = "";
		String ifnMessageType = "";
		// String ourRef21 = "";
		// String theirRef20 = "";
		String messageIdentifier = "";
		String pushingTowards = "TIPLUS";
		String operation = OPERATION_SFMS_IN; // "SFMSIn";
		tiReqTime = DateTimeUtil.getSqlLocalDateTime();
		String processStatus = ThemeBridgeStatusEnum.SUPPRESSED.toString();
		try {
			ackOrNak = sfmsObj.getAckOrNak();
			if (ackOrNak.equalsIgnoreCase("ACK"))
				operation = OPERATION_SFMS_IN_ACK;
			else if (ackOrNak.equalsIgnoreCase("NAK"))
				operation = OPERATION_SFMS_IN_NAK;
			// ourRef21 = sfmsObj.getOurRef21();
			// theirRef20 = sfmsObj.getTheirRef20();
			msgSequence = sfmsObj.getMessageSeq27();
			String isAckNak = sfmsObj.getIsAckNak();
			ifnMessageType = sfmsObj.getIfnmessageType();
			messageIdentifier = sfmsObj.getMessageIdentifier();
			logger.debug("MessageIdentifier : " + messageIdentifier);
			String sequence = sfmsObj.getSequenceNum();
			String senderIFSC = sfmsObj.getSenderIfsc();
			bankResponse = sfmsObj.getErrorCode() + "\t" + sfmsObj.getErrorDesc();
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();

			System.out.println("SelectQ " + DateTimeUtil.getSqlLocalDateTime());
			List<Servicelog> serviclog = getSFMSoutwardDetails(sequence, senderIFSC);
			// logger.debug("Serviclog List : " + serviclog);
			if (serviclog.size() > 0) {
				tiResponse = serviclog.get(0).getBankrequest();
				event = serviclog.get(0).getEventreference();
				master = serviclog.get(0).getMasterreference();

				if (messageIdentifier.equalsIgnoreCase("F20") || messageIdentifier.equalsIgnoreCase("F27")) {
					boolean updateStatus = updateSFMSOutward(serviclog, sfmsObj, ackOrNak, sequence, senderIFSC);
					// bankResTime = DateTimeUtil.getSqlLocalDateTime();
					if (updateStatus) {
						logger.debug("Update Status SFMSACKNAK");
						processStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
					} else {
						processStatus = ThemeBridgeStatusEnum.FAILED.toString();
					}
				} else {
					/** F22, F23, F24, F25, F26 **/
					processStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				}
			} else {
				/** No records found **/
				processStatus = ThemeBridgeStatusEnum.SUPPRESSED.toString();
			}

			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			descMsg = e.getMessage();
			logger.error("SFMS ACKNAK Exceptions!!! " + descMsg);
			e.printStackTrace();

		} finally {
			boolean res = ServiceLogging.pushLogData(SERVICE_TI, operation, SOURCE_SYSTEM, "", inQueueName, "TI",
					master, event, processStatus, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime,
					bankReqTime, bankResTime, tiResTime, messageIdentifier, ackOrNak, messageIdentifier + ackOrNak,
					msgSequence, false, "0", descMsg);
		}
		return processStatus;
	}

	public static List<Servicelog> getSFMSoutwardDetails(String sequence, String senderIFSC) {

		logger.debug("getSFMSoutwardDetails : " + sequence + "\t" + senderIFSC);
		ResultSet rs = null;
		Connection aConnection = null;
		List<Servicelog> serviceList = new ArrayList<Servicelog>();
		Servicelog serviceObj = null;
		PreparedStatement aPreparedStatement = null;
		try {
			String query = "SELECT * FROM SERVICELOG WHERE BANKREQUEST LIKE '%" + sequence + "%' "
					+ " AND OPERATION in ('SFMSOut', 'EBGIFN298SDROut', 'EBGIFN760COVOut', 'EBGIFN767COVOut' ) ";
			logger.debug("SelectQuery : " + query);
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(query);
			// aPreparedStatement.setString(1, masterReference);
			// aPreparedStatement.setString(2, eventReference);
			rs = aPreparedStatement.executeQuery();
			while (rs.next()) {
				serviceObj = new Servicelog();
				serviceObj.setId(rs.getBigDecimal("ID"));
				serviceObj.setService(rs.getString("SERVICE"));
				serviceObj.setOperation(rs.getString("OPERATION"));
				serviceObj.setMasterreference(rs.getString("MASTERREFERENCE"));
				serviceObj.setEventreference(rs.getString("EVENTREFERENCE"));
				serviceObj.setBankrequest(rs.getString("BANKREQUEST"));
				serviceObj.setBankresponse(rs.getString("BANKRESPONSE"));
				serviceList.add(serviceObj);
			}

		} catch (Exception e) {
			logger.debug("ServiceLogList Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, rs);

		}
		return serviceList;
	}

	public static boolean updateSFMSOutward(List<Servicelog> serviclog, ThemeSfmsBean sfmsObj, String ackOrNak,
			String sequence, String senderIFSC) {

		logger.debug("updateSFMSOutward : " + ackOrNak + "\t" + sequence + "\t" + senderIFSC);
		boolean response = false;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();

		if (ackOrNak.equals("ACK")) {
			status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		} else {
			status = ThemeBridgeStatusEnum.FAILED.toString();
		}

		BigDecimal id = serviclog.get(0).getId();
		String outMsg = serviclog.get(0).getBankrequest();

		try {
			// String query = "UPDATE SERVICELOG SET STATUS = ? WHERE
			// BANKREQUEST LIKE '%" + sequence
			// + "%' AND OPERATION = 'SFMSOut' ";

			String query = "UPDATE SERVICELOG SET STATUS = ? WHERE id = ? ";
			// + "AND OPERATION = 'SFMSOut' ";
			logger.debug("UpdateQuery : " + query);

			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(query);
			aPreparedStatement.setString(1, status);
			aPreparedStatement.setBigDecimal(2, id);
			int rs = aPreparedStatement.executeUpdate();
			if (rs > 0)
				response = true;
			logger.debug("UpdateStatus ACK/NAK : " + rs + "\t" + response);

		} catch (Exception e) {
			response = false;
			logger.debug("Force Debit Credit Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, null);

		}
		return response;
	}

	// 2.5.0.1
	public static List<String> separateTextFromFile(String textMessage, String textSeparator) throws Exception {
		String line = null;
		List<String> list = null;
		InputStreamReader isr = null;
		BufferedReader bufferedReader = null;
		try {
			isr = new InputStreamReader(new ByteArrayInputStream(textMessage.getBytes()));
			bufferedReader = new BufferedReader(isr);
			StringBuilder stringBuilder = null;
			String ls = "\r\n"; // System.getProperty("line.separator");
			if (ValidationsUtil.isValidString(textSeparator)) {
				list = new ArrayList<String>();
				stringBuilder = new StringBuilder();
				while ((line = bufferedReader.readLine()) != null) {
					// new changes
					if (line.contains(textSeparator)) {
						int textSeparatorIndex = line.indexOf(textSeparator);
						String prefixText = ThemeBridgeUtil.getSubStringData(line, 0, textSeparatorIndex);
						String suffixText = ThemeBridgeUtil.getSubStringData(line, textSeparatorIndex, line.length());
						stringBuilder.append(prefixText);
						list.add(stringBuilder.toString());
						stringBuilder = new StringBuilder();
						int suffixSeparatorIndex = suffixText.indexOf(textSeparator);
						if (suffixSeparatorIndex != -1)
							suffixText = ThemeBridgeUtil.getSubStringData(suffixText, suffixSeparatorIndex + 1,
									suffixText.length());
						stringBuilder.append(suffixText);
						stringBuilder.append(ls);
					} else {
						stringBuilder.append(line);
						stringBuilder.append(ls);
					}
				}
				list.add(stringBuilder.toString());
			}
		} catch (Exception e) {
			logger.error("Exception -> " + e.getMessage(), e);
		} finally {
			isr.close();
			bufferedReader.close();
		}
		return list;
	}

	// public String getBillReferenceNo(String swiftMessage) {
	//
	// String tag21Value = "";
	//
	// if (swiftMessage.contains(":21:")) {
	// int indexOf21 = swiftMessage.lastIndexOf("21:");
	// // logger.debug("index of 21 : " +
	// // swiftMessage.substring(indexOf21));
	//
	// if (indexOf21 >= 0 && indexOf21 < swiftMessage.length()) {
	// tag21Value = new StringBuffer(swiftMessage).substring(indexOf21 + 3,
	// indexOf21 + 19);
	// tag21Value = tag21Value.replaceAll("/", "_");
	// }
	// logger.debug("Bill Reference Number : " + tag21Value);
	//
	// } else {
	// logger.info("Tag 21 is Not available");
	// }
	//
	// return tag21Value;
	// }

	/**
	 * Get row number from file using keyword search
	 * 
	 * @param filepath
	 * @param swiftNum
	 * @return
	 * @throws IOException
	 */
	public int GetRowNumberFromFile(String message, String swiftNum) throws IOException {

		// FileReader fr = new FileReader(filepath);
		// BufferedReader br = new BufferedReader(fr);

		InputStream fr = new ByteArrayInputStream(message.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(fr));
		String s;
		int linecount = 0;
		int rowNumber = 0;
		while ((s = br.readLine()) != null) {
			linecount++;
			int indexfound = s.indexOf(swiftNum);
			if (indexfound > -1) {
				rowNumber = linecount;
				rowNumber++;
			}
		}
		fr.close();
		logger.info("Reference number position : " + rowNumber);
		return rowNumber;
	}

	/**
	 * Get specific line from file
	 * 
	 * @throws FileNotFoundException
	 */
	public String GetReferenceNumber(String filepath, int rowNumber) throws FileNotFoundException {

		File f = new File(filepath);
		Scanner fileScanner = new Scanner(f);

		int lineNumber = 1;
		String d = "";
		while (fileScanner.hasNextLine()) {
			fileScanner.nextLine();
			lineNumber++;
			if (lineNumber == rowNumber) {
				logger.info("\n\n");
				d = fileScanner.nextLine().trim();
				logger.info("\n\n");
			}
		}
		fileScanner.close();
		return d;
	}

	// public String getBillReferenceNo(String swiftMessage) {
	//
	// StringReader strReader = new StringReader(swiftMessage);
	// BufferedReader reader = new BufferedReader(strReader);
	// // String line;
	// String ref = "";
	// try {
	// String line = reader.readLine();
	// while (line != null) {
	// if (line.startsWith(":21:")) {
	// ref = line.substring(4).trim();
	// }
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// return ref;
	// }

	/**
	 * To get Bill reference number
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getMasterRefByBillRef(String billRef) {

		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet res = null;
		String masterRef = null;
		// String eventRefQuery = "select mas.master_ref from master mas,
		// baseevent bev, extevent ext where mas.key97 = bev.master_key and
		// bev.key97 = ext.event AND bllrefno='" + billRef + "'";

		String billRefQuery = "SELECT trim(MAS.MASTER_REF) as MASTER_REF FROM MASTER MAS, BASEEVENT BEV, EXTEVENT EXT "
				+ "WHERE MAS.KEY97 = BEV.MASTER_KEY AND BEV.KEY97 = EXT.EVENT " + "AND BLLREFNO = ? ";
		logger.debug("MasterRef(by)BillRefQuery : " + billRefQuery);
		// logger.debug("BillRef : " + billRef);
		try {
			con = DatabaseUtility.getTizoneConnection();
			pstmt = con.prepareStatement(billRefQuery);
			pstmt.setString(1, billRef);
			res = pstmt.executeQuery();
			while (res.next()) {
				masterRef = res.getString("MASTER_REF");
			}

		} catch (SQLException e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, pstmt, res);
		}
		logger.debug("MasterRef by BillRef >>-->>" + masterRef + "<<--<<");
		return masterRef;
	}

}
