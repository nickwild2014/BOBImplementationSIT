package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSIN_298RES;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_TI;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;

public class IFN298SDPInwardAdaptee {

	private final static Logger logger = Logger.getLogger(IFN298SDPInwardAdaptee.class);

	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private static Timestamp tiReqTime = null;
	private static Timestamp tiResTime = null;
	private static Timestamp bankReqTime = null;
	private static Timestamp bankResTime = null;

	public static void main(String[] args) {

		IFN298SDPInwardAdaptee ifn298 = new IFN298SDPInwardAdaptee();
		try {
			String SFMSIncomingMsg = ThemeBridgeUtil
					.readFile("D:\\_Prasath\\00_TASK\\task-sfms-e-BG\\sample 298SDP-request-test.txt");
			logger.debug(SFMSIncomingMsg);

			logger.debug(ifn298.processIFN298SDP(SFMSIncomingMsg, "BG.INCOMING"));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String processIFN298SDP(String sfmsInwardMsg, String queueName) {

		logger.debug("SWIFT Incoming IFN298 SDP process initiated..!!");

		String errorMsg = "";
		String eventRef = "";
		String masterRef = "";
		String status = ThemeBridgeStatusEnum.FAILED.toString();

		try {
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			HashMap<String, String> stampDutyPayMap = getStampDutyMap(sfmsInwardMsg);
			String eStampCertNo = stampDutyPayMap.get("E-StampCertificateNumber");
			String eStampTimestamp = stampDutyPayMap.get("E-StampDateTime");
			masterRef = stampDutyPayMap.get("TransactionNumber");
			eventRef = stampDutyPayMap.get("RelatedReference");
			eventRef = eventRef.substring(0, 6);
			logger.debug("7041 eStampCertNo : " + eStampCertNo);
			logger.debug("7042 eStampTimestamp : " + eStampTimestamp);
			logger.debug("7020 & 7021 : " + masterRef + "\t" + eventRef);

			boolean result = updateIFN298SDP(eStampCertNo, eStampTimestamp, masterRef, eventRef);
			logger.debug("e-BG 298p UpdateStatus: " + result);
			if (result)
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			else
				status = ThemeBridgeStatusEnum.FAILED.toString();

			// Iterator<Entry<String, String>> iteratorMap =
			// StampDutyPayMap.entrySet().iterator();
			// while (iteratorMap.hasNext()) {
			// Map.Entry<String, String> entry = iteratorMap.next();
			// String key = entry.getKey();
			// String value = entry.getValue();
			// if (key.equals("E-StampCertificateNumber")) {
			// logger.debug("E-StampCertificateNumber >> " + value);
			// } else if (key.equals("E-StampDateTime")) {
			// logger.debug("E-StampDateTime >> " + value);
			// }
			// }

			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			errorMsg = e.getMessage();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			logger.error("Exception while parsing eSDP..!! " + errorMsg);
			e.printStackTrace();

		} finally {
			boolean res = ServiceLogging.pushLogData(SERVICE_TI, OPERATION_SFMSIN_298RES, SOURCE_SYSTEM, "", queueName,
					"TI", masterRef, eventRef, status, "UPDATE", "", sfmsInwardMsg, status, tiReqTime, bankReqTime,
					bankResTime, tiResTime, "", "TIPLUS", "298SDP", "1/1", false, "0", errorMsg);
		}

		logger.debug("SWIFT Incoming IFN298SDP process finished..!!");

		return status;
	}

	private static boolean updateIFN298SDP(String eStampCertNo, String eStampTimestamp, String masterRef,
			String eventRef) {

		boolean result = true;
		Connection con = null;
		PreparedStatement ps = null;
		String formatEtimeStamp = null;
		bankResTime = DateTimeUtil.getSqlLocalDateTime();
		try {
			if (!eStampTimestamp.isEmpty() && eStampTimestamp != null) {
				formatEtimeStamp = DateTimeUtil.getDateTimeChangeFormat(eStampTimestamp, "yyyyMMddHHmmss",
						"dd-MM-yyyy HH:mm:ss");
			}
			// logger.debug("FormattedEtimeStamp : " + formatEtimeStamp);

			String query = "Update EXTEVENT ext set EXT.ESTMPCNO = ?, EXT.ESTPTIME = ? "
					+ " where ext.event = ( select BEV.KEY97 from master mas, BASEEVENT bev "
					+ " where TRIM(MAS.MASTER_REF) = ? AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) = ? "
					+ " and BEV.MASTER_KEY = MAS.KEY97 )";

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			ps.setString(1, eStampCertNo);
			ps.setString(2, formatEtimeStamp);
			ps.setString(3, masterRef);
			ps.setString(4, eventRef);
			int updatedRows = ps.executeUpdate();

			if (updatedRows > 0) {
				result = true;
				logger.debug("Row updated successfully!!! ");
			} else {
				result = false;
				logger.debug("Row update Failed");
			}

		} catch (Exception e) {
			result = false;
			logger.debug("Exception e " + e.getMessage());
		}

		return result;
	}

	private static HashMap<String, String> getStampDutyMap(String tiGwRequestXML) {

		// String EBGmessageLines[] =
		// tiGwRequestXML.split(System.lineSeparator());
		bankReqTime = DateTimeUtil.getSqlLocalDateTime();
		String EBGmessageLines[] = tiGwRequestXML.split(System.getProperty("line.separator"));
		HashMap<String, String> StampDutyPayMap = new HashMap<String, String>();

		for (String lines : EBGmessageLines) {

			if (lines.contains(":7020:"))
				StampDutyPayMap.put("TransactionNumber", lines.replace(":7020:", ""));
			else if (lines.contains(":7021:"))
				StampDutyPayMap.put("RelatedReference", lines.replace(":7021:", ""));
			else if (lines.contains(":7044:"))
				StampDutyPayMap.put("StateCode", lines.replace(":7044:", ""));
			else if (lines.contains(":7046:"))
				StampDutyPayMap.put("DateofPayment", lines.replace(":7046:", ""));
			else if (lines.contains(":7031:"))
				StampDutyPayMap.put("IssuingBranchIFSC", lines.replace(":7031:", ""));
			else if (lines.contains(":7025:"))
				StampDutyPayMap.put("CurrencyCodeAmount", lines.replace(":7025:", ""));
			else if (lines.contains(":7051:"))
				StampDutyPayMap.put("SendingPartyName", lines.replace(":7051:", ""));
			else if (lines.contains(":7052:"))
				StampDutyPayMap.put("ReceivingPartyName", lines.replace(":7052:", ""));
			else if (lines.contains(":7053:"))
				StampDutyPayMap.put("Stampdutypaidby", lines.replace(":7053:", ""));
			else if (lines.contains(":7043:"))
				StampDutyPayMap.put("Amountpaid", lines.replace(":7043:", ""));
			else if (lines.contains(":7045:"))
				StampDutyPayMap.put("Articlenumber", lines.replace(":7045:", ""));
			else if (lines.contains(":7041:"))
				StampDutyPayMap.put("E-StampCertificateNumber", lines.replace(":7041:", ""));
			else if (lines.contains(":7042:"))
				StampDutyPayMap.put("E-StampDateTime", lines.replace(":7042:", ""));

		}

		return StampDutyPayMap;
	}

}
