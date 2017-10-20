package com.bs.theme.bob.adapter.email;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
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
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ValidationsUtil;

public class EmailAlertWorkFlowStatusUtil {

	private final static Logger logger = Logger.getLogger(EmailAlertWorkFlowStatusUtil.class.getName());

	public static void main(String[] args) throws Exception {

		// NotificationsEventStepAdaptee eventStepAdaptee = new
		// NotificationsEventStepAdaptee();
		// List<Map<String, String>> ts =
		// eventStepAdaptee.fxdealprocess("0958ICF160100195", "PAY001");
		// logger.debug(ts);
		// Map<String, String> m = getNotesMap("0958OCF160001055", "PAY001");
		// logger.debug(m.get("NOTE_TEXT"));

		// logger.debug(getNotesMap("0958OCF160001261", "PAY001"));
		// logger.debug(getMasterMap("0958OCF160001261", "PAY001"));

		logger.debug(getMasterMap("0176ELD160100576", "ADV001"));

		// logger.debug(getDiscrepanciesMap("0159ILF160100284",
		// "ISS001"));
	}

	public static boolean isEligibleWorkFlowEAlert(String stepId, String stepStatus) {

		logger.debug("******* 3.WORKFLOW REJ/ABT/CMP TRXN ************");

		boolean eligible = false;
		int insertedRows = 0;
		Connection conn = null;
		ResultSet resultSet = null;
		PreparedStatement ps = null;
		conn = DatabaseUtility.getThemebridgeConnection();
		HashMap<String, String> map = new HashMap<String, String>();
		String WorkFlowEmailQuery = "select STEPID, STEPSTATUS, TOEMAILID, CCEMAILID, ALERTACTION from WFNOTIFICATIONS where STEPID = ? and STEPSTATUS = ?  ";
		logger.debug("WorkFlowEmailQuery : " + WorkFlowEmailQuery);
		try {
			ps = conn.prepareStatement(WorkFlowEmailQuery);
			ps.setString(1, stepId);
			ps.setString(2, stepStatus);
			resultSet = ps.executeQuery();
			int count = 0;
			while (resultSet.next()) {
				count++;
				map.put("STEPID", resultSet.getString("STEPID"));
				map.put("STEPSTATUS", resultSet.getString("STEPSTATUS"));
				map.put("TOEMAILID", resultSet.getString("TOEMAILID"));
				map.put("CCEMAILID", resultSet.getString("CCEMAILID"));

				String alertFlag = resultSet.getString("ALERTACTION");
				map.put("ALERTACTION", alertFlag);
				if (alertFlag.equals("Y")) {
					eligible = true;
				}
			}

			logger.debug(stepId + stepStatus);

		} catch (SQLException e) {
			logger.error("SQLException! " + e.getMessage(), e);
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exception! " + e.getMessage(), e);
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, null);
		}
		return eligible;

	}

	public static boolean sendWorkFlowAlertMail(final String service, final String operation, final String zone,
			final String branch, final String sourceSys, final String targetSys, final String masterReference,
			final String eventReference, final String user, final String stepId, final String stepStatus,
			final String productlongname, final String eventlongname, final String customerMnemonicName) {

		// Reduce Time consuming
		Thread thread = new Thread() {
			public void run() {
				boolean status = sendWorkFlowAlertMail2(service, operation, zone, branch, sourceSys, targetSys,
						masterReference, eventReference, user, stepId, stepStatus, productlongname, eventlongname,
						customerMnemonicName);
				// logger.debug("EmailAlert-WorkFlowTrxn : " + status);
			}
		};
		thread.start();
		return true;
	}

	public static boolean sendWorkFlowAlertMail2(String service, String operation, String zone, String branch,
			String sourceSys, String targetSys, String masterReference, String eventReference, String user,
			String stepId, String stepStatus, String productlongname, String eventlongname,
			String customerMnemonicName) {

		boolean mailSendingStatus = false;
		try {
			String[] toEmailId = null;
			String toEmailAddrStr = ConfigurationUtil.getValueFromKey("WFTrxnStatusToEmailId");
			toEmailId = toEmailAddrStr.split(",");
			// logger.debug("toEmailId : " + toEmailId);

			String[] ccEmailId = null;
			String ccEmailIdStr = ConfigurationUtil.getValueFromKey("WFTrxnStatusCcEmailId");
			ccEmailId = ccEmailIdStr.split(",");
			// logger.debug("ccEmailId : " + ccEmailId);

			Map<String, String> notesMap = getNotesMap(masterReference, eventReference);
			Map<String, String> discrepanciesMap = getDiscrepanciesMap(masterReference, eventReference);

			Map<String, String> masterMap = getMasterMap(masterReference, eventReference);
			String trxncurrency = masterMap.get("CURRENCY");
			String trnxamount = masterMap.get("AMOUNT");
			trnxamount = amountConversion(trnxamount, trxncurrency);
			// String trnxosamount = notesMap.get("OSAMOUNT");
			String prodCode = masterMap.get("PRODUCT_CODE");
			String prodDesc = masterMap.get("PRODUCT_DESC");
			String eventPrefix = masterMap.get("EVENT_CODE");
			String eventDesc = masterMap.get("EVENT_DESC");
			String subProdCode = masterMap.get("SUB_PRODUCT_CODE");
			String subProdDesc = masterMap.get("SUB_PRODUCT_DESC");
			String valueDate = masterMap.get("VALUEDATE");

			String[] bccEmailId = null;
			byte[] attachmentData = null;
			String attachmentFileName = "";
			/** EMAIL SUBJECT CONTENT **/
			String emailSubject = "";
			emailSubject = emailSubject(productlongname, customerMnemonicName, trxncurrency, trnxamount, valueDate,
					masterReference, eventReference, prodCode, prodDesc, subProdCode, subProdDesc, eventPrefix,
					eventDesc, stepId, stepStatus);

			// logger.debug("Milestone 01 ");
			/** EMAIL BODY CONTENT **/
			String emailBodyText = emailBody(masterReference, eventReference, user, stepId, stepStatus, notesMap,
					discrepanciesMap, productlongname, eventlongname, prodCode, prodDesc, subProdCode, subProdDesc,
					eventPrefix, eventDesc, customerMnemonicName, trnxamount, trxncurrency, valueDate);

			// logger.debug("Milestone 02 ");
			mailSendingStatus = EmailAlertWorkFlowStatusUtil.sendEmailNotification(service, operation, zone, branch,
					sourceSys, targetSys, masterReference, eventReference, emailSubject, emailBodyText, attachmentData,
					attachmentFileName, toEmailId, ccEmailId, bccEmailId, stepId, stepStatus);

		} catch (Exception e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();
		}

		return mailSendingStatus;
	}

	public static String emailSubject(String productlongname, String customerMnemonicName, String trxncurrency,
			String trnxamount, String valueDate, String masterReference, String eventReference, String prodCode,
			String prodDesc, String subProdCode, String subProdDesc, String eventPrefix, String eventDesc,
			String stepId, String stepStatus) {

		String subject = "";
		// Discrepancy raised for System Reference No.XXXX for CRN XXXX : Client
		// Name : Product : Sub Product : Event : Currency and Amount

		try {
			if (stepStatus.equals("Rejected")) {
				subject = "Discrepancy raised for Ref No. ";
			} else if ((stepId.equals("Abort") && stepStatus.equals("Completed")) || stepStatus.equals("Aborted")) {
				subject = "Case Declined : Ref. No. ";
			} else if (stepId.equals("CBS Authoriser") && stepStatus.equals("Completed")) {
				subject = "Document processed : Ref. No. ";
			}

			if (ValidationsUtil.isValidString(masterReference))
				subject = subject + masterReference;

			if (ValidationsUtil.isValidString(eventReference))
				subject = subject + "-" + eventReference;

			if (ValidationsUtil.isValidString(customerMnemonicName))
				subject = subject + " for " + customerMnemonicName;

			// if (ValidationsUtil.isValidString(prodCode))
			// subject = subject + " : " + prodCode;
			if (ValidationsUtil.isValidString(prodDesc))
				subject = subject + " : " + prodDesc;

			// if (ValidationsUtil.isValidString(subProdCode))
			// subject = subject + " : " + subProdCode;
			if (ValidationsUtil.isValidString(subProdDesc))
				subject = subject + " : " + subProdDesc;

			// if (ValidationsUtil.isValidString(eventPrefix))
			// subject = subject + " : " + eventPrefix;
			if (ValidationsUtil.isValidString(eventDesc))
				subject = subject + " : " + eventDesc;

			if (ValidationsUtil.isValidString(trxncurrency))
				subject = subject + " : " + trxncurrency;

			if (ValidationsUtil.isValidString(trnxamount))
				subject = subject + " " + trnxamount;

			if (ValidationsUtil.isValidString(stepId))
				subject = subject + " - " + stepId;

			// if (ValidationsUtil.isValidString(productlongname))
			// subject = subject + productlongname;
			// if (ValidationsUtil.isValidString(customerMnemonicName))
			// subject = subject + "-" + customerMnemonicName;
			// if (ValidationsUtil.isValidString(trxncurrency))
			// subject = subject + "-" + trxncurrency;
			// if (ValidationsUtil.isValidString(trnxamount))
			// subject = subject + "-" + trnxamount;
			// if (ValidationsUtil.isValidString(masterReference))
			// subject = subject + "-" + masterReference;
			// if (ValidationsUtil.isValidString(eventReference))
			// subject = subject + "-" + eventReference;
			// if (ValidationsUtil.isValidString(stepId))
			// subject = subject + "-" + stepId;

		} catch (Exception e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();

		} finally {

		}

		// logger.debug("Notification Email Subject : " + subject);
		return subject;
	}

	public static String emailBody(String masterReference, String eventReference, String user, String stepId,
			String stepStatus, Map<String, String> notesMap, Map<String, String> discrepanciesMap,
			String productlongname, String eventlongname, String prodCode, String prodDesc, String subProdCode,
			String subProdDesc, String eventPrefix, String eventDesc, String customerMnemonicName, String trnxamount,
			String trxncurrency, String valueDate) {

		String emailBodytext = "";
		String emailBodyTemplate = "";

		if (stepStatus.equals("Rejected")) {
			emailBodyTemplate = ConfigurationUtil.getValueFromKey("WFRejectionENotificationTemplate");
			// logger.debug("WFRejectionENotificationTemplate : " +
			// emailBodyTemplate);

		} else if ((stepId.equals("Abort") && stepStatus.equals("Completed")) || stepStatus.equals("Aborted")) {
			emailBodyTemplate = ConfigurationUtil.getValueFromKey("WFAbortENotificationTemplate");
			// logger.debug("WFAbortENotificationTemplate : " +
			// emailBodyTemplate);

		} else if (stepId.equals("CBS Authoriser") && stepStatus.equals("Completed")) {
			emailBodyTemplate = ConfigurationUtil.getValueFromKey("WFCompleteENotificationTemplate");
			// logger.debug("WFCompleteENotificationTemplate : " +
			// emailBodyTemplate);

		} else {
			// emailBodyTemplate =
			// ConfigurationUtil.getValueFromKey("CSMEmailBodyTemplate");
			// logger.debug("CSMEmailBodyTemplate : " + emailBodyTemplate);

		}

		try {
			// Map<String, String> notesMap = getNotesMap(masterReference,
			// eventReference);
			String notes = notesMap.get("NOTE_TEXT");
			String discrepancies = notesMap.get("CHECKLIST_DESCR");//

			Map<String, String> mapTokens = new HashMap<String, String>();
			mapTokens.put("MasterReference", masterReference);
			mapTokens.put("EventReference", eventReference + " - " + eventlongname);
			mapTokens.put("User", user);
			mapTokens.put("StepId", stepId);
			mapTokens.put("Status", stepStatus);
			mapTokens.put("Productlongname", productlongname);// Not used

			if (ValidationsUtil.isValidString(prodCode) && ValidationsUtil.isValidString(prodDesc))
				mapTokens.put("ProductData", prodCode + " - " + prodDesc);
			else
				mapTokens.put("ProductData", "");

			if (ValidationsUtil.isValidString(subProdCode) && ValidationsUtil.isValidString(subProdDesc))
				mapTokens.put("SubProductData", subProdCode + " - " + subProdDesc);
			else
				mapTokens.put("SubProductData", "");

			if (ValidationsUtil.isValidString(eventPrefix) && ValidationsUtil.isValidString(eventDesc))
				mapTokens.put("EventData", eventPrefix + " - " + eventDesc);
			else
				mapTokens.put("EventData", "");

			if (ValidationsUtil.isValidString(customerMnemonicName))
				mapTokens.put("CustomerMnemonicName", customerMnemonicName);
			else
				mapTokens.put("CustomerMnemonicName", "");

			if (ValidationsUtil.isValidString(trnxamount))
				mapTokens.put("trnxamount", trnxamount);
			else
				mapTokens.put("trnxamount", "");

			if (ValidationsUtil.isValidString(trxncurrency))
				mapTokens.put("trxncurrency", trxncurrency);
			else
				mapTokens.put("trxncurrency", "");

			// Abort Completed
			// BRANCHLOG Aborted

			if ((stepStatus.equals("Rejected") || stepStatus.equals("Aborted")
					|| (stepId.equals("Abort") && stepStatus.equals("Completed")))) {
				if (discrepancies == null)
					discrepancies = "";
				if (notes == null)
					notes = "";
				mapTokens.put("Notes", notes + "\n" + discrepancies);
			} else {
				mapTokens.put("Notes", "");
			}
			// logger.debug("valueDate : " + valueDate);
			if (ValidationsUtil.isValidString(valueDate))
				mapTokens.put("trxndate", valueDate);
			else
				mapTokens.put("trxndate", "");

			MapTokenResolver resolver = new MapTokenResolver(mapTokens);
			Reader fileValue = new StringReader(emailBodyTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			emailBodytext = reader.toString();
			reader.close();

		} catch (IOException e) {
			logger.error("Exception" + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("emailBodytext : \n" + emailBodytext);
		return emailBodytext;
	}

	/**
	 * @since 2016-OCT-18
	 * @version v1.2
	 * @author Prasath Ravichandran
	 * 
	 * @param service
	 *            {@code allows }{@link String}
	 * @param operation
	 *            {@code allows }{@link String}
	 * @param zone
	 *            {@code allows }{@link String}
	 * @param branch
	 *            {@code allows }{@link String}
	 * @param sourceSys
	 *            {@code allows }{@link String}
	 * @param targetSys
	 *            {@code allows }{@link String}
	 * @param masterReference
	 *            {@code allows }{@link String}
	 * @param eventReference
	 *            {@code allows }{@link String}
	 * @param emailSubject
	 *            {@code allows }{@link String}
	 * @param emailBodyText
	 *            {@code allows }{@link String}
	 * @param attachmentData
	 *            {@code allows }{@link String}
	 * @param attachmentFileName
	 *            {@code allows }{@link String}
	 * @param toEmailId
	 *            {@code allows }{@link String}
	 * @param ccEmailId
	 *            {@code allows }{@link String}
	 * @param bccEmailId
	 *            {@code allows }{@link String}
	 * @param purpose
	 *            {@code allows }{@link String}
	 * @return
	 */
	public static boolean sendEmailNotification(String service, String operation, String zone, String branch,
			String sourceSys, String targetSys, String masterReference, String eventReference, String emailSubject,
			String emailBodyText, byte[] attachmentData, String attachmentFileName, String[] toEmailId,
			String[] ccEmailId, String[] bccEmailId, String stepId, String stepStatus) {

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
					// logger.debug("SetCcAddress >-->>" + ccAddress[i] +
					// "<<--<");
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
			// String with body Text
			String bodyText = addColor("This line is red.", Color.RED);
			bodyText += "<br>" + addColor("This line is blue.", Color.BLUE);
			bodyText += "<br>" + addColor("This line is black.", Color.BLACK);

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
				attachmentFileName = masterReference + eventReference + ".pdf";
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
			logger.error("EMail sending failed due to " + errorDesc);
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
					ccMailAddressLogList, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
					errorDesc);
		}

		logger.debug("Milestone 10 : Mail response " + sendMailResponse);
		return sendMailResponse;
	}

	/**
	 * add color
	 * 
	 * @param msg
	 * @param color
	 * @return
	 */
	public static String addColor(String msg, Color color) {
		String hexColor = String.format("#%06X", (0xFFFFFF & color.getRGB()));
		String colorMsg = "<FONT COLOR=\"#" + hexColor + "\">" + msg + "</FONT>";
		return colorMsg;
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

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> getNotesMap(String masterR, String eventR) {

		ResultSet resultSet = null;
		Connection connection = null;
		PreparedStatement psstatement = null;
		HashMap<String, String> map = new HashMap<String, String>();

		String query = "SELECT TRIM(MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF, MAS.CCY AS CURRENCY, MAS.AMOUNT AS AMOUNT, MAS.AMT_O_S AS OSAMOUNT, N.TYPE, N.CODE, N.NOTE_TEXT, N.STYLE, N.EMPHASIS, N.CREATED_AT FROM NOTE N, BASEEVENT BEV, MASTER MAS WHERE BEV.KEY97 = N.NOTE_EVENT AND MAS.KEY97 = BEV.MASTER_KEY AND TRIM(MAS.MASTER_REF) = ? AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) = ? ORDER BY N.CREATED_AT ASC ";
		// logger.debug("NotesQuery : " + query);
		try {
			connection = DatabaseUtility.getTizoneConnection();
			psstatement = connection.prepareStatement(query);
			psstatement.setString(1, masterR);
			psstatement.setString(2, eventR);
			resultSet = psstatement.executeQuery();

			while (resultSet.next()) {
				map.put("NOTE_TEXT", resultSet.getString("NOTE_TEXT"));
				map.put("STYLE", resultSet.getString("STYLE"));
				map.put("CODE", resultSet.getString("CODE"));
				map.put("TYPE", resultSet.getString("TYPE"));
				map.put("EMPHASIS", resultSet.getString("EMPHASIS"));
				map.put("CREATED_AT", resultSet.getString("CREATED_AT"));
				map.put("MASTER_REF", resultSet.getString("MASTER_REF"));
				map.put("EVENT_REF", resultSet.getString("EVENT_REF"));

				map.put("CURRENCY", resultSet.getString("CURRENCY"));
				map.put("AMOUNT", resultSet.getString("AMOUNT"));
				map.put("OSAMOUNT", resultSet.getString("OSAMOUNT"));
			}

		} catch (SQLException e) {
			logger.error("SQLException..!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(connection, psstatement, resultSet);
		}

		// logger.debug("Map size : " + map.size());
		return map;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> getDiscrepanciesMap(String masterR, String eventR) {

		PreparedStatement psstatement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		HashMap<String, String> map = new HashMap<String, String>();

		String query = "select MASTER_REF, EVENTREF, CHECKLIST_DESCR, REJ_REASON from ETT_WF_CHKLST_TRACKING where MASTER_REF= ? and EVENTREF = ? and MANDATORY= 'REJ' ";
		// logger.debug("DiscrepanciesQuery : " + query);
		try {
			connection = DatabaseUtility.getTizoneConnection();
			psstatement = connection.prepareStatement(query);
			psstatement.setString(1, masterR);
			psstatement.setString(2, eventR);
			resultSet = psstatement.executeQuery();

			while (resultSet.next()) {
				map.put("CHECKLIST_DESCR", resultSet.getString("CHECKLIST_DESCR"));
				map.put("REJ_REASON", resultSet.getString("REJ_REASON"));
				map.put("MASTER_REF", resultSet.getString("MASTER_REF"));
				map.put("EVENT_REF", resultSet.getString("EVENTREF"));
			}

		} catch (SQLException e) {
			logger.error("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(connection, psstatement, resultSet);
		}

		// logger.debug("Map size : " + map.size());
		return map;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> getMasterMap(String masterR, String eventR) {

		PreparedStatement psstatement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		HashMap<String, String> map = new HashMap<String, String>();

		// String query = "SELECT TRIM(MASTER_REF) AS MASTER_REF,
		// TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF,
		// MAS.REFNO_PFIX AS PRODUCT_CODE, MAS.CCY AS CURRENCY, MAS.AMOUNT AS
		// AMOUNT, MAS.AMT_O_S AS OSAMOUNT FROM BASEEVENT BEV, MASTER MAS WHERE
		// MAS.KEY97 = BEV.MASTER_KEY AND TRIM(MAS.MASTER_REF) = ? AND
		// TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) = ? ";

		/** 2017-FEB-17 **/
		// String query = "SELECT TRIM(MASTER_REF) AS MASTER_REF,
		// TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF,
		// TRIM(MAS.REFNO_PFIX) AS PRODUCT_CODE, TRIM(PT.NAME) AS
		// SUB_PRODUCT_CODE, TRIM(PT.DESCRIP) AS SUB_PRODUCT_DESC,
		// TRIM(BEV.REFNO_PFIX) AS EVENT_CODE, MAS.CCY AS CURRENCY, MAS.AMOUNT
		// AS AMOUNT, MAS.AMT_O_S AS OSAMOUNT, TO_CHAR(DPC.PROCDATE,
		// 'DD-MON-YYYY') AS VALUEDATE FROM BASEEVENT BEV, MASTER MAS, PRODTYPE
		// PT, DLYPRCCYCL DPC WHERE MAS.KEY97 = BEV.MASTER_KEY AND MAS.PRODTYPE
		// = PT.KEY97 AND TRIM(MAS.MASTER_REF) = ? AND
		// TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) = ? ";

		String query = "SELECT TRIM(MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF, TRIM(MAS.REFNO_PFIX) AS PRODUCT_CODE, TRIM(EX301.LONGNA85) AS PRODUCT_DESC, TRIM(BEV.REFNO_PFIX) AS EVENT_CODE, TRIM(EX30.LONGNA85) AS EVENT_DESC, TRIM(PT.NAME) AS SUB_PRODUCT_CODE, TRIM(PT.DESCRIP) AS SUB_PRODUCT_DESC,  MAS.CCY AS CURRENCY,MAS.AMOUNT AS AMOUNT, MAS.AMT_O_S AS OSAMOUNT, TO_CHAR(DPC.PROCDATE,'DD-MON-YYYY') AS VALUEDATE FROM BASEEVENT BEV, MASTER MAS, PRODTYPE PT, EXEMPL30 EX30, DLYPRCCYCL DPC, EXEMPL30 EX301 WHERE MAS.KEY97 = BEV.MASTER_KEY AND MAS.PRODTYPE = PT.KEY97 AND EX30.KEY97 = BEV.EXEMPLAR AND EX301.KEY97 = MAS.EXEMPLAR  AND TRIM(MAS.MASTER_REF) = ? AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) = ? ";
		logger.debug("MasterQuery : " + query);
		try {
			connection = DatabaseUtility.getTizoneConnection();
			psstatement = connection.prepareStatement(query);
			psstatement.setString(1, masterR);
			psstatement.setString(2, eventR);
			resultSet = psstatement.executeQuery();

			while (resultSet.next()) {
				map.put("MASTER_REF", resultSet.getString("MASTER_REF"));
				map.put("EVENT_REF", resultSet.getString("EVENT_REF"));
				map.put("CURRENCY", resultSet.getString("CURRENCY"));

				map.put("PRODUCT_CODE", resultSet.getString("PRODUCT_CODE"));
				map.put("PRODUCT_DESC", resultSet.getString("PRODUCT_DESC"));

				map.put("EVENT_CODE", resultSet.getString("EVENT_CODE"));
				map.put("EVENT_DESC", resultSet.getString("EVENT_DESC"));

				map.put("SUB_PRODUCT_CODE", resultSet.getString("SUB_PRODUCT_CODE"));
				map.put("SUB_PRODUCT_DESC", resultSet.getString("SUB_PRODUCT_DESC"));

				map.put("AMOUNT", resultSet.getString("AMOUNT"));
				map.put("OSAMOUNT", resultSet.getString("OSAMOUNT"));
				map.put("VALUEDATE", resultSet.getString("VALUEDATE"));
			}

		} catch (SQLException e) {
			logger.error("SQLException..!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(connection, psstatement, resultSet);
		}

		// logger.debug("Map size : " + map.size());
		return map;
	}

	/**
	 */
	public static String amountConversion(String tiamount, String CCY) {

		String amount = "";
		BigDecimal bg = null;
		if (ValidationsUtil.isValidString(tiamount) && ValidationsUtil.isValidString(CCY)
				&& (CCY.equals("OMR") || CCY.equals("BHD") || CCY.equals("KWD"))) {
			bg = new BigDecimal(tiamount);
			bg = bg.divide(new BigDecimal(1000));
		} else if (ValidationsUtil.isValidString(tiamount) && ValidationsUtil.isValidString(CCY) && CCY.equals("JPY")) {
			bg = new BigDecimal(tiamount);
		} else if (ValidationsUtil.isValidString(tiamount) && ValidationsUtil.isValidString(CCY)) {
			bg = new BigDecimal(tiamount);
			bg = bg.divide(new BigDecimal(100));
		}
		if (bg != null)
			amount = bg.toString();

		return amount;
	}
}
