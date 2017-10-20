package com.bs.theme.bob.adapter.sfms;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSADVICE;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_EMAIL;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.SwiftFriendlyFormateAdaptee;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;

public class SfmsAdviceHandler {

	private final static Logger logger = Logger.getLogger(SfmsAdviceHandler.class);

	public static void main(String[] args) {

		try {
			String SfmsOutMsg = ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\sfms printer friendly\\sfmsSIT.txt");
			// adviceHandler(SfmsOutMsg, "700", "0958ILD170200545", "ISS001");

			String sfmsOutHeaderAddr = getSfmsIFSCAddrDetails(SfmsOutMsg, "700");
			logger.debug(sfmsOutHeaderAddr);

			// getIFSCAddr("KKBK0000958", "SENDER");
			// getIFSCAddr("SBHY0020147", "RECEIVER");

			// adviceHandler(SfmsOutMsg, "410", "0958ILD170200545", "ISS001");

		} catch (Exception e) {
			logger.debug("SfmsOutMsg : " + e.getMessage());
			e.printStackTrace();
		}

	}

	public static boolean adviceHandler(String SfmsOutMsg, String msgType, String masterRef, String eventRef,
			String sourceSystem, String targetSystem) {

		boolean emailStatus = false;
		try {
			/** Remove UMAC **/
			String sfmsMessage = removeUMACTagProcess(SfmsOutMsg);
			logger.debug("Removed UMAC (:) ");

			/** Convert into printer format **/
			String printerFrndlySfmsMsg = SwiftFriendlyFormateAdaptee.getFriendlySWIFT(sfmsMessage, msgType);
			// logger.debug("Friendly formatted : \n" + printerFrndlyMsg);
			logger.debug("Friendly formatted (:) ");

			/** Remove Header **/
			String messageBodyWithoutHearder = removeSFMSHeader(printerFrndlySfmsMsg, msgType);
			logger.debug("Removed Header (:) ");

			/** Message title **/
			String messageTitle = getMessageTitle(msgType);

			/** SFMS IFSC address **/
			String sfmsIFSCAddr = getSfmsIFSCAddrDetails(SfmsOutMsg, msgType);

			/** PDF Doc generator **/
			byte[] pdfByteArray = SfmsAdvicePDFCreator.pdfDocumentCreator(msgType, messageTitle, sfmsIFSCAddr,
					messageBodyWithoutHearder);
			logger.debug("PdfByteArray (:) " + pdfByteArray);

			String[] toEmailId = null;
			toEmailId = getPrimaryCustomerEMailId(masterRef);

			Map<String, String> emailDetails = getSfmsAdviceEmailDetails("03SFMSCOPY", masterRef);

			// insert table, not required 2017-08-08
			// insertSFMSAdviceCopyLog();

			/** Send EMAIL **/
			emailStatus = sendEmailSfmsAdvice(SERVICE_EMAIL, OPERATION_SFMSADVICE, sourceSystem, "", sourceSystem,
					targetSystem, masterRef, eventRef, SfmsOutMsg, emailDetails.get("subject"),
					emailDetails.get("body"), pdfByteArray, "dummyFileName", msgType, toEmailId, null, null);
			logger.debug("EmailStatus : " + emailStatus);

		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException : " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error("IOException : " + e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("IOException : " + e.getMessage());
			e.printStackTrace();
		}
		return emailStatus;
	}

