package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SWIFT_IN;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SWIFT_IN_ACK;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SWIFT_IN_NAK;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_TI;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.sfms.SFMSInMessageGenerator;
import com.bs.theme.bob.adapter.sfms.SFMSInwardMessageAdaptee;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.theme.bob.template.util.StaticDataListenerConstant;
import com.bs.themebridge.entity.model.Servicelog;
import com.bs.themebridge.incoming.util.IncomingServiceProcessor;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.swift.model.ThemeSwiftModel;
import com.bs.themebridge.swift.parser.ThemeSwiftParser;
import com.bs.themebridge.swift.util.ThemeSwiftUtil;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ThemeConstant;
import com.bs.themebridge.util.ValidationsUtil;
import com.misys.tiplus2.services.control.StatusEnum;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.mt.mt9xx.MT940;
import com.prowidesoftware.swift.model.mt.mt9xx.MT950;

/**
 * 
 * @author KXT51472
 *
 */
public class SWIFTSwiftInAdaptee extends IncomingServiceProcessor {

	private final static Logger logger = Logger.getLogger(SWIFTSwiftInAdaptee.class);

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
	private String operation = OPERATION_SWIFT_IN;

	public SWIFTSwiftInAdaptee() throws Exception {
		super(StaticDataListenerConstant.REQUEST_HEADER_NAME, "", StaticDataListenerConstant.REQUEST_HEADER_SERVICE_TI,
				StaticDataListenerConstant.REQUEST_HEADER_OPERATION_SWIFTIN);
	}

