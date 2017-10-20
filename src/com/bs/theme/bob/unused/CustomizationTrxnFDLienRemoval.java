package com.bs.theme.bob.unused;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bob.client.finacle.FinacleHttpClient;
import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.adapter.util.LimitServicesUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.LiendetailsLogging;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.CustomisationFDLienMarkXpath;
import com.bs.themebridge.xpath.CustomisationFDLienModifyXpath;
import com.bs.themebridge.xpath.CustomisationFDLienRemovalXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * 
 * @since 2016-09-07
 * @version 1.0.2
 * @author KXT51472, Prasath Ravichandran
 */
public class CustomizationTrxnFDLienRemoval {

	private final static Logger logger = Logger.getLogger(CustomizationTrxnFDLienRemoval.class.getName());

	private String bankRequest = "";
	private String bankRespStatus = "FAILED";

	public static void main(String args[]) {

		try {
			// List<HashMap<String, String>> obj =
			// processPAYLienRemovalDetails("0958IGP160100007", "ISS001");
			// logger.debug(">>>" + obj.size());
			CustomizationTrxnFDLienRemoval anAdaptee = new CustomizationTrxnFDLienRemoval();
			// logger.debug(">> " + anAdaptee.process("0958IGP160100007",
			// "ISS0012"));

			// boolean s = updateLienStatus("0958ILD160001479", "ISX001", "MARK
			// SUCCEEDED", "KB2074783");
			// logger.debug(s);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String processLien(String masterRef, String eventRef) {

		logger.info(" ************ ONLINE.FDLienRemoval adaptee process started ************ ");

		String tiResponseStatus = "";
		try {
			// tiRequest = requestXML;
			// tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("FDLienRemoval TI Request--->" + tiRequest);

			List<HashMap<String, String>> lienList = processPAYLienRemovalDetails(masterRef, eventRef);
			logger.debug(lienList.size());

			for (HashMap<String, String> lienData : lienList) {
				tiResponseStatus = processPAYLienRemoval(lienData);
			}

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
			e.printStackTrace();
		}

		logger.info(" ************ ONLINE.FDLienRemoval adaptee process started ************ ");

		return tiResponseStatus;
	}

	/**
	 * 
	 * @param requestXML
	 *            {@code allows }{@link String}
	 * @return
	 */
	private String processPAYLienRemoval(HashMap<String, String> lienData) {

		String tiRequest = "";
		String tiResponse = "";
		String bankRequest = "";
		String bankResponse = "";
		String eventReference = "";
		String masterReference = "";
		Timestamp tiReqTime = null;
		Timestamp tiResTime = null;
		Timestamp bankReqTime = null;
		Timestamp bankResTime = null;
		String bankRespStatus = "FAILED";

		String result = "";
		String lienrate = null;
		String lienamount = null;
		InputStream anInputStream = null;
		// logger.debug("Milestone 01");
		try {
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();

			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FD_LIEN_REMOVAL_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			// logger.debug("EODLienRemovalCount : " + lienData.getId());
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = ThemeBridgeUtil.randomCorrelationId();
			String accountID = lienData.get("DEPOSTNO");
			// logger.debug("Milestone 02");
			String tilienAmount = lienData.get("TRXNAMOUNT"); // TRXNAMOUNT
			String lienCurrency = lienData.get("TRXNCCY");
			// logger.debug("Milestone 02 a");
			String lienAmount = amountConversion(tilienAmount, lienCurrency);
			String marginLienAmount = lienData.get("MARGAMT");
			String marginLienCcy = lienData.get("CCY_1");
			// logger.debug("Milestone 02 b");
			String marginlienConvertedAmount = amountConversion(marginLienAmount, marginLienCcy);

			logger.debug("MarginlienAmount : " + marginlienConvertedAmount);
			String lienId = lienData.get("LIENID");
			String lienStatus = lienData.get("LINEST");
			String lienremarksdesc = lienData.get("LIENREM");
			masterReference = lienData.get("MASTER_REF");
			eventReference = lienData.get("EVENT_REF");
			// logger.debug("Milestone 03");
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("DealRefNo", masterReference);
			tokens.put("accountNumber", accountID);
			// logger.debug("Milestone 04");
			double fcyRate = 0;
			if (!marginLienCcy.equals("INR")) {
				logger.debug("FCY currency");
				LimitServicesUtil limitUtil = new LimitServicesUtil();
				fcyRate = LimitServicesUtil.getSpotRateFCY(marginLienCcy);
				logger.debug("fcyAmountRate " + fcyRate);
				String convertedAmount = limitUtil.getEquivalentINRAmount(marginlienConvertedAmount, fcyRate);
				logger.debug("convertedAmount " + convertedAmount);
				tokens.put("TranAmt", convertedAmount);
				tokens.put("currencyCode", "INR");
				lienrate = Double.toString(fcyRate);
				lienamount = convertedAmount;

			} else {
				logger.debug("INR currency");
				String tramount = AmountConversion.getAmountValues(marginlienConvertedAmount);
				tokens.put("TranAmt", tramount);
				tokens.put("currencyCode", "INR");
				lienrate = "1.0";
				lienamount = tramount;
			} // REVERSAL SUCCEEDED
				// logger.debug("Milestone 05");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();
			// logger.debug("Milestone 06");
			bankRequest = result;
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("FDLienRemoval Bank Request : <(*_*)> \n" + bankRequest);

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("FDLienRemoval Bank Response : <(*_*)> \n" + bankResponse);

			tiResponse = getTIResponseFromBankResponse(bankResponse, masterReference, eventReference, lienId);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("Milestone 07");
			boolean res1 = LiendetailsLogging.pushLiendetailsLogging(masterReference, eventReference, accountID,
					tilienAmount, lienrate, lienamount, masterReference, "R", lienId);

		} catch (Exception e) {
			logger.error("FDLienRemoval Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// NEW LOGGING
			boolean res = ServiceLogging.pushLogData("Customization", "FDLienRemoval", "ZONE1", "", "ZONE1", "KOTAK",
					masterReference, eventReference, bankRespStatus, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "TXN", "", "", "", false, "0", "");

			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		return tiResponse;
	}

	/**
	 * 
	 * @param bankRequest
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	private String getBankResponseFromBankRequest(String bankRequest) {

		String bankResponse = "";
		try {
			bankResponse = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			e.getMessage();
			return null;
		}
		return bankResponse;
	}

	/**
	 * 
	 * @param accountID
	 *            {@code allows }{@link String}
	 * @param bankResponseXML
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getTIResponseFromBankResponse(String bankResponseXML, String masterReference, String eventReference,
			String lienid) {

		String bankRespStatus = "";
		boolean updateStatus = false;
		try {
			String status = XPathParsing.getValue(bankResponseXML, CustomisationFDLienRemovalXpath.LienStatusXPath);
			logger.debug("Status : " + status);

			if (status.equalsIgnoreCase("FAILURE")) {
				logger.debug("Lien removal FAILURE");
				bankRespStatus = ThemeBridgeStatusEnum.FAILED.toString();
				String remarks = XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.ErrorDesc)
						+ XPathParsing.getValue(bankResponseXML, CustomisationFDLienMarkXpath.ErrorDescXPath);

			} else if (status.equalsIgnoreCase("SUCCESS")) {
				logger.debug("Lien removal SUCCESS");
				updateStatus = updateLienStatus(masterReference, eventReference, "REVERSAL SUCCEEDED", lienid);
				bankRespStatus = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			}

		} catch (Exception e) {
			logger.error("FDLien Removal Exceptions while TIResponse! " + e.getMessage());
		}

		return bankRespStatus;
	}

	/**
	 * 
	 * @param lienStatus
	 * @param masterRef
	 * @param eventRef
	 * @return
	 */
	public static boolean updateLienStatus(String masterRef, String eventRef, String lienStatus, String lienid) {

		boolean updatedStatus = false;
		int updatedCount = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		String updateUTRQuery = "UPDATE EXTEVENTLMG EXM SET EXM.LINEST = ?, EXM.LIENREM = 'FD Lien removed(EOD)'  where EXM.FK_EVENT in ( SELECT FK_EVENT FROM MASTER MAS, BASEEVENT BEV, EXTEVENTLMG EXM, EXTEVENT EXTE where MAS.KEY97 = bev.MASTER_KEY and EXM.FK_EVENT = EXTE.KEY29 AND EXTE.EVENT = BEV.KEY97 and trim(MAS.master_ref)  = ? and BEV.REFNO_PFIX||lpad(bev.refno_serl,3,0) = ? AND TRIM(EXM.LIENID) = ? )";
		logger.debug("UTR Update Query : " + updateUTRQuery);
		logger.debug("lienStatus : " + lienStatus);
		logger.debug("EventRef : " + eventRef);
		logger.debug("MasterRef : " + masterRef);
		logger.debug("lienid : " + lienid);

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(updateUTRQuery);
			aPreparedStatement.setString(1, lienStatus);
			aPreparedStatement.setString(2, masterRef);
			aPreparedStatement.setString(3, eventRef);
			aPreparedStatement.setString(4, lienid);

			updatedCount = aPreparedStatement.executeUpdate();
			logger.debug(updatedCount + " rows updated for Lien status");

			if (updatedCount > 0) {
				updatedStatus = true;
			}

		} catch (Exception e) {
			logger.error("Lien Status update excetions " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return updatedStatus;
	}

	/**
	 * 
	 * @return {@code allows }{@link List<Liendetails>}
	 */
	public static List<HashMap<String, String>> processPAYLienRemovalDetails(String masterRef, String eventRef) {

		ResultSet res = null;
		PreparedStatement bStatement = null;
		Connection bConnection = null;
		List<HashMap<String, String>> mapList = new ArrayList<HashMap<String, String>>();

		/** 2017-02-24 **/
		String payLienRemovalListQuery = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF, TRIM(BEV.AMOUNT) AS TRXNAMOUNT, TRIM(BEV.CCY) AS TRXNCCY, TRIM(EX.TYPOFDEP) AS TYPOFDEP, TRIM(EX.DEPOSTNO) AS DEPOSTNO, TRIM(EX.MARGAMT) AS MARGAMT, TRIM(EX.CCY_1) AS CCY_1, TRIM(EX.LIENID) AS LIENID, TRIM(EX.LINEST) AS LINEST, TRIM(EX.LIENREM) AS LIENREM FROM MASTER MAS, BASEEVENT BEV, EXTMASTERLMG EX, EXTEVENTLMG EXE WHERE MAS.KEY97 = BEV.MASTER_KEY AND MAS.EXTFIELD = EX.FK_MASTER AND BEV.EXTFIELD = EXE.FK_EVENT AND BEV.STATUS='c' AND EX.LINEST IN ('MARK SUCCEEDED', 'REVERSAL FAILED') AND TRIM(MAS.MASTER_REF) = ? AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) = ?  ";
		// + " AND MAS.MASTER_REF = '0958ILD160100544' ";
		logger.debug("LienListQuery : " + payLienRemovalListQuery);

		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.prepareStatement(payLienRemovalListQuery);
			bStatement.setString(1, masterRef);
			bStatement.setString(2, eventRef);
			res = bStatement.executeQuery();

			while (res.next()) {
				HashMap<String, String> fdlienList = new HashMap<String, String>();
				fdlienList.put("MASTER_REF", res.getString("MASTER_REF"));
				fdlienList.put("EVENT_REF", res.getString("EVENT_REF"));
				fdlienList.put("TRXNAMOUNT", res.getString("TRXNAMOUNT"));
				fdlienList.put("TRXNCCY", res.getString("TRXNCCY"));
				fdlienList.put("TYPOFDEP", res.getString("TYPOFDEP"));
				fdlienList.put("DEPOSTNO", res.getString("DEPOSTNO"));
				fdlienList.put("MARGAMT", res.getString("MARGAMT"));
				fdlienList.put("CCY_1", res.getString("CCY_1"));
				fdlienList.put("LIENID", res.getString("LIENID"));
				fdlienList.put("LINEST", res.getString("LINEST"));
				fdlienList.put("LIENREM", res.getString("LIENREM"));
				mapList.add(fdlienList);
			}
		} catch (Exception e) {
			logger.error("EOD FDLien exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, res);
		}
		// logger.debug("FDLien details from Themebridge : " + mapList);
		return mapList;
	}

	/**
	 * 
	 * @param pstAma
	 * @param pstCCY
	 * @return
	 */
	public static String amountConversion(String tilienamount, String lienCCY) {

		logger.debug(tilienamount + lienCCY);
		String amount = "";
		BigDecimal bg = null;
		if (ValidationsUtil.isValidString(tilienamount) && ValidationsUtil.isValidString(lienCCY)
				&& (lienCCY.equals("OMR") || lienCCY.equals("BHD") || lienCCY.equals("KWD"))) {
			bg = new BigDecimal(tilienamount);
			bg = bg.divide(new BigDecimal(1000));
		} else if (ValidationsUtil.isValidString(tilienamount) && ValidationsUtil.isValidString(lienCCY)
				&& lienCCY.equals("JPY")) {
			bg = new BigDecimal(tilienamount);
		} else if (ValidationsUtil.isValidString(tilienamount) && ValidationsUtil.isValidString(lienCCY)) {
			bg = new BigDecimal(tilienamount);
			bg = bg.divide(new BigDecimal(100));
		}
		if (bg != null)
			amount = bg.toString();

		// logger.debug("amount : " + amount);
		return amount;
	}
}