	public static String getSfmsIFSCAddrDetails(String sfmsinmsg, String msgType) {

		String sfmsOutHeaderAddr = "";

		try {
			String senderAddr = "";
			String receiverAddr = "";
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
			logger.debug("senderCountryCode " + senderCountryCode);
			if (senderCountryCode.equals("00") || senderCountryCode.equals("XY") || senderCountryCode.equals("XZ"))
				senderAddr = getIFSCAddr(senderIFSC, "SENDER");
			// logger.debug("SenderIFSC :- " + senderAddr);

			/** New RECEIVER IFSC/BIC **/
			String receiverIFSC = sfmsinmsg.substring(27, 38);
			String receiverCountryCode = sfmsinmsg.substring(31, 33);
			logger.debug("receiverCountryCode " + receiverCountryCode);
			if (senderCountryCode.equals("00") || senderCountryCode.equals("XY") || senderCountryCode.equals("XZ"))
				receiverAddr = getIFSCAddr(receiverIFSC, "RECEIVER");
			// logger.debug("ReceiverIFSC :- " + receiverAddr);

			sfmsOutHeaderAddr = senderAddr + "\n\n" + receiverAddr;

		} catch (Exception e) {
			logger.debug("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
		}
		return sfmsOutHeaderAddr;

	}

	public static String getIFSCAddr(String ifscCode, String SndRcrFlag) {

		String address = "";
		ResultSet rs = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			String ifscQuery = "select trim(IFSC) as IFSC, trim(BANK) as BANK, trim(BRANCH) as BRANCH, trim(BRAADD) as ADDRESS, trim(CITY) as CITY, trim(STATE) as STATE from EXTIFSCC where IFSC = ? ";

			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(ifscQuery);
			aPreparedStatement.setString(1, ifscCode);

			rs = aPreparedStatement.executeQuery();
			while (rs.next()) {
				/*
				 * Constructing Body
				 */
				StringBuilder body = new StringBuilder();
				// body.append("----------- Message Header -----------");

				if (rs.getString("IFSC") != null)
					body.append(SndRcrFlag + " IFSC: " + rs.getString("IFSC"));

				if (rs.getString("BANK") != null)
					body.append("\n" + rs.getString("BANK"));

				// if (rs.getString("BRANCH") != null)
				// body.append("\n" + rs.getString("BRANCH"));
				//
				// if (rs.getString("ADDRESS") != null)
				// body.append("\n" + rs.getString("ADDRESS"));

				if (rs.getString("CITY") != null)
					body.append("\n" + rs.getString("CITY"));

				if (rs.getString("STATE") != null)
					body.append("\n" + rs.getString("STATE"));

				// body.append("\n----------- Message Text -----------");
				// body.append("\n");
				address = body.toString();
				// logger.debug("Address: " + address);
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, rs);
		}
		return address;
	}

	// public static int insertSFMSAdviceCopyLog(String mastRefnc) {
	//
	// int resp = 0;
	// Connection dbConnection = null;
	// PreparedStatement preStmt = null;
	//
	// String insertTableSQL = "";
	// logger.debug("ToMailListQuery : " + insertTableSQL);
	// try {
	// dbConnection = DatabaseUtility.getTizoneConnection();
	// preStmt = dbConnection.prepareStatement(insertTableSQL);
	// preStmt.setInt(1, 11);
	// preStmt.setString(2, "mkyong");
	// // preStmt.setClob(arg0, arg1);
	// preStmt.setTimestamp(4, null);
	// resp = preStmt.executeUpdate();
	//
	// } catch (Exception e) {
	// logger.error("Primary Mail ID Exceptions!!! " + e.getMessage());
	// e.printStackTrace();
	//
	// } finally {
	// DatabaseUtility.surrenderPrepdConnection(dbConnection, preStmt, null);
	// }
	//
	// return resp;
	// }

