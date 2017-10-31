package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_FAILURE_EMAIL_RTGSNEFT;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_GATEWAY;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.email.EmailAlertServiceFailureUtil;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.NeftRtgsXpath;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.test.XmlSpecialCharacterEncoding;

public class GatewayRtgsNeftAdapteeStaging {

	private final static Logger logger = Logger.getLogger(GatewayRtgsNeftAdapteeStaging.class.getName());

	private String branch = "";
	private String errorMsg = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String status = "FAILED";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public GatewayRtgsNeftAdapteeStaging() {
	}

	public static void main(String a[]) throws Exception {
		//getISOMessage("", null);
		//System.out.println(getTcpIP());
		//System.out.println(getTcpPort());
		
		
		//String request = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\RTGSGateWay.txt");
		//new GatewayRtgsNeftAdapteeStaging().process(request);
		
		//String request = ThemeBridgeUtil.readFile("C:\\Users\\subhash\\Desktop\\RTGSConnectResponse.txt");
		//System.out.println(getTagAddressValue(request,"126:").replaceAll(" ", ""));
		
		
	}
	
	static String getTagAddressValue(String coverMsg,String tagNumber)
	{
		String address = "";
		try
		{
		StringBuilder sb = new StringBuilder();
		sb.append(coverMsg);
		String temp ="";
		temp = sb.substring(sb.indexOf(tagNumber)+4, sb.length());
		address = temp.substring(0, temp.indexOf(System.getProperty("line.separator"))).toString();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return address;
		}
		return address;
	}

	public String process(String requestXML) throws Exception {

		logger.info(" ************ GATEWAY.NEFTRTGS adaptee process started ************ ");

		tiRequest = "";
		tiResponse = "";
		bankRequest = "";
		String response = "";
		String utrNumber ="";
		// bankResponse = "";
		String rType = "";
		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
			logger.debug("GATEWAY.NEFTRTGS TI Request : \n" + tiRequest);

			requestXML = XmlSpecialCharacterEncoding.xmlEscapeText(requestXML);
			// requestXML =
			// XmlSpecialCharacterEncoding.escapeXmlSpecialCahr(requestXML);
			// logger.debug("GATEWAY.NEFTRTGS Special char removed : " +
			// requestXML);

			Map<String, String> rtgsNeftMapList = getNeftRtgsTagValues(requestXML);
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();

			String paymentType = rtgsNeftMapList.get("paymentType");
			String paymentSubType = rtgsNeftMapList.get("paymentSubType");
			

			if (paymentType.equalsIgnoreCase("RTGS") || (paymentType.equalsIgnoreCase("NEFT"))) {

				// insertRowStatusCount = pushBankPaymentHubExtDataBase(rtgsNeftMapList);
				 response = pushMessage(tiRequest, rtgsNeftMapList);
				 utrNumber =  getTagAddressValue(response,"126:");
				if(utrNumber!=null)
				{
					utrNumber = utrNumber.replaceAll(" ", "");
				}
				System.out.println("utrNumber "+utrNumber);
				try {
					if(utrNumber!=null) {
				updateRegularUtrSlNo(utrNumber, rtgsNeftMapList.get("creditAmount"), rtgsNeftMapList.get("masterReference"),
						rtgsNeftMapList.get("eventReference"));
					}
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
				rType = "R42-" + paymentSubType + "-" + paymentType;

			}
			// else if ((paymentType.equalsIgnoreCase("RTGS") &&
			// paymentSubType.equalsIgnoreCase("B2C"))
			// || (paymentType.equalsIgnoreCase("NEFT") &&
			// paymentSubType.equalsIgnoreCase("B2C"))
			// || (paymentType.equalsIgnoreCase("NEFT") &&
			// paymentSubType.equalsIgnoreCase("B2B"))) {
			//
			// insertRowStatusCount = pushCustomerPaymentHubExtDataBase(rtgsNeftMapList);
			// rType = "R41-" + paymentSubType + "-" + paymentType;
			// }
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			if (!response.isEmpty()) {
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
				tiResponse = getTIResponse(status);
			} else {
				status = ThemeBridgeStatusEnum.FAILED.toString();
				tiResponse = getTIResponse(status);
			}

			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.NEFTRTGS TI Response : \n" + tiResponse);

		} catch (Exception e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			tiResponse = getTIResponse(status);
			errorMsg = e.getMessage();
			e.printStackTrace();

		} finally {
			// logger.info("Enter into finally block utrSlNo : " + utrSlNo);
			boolean res = ServiceLogging.pushLogData(SERVICE_GATEWAY, operation, sourceSystem, branch, sourceSystem,
					targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,response, tiReqTime, bankReqTime, bankResTime,
					tiResTime, "", "", utrNumber, rType, false, "0", errorMsg);

			if (status.equals("FAILED"))
				EmailAlertServiceFailureUtil.sendFailureAlertMail(SERVICE_GATEWAY, OPERATION_FAILURE_EMAIL_RTGSNEFT,
						masterReference, eventReference, sourceSystem, targetSystem);
		}
		logger.info(" ************ GATEWAY.NEFTRTGS adaptee process End ************ ");
		return tiResponse;

	}

