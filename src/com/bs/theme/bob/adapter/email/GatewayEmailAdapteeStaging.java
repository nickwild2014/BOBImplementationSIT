package com.bs.theme.bob.adapter.email;

import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;
import static com.bs.theme.bob.template.util.KotakConstant.TARGET_SYSTEM;
import static com.bs.theme.bob.template.util.KotakConstant.ZONE;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
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

import com.bs.themebridge.entity.model.Postingstaging;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;

/**
 * End system communication implementation for Account Open services is handled
 * in this class.
 * 
 * @author PRASATH RAVICHANDRAN
 */
public class GatewayEmailAdapteeStaging {

	private final static Logger logger = Logger.getLogger(GatewayEmailAdapteeStaging.class.getName());

	// public static URL resource =
	// GatewayEmailAdapteeStaging.class.getResource(".");
	// String filePath = new File("").getAbsolutePath();

	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String emailCustomerId = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	/**
	 * <p>
	 * Process the incoming Account available balance Service XML from the TI
	 * </p>
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@link String} {@code value is TI Response}
	 * @throws Exception
	 */
	public String process(Postingstaging aPostingstaging) {

		logger.info(" ************ EMAIL.StagingAdaptee adaptee process started ************ ");

		String errorMsg = "";
		String adviceID = "";
		String adviceDesc = "";
		String servicelogID = "";
		String eventReference = "";
		String masterReference = "";
		String sourceSystem = "";
		String service = "";
		String operation = "";
		try {
			service = aPostingstaging.getService();
			operation = aPostingstaging.getOperation();
			sourceSystem = aPostingstaging.getSourcesystem();
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			tiRequest = aPostingstaging.getTirequest();
			// logger.debug("EMAIL TI Request:\n" + tiRequest);
			masterReference = aPostingstaging.getMasterreference();
			eventReference = aPostingstaging.getEventreference();
			BigDecimal postingtblID = aPostingstaging.getId();
			servicelogID = String.valueOf(postingtblID);
			// logger.debug("GWY EMAIL adaptee staging ! Milestone 01");
			// if (eventReference.startsWith("TRC")) { operation =
			// KotakConstant.OPERATION_EMAIL_TRACER;}

			String docDetails = aPostingstaging.getTiresponse();
			// logger.debug("docDetails : " + docDetails);
			String[] docDetailArray = docDetails.split("~");
			String documentId = docDetailArray[0];
			String subject = docDetailArray[1];
			String emailBody = docDetailArray[2];
			adviceID = docDetailArray[3];
			adviceDesc = docDetailArray[4];
			String customerId = null;
			String bccEmail = null;

			final String[] toEmailArray = getPrimaryMailId(masterReference);
			// logger.debug("GWY EMAIL adaptee staging ! Milestone 02");

			String toMailAddressLogList = "";
			if (toEmailArray != null) {
				// bankRequest = toEmailArray[0];
				for (int i = 0; i < toEmailArray.length; i++) {
					toMailAddressLogList = toMailAddressLogList + "\n" + toEmailArray[i];
				}
				bankRequest = toMailAddressLogList;
			} else
				bankRequest = "Email id is not available for the customer";

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			HashMap<String, String> bridgePropertiesMap = new HashMap<String, String>();
			bridgePropertiesMap = getBridgePropertiesConfig();

			customerId = emailCustomerId;
			boolean mailSendingStatus = sendEmail(bridgePropertiesMap.get("EmailHost"),
					bridgePropertiesMap.get("EmailUser"), bridgePropertiesMap.get("EmailPort"),
					bridgePropertiesMap.get("EmailPassword"), subject, emailBody, toEmailArray, documentId, bccEmail,
					customerId, masterReference, eventReference, adviceID, adviceDesc);
			// logger.debug("Emil mailSendingStatus : " + mailSendingStatus);

			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			if (mailSendingStatus)
				tiResponse = "SUCCEEDED";
			else if (!mailSendingStatus)
				tiResponse = "FAILED";

		} catch (Exception e) {
			errorMsg = e.getMessage();
			logger.error("EMAIL staging process Exceptions !" + e.getMessage());
			e.printStackTrace();
			return tiResponse = "FAILED";

		} finally {
			// NEW LOGGING
			// "GATEWAY", "EMAIL",
			boolean res = ServiceLogging.pushLogData(service, operation, sourceSystem, "", sourceSystem, TARGET_SYSTEM,
					masterReference, eventReference, tiResponse, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, servicelogID, "", adviceDesc, adviceID, false, "0",
					errorMsg);
		}

		logger.info(" ************ EMAIL.StagingAdaptee adaptee process ended ************ ");

		return tiResponse;
	}