	/**
	 * SX20LF
	 * 
	 * @since 2016-SEP-26
	 * @author Prasath Ravichandran
	 * @veesion 1.2
	 * @param mastRefnc
	 * @return
	 */
	public static String[] getPrimaryCustomerEMailId(String mastRefnc) {

		ResultSet res = null;
		String customerId = "";
		String[] mailList = null;
		Statement bStatement = null;
		String customerEMailId = "";
		Connection bConnection = null;

		// SELECT * FROM ETT_CUSTOMER_MAIL;
		String mailListQuery = "SELECT TRIM(SXCUS1) AS CUSTOMERID, TRIM(EMAIL) AS EMAILID FROM SX20LF S WHERE TRIM(SXCUS1) = ( SELECT CASE REFNO_PFIX WHEN 'ILC' then TRIM(PRICUSTMNM) WHEN 'TRF' then TRIM(PRICUSTMNM) WHEN 'ISB' THEN TRIM(PRICUSTMNM) WHEN 'FIC' THEN TRIM(PRICUSTMNM) WHEN 'FIL' THEN TRIM(PRICUSTMNM) WHEN 'SHG' THEN TRIM(PRICUSTMNM) WHEN 'OCL' THEN TRIM(PRICUSTMNM) WHEN 'IGT' THEN TRIM(PRICUSTMNM) WHEN 'CPBO' THEN TRIM(PRICUSTMNM) WHEN 'CPCO' THEN TRIM(PRICUSTMNM) WHEN 'CPHO' THEN TRIM(PRICUSTMNM) WHEN 'CPC' THEN TRIM(PRICUSTMNM) WHEN 'IRF' THEN TRIM(PRICUSTMNM) WHEN 'ICC' THEN TRIM(PRICUSTMNM) WHEN 'CPH' THEN TRIM(PRICUSTMNM) WHEN 'CPB' THEN TRIM(PRICUSTMNM) WHEN 'ODC' THEN TRIM(PRICUSTMNM) WHEN 'IDC' THEN TRIM(NPRCUSTMNM) WHEN 'ELC' THEN TRIM(NPRCUSTMNM) WHEN 'CPBI' THEN TRIM(NPRCUSTMNM) WHEN 'EGT' THEN TRIM(NPRCUSTMNM) WHEN 'ESB' THEN TRIM(NPRCUSTMNM) WHEN 'FRN' THEN TRIM(NPRCUSTMNM) WHEN 'TRF' THEN TRIM(NPRCUSTMNM) WHEN 'FSA' THEN TRIM(NPRCUSTMNM) WHEN 'ICL' THEN TRIM(NPRCUSTMNM) WHEN 'FEL' THEN TRIM(NPRCUSTMNM) WHEN 'FOC' THEN TRIM(NPRCUSTMNM) WHEN 'CPCI' THEN TRIM(NPRCUSTMNM) WHEN 'CPHI' THEN TRIM(NPRCUSTMNM) WHEN 'IDS' THEN TRIM(NPRCUSTMNM) WHEN 'IBP' THEN TRIM(NPRCUSTMNM) WHEN 'ICP' THEN TRIM(NPRCUSTMNM) END AS CUSTOMER "
				+ " FROM MASTER WHERE TRIM(MASTER_REF) = '" + mastRefnc + "')";
		// System.out.println("ToMailListQuery : " + mailListQuery);
		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			res = bStatement.executeQuery(mailListQuery);
			while (res.next()) {
				customerId = res.getString("CUSTOMERID");
				customerEMailId = res.getString("EMAILID");
				if (customerEMailId != null && !customerEMailId.isEmpty()) {
					mailList = customerEMailId.split(",");
				}
			}
			logger.debug("CustomerId : " + customerId);
			logger.debug("EmailId : " + customerEMailId);

		} catch (Exception e) {
			logger.error("Primary Mail ID Exceptions!!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, res);
		}

		return mailList;
	}