	private String getBankResponseFromBankRequest(String bankRequest)
			throws HttpException, IOException, FinacleServiceException {

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.debug("Exception..! " + e.getMessage());
			e.printStackTrace();

		}
		return result;
	}
	
	public String pushMessage(String requestMessage, Map<String, String> rtgsNeftMapList) {
		logger.info("Enter into TCP/IP pushMessage method");
		String serverName = getTcpIP();
		int port = Integer.parseInt(getTcpPort());
		String reposneMessage = "";
		DataOutputStream out = null;
		InputStream in = null;
		Socket client = null;
//		String isoMessage = getISOMessage(requestMessage, rtgsNeftMapList);
//		try {
//			reposneMessage = getBankResponseFromBankRequest(isoMessage);
//		} catch (HttpException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (FinacleServiceException e) {
//			e.printStackTrace();
//		}
		
		try {
			byte[] isoMessage = getISOMessage(requestMessage, rtgsNeftMapList);
			bankRequest=isoMessage.toString();
			logger.info("Connecting to " + serverName + " on port " + port);
			logger.info("ISOMsg RESULT : " + isoMessage);
			client = new Socket(serverName, port);
			client.setSoTimeout(100000);
			logger.info("Just connected to " + client.getRemoteSocketAddress());
			OutputStream outToServer = client.getOutputStream();
			out = new DataOutputStream(outToServer);
			out.write(isoMessage);
			out.flush();
			logger.info("Push message to server");
			 in = client.getInputStream();
			reposneMessage = IOUtils.toString(in, StandardCharsets.UTF_8);
//			DataInputStream dis = new DataInputStream(in);
//			int len = dis.readInt();
//			byte[] data = new byte[len];
//			if (len > 0)
//				dis.readFully(data);
//			reposneMessage = new String(data);
			logger.info("TCP / IP Server result =>" + reposneMessage);
			
		} catch (Exception e) {
			e.printStackTrace();
			reposneMessage = "";
		} finally {
			try {
				client.close();
				out.close();
				in.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return reposneMessage;
	}
	
	

	public static byte[] getISOMessage(String requestMessage, Map<String, String> rtgsNeftMapList) {
		String isoMessage = "";
		byte[] data = null;
		try {
			// Create Packager based on XML that contain DE type
			GenericPackager packager = new GenericPackager("Files/isoFields.xml");
			// GenericPackager packager = new GenericPackager("Files/basic.xml");

			// Create ISO Message
			ISOMsg isoMsg = new ISOMsg();
			isoMsg.setPackager(packager);
			isoMsg.setMTI("1200");// Message Type Identifier
			//isoMsg.set(1, "");
			isoMsg.set(2, "0000000000000000");// DEFAULT VALUE
			isoMsg.set(3, "970000");// Processing Code
			isoMsg.set(4, rtgsNeftMapList.get("amount"));// Transaction Amount
			// isoMsg.set(4,"1000USD");//Transaction Amount
			String uniqueNum = getUniqueNumber().replaceAll(" ", "");
			isoMsg.set(11, uniqueNum);// System Trace Audit Number(12 DIGIT UNIQUE)
			isoMsg.set(12, getTimeStampFromSql());// Date and time of Local Txn
			isoMsg.set(17, getSqlDate());// Date Capture
			isoMsg.set(24, "200");// Function Code DEFAULT 200
			isoMsg.set(32, "504511");// Acquiring institution identification code Default value 504511
			// isoMsg.set(34,"");//Card ID (if 002 value exist then no need for this value,
			// else value should be dafault to TIUSER)
			isoMsg.set(37, uniqueNum);// Retrival Reference No (same as TAG 011)
			//isoMsg.set(39, "");// Action Code (DEFAULT NULL)
			isoMsg.set(41, "TRD");// Device ID Default value TRD
			isoMsg.set(43, "TRADE");// Device Desc Default value TRADE
			//isoMsg.set(48, "");// Additional data private
			isoMsg.set(49, "INR");// Currency Code Default value INR
			isoMsg.set(102, "012        0000    00000000000000");// Account Identifier 1 (Value is 012 (Bankcode) + 8
			isoMsg.set(123, "TRD");// Reserved for Private Use Deafault value TRD
			String paymentType = rtgsNeftMapList.get("paymentType");
			String debitAccNo = rtgsNeftMapList.get("debitAccNo");
			String creditAccNo = rtgsNeftMapList.get("creditAccNo"); 
			String debitAmount = rtgsNeftMapList.get("debitAmount");
			String creditAmount = rtgsNeftMapList.get("creditAmount");
			String NARRATION = rtgsNeftMapList.get("NARRATION");
			String debitBranchCode = rtgsNeftMapList.get("debitBranchCode");
			String creditBranchCode = rtgsNeftMapList.get("creditBranchCode");
			String beneName = rtgsNeftMapList.get("beneName");
			String beneIFSCcode = rtgsNeftMapList.get("beneIFSCcode");
			String beneAcType = rtgsNeftMapList.get("beneAcType");
			String beneAcNo = rtgsNeftMapList.get("beneAcNo");
			String field125 = "" + paymentType + "|" + debitAccNo + "|" + debitAmount + "|D||" + NARRATION + "|N|Y|"
					+ debitBranchCode + "~" + creditAccNo + "|" + creditAmount + "|C||" + NARRATION + "" + "|N|Y|"
					+ creditBranchCode + "~";
			String field127 = "" + paymentType + "|BOB|" + beneName + "|" + beneIFSCcode + "|" + beneAcType + "|"
					+ beneAcNo + "";
			isoMsg.set(125, field125);// Reserved for Private Use
			// isoMsg.set(126,);//Reserved for Private Use
			isoMsg.set(127, field127);// Reserved for Private Use
			// print the DE list
			 isoMessage = logISOMsg(isoMsg);
			 data = isoMsg.pack();
			// Get and print the output result
		} catch (ISOException e) {
			System.out.println(e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;

	}

	public static String getUniqueNumber() {

		String unNo = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"select 'UNQ'||to_char(RTGS_NEFT_UNIQUE_NO_SEQ.nextval,'00000000') as RTGS_NEFT_UNIQUE from dual");
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				unNo = aResultset.getString("RTGS_NEFT_UNIQUE");
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return unNo;
	}
	public static String getTcpIP() {

		String ip = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement("select value as RTGSNEFTTcpIp from BRIDGEPROPERTIES where key ='RTGSNEFTTcpIp'");
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				ip=aResultset.getString("RTGSNEFTTcpIp");
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return ip;
	}
	public static String getTcpPort() {

		String port = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement("select value as RTGSNEFTTcpPORT from BRIDGEPROPERTIES where key ='RTGSNEFTTcpPORT'");
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				port=aResultset.getString("RTGSNEFTTcpPORT");
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return port;
	}

	public static String getTimeStampFromSql() {

		String timeStamp = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection
					.prepareStatement("select to_char(to_date(SYSDATE),'YYYYMMDDHH24MISS') TimeStamp from dual");
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				timeStamp = aResultset.getString("TimeStamp");
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return timeStamp;
	}

	public static String getSqlDate() {

		String sqlDate = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection
					.prepareStatement("select to_char(to_date(SYSDATE),'YYYYMMDD') TimeStamp from dual");
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				sqlDate = aResultset.getString("TimeStamp");
			}

		} catch (Exception e) {
			logger.debug("Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return sqlDate;
	}

	private static String logISOMsg(ISOMsg msg) {
		logger.info("----ISO MESSAGE-----");
		String message = "";
		try {
			logger.info("  MTI : " + msg.getMTI());
			 message = "MessageId: "+msg.getMTI();
			StringBuilder sb = new StringBuilder();
			for (int index = 1; index <= msg.getMaxField(); index++) {
				
				if (msg.hasField(index)) {
					// logger.info(" Field-" + index + " : " + msg.getString(index));
					sb.append("Field " + String.format("%03d", index) + " : " + msg.getString(index));
					sb.append(System.getProperty("line.separator"));
				}
			}
			message=message+System.getProperty("line.separator");
			message = message+sb.toString();
		} catch (ISOException e) {
			e.printStackTrace();
		} finally {
			logger.info("--------------------");
		}
		System.out.println("final message \n\n"+ message);
		return message;
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Map<String, String> getNeftRtgsTagValues(String requestXML) {

		Map<String, String> rtgsNeftMapList = new HashMap<String, String>();

		try {
			branch = XPathParsing.getValue(requestXML, NeftRtgsXpath.Branch);
			operation = XPathParsing.getValue(requestXML, NeftRtgsXpath.operation);
			correlationId = XPathParsing.getValue(requestXML, NeftRtgsXpath.correlationIdXPath);

			String requestOperation = "/ServiceRequest/" + operation.toLowerCase();
			// logger.debug("Milestone 01 : " + requestOperation);

			String masterRef = XPathParsing.getValue(requestXML, requestOperation + "/MasterReference");
			masterReference = masterRef;
			String eventRef = XPathParsing.getValue(requestXML, requestOperation + "/EventReference");
			eventReference = eventRef;
			/** **/
			// String channelId = "2"; // ALWAYS - CASH MANAGEMENT
			String channelId = "5"; // JIRA2801, 2-as per email confirmation
			String paymentType = XPathParsing.getValue(requestXML,
					requestOperation + NeftRtgsXpath.V_PAYMENT_TYPE_CODE);
			String uniqueReferenceNum = DateTimeUtil.getStringEpochLocalDateTime();// TODO

			rtgsNeftMapList.put("masterReference", masterReference);
			rtgsNeftMapList.put("eventReference", eventReference);
			rtgsNeftMapList.put("source", XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_SOURCE));
			rtgsNeftMapList.put("sourceRefNo", uniqueReferenceNum);

			// RTGS or NEFT
			rtgsNeftMapList.put("paymentType",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_PAYMENT_TYPE_CODE));
			// R41 or R42
			rtgsNeftMapList.put("paymentSubType",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_PAYMENT_SUB_TYPE_CODE));
			rtgsNeftMapList.put("txn_date",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_TXN_DATE));
			rtgsNeftMapList.put("beneName",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_NAME));
			rtgsNeftMapList.put("beneIFSCcode",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_IFSC_CODE));
			rtgsNeftMapList.put("beneAcNo",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_AC_NO));
			rtgsNeftMapList.put("debitAccNo", XPathParsing.getValue(requestXML, requestOperation + "/DebitAccNo"));
			rtgsNeftMapList.put("NARRATION", XPathParsing.getValue(requestXML, requestOperation + "/NARRATION"));
			rtgsNeftMapList.put("debitBranchCode",
					XPathParsing.getValue(requestXML, requestOperation + "/DebitBranchCode"));
			rtgsNeftMapList.put("creditAccNo", XPathParsing.getValue(requestXML, requestOperation + "/CreditAccNo"));
			rtgsNeftMapList.put("debitAmount", XPathParsing.getValue(requestXML, requestOperation + "/DebitAmount"));
			rtgsNeftMapList.put("creditAmount", XPathParsing.getValue(requestXML, requestOperation + "/CreditAmount"));
			rtgsNeftMapList.put("creditBranchCode",
					XPathParsing.getValue(requestXML, requestOperation + "/CreditBranchCode"));
			logger.debug("Milestone 01 A");
			String benefOtherDetail = XPathParsing.getValue(requestXML,
					requestOperation + NeftRtgsXpath.V_BENEF_OTHER_DETAIL);
			if (benefOtherDetail != null && benefOtherDetail.isEmpty()) {
				rtgsNeftMapList.put("beneOtherDetail",
						XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_OTHER_DETAIL)
								.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " "));
			} else {
				rtgsNeftMapList.put("beneOtherDetail", "");
			}
			logger.debug("Milestone 01 B");

			String beneOtherDetail1 = XPathParsing.getValue(requestXML,
					requestOperation + NeftRtgsXpath.V_BENEF_OTHER_DETAIL1);
			if (beneOtherDetail1 != null && beneOtherDetail1.isEmpty())
				rtgsNeftMapList.put("beneOtherDetail1",
						beneOtherDetail1.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " " + "z"));
			else
				rtgsNeftMapList.put("beneOtherDetail1", "");
			logger.debug("Milestone 01 C");
			// String paymentAmount = getPaymentAmount(masterRef, eventRef);
			//HashMap<String, String> paymentAmountMap = getPaymentAmount(masterRef, eventRef);