	/**
	 * 
	 * @param host
	 * @param fromEmailID
	 * @param port
	 * @param password
	 * @param subject
	 * @param body
	 * @param to
	 * @param documentId
	 * @param bccEmail
	 * @param customerId
	 * @param masterReference
	 * @param eventReference
	 * @return
	 */
	public boolean sendEmail(String host, String fromEmailID, String port, String password, String subject, String body,
			String[] to, String documentId, String bccEmail, String customerId, String masterReference,
			String eventReference, String adviceID, String adviceDesc) {

		boolean result = true;
		final String SMTPHost = host; // 10.10.19.56
		final String SMTPPort = port; // 25
		final String EmailUser = fromEmailID; // tiplus@kotak.com

		// TODO comma separated to address
		final String[] AlertEmail = to;

		if (to == null)
			return false;
		final String EmailPassword = "";
		// final String EmailPassword = password;
		// logger.debug("SMTPHost : " + SMTPHost + "\tSMTPPort : " + SMTPPort);
		// logger.debug("customerId : " + customerId);

		try {
			// logger.debug("Milestone 01");
			Properties props = System.getProperties();
			props.put("mail.smtp.host", SMTPHost);
			props.put("mail.smtp.port", SMTPPort);
			props.put("mail.smtp.auth", "true");
			// logger.debug("Milestone 02");
			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(EmailUser, EmailPassword);
				}
			});

			// logger.debug("Seesion : " + session);
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EmailUser));

			// logger.debug("Milestone 03 : Set TO address");

			InternetAddress[] toAddress = new InternetAddress[AlertEmail.length];
			for (int i = 0; i < to.length; i++) {
				toAddress[i] = new InternetAddress(to[i]);
			}
			for (int i = 0; i < toAddress.length; i++) {
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
				// logger.debug("SetToAddress >-->>" + toAddress[i] + "<<--<");
			}

			// logger.debug("Milestone 04 : Set BCC mail address");
			if (bccEmail != null) {
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccEmail));
			}

			// logger.debug("Milestone 05 Set CC mail address");
			String[] ccMailList = null;
			// ccMailList = getSpecialInstructionMailId(customerId);

			// logger.debug("Milestone 06");
			message.setSubject(subject);
			message.setSentDate(new Date());
			Multipart multiPart = new MimeMultipart();
			BodyPart textPart = new MimeBodyPart();
			textPart.setContent(body, "text/plain");// body content alignment
			multiPart.addBodyPart(textPart);
			// logger.debug("Milestone 07");

			// "c4759437f01a2888:348d212a:155fc06c48c:766d";
			// Blob aData = getDocumentAsBlob(dpd.getDocumentID());
			byte[] attachmentData = getDocumentAsByteArray(documentId);
			// logger.debug("Milestone 08 attachmentData");

			/** Set Password to PDF document **/
			// String protectionKey = customerId;
			// byte[] attachmentData = pdfFileAsByteArray(aData,
			// protectionKey);

			if (attachmentData != null) {
				String attachmentFileName = masterReference + eventReference + ".pdf";
				MimeBodyPart attachFiles = new MimeBodyPart();
				attachFiles.setFileName(attachmentFileName);

				attachFiles.setContent(attachmentData, "application/pdf");
				// attachFiles.setContent(aData, "application/pdf");

				multiPart.addBodyPart(attachFiles);
				logger.debug("Document attached successfully");
			} else {
				logger.debug("Attchment data is null");
			}

			message.setContent(multiPart);
			Transport.send(message);
			logger.debug("Client advice Email Sent successfully!");

		} catch (Exception e) {
			logger.error("Mail sending failed due to : " + e.getMessage());
			e.printStackTrace();
			result = false;
		}

		logger.debug("Milestone 10 : " + result);
		return result;
	}

	/**
	 * 
	 * @param mastRefnc
	 * @return
	 */
	public String[] getPrimaryMailId(String mastRefnc) {

		ResultSet res = null;
		String[] mailList = null;
		Statement bStatement = null;
		Connection bConnection = null;
		String customerSplInsMailId = "";

		// SELECT * FROM ETT_CUSTOMER_MAIL;
		String mailListQuery = "SELECT TRIM(SXCUS1) AS CUSTOMERID, TRIM(EMAIL) AS EMAILID FROM SX20LF S WHERE TRIM(SXCUS1) = ( SELECT CASE REFNO_PFIX WHEN 'ILC' then TRIM(PRICUSTMNM) WHEN 'TRF' then TRIM(PRICUSTMNM) WHEN 'ISB' THEN TRIM(PRICUSTMNM) WHEN 'FIC' THEN TRIM(PRICUSTMNM) WHEN 'FIL' THEN TRIM(PRICUSTMNM) WHEN 'SHG' THEN TRIM(PRICUSTMNM) WHEN 'OCL' THEN TRIM(PRICUSTMNM) WHEN 'IGT' THEN TRIM(PRICUSTMNM) WHEN 'CPBO' THEN TRIM(PRICUSTMNM) WHEN 'CPCO' THEN TRIM(PRICUSTMNM) WHEN 'CPHO' THEN TRIM(PRICUSTMNM) WHEN 'CPC' THEN TRIM(PRICUSTMNM) WHEN 'IRF' THEN TRIM(PRICUSTMNM) WHEN 'ICC' THEN TRIM(PRICUSTMNM) WHEN 'CPH' THEN TRIM(PRICUSTMNM) WHEN 'CPB' THEN TRIM(PRICUSTMNM) WHEN 'ODC' THEN TRIM(PRICUSTMNM) WHEN 'IDC' THEN TRIM(NPRCUSTMNM) WHEN 'ELC' THEN TRIM(NPRCUSTMNM) WHEN 'CPBI' THEN TRIM(NPRCUSTMNM) WHEN 'EGT' THEN TRIM(NPRCUSTMNM) WHEN 'ESB' THEN TRIM(NPRCUSTMNM) WHEN 'FRN' THEN TRIM(NPRCUSTMNM) WHEN 'FSA' THEN TRIM(NPRCUSTMNM) WHEN 'ICL' THEN TRIM(NPRCUSTMNM) WHEN 'FEL' THEN TRIM(NPRCUSTMNM) WHEN 'FOC' THEN TRIM(NPRCUSTMNM) WHEN 'CPCI' THEN TRIM(NPRCUSTMNM) WHEN 'CPHI' THEN TRIM(NPRCUSTMNM) WHEN 'IDS' THEN TRIM(NPRCUSTMNM) WHEN 'IBP' THEN TRIM(NPRCUSTMNM) WHEN 'ICP' THEN TRIM(NPRCUSTMNM) END AS CUSTOMER "
				+ " FROM MASTER WHERE TRIM(MASTER_REF) = '" + mastRefnc + "')";
		// logger.debug("ToMailListQuery : " + mailListQuery);
		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			res = bStatement.executeQuery(mailListQuery);
			while (res.next()) {
				emailCustomerId = res.getString("CUSTOMERID");
				customerSplInsMailId = res.getString("EMAILID");

				if (customerSplInsMailId != null && !customerSplInsMailId.isEmpty()) {
					logger.debug("TEST");
					mailList = customerSplInsMailId.split(",");
				}
			}
			logger.debug("CustomerID: " + emailCustomerId);
			logger.debug("EMailID: " + customerSplInsMailId);

		} catch (Exception e) {
			logger.error("ToMailListQuery exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, res);

		}
		// logger.debug("To EMail Id List : " + mailList[0] + "\t" + mailList);
		return mailList;
	}

	/**
	 * 
	 * @param dmsId
	 * @return
	 */
	private byte[] getDocumentAsByteArray(String dmsId) {

		// getDocumentFromDb
		Connection aConnection = null;
		Statement aStatement = null;
		ResultSet aResultset = null;
		Blob blob = null;
		byte[] blobAsBytes = null;
		String getBlobQuery = "select * from CMS_ITEM where item_id = '" + dmsId + "'";
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(getBlobQuery);
			// logger.debug("Get Blob Query : " + getBlobQuery);
			while (aResultset.next()) {
				// logger.debug("Blob from DB:" +
				// aResultset.getBlob("item"));
				blob = aResultset.getBlob("item");
				int blobLength = (int) blob.length();
				blobAsBytes = blob.getBytes(1, blobLength);

				// release the blob and free up memory. (since JDBC 4.0)
				// 2016-08-04
				blob.free();
			}
		} catch (Exception e) {
			logger.error("Get getDocumentAsByteArray exceptions!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}
		// logger.debug("blobAsBytes : " + blobAsBytes);
		return blobAsBytes;
	}

	/**
	 * 
	 * @param customerid
	 * @return
	 */
	public static String[] getSpecialInstructionMailId(String customerid) {

		// String ccMailListId = "";
		ResultSet res = null;
		String[] mailList = null;
		Statement bStatement = null;
		Connection bConnection = null;
		String customerSplInsMailId = "";

		String ccMailListQuery = "SELECT TRIM(C.DETAILS) AS MAILLIST FROM CUSSPECINS C WHERE TRIM(CUSTOMER) = '"
				+ customerid
				+ "' AND NOTETYPE = (SELECT MYKEY95 FROM NOTETY24 WHERE TRIM(DESCRI56) = 'EmailCommunication')";
		// logger.debug("ccMailListQuery : " + ccMailListQuery);
		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			res = bStatement.executeQuery(ccMailListQuery);
			while (res.next()) {
				customerSplInsMailId = res.getString("MAILLIST");
				// ccMailListId = customerSplInsMailId;

				if (customerSplInsMailId != null && !customerSplInsMailId.equalsIgnoreCase("")) {
					mailList = customerSplInsMailId.split(",");
				}
			}
			// logger.debug("cc Mail List method: " + mailList);

		} catch (Exception e) {
			logger.error("Get SpecialInstructions mail id exceptions!" + e.getMessage());

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, res);
		}
		return mailList;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, String> getBridgePropertiesConfig() {

		Statement statement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		HashMap<String, String> map = new HashMap<String, String>();

		try {
			connection = DatabaseUtility.getThemebridgeConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT * FROM BRIDGEPROPERTIES");

			while (resultSet.next()) {
				map.put(resultSet.getString("KEY"), resultSet.getString("VALUE"));
			}

		} catch (SQLException e) {
			logger.error("Exception while get BridgeProperties");
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(connection, statement, resultSet);

		}
		// logger.debug("Map size : " + map.size());
		return map;
	}

	/**
	 * 
	 * @param a
	 * @throws Exception
	 */
	public static void main(String a[]) throws Exception {

		GatewayEmailAdapteeStaging sAdap = new GatewayEmailAdapteeStaging();
		sAdap.getPrimaryMailId("0958TRF170200007");

		// String inputXML =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\TI-Sample-Otherbanks\\GATEWAY.SMS01.xml");

		// sAdap.process(inputXML);
		// sAdap.insertToSMS(null);

		// logger.debug(getAmountValues("1,000.00 USD"));

	}

}