	private static Map<String, String> getSfmsAdviceEmailDetails(String documentName, String masterRef) {

		ResultSet rs = null;
		PreparedStatement pst = null;
		Connection connection = null;
		String defaultSubjBodyQuery = "";
		Map<String, String> emailDetails = new HashMap<String, String>();
		try {
			defaultSubjBodyQuery = "SELECT TEMPLATE_ID, DESCRIPTION, SUBJECT, BODY, IMPORTANT, DISCLAIMER FROM LOOKUP_EMAIL_SUBJECTBODY WHERE TEMPLATE_ID = ? ";
			logger.debug("SFMSAdviceCopyQuery:- " + defaultSubjBodyQuery);

			connection = DatabaseUtility.getThemebridgeConnection();
			pst = connection.prepareStatement(defaultSubjBodyQuery);
			pst.setString(1, documentName);
			rs = pst.executeQuery();
			while (rs.next()) {
				String subject = rs.getString("SUBJECT");
				subject = subject.replaceAll("&masterRef&", masterRef);
				emailDetails.put("subject", subject);

				emailDetails.put("templateid", rs.getString("TEMPLATE_ID"));
				emailDetails.put("description", rs.getString("DESCRIPTION"));
				/*
				 * Constructing Body
				 */
				StringBuilder body = new StringBuilder();
				body.append(rs.getString("BODY"));
				if (rs.getString("IMPORTANT") != null) {
					body.append("\n\n");
					body.append(rs.getString("IMPORTANT"));
				}
				if (rs.getString("DISCLAIMER") != null) {
					body.append("\n\n");
					body.append(rs.getString("DISCLAIMER"));
				}
				emailDetails.put("body", body.toString());
			}

		} catch (Exception e) {
			logger.error("Exceptions default subjBody!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(connection, pst, rs);
		}

		// logger.debug("emailDetails " + emailDetails);
		return emailDetails;
	}

	public static String getMessageTitle(String msgType) {

		String messagedesc = "";
		ResultSet rs = null;
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			String query = "SELECT TRIM(MESSAGETYPE) as MESSAGETYPE, TRIM(TYPEDESCRIPTION) as TYPEDESCRIPTION FROM LOOKUP_SWIFT_TYPES WHERE MESSAGETYPE = ?  ";
			logger.debug("EmailSubjectBodyMapping : " + query);

			connection = DatabaseUtility.getThemebridgeConnection();
			pst = connection.prepareStatement(query);
			pst.setString(1, "MT" + msgType);
			rs = pst.executeQuery();
			while (rs.next()) {
				messagedesc = rs.getString("TYPEDESCRIPTION");
				logger.debug("Message Desc : " + messagedesc);
			}

		} catch (Exception e) {
			logger.debug("Exceptions e" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(connection, pst, rs);
		}

		return messagedesc;
	}

	private static boolean isMatchesWholeWord(String source, String subItem) {
		String pattern = "\\b" + subItem + "\\b";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(source);
		return m.find();
	}

	/**
	 * 2.2
	 * 
	 * @param smfsmsginqueue
	 * @return
	 */
	private static String removeUMACTagProcess(String sfmsIncomingQueueMessage) {

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
	 * III . THIRD
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String removeSFMSHeader(String tiswiftMessage, String messageType) {

		// logger.error("GetSFMSBody " + swiftMessage);
		String SFMSBody = "";
		try {
			SFMSBody = tiswiftMessage.substring(tiswiftMessage.indexOf('\n') + 1);
			System.out.println(SFMSBody);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("GetSFMSBody Exceptions! " + e.getMessage());
		}

		// logger.error("GetSFMSBody return " + SFMSBody);
		return SFMSBody;
	}

	public static boolean sendEmailSfmsAdvice(String service, String operation, String zone, String branch,
			String sourceSys, String targetSys, String masterReference, String eventReference, String SfmsOutMsg,
			String emailSubject, String emailBodyText, byte[] attachmentData, String attachmentFileName, String msgType,
			String[] toEmailId, String[] ccEmailId, String[] bccEmailId) {

		// toEmailId[0] = "seshank.pulle@kotak.com";
		HashMap<String, String> bridgePropertiesMap = new HashMap<String, String>();
		bridgePropertiesMap = getThemeBridgeConfigurationMap();
		String SMTPHost = bridgePropertiesMap.get("EmailHost");
		String SMTPPort = bridgePropertiesMap.get("EmailPort");
		String EmailUser = bridgePropertiesMap.get("EmailUser");
		String EmailPassword = ""; // bridgePropertiesMap.get("EmailPassword");

		String errorDesc = "";
		Timestamp tiResTime = null;
		Timestamp bankResTime = null;
		boolean sendMailResponse = true;
		String ccMailAddressLogList = "";
		String toMailAddressLogList = "";
		String bccMailAddressLogList = "";
		Timestamp tiReqTime = DateTimeUtil.getSqlLocalDateTime();
		Timestamp bankReqTime = DateTimeUtil.getSqlLocalDateTime();

		final String emailUserId = EmailUser;
		final String emailPassword = EmailPassword;
		// logger.debug("EmailUser : " + EmailUser);
		// logger.debug("EmailPassword : " + EmailPassword);
		try {
			// logger.debug("Milestone 01 : Set Host and Port");
			Properties props = System.getProperties();
			props.put("mail.smtp.host", SMTPHost);
			props.put("mail.smtp.port", SMTPPort);
			// props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.auth", false);

			// logger.debug("Milestone 02 : Get session");
			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailUserId, emailPassword);
				}
			});
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EmailUser));

			// logger.debug("Milestone 03 : Set TO address");
			InternetAddress[] toAddress = new InternetAddress[toEmailId.length];
			for (int i = 0; i < toEmailId.length; i++) {
				toAddress[i] = new InternetAddress(toEmailId[i]);
			}
			for (int i = 0; i < toAddress.length; i++) {
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
				toMailAddressLogList = toMailAddressLogList + ", " + toAddress[i];
				logger.debug("SetToAddress >-->>" + toAddress[i] + "<<--<");
			}

			// logger.debug("Milestone 04 Set CC mail address");
			if (ccEmailId != null) {
				InternetAddress[] ccAddress = new InternetAddress[ccEmailId.length];
				for (int i = 0; i < ccEmailId.length; i++) {
					ccAddress[i] = new InternetAddress(ccEmailId[i]);
				}
				for (int i = 0; i < ccAddress.length; i++) {
					logger.debug("SetCcAddress >-->>" + ccAddress[i] + "<<--<");
					message.addRecipient(Message.RecipientType.CC, ccAddress[i]);
					ccMailAddressLogList = ccMailAddressLogList + ", " + ccAddress[i];
				}
			}

			// logger.debug("Milestone 05 : Set BCC mail address");
			if (bccEmailId != null) {
				InternetAddress[] bccAddress = new InternetAddress[bccEmailId.length];
				for (int i = 0; i < bccEmailId.length; i++) {
					bccAddress[i] = new InternetAddress(ccEmailId[i]);
				}
				for (int i = 0; i < bccAddress.length; i++) {
					logger.debug("SetBccAddress >-->>" + bccAddress[i] + "<<--<");
					message.addRecipient(Message.RecipientType.BCC, bccAddress[i]);
					bccMailAddressLogList = bccMailAddressLogList + ", " + bccAddress[i];
				}
			}

			// logger.debug("Milestone 06 : Set Subject and Body");
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			message.setSubject(emailSubject);
			message.setSentDate(new Date());
			Multipart multiPart = new MimeMultipart();
			BodyPart textPart = new MimeBodyPart();
			// body content alignment
			textPart.setContent(emailBodyText, "text/plain");
			multiPart.addBodyPart(textPart);

			// logger.debug("Milestone 07 : Set attachment");
			if (attachmentData != null) {
				attachmentFileName = masterReference + "-" + eventReference + "-" + msgType + ".pdf";
				MimeBodyPart attachFiles = new MimeBodyPart();
				attachFiles.setFileName(attachmentFileName);
				attachFiles.setContent(attachmentData, "application/pdf");
				multiPart.addBodyPart(attachFiles);
				// logger.debug("Document attached successfully");
			} else {
				// logger.debug("Attchment data is empty");
			}

			// logger.debug("Milestone 08 : Set Content");
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			message.setContent(multiPart);
			Transport.send(message);
			sendMailResponse = true;
			// logger.debug("Milestone 09 : Email Sent successfully..!" +
			// purpose);

		} catch (Exception e) {
			errorDesc = e.getMessage();
			logger.debug("EMail sending failed due to " + errorDesc);
			e.printStackTrace();
			sendMailResponse = false;

		} finally {
			String status = "FAILED";
			if (sendMailResponse) {
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			} else {
				status = ThemeBridgeStatusEnum.FAILED.toString();
			}
			ServiceLogging.pushLogData(service, operation, zone, branch, sourceSys, targetSys, masterReference,
					eventReference, status, emailSubject + "\n" + emailBodyText, status, toMailAddressLogList,
					SfmsOutMsg, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", msgType, "", false, "0",
					errorDesc);
		}

		logger.debug("Milestone 10 : Mail response " + sendMailResponse);
		return sendMailResponse;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> getThemeBridgeConfigurationMap() {

		Statement statement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		HashMap<String, String> map = new HashMap<String, String>();

		try {
			connection = DatabaseUtility.getThemebridgeConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT KEY, VALUE FROM BRIDGEPROPERTIES");
			while (resultSet.next()) {
				map.put(resultSet.getString("KEY"), resultSet.getString("VALUE"));
			}

		} catch (SQLException e) {
			logger.error("SQLException..!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(connection, statement, resultSet);
		}

		// logger.debug("Map size : " + map.size());
		return map;
	}
}