//			String paymentAmount = paymentAmountMap.get("amount");
//			String updatetiAmount = paymentAmountMap.get("tiamount");
			String paymentAmount = "";
			String updatetiAmount = "";
			if (!paymentAmount.isEmpty() && paymentAmount != null) {
				rtgsNeftMapList.put("transferAmount", paymentAmount);
				rtgsNeftMapList.put("updatetiAmount", updatetiAmount);
			} else {
				rtgsNeftMapList.put("transferAmount", "");
				rtgsNeftMapList.put("updatetiAmount", "");
			}
			// BigDecimal transferAmount = transferAmount =
			// getAmountValues(rtgsNeftMapList.get("transferAmount"));
			// logger.debug("\n\nPaymentAmount from DB*100 >>-->> " +
			// transferAmount);
			logger.debug("\n\nPaymentAmount from XML/100 >>-->> " + paymentAmount);
			logger.debug("Milestone 02");

			rtgsNeftMapList.put("remitterName",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_REMITTER_NAME));
			rtgsNeftMapList.put("remitterApac",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_REMITTER_APAC));
			String branchCode = XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BRANCH_CODE);
			/**
			 * new Change, branch Code always 958
			 */
			branchCode = "0958";
			// branchCode = branchCode.replaceFirst("^0+(?!$)", "");
			rtgsNeftMapList.put("branchCode", branchCode);

			rtgsNeftMapList.put("beneAcType",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_AC_TYPE));

			rtgsNeftMapList.put("beneBank",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_BANK));
			rtgsNeftMapList.put("beneBranch",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_BRANCH));
			rtgsNeftMapList.put("beneCity",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_CITY));

			String remitterAddr = XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_REMITTER_ADDR);
			if (remitterAddr != null && !remitterAddr.isEmpty())
				rtgsNeftMapList.put("remitterAddr", remitterAddr.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " "));
			else
				rtgsNeftMapList.put("remitterAddr", "");
			// 2017-06-27 Subhash
			String remitterAddr1 = XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_ADDR1);
			if (remitterAddr1 != null && !remitterAddr1.isEmpty())
				rtgsNeftMapList.put("remitterAddr1", remitterAddr1.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " "));
			else
				rtgsNeftMapList.put("remitterAddr1", "");

			String remitterAddr2 = XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_ADDR2);
			if (remitterAddr2 != null && !remitterAddr2.isEmpty())
				rtgsNeftMapList.put("remitterAddr2", remitterAddr2.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " "));
			else
				rtgsNeftMapList.put("remitterAddr2", "");

			String remitterAddr3 = XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_BENEF_ADDR3);
			if (remitterAddr3 != null && !remitterAddr3.isEmpty())
				rtgsNeftMapList.put("remitterAddr3", remitterAddr3.replaceAll("[~!@#$%^&*(){}\\[\\]:;\"<,>.?/-]", " "));
			else
				rtgsNeftMapList.put("remitterAddr3", "");

			rtgsNeftMapList.put("confirmFlag",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_TREASURY_CONFIM_FLAG));
			logger.debug("Milestone 03");
			rtgsNeftMapList.put("exchgHsgCode", "");
			rtgsNeftMapList.put("nonCustName", "");
			rtgsNeftMapList.put("nonCustAcNo", "");
			rtgsNeftMapList.put("nonCustAddr", "");
			rtgsNeftMapList.put("nonCustEmail", "");
			rtgsNeftMapList.put("nonCustMobNo", "");

			logger.debug("Milestone 04");
			rtgsNeftMapList.put("utrNo", "");

			rtgsNeftMapList.put("externalAppl",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_EXTERNAL_APPL));
			rtgsNeftMapList.put("externalRefNo", uniqueReferenceNum);
			rtgsNeftMapList.put("errorCode", "");// OUT Param, always empty
			rtgsNeftMapList.put("errorDesc", "");// OUT Param, always empty
			rtgsNeftMapList.put("utrSlNo", "");// OUT Param, always empty

			rtgsNeftMapList.put("purposeCode",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_PURPOSE));
			rtgsNeftMapList.put("channelId", channelId);

			rtgsNeftMapList.put("custRefNo",
					XPathParsing.getValue(requestXML, requestOperation + NeftRtgsXpath.V_CUST_REF_NO));//
			logger.debug("Milestone 05");

		} catch (XPathExpressionException e) {
			logger.debug("XPathExpressionException! " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.debug("SAXException! " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.debug("IOException! " + e.getMessage());
			e.printStackTrace();
		}

		logger.debug("rtgsNeftMapList : \n" + rtgsNeftMapList);
		bankRequest = rtgsNeftMapList.toString();
		return rtgsNeftMapList;
	}

	/**
	 * 
	 * @param master
	 * @param event
	 * @return
	 */
	public static HashMap<String, String> getPaymentAmount(String master, String event) {

		ResultSet res = null;
		Connection con = null;
		PreparedStatement ps = null;

		/** DEEKKAN **/
		// String paymentAmountQuery = "SELECT SUM(DECODE(POS.DR_CR_FLG, 'D',
		// (POS.AMOUNT/100)*-1, (POS.AMOUNT/100))) AS AMOUNT,
		// SUM(DECODE(POS.DR_CR_FLG, 'D', (POS.AMOUNT)*-1, (POS.AMOUNT))) AS
		// UPDAMOUNT, TRIM(POS.CCY) AS CURRENCY FROM master MAS, BASEEVENT BEV,
		// RELITEM REL, POSTING POS WHERE MAS.KEY97 = BEV.MASTER_KEY AND
		// BEV.KEY97 = REL.EVENT_KEY AND REL.KEY97 = POS.KEY97 AND Bev.Status =
		// 'c' AND POS.POSTED_AS IS NULL AND POS.ACC_TYPE = 'RTGS' AND
		// trim(Mas.Master_Ref) = ? AND
		// trim(Bev.Refno_Pfix||lpad(Bev.Refno_Serl,3,0)) = ? GROUP BY POS.CCY
		// ";

		InputStream anInputStream = GatewayRtgsNeftAdapteeStaging.class.getClassLoader()
				.getResourceAsStream("NeftRtgsPaymentAmount.sql");

		String paymentAmountQuery = ThemeBridgeUtil.readFile(anInputStream);
		logger.debug("PaymentAmountQuery(Netted/Unnetted) : " + paymentAmountQuery);
		logger.debug("Master reference : " + master);
		logger.debug("Event reference  : " + event);

		HashMap<String, String> hmap = new HashMap<String, String>();
		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(paymentAmountQuery);
			ps.setString(1, master);
			ps.setString(2, event);
			res = ps.executeQuery();

			String amount = "";
			String tiamount = "";
			while (res.next()) {
				logger.debug("Payment amount query result");
				amount = res.getString("AMOUNT"); // 1000.23
				tiamount = res.getString("UPDAMOUNT"); // 100023
			}
			hmap.put("amount", amount);
			hmap.put("tiamount", tiamount);

		} catch (SQLException e) {
			logger.error("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}
		logger.debug("Payment Amount Map : " + hmap);
		return hmap;
	}

	/**
	 * PRASATH R
	 * 
	 * @since 2016-Aug-09
	 * @param amt
	 * @return
	 */
	public static BigDecimal getAmountValues(String checkAmountValue) {

		BigDecimal result = null;
		try {
			if (ValidationsUtil.isValidString(checkAmountValue)) {
				checkAmountValue = checkAmountValue.replace(",", "");
				checkAmountValue = checkAmountValue.replaceAll("\\s+", "");
				checkAmountValue = checkAmountValue.replaceAll("[^0-9 , .]", "");
				result = new BigDecimal(checkAmountValue);
			}

		} catch (Exception e) {
			logger.debug("Exception e");
		}
		return result;
	}

	/**
	 * Wrong Method output
	 * 
	 * @param amt
	 * @return
	 */
	public static int getAmountValuesOld(String amt) {

		int amount = 0;
		String result = amt;

		if (result == null) {
			return amount;
		}
		result = result.replaceAll("[^0-9]", "");
		amount = Integer.parseInt(result);
		return amount;
	}

	/**
	 * 
	 * @param amountWithCcy
	 * @return
	 */
	public static int getAmount(String amountWithCcy) {

		int amount = 0;
		// Match int or float
		Pattern pattern = Pattern.compile("\\d+(?:\\.\\d+)?");
		// String str = null;
		Matcher matcher = pattern.matcher(amountWithCcy);
		if (matcher.find()) {
			logger.debug(matcher.group());
		}

		return amount;
	}

	/**
	 * 
	 * @param amt
	 * @return
	 */
	public static String getAmountFromEventField(String amt) {
		String result = amt;
		if (result == null) {
			return result;
		}
		result = result.replaceAll("[^0-9]", "");
		return result;
	}

	/**
	 * 
	 * @param amt
	 * @return
	 */
	public static String getCcyFromEventField(String amt) {
		String result = amt;
		if (result == null) {
			return result;
		}
		result = result.replaceAll("[^a-zA-Z]", "");
		return result;
	}

	/**
	 * 
	 * NOT IN USE NOW - SCF
	 * 
	 * @param utrSlNo
	 * @param trxnamountStr
	 * @param masterRef
	 * @param eventRef
	 * @return
	 */
	public static int updateSCFUtrSlNo(String utrSlNo, String trxnamountStr, String masterRef, String eventRef) {

		int updatedCount = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		// String

		// String str2 = "50100.00";
		// double d = Double.parseDouble(trxnamount);
		// logger.debug("double d : " + d * 100);// 1025550.2223
		// d = d * 100;
		// String DoubleString = String.valueOf(d);
		// logger.debug("DoubleString : " + DoubleString);

		// String updateUTRQuery = "UPDATE EXTEVENT E SET E.UTRNO = ? WHERE
		// E.EVENT = (SELECT B.KEY97 FROM MASTER M, BASEEVENT B WHERE M.KEY97 =
		// B.MASTER_KEY AND M.MASTER_REF = ? AND (B.REFNO_PFIX ||
		// LPAD(B.REFNO_SERL, 3, 000)) = ? ) ";
		String updateUTRQuery = "UPDATE EXTEVENT EXTE SET EXTE.UTRNO = ?, EXTE.RTGSNEFT = ? WHERE EXTE.EVENT IN (SELECT BEV.KEY97 FROM MASTER MAS, BASEEVENT BEV, FNCEMASTER FIN WHERE MAS.KEY97 = BEV.MASTER_KEY AND MAS.KEY97 = FIN.KEY97 AND TRIM(SUBSTR(FIN.FINCE_REF,1,INSTR(FIN.FINCE_REF,'_')-1)) = ? ) ";
		logger.debug("UTR Update Query : " + updateUTRQuery);
		logger.debug("UTRSlNo : " + utrSlNo);
		logger.debug("EventRef : " + eventRef);
		logger.debug("MasterRef(BatchID) : " + masterRef);

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(updateUTRQuery);
			aPreparedStatement.setString(1, utrSlNo);

			trxnamountStr = trxnamountStr.replace(".", "");
			logger.debug("TrxnamountStr Replaced : " + trxnamountStr);
			aPreparedStatement.setBigDecimal(2, new BigDecimal(trxnamountStr));

			// aPreparedStatement.setString(2, trxnamountStr);
			// aPreparedStatement.setBigDecimal(2, trxnamount); // 100023

			aPreparedStatement.setString(3, masterRef);
			// aPreparedStatement.setString(4, eventRef);

			updatedCount = aPreparedStatement.executeUpdate();
			logger.debug(updatedCount + " rows updated for the SCF UTR reference number");

		} catch (Exception e) {
			logger.error("UTR update exceptions " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return updatedCount;
	}

	public static int updateRegularUtrSlNo(String utrSlNo, String trxnamountStr, String masterRef, String eventRef) {

		int updatedCount = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		// // String str2 = "50100.00";
		// double d = Double.parseDouble(trxnamount);
		// logger.debug("double d : " + d * 100);// 1025550.2223
		// d = d * 100;
		// String DoubleString = String.valueOf(d);
		// logger.debug("DoubleString : " + DoubleString);

		String updateUTRQuery = "UPDATE EXTEVENT EXTE SET EXTE.UTRNO = ?, EXTE.RTGSNEFT = ?, CCY_62 = 'INR' where EXTE.event in (SELECT BEV.KEY97 FROM MASTER MAS, BASEEVENT BEV where mas.KEY97 = bev.MASTER_KEY and trim(mas.master_ref)  = ? and  BEV.REFNO_PFIX||lpad(bev.refno_serl,3,0) = ? )  ";
		logger.debug("UTR Update Query : " + updateUTRQuery);
		logger.debug("1 UTRSlNo : " + utrSlNo);
		logger.debug("2 trxnamount : " + trxnamountStr);
		// BigDecimal bdAmount = new BigDecimal(trxnamount);
		logger.debug("3 MasterRef(BatchID) : " + masterRef);
		logger.debug("4 EventRef : " + eventRef);

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(updateUTRQuery);
			aPreparedStatement.setString(1, utrSlNo);
			// aPreparedStatement.setString(2, trxnamount);
			// aPreparedStatement.setBigDecimal(2, trxnamount);

			trxnamountStr = trxnamountStr.replace(".", "");
			logger.debug("TrxnamountStr Replaced : " + trxnamountStr);
			aPreparedStatement.setBigDecimal(2, new BigDecimal(trxnamountStr));

			aPreparedStatement.setString(3, masterRef);
			aPreparedStatement.setString(4, eventRef);
			logger.debug("Logs");
			updatedCount = aPreparedStatement.executeUpdate();
			logger.debug(updatedCount + " rows updated for the regular UTR reference number");

		} catch (Exception e) {
			logger.error("UTR update excetions " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return updatedCount;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getTIResponse(String status) throws Exception {

		String result = "";
		try {
			InputStream anInputStream = GatewayRtgsNeftAdapteeStaging.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.GATEWAY_NEFTRTGS_TI_RESPONSE_TEMPLATE);

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
		} catch (Exception e) {
			logger.debug("Exception..! " + e.getMessage());
		}

		// logger.debug("The NeftRtgs Response is: " + result);
		return result;
	}

}