	public static void main(String[] args) throws Exception {

		try {
			SWIFTSwiftInAdaptee bob = new SWIFTSwiftInAdaptee();
			String fileNamePrefix = DateTimeUtil.getStringLocalDateInFormat("yyyyMMddHHmmssSSS");//201710051254393
			System.out.println(fileNamePrefix);
			
			String filePath = "";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\01SwiftInward01.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\01SwiftInward02.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\02SwiftInward_ACK.txt";

			
//			filePath = "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\02SwiftInward_ACK.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\02SwiftInward_NAK.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\03SwiftInward01_BillRef.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\03SwiftInward01_NoBillRef.txt";

			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\03SwiftInward01_BillRef.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\02SwiftInward_NAK.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\03SwiftInward_940_Nostro.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\03SwiftInward_202_Nostro.txt";
			// filePath =
			// "D:\\_Prasath\\00_TASK\\SWIFT-Inward-TEST\\04SwiftInward_BillReference.txt";
//			String swiftMessage = ThemeSwiftUtil.readFile(filePath);
//			bob.processSwiftInMessages(swiftMessage, "$", "TES");

		} catch (Exception e) {
			System.out.println("SwiftIn Exceptions!!! " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param swiftInMessage
	 * @param messageSeparator
	 * @return
	 */
	public boolean processSwiftInMessages(String swiftInMessage, String messageSeparator, String inQueueName) {

		logger.info(" ************ SWIFT.SwiftIn adaptee process started ************ ");

		String msgType = "";
		// String tag20Ref = "";
		String narrative1 = "";
		String narrative2 = "";
		boolean isValid = false;
		String billDescMsg = null;
		String messageProcess = "";
		String ackNakRegular = ""; // ACK / NAK / Regular
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		try {
			if (ValidationsUtil.isValidString(swiftInMessage)) {
				List<String> aMessage = separateTextFromFile(swiftInMessage, messageSeparator);
				// logger.debug("MqSwiftInMessage : " + swiftInMessage);

				bankRequest = swiftInMessage;
				for (String swiftInQmsg : aMessage) {
					String swiftIn = swiftInQmsg;
					/** 2.4 Getting Their Reference **/ // MessageUtil
					// theirRef =
					// SFMSInMessageGenerator.getTheirReference(swiftInMessage);
					// logger.debug("Their Master Reference : " + theirRef);

					ThemeSwiftParser themeSwiftObj = new ThemeSwiftParser();
					ThemeSwiftModel themeSwiftBean = themeSwiftObj.parseSwiftMessage(swiftInQmsg);

					String isAckNak = themeSwiftBean.getIsAckNak();
					logger.debug("AckNak : " + isAckNak);
					String ourRef = themeSwiftBean.getOurReference();
					logger.debug("OurRef/BillRef : " + ourRef);
					String theirRef = themeSwiftBean.getTheirReference();
					logger.debug("TheirRef : " + theirRef);

					msgType = themeSwiftBean.getMessageType();// XXX
					narrative1 = themeSwiftBean.getMessageType();// XXX
					narrative2 = themeSwiftBean.getSequenceOfTotal();

					String origMasterRef = null;
					/** -----Swift Bill Ref No Change start----- **/
					String billRefNumber = ourRef;
					if (ValidationsUtil.isValidString(billRefNumber)) {
						logger.debug("Valid OurRef/BillRef");
						origMasterRef = SFMSInwardMessageAdaptee.getMasterRefByBillRef(billRefNumber);
						if (ValidationsUtil.isValidString(origMasterRef)) {
							logger.debug("Valid origMasterRef");
							logger.debug(billRefNumber + "\t" + origMasterRef);
							// swiftIn = swiftIn.replace("\n", "\r\n");
							if (!msgType.equals("202")) {
								if (isAckNak.contentEquals("Y")) {
									swiftIn = swiftInQmsg.replaceAll(":20:" + billRefNumber, ":20:" + origMasterRef);
									billDescMsg = "Tag20 MasterRef " + origMasterRef + " replaced on BillRef "
											+ billRefNumber;
								} else if (isAckNak.contentEquals("N")) {
									swiftIn = swiftInQmsg.replaceAll(":21:" + billRefNumber, ":21:" + origMasterRef);
									billDescMsg = "Tag21 MasterRef " + origMasterRef + " replaced on BillRef "
											+ billRefNumber;
								}
							}
							logger.debug("Description : " + billDescMsg);
							logger.debug("SWIFT with origMasterRef : " + swiftIn);
						} else {
							origMasterRef = ourRef; // if null
							billDescMsg = "No bill reference number available for this swift message";
						}
					}

					/** ----------- ACK NAK update ------------ **/
					ackNakRegular = themeSwiftBean.getAckNak();
					logger.debug("is Ack/Nak/Regular : " + ackNakRegular);

					if (ackNakRegular.equals("ACK") || ackNakRegular.equals("NAK")) {
						if (ackNakRegular.equalsIgnoreCase("ACK")) {
							operation = OPERATION_SWIFT_IN_ACK;
							logger.info("ACK_-_ACK : " + operation);
						} else if (ackNakRegular.equalsIgnoreCase("NAK")) {
							operation = OPERATION_SWIFT_IN_NAK;
							logger.info("NAK_-_NAK : " + operation);
						}

						System.out.println("SelectQ " + DateTimeUtil.getSqlLocalDateTime());
						List<Servicelog> serviclog = getSwiftOutwardDetails(themeSwiftBean.getHdrRemovedSwiftMsg(),
								origMasterRef);
						logger.debug("Serviclog List : " + serviclog);
						if (serviclog.size() > 0) {
							serviclog.get(0).getBankrequest();
							String event = serviclog.get(0).getEventreference();
							String master = serviclog.get(0).getMasterreference();
							logger.debug("Reference : " + master + "-" + event);
						}
						System.out.println("UpdateQ " + DateTimeUtil.getSqlLocalDateTime());
						int resp = updateServiceLog(ackNakRegular, origMasterRef,
								themeSwiftBean.getHdrRemovedSwiftMsg(), themeSwiftBean.getSwiftFullInMsg());
						logger.debug("ServiceLog update : " + resp);
						// }
					}
					/** ----------- ACK NAK update ------------ **/

					// String swiftInCurrency = swiftInParserMap.get("ccy");
					// mtType = swiftInParserMap.get("mtType");

					String swiftInCurrency = themeSwiftBean.getCurrency();
					// mtType = getSwiftMsgType(swiftIn);
					// logger.debug(">>>>" + swiftInCurrency + "<<<");
					// msgType = themeSwiftBean.getMessageType();
					logger.debug("MessageType(XXX) >>>>" + msgType + "<<<");

					if (((msgType.equals("103") || msgType.equals("202")) && !swiftInCurrency.equals("INR"))
							|| msgType.equals("940") || msgType.equals("950")) {

						boolean isValidNostro = true;
						// 103, 202 SKIP and throw away. Not required to TIPLUS
						// and NOSTRO if comes as incoming with INR
						if (msgType.equals("940") || msgType.equals("950")) {

							String nostroMsgCcy = "";
							nostroMsgCcy = getNostroMsgCcy(swiftIn, msgType);
							if (!nostroMsgCcy.equalsIgnoreCase("INR")) {
								// String accountnostro =
								// SwiftMailerUtil.getSwiftTagValue(swiftIn,
								// ":25:");
								String accountnostro = themeSwiftBean.getNostroAccount();
								logger.debug("Account Nostro : " + accountnostro);
								// String senderBIC =
								// getSwiftSenderBIC(swiftIn);
								// logger.debug("senderBIC w/o terminator : " +
								// senderBIC);
								String senderBIC = themeSwiftBean.getSenderBICCode();
								logger.debug("senderBIC w/o terminator (Bean 11X): " + senderBIC);
								isValidNostro = isValidNostroMessage(msgType, senderBIC, accountnostro);
							} else {
								isValidNostro = false;
							}
						} else {
							logger.info("SWIFT Inward MsgType(XXX) : " + msgType);
							logger.info(
									"103, 202 >>-->> is not required for the NOSTRO and TIPLUS. Hence SUPPRESSED in THEMEBRIDGE!!!");
						}
						logger.debug("Milestone 03 isValidNostro " + isValidNostro);
						if (isValidNostro) {
							messageProcess = "NOSTRO";
							String fileNamePrefix = DateTimeUtil.getStringLocalDateInFormat("yyyyMMddHHmmssSSS");
							String fileNameSufix = "MT" + msgType + ".swift";
							String nostrofileName = fileNamePrefix + fileNameSufix;
							bankResponse = nostrofileName;
							String filePath = ConfigurationUtil
									.getValueFromKey(ThemeConstant.PROPERTY_SWIFT_NOSTRO_PATH);
							// logger.debug("Nostro FilePath : " + filePath);
							// logger.debug("Nostro FileName : " +
							// nostrofileName);
							File folder = new File(filePath);
							if (folder.exists()) {
								isValid = writeFile(filePath + "/" + nostrofileName, swiftIn);
								if (isValid)
									status = ThemeBridgeStatusEnum.TRANSMITTED.toString();// SUCCEEDED
								else
									status = ThemeBridgeStatusEnum.FAILED.toString();
								logger.debug("Nostro status : " + status);
							} else {
								logger.debug("folder does not exist");
							}

						} else {
							isValid = true;
							messageProcess = "NOSTRO";
							operation = OPERATION_SWIFT_IN;
							status = ThemeBridgeStatusEnum.SUPPRESSED.toString();
							logger.debug("Nostro status : " + status);
							logger.debug("Nostro message suppressed");
						}

					} else {
						/** Swift Inward Regular message Processing **/
						messageProcess = "TIPLUS";
						isValid = pushSwiftMessage(swiftIn, ackNakRegular);
						if (isValid)
							status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
						else
							status = ThemeBridgeStatusEnum.FAILED.toString();
						logger.debug("TIPLUS status : " + status);
					}
				}
			} else {
				logger.debug("SwiftIn message is empty or null!");
			}

		} catch (IOException e) {
			isValid = false;
			e.printStackTrace();
			errorMsg = e.getMessage();
			logger.error("IOException -> " + errorMsg);
			status = ThemeBridgeStatusEnum.FAILED.toString();

		} catch (Exception e) {
			isValid = false;
			e.printStackTrace();
			errorMsg = e.getMessage();
			logger.error("Exception -> " + errorMsg);
			status = ThemeBridgeStatusEnum.FAILED.toString();

		} finally {
			boolean res = ServiceLogging.pushLogData(SERVICE_TI, operation, SOURCE_SYSTEM, "", inQueueName, "TI",
					"MT" + msgType, ackNakRegular, status, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime,
					bankReqTime, bankResTime, tiResTime, "", messageProcess, narrative1, narrative2, false, "0",
					billDescMsg);
		}
		logger.debug("SwiftIn processed successfully");
		logger.info(" ************ SWIFT.SwiftIn adaptee process ended ************ ");
		return isValid;
	}

	public static List<Servicelog> getSwiftOutwardDetails(String msg, String masterReference) {

		// logger.debug("getSFMSoutwardDetails : " + sequence + "\t" +
		// senderIFSC);
		ResultSet rs = null;
		Connection aConnection = null;
		List<Servicelog> serviceList = new ArrayList<Servicelog>();
		Servicelog serviceObj = null;
		PreparedStatement aPreparedStatement = null;
		try {
			String query = "SELECT * FROM SERVICELOG WHERE BANKREQUEST LIKE '%" + msg + "%' "
					+ " AND OPERATION in ('SwiftOut', 'EBGIFN298SDROut', 'EBGIFN760COVOut', 'EBGIFN767COVOut' ) ";
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

	public static String getSwiftMsgType(String swiftmsg) {
		String swiftCode = "";
		int indexCode = swiftmsg.indexOf("{2:");
		if (indexCode >= 0 && indexCode < swiftmsg.length()) {
			swiftCode = swiftmsg.substring(indexCode + 4, indexCode + 7);
			// Flogger.debug("SwiftInMsg Type -> " + swiftCode);
		}
		return swiftCode;
	}

	public static String getNostroMsgCcy(String swiftIn, String mtType) {

		String swiftCcy = "";
		try {
			SwiftParser parser = new SwiftParser();
			parser.setData(swiftIn);
			SwiftMessage msg;

			msg = parser.message();

			if (mtType.equals("940")) {
				MT940 mt940 = new MT940(msg);

				if (mt940.getField60M() != null) {
					swiftCcy = mt940.getField60M().getComponent3();
					logger.debug("Nostro ccy : " + swiftCcy);
				} else if (mt940.getField60F() != null) {
					swiftCcy = mt940.getField60F().getComponent3();
					logger.debug("Nostro ccy : " + swiftCcy);
				}
			} else if (mtType.equals("950")) {
				MT950 mt950 = new MT950(msg);

				if (mt950.getField60M() != null) {
					swiftCcy = mt950.getField60M().getComponent3();
					logger.debug("Nostro ccy : " + swiftCcy);
				} else if (mt950.getField60F() != null) {
					swiftCcy = mt950.getField60F().getComponent3();
					logger.debug("Nostro ccy : " + swiftCcy);
				}
			}
		} catch (IOException e) {
			logger.error("Exception " + e.getMessage());
			e.printStackTrace();
		}

		return swiftCcy;
	}

	// public static String getSwiftSenderBIC(String swiftmsg) {
	// String swiftCode = "";
	// int indexCode = swiftmsg.indexOf("{2:");
	// if (indexCode >= 0 && indexCode < swiftmsg.length()) {
	// swiftCode = swiftmsg.substring(indexCode + 17, indexCode + 29);
	// // logger.debug("SwiftIn sender BIC -> " + swiftCode);
	// }
	// // Take substring
	//
	// return swiftCode;
	// }

	public static String getSwiftSenderBIC(String swiftmsg) {
		String result = "XXXXXXXXXXX";
		try {
			int indexCode = swiftmsg.lastIndexOf("{2:");
			if (indexCode >= 0 && indexCode < swiftmsg.length()) {
				result = new StringBuffer(swiftmsg).substring(indexCode + 17, indexCode + 25);
				// result = result.replaceAll("/", "_");
				result = result + new StringBuffer(swiftmsg).substring(indexCode + 26, indexCode + 29);
				// logger.debug("SwiftIn sender BIC " + result);
			}
		} catch (Exception e) {
			logger.error("Exception..! " + e.getMessage());
			e.printStackTrace();
			result = "XXXXXXXXXXX";
		}
		logger.debug("SwiftIn sender BIC --->" + result);
		return result;
	}

	public static boolean writeFile(String filePath, String bankRequest) {

		boolean result = false;
		// logger.debug("Write swift file initiated");
		// logger.debug("filePath ->" + filePath);
		File file = null;
		Writer output = null;
		file = new File(filePath);
		try {
			output = new BufferedWriter(new FileWriter(file));
			if (ValidationsUtil.isValidString(bankRequest)) {
				StringBuilder stringBuilder = new StringBuilder();
				InputStreamReader isr = null;
				BufferedReader bufferedReader = null;
				String line = null;
				isr = new InputStreamReader(new ByteArrayInputStream(bankRequest.getBytes()));
				bufferedReader = new BufferedReader(isr);
				while ((line = bufferedReader.readLine()) != null) {
					stringBuilder.append(line + "\r\n");
				}
				output.write(stringBuilder.toString());
				// logger.debug("stringBuilder ->" + stringBuilder.toString() +
				// "<-");
			}
			result = true;
			// logger.debug("Write swift file completed");

		} catch (IOException e) {
			result = false;
			logger.error("Write file exceptions " + e.getMessage(), e);

		} finally {
			try {
				output.close();
			} catch (IOException e) {
				logger.error("IOException -> " + e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * 
	 * @param swiftInMessage
	 * @return
	 */
	public boolean pushSwiftMessage(String swiftInMessage, String ackNakRegular) {

		logger.debug("PushSwiftMessage initiated");

		int endIndex;
		int startIndex;
		boolean result = false;
		StatusEnum statusEnum = null;
		bankReqTime = DateTimeUtil.getSqlLocalDateTime();
		bankResTime = DateTimeUtil.getSqlLocalDateTime();
		try {
			if (ValidationsUtil.isValidString(swiftInMessage)) {
				String swiftInmsg = swiftInMessage;
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
				logger.debug("Milestone 100 swiftin");
				InputStream anInputStream = SWIFTSwiftInAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.TI_SWIFTIN_REQUEST_TEMPLATE);
				String swiftInTiRequestTemplate = ThemeBridgeUtil.readFile(anInputStream);

				Map<String, String> tokens = new HashMap<String, String>();
				String correlationId = ThemeBridgeUtil.randomCorrelationId();
				tokens.put("correlationId", correlationId);
				tokens.put("name", ConfigurationUtil.getValueFromKey("SwiftInUser"));
				tokens.put("acknowledged", "true");
				tokens.put("message", swiftInmsg);
				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(swiftInTiRequestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				// logger.debug("Milestone 03 swiftin");
				tiRequest = reader.toString();
				tiReqTime = DateTimeUtil.getSqlLocalDateTime();
				logger.debug("SwiftIn TI Request to TI : " + tiRequest);
				reader.close();
				// logger.debug("Milestone 04 swiftin");
				try {
					// logger.debug("Pushing msg into EJB client->");
					tiResponse = TIPlusEJBClient.process(tiRequest);
					tiResTime = DateTimeUtil.getSqlLocalDateTime();
					logger.debug("SwiftIn TI Response : " + tiResponse);

					statusEnum = ResponseHeaderUtil.processEJBClientResponse(tiResponse);
					logger.debug("SwiftIn TI Response status : " + statusEnum.toString());

					if (statusEnum.equals(StatusEnum.SUCCEEDED)) {
						result = true;
					} else if (statusEnum.equals(StatusEnum.FAILED) || statusEnum.equals(StatusEnum.UNAVAILABLE)) {
						result = false;
					}
					// logger.debug("Swift In process status " + result);

				} catch (Exception e) {
					errorMsg = e.getMessage();
					statusEnum = StatusEnum.FAILED;
					result = false;

				} finally {
					logger.debug("SwiftIn processed successfully..!");
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.debug("Error pushSwiftMessage : " + statusEnum.toString());

		}
		logger.debug("pushSwiftMessage :: " + statusEnum.toString());
		return result;
	}

	/**
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public static String getNString(String name, int value) {

		String nvalue = null;
		try {
			nvalue = name.substring(0, Math.min(name.length(), value));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return nvalue;
	}

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

	/**
	 * Method getErrorResponse
	 * 
	 * @param errorMessage
	 *            {@code allows }{@link String}
	 * @return
	 */
	// public static String getErrorResponse(String errorMessage) {
	//
	// logger.debug("***** SWIFT SwiftOut error response initiated *****");
	// String result = "";
	// if (ValidationsUtil.isValidString(errorMessage)) {
	// ResponseHeader responseHeader = ResponseHeaderUtil.failedResponse("", "",
	// "", "", errorMessage + " (IM)");
	// result =
	// ResponseHeaderUtil.getTIFailedResponseByResponseHeader(responseHeader);
	// } else {
	// ResponseHeader responseHeader = ResponseHeaderUtil.failedResponse("", "",
	// "", "",
	// "Unexpected Error" + " (IM)");
	// result =
	// ResponseHeaderUtil.getTIFailedResponseByResponseHeader(responseHeader);
	// }
	// return result;
	// }

	/**
	 * 
	 * @param filePath
	 * @param messageToBeWrite
	 * @param name
	 * @param status
	 * @return
	 */
	// public static boolean writeSwiftInMessage(String filePath, String
	// messageToBeWrite, String name, String status) {
	// boolean isSucceed = false;
	// File file = null;
	// Writer output = null;
	// String fileName = getTime() + "_" + name;
	// String folderPath = filePath + "/" + getDate();
	// // logger.debug("folderPath : " + folderPath);
	// try {
	// File folder = new File(folderPath);
	// if (!folder.exists()) {
	// if (folder.mkdir()) {
	// logger.debug("Folder is created!");
	// } else {
	// logger.debug("Failed to create directory!");
	// }
	// } else {
	// logger.debug("Folder already exists");
	// }
	// String path = folderPath + "/" + fileName;
	// file = new File(path);
	// output = new BufferedWriter(new FileWriter(file));
	// if (ValidationsUtil.isValidString(messageToBeWrite)) {
	// StringBuilder stringBuilder = new StringBuilder();
	// InputStreamReader isr = null;
	// BufferedReader bufferedReader = null;
	// String line = null;
	// isr = new InputStreamReader(new
	// ByteArrayInputStream(messageToBeWrite.getBytes()));
	// bufferedReader = new BufferedReader(isr);
	// while ((line = bufferedReader.readLine()) != null) {
	// stringBuilder.append(line + "\r\n");
	// }
	// output.write(stringBuilder.toString());
	// }
	// boolean isConfirm = file.renameTo(new File(folderPath, fileName));
	// // logger.debug("File created " + isConfirm);
	// logger.debug("File created " + isConfirm);
	// isSucceed = true;
	// logger.debug("Swift file stored in " + filePath);
	// } catch (Exception e) {
	// logger.error("Exception -> " + e.getMessage(), e);
	// isSucceed = false;
	// } finally {
	// try {
	// output.close();
	// } catch (IOException e) {
	// logger.error("IOException -> " + e.getMessage(), e);
	// }
	// }
	// return isSucceed;
	// }

	/**
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public String getBillReferenceNo(String swiftMessage) {

		String tag21Value = "";

		// :21: receiver's reference
		// :20: sender's reference

		if (swiftMessage.contains(":21:")) {
			int indexOf21 = swiftMessage.lastIndexOf("21:");
			logger.debug("index of 21 : " + swiftMessage.substring(indexOf21));

			if (indexOf21 >= 0 && indexOf21 < swiftMessage.length()) {
				tag21Value = new StringBuffer(swiftMessage).substring(indexOf21 + 3, indexOf21 + 19);
				tag21Value = tag21Value.replaceAll("/", "_");
			}
			logger.debug("Bill Reference Number************" + tag21Value);

		} else {
			logger.info("Tag 21 is Not available");
		}

		return tag21Value;
	}

	/**
	 * Nostro
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public String getswifttagvalues(String swiftinMessage, String swifttagname) {

		String tag21Value = "";

		if (swiftinMessage.contains(swifttagname)) {
			int indexOf21 = swiftinMessage.lastIndexOf(swifttagname);
			// logger.debug("TAG index " + swifttagname + " >>>" +
			// swiftinMessage.substring(indexOf21));

			if (indexOf21 >= 0 && indexOf21 < swiftinMessage.length()) {
				tag21Value = new StringBuffer(swiftinMessage).substring(indexOf21 + 4, indexOf21 + 19);
				tag21Value = tag21Value.replaceAll("/", "_");
			}
			logger.debug("Tag21Value : " + tag21Value);

		} else {
			logger.info("Tag is Not available");
		}

		return tag21Value;
	}

	/**
	 * To get Bill reference number
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getMasterRefNumber(String billRefNumber) {

		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		String masterRefNumber = null;
		String eventRefQuery = "select mas.master_ref  from master mas, baseevent bev, extevent ext "
				+ "where mas.key97 = bev.master_key and bev.key97 = ext.event " + "AND bllrefno='" + billRefNumber
				+ "'";
		logger.debug("getMasterRefNumber query : " + eventRefQuery);
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(eventRefQuery);
			while (res.next()) {
				masterRefNumber = res.getString("master_ref");
			}

		} catch (SQLException e) {
			logger.error("Exception! Check the logs for details", e);

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return masterRefNumber;
	}

	/**
	 * To get Bill reference number
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static boolean isValidNostroMessage(String incomingMsgType, String incomingSenderSwiftBic,
			String incomingaccountNo) {

		boolean isValidNostro = false;

		ResultSet res = null;
		Connection con = null;
		PreparedStatement ps = null;
		// String nostroquery = "Select count(*) AS COUNT From
		// Ett_Nostro_Bic_Tbl Where Msg_Type = ? And Sender_Bic = ? And
		// Account_No = ? ";
		/** **/
		// String nostroquery = "SELECT COUNT(*) AS COUNT FROM EXTNOSTROBIC
		// WHERE TRIM(MSGTYPE) = ? AND TRIM(SENBIC) = ? AND TRIM(ACCTNO) = ? ";
		/** **/
		// String nostroquery = "SELECT TRIM(SENBIC) AS SENDERBIC, TRIM(MSGTYPE)
		// AS MSGTYPE, TRIM(ACCTNO) AS ACCOUNTNO FROM EXTNOSTROBIC WHERE
		// TRIM(MSGTYPE) = ? AND TRIM(SENBIC) = ? AND TRIM(ACCTNO) = ? ";
		String nostroquery = "SELECT TRIM(SENBIC) AS SENDERBIC, TRIM(MSGTYPE) AS MSGTYPE, TRIM(ACCTNO) AS ACCOUNTNO FROM EXTNOSTROBIC WHERE TRIM(MSGTYPE) = ? AND TRIM(SENBIC) = ? ";

		logger.debug("IsValidNostroQuery : " + nostroquery);
		try {

			// incomingSenderSwiftBic

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(nostroquery);
			ps.setString(1, incomingMsgType);
			ps.setString(2, incomingSenderSwiftBic);
			// ps.setString(3, incomingaccountNo);
			res = ps.executeQuery();
			logger.debug(incomingMsgType + "\t" + incomingSenderSwiftBic + "\t" + incomingaccountNo);

			while (res.next()) {
				String rsSenderBIC = res.getString("SENDERBIC");
				String rsMsgType = res.getString("MSGTYPE");
				String rsAccountNo = res.getString("ACCOUNTNO");
				logger.debug("rsAccountNo : " + rsAccountNo);

				if (incomingaccountNo.contains(rsAccountNo))
					isValidNostro = true;

				// int count = res.getInt("COUNT");
				// if (count > 0)
				// isValidNostro = true;

			}

		} catch (SQLException e) {
			logger.error("Exception! Check the logs for details", e);

		} finally {
			DatabaseUtility.surrenderConnection(con, ps, res);
		}
		return isValidNostro;
	}

	public static String getDate() {
		String result = "";
		Date today = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		result = formatter.format(today);
		return result;
	}

	public static String getTime() {
		String result = new Date() + "";
		Date today = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("HHmmss");
		result = formatter.format(today);
		return result;
	}

	public static int updateServiceLog(String ackNak, String master, String swiftInmsgF21removed,
			String swiftInAckNak) {

		logger.debug(ackNak + " / " + master);
		int update = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		String status = "TRANSMITTEDD";
		if (ackNak.equals("ACK"))
			status = "SUCCEEDED";
		if (ackNak.equals("NAK"))
			status = "FAILED";

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"UPDATE SERVICELOG SET STATUS = ?, BANKRESPONSE = ? WHERE SERVICE = 'SWIFT' AND MASTERREFERENCE = ? AND BANKREQUEST LIKE '%"
							+ swiftInmsgF21removed + "%'");
			aPreparedStatement.setString(1, status);
			aPreparedStatement.setString(2, status);// STATUS
			// aPreparedStatement.setString(3, swiftInAckNak);
			aPreparedStatement.setString(3, master);
			update = aPreparedStatement.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		// logger.debug("Themebridge Log update : " + update);
		return update;
	}

}
