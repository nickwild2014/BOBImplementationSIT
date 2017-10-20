package com.bs.theme.bob.unused;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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
import com.bs.themebridge.entity.model.LienAbortedDetails;
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
public class CustomizationEodFDLienRemoval2 {

	private final static Logger logger = Logger.getLogger(CustomizationEodFDLienRemoval2.class.getName());

	private String bankRequest = "";
	private String bankRespStatus = "FAILED";

	public static void main(String args[]) {

		// CustomizationEodFDLienRemoval anAdaptee = new
		// CustomizationEodFDLienRemoval();
		try {
			// String inputXML = ThemeBridgeUtil.readFile(
			// "D:\\_Prasath\\Filezilla\\TI_sample_messages_req_res\\TIRequest_FD_Lien_FDLienRemoval_Req.xml");
			// anAdaptee.process(inputXML);

			List<LienAbortedDetails> obj = getAbortedLienDetails();
			logger.debug(obj.size());

			// boolean s = updateLienStatus("0958ILD160001479", "ISX001", "MARK
			// SUCCEEDED", "KB2074783");
			// logger.debug(s);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String process(String inputXml) {

		logger.info(" ************ EOD.FDLienRemoval adaptee process started ************ ");

		String tiResponseStatus = "";
		try {
			// tiRequest = requestXML;
			// tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("FDLienRemoval TI Request--->" + tiRequest);

			List<LienAbortedDetails> lienList = getAbortedLienDetails();
			logger.debug(lienList.size());

			for (int iterator = 0; iterator < lienList.size(); iterator++) {
				tiResponseStatus = processLienRemoval(lienList.get(iterator));
			}

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
			e.printStackTrace();
		}

		logger.info(" ************ EOD.FDLienRemoval adaptee process started ************ ");

		return tiResponseStatus;
	}

	/**
	 * 
	 * @param requestXML
	 *            {@code allows }{@link String}
	 * @return
	 */
	private String processLienRemoval(LienAbortedDetails lienData) {

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

		try {
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();

			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FD_LIEN_REMOVAL_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			logger.debug("EODLienRemovalCount : " + lienData.getId());
			String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
			String requestId = ThemeBridgeUtil.randomCorrelationId();
			String accountID = lienData.getAccountid();
			String tilienAmount = lienData.getAmount();
			String lienCurrency = lienData.getCurrency();
			String lienAmount = amountConversion(tilienAmount, lienCurrency);
			// logger.debug(lienAmount);
			String lienId = lienData.getLienid();
			String lienStatus = lienData.getLienstatus();
			String lienremarksdesc = lienData.getRemarks();
			masterReference = lienData.getMasterreference();
			eventReference = lienData.getEventreference();

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("dateTime", dateTime);
			tokens.put("requestId", requestId);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("DealRefNo", masterReference);
			tokens.put("accountNumber", accountID);

			double fcyRate = 0;
			if (!lienCurrency.equals("INR")) {
				logger.debug("FCY currency");
				LimitServicesUtil limitUtil = new LimitServicesUtil();
				fcyRate = LimitServicesUtil.getSpotRateFCY(lienCurrency);
				logger.debug("fcyAmountRate " + fcyRate);
				String convertedAmount = limitUtil.getEquivalentINRAmount(lienAmount, fcyRate);
				logger.debug("convertedAmount " + convertedAmount);
				tokens.put("TranAmt", convertedAmount);
				tokens.put("currencyCode", "INR");
				lienrate = Double.toString(fcyRate);
				lienamount = convertedAmount;

			} else {
				logger.debug("INR currency");
				String tramount = AmountConversion.getAmountValues(lienAmount);
				tokens.put("TranAmt", tramount);
				tokens.put("currencyCode", "INR");
				lienrate = "1.0";
				lienamount = tramount;
			} // REVERSAL SUCCEEDED

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

			bankRequest = result;
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("FDLienRemoval Bank Request : <(*_*)> \n" + bankRequest);

			bankResponse = getBankResponseFromBankRequest(bankRequest);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("FDLienRemoval Bank Response : <(*_*)> \n" + bankResponse);

			tiResponse = getTIResponseFromBankResponse(bankResponse, masterReference, eventReference, lienId);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();

			boolean res1 = LiendetailsLogging.pushLiendetailsLogging(masterReference, eventReference, accountID,
					tilienAmount, lienrate, lienamount, masterReference, "R", lienId);

		} catch (Exception e) {
			logger.error("FDLienRemoval Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// NEW LOGGING
			boolean res = ServiceLogging.pushLogData("Customization", "FDLienRemoval", "ZONE1", "", "ZONE1", "KOTAK",
					masterReference, eventReference, bankRespStatus, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", "");
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
	public static List<LienAbortedDetails> getAbortedLienDetails() {

		ResultSet res = null;
		Statement bStatement = null;
		Connection bConnection = null;
		List<LienAbortedDetails> mapList = new ArrayList<LienAbortedDetails>();
		// String abortedLienListQuery = "SELECT TRIM(MAS.MASTER_REF)
		// MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) EVENT_REF,
		// TRIM(EX.DEPOSTNO) AS ACCOUNT_NUMBER, TRIM(EX.MARGAMT) AS LIENAMOUNT,
		// TRIM(EX.CCY_1) AS LIENCCY, TRIM(EX.LIENID) AS LIENID, TRIM(EX.LINEST)
		// AS LIENSTATUS, TRIM(EX.LIENREM) AS LIENREMARKS FROM EXTEVENTLMG EX,
		// EXTEVENT EXTE, BASEEVENT BEV, MASTER MAS,DLYPRCCYCL DLY WHERE
		// TRIM(EX.LINEST) = 'MARK SUCCEEDED' AND EX.FK_EVENT = EXTE.KEY29 AND
		// EXTE.EVENT = BEV.KEY97 AND BEV.MASTER_KEY = MAS.KEY97 AND BEV.STATUS
		// = 'a' AND BEV.FINISHED = DLY.PROCDATE ";
		/** 2017-02-23 **/
		// String abortedLienListQuery = "SELECT TRIM(MAS.MASTER_REF)
		// MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) EVENT_REF,
		// TRIM(EX.DEPOSTNO) AS ACCOUNT_NUMBER, TRIM(EX.MARGAMT) AS LIENAMOUNT,
		// TRIM(EX.CCY_1) AS LIENCCY, TRIM(EX.LIENID) AS LIENID, TRIM(EX.LINEST)
		// AS LIENSTATUS, TRIM(EX.LIENREM) AS LIENREMARKS FROM EXTEVENTLMG EX,
		// EXTEVENT EXTE, BASEEVENT BEV, MASTER MAS,DLYPRCCYCL DLY WHERE
		// TRIM(EX.LINEST) IN ('MARK SUCCEEDED', 'REVERSAL FAILED') AND
		// EX.FK_EVENT = EXTE.KEY29 AND EXTE.EVENT = BEV.KEY97 AND
		// BEV.MASTER_KEY = MAS.KEY97 AND ( BEV.STATUS in ('a', 'i') OR
		// MAS.STATUS IN ('NEW','CAN','EXP') ) AND BEV.START_DATE = DLY.PROCDATE
		// ";
		/** 2017-02-24 **/
		String abortedLienListQuery = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,0)) AS EVENT_REF, TRIM(BEV.AMOUNT) AS TRXNAMOUNT, TRIM(BEV.CCY) AS TRXNCCY, EX.TYPOFDEP, EX.DEPOSTNO, EX.MARGAMT, EX.CCY_1, EX.LIENID, EX.LINEST, EX.LIENREM FROM MASTER MAS, BASEEVENT BEV, EXTMASTERLMG EX, EXTEVENTLMG EXE WHERE MAS.KEY97 = BEV.MASTER_KEY AND MAS.EXTFIELD = EX.FK_MASTER AND BEV.EXTFIELD = EXE.FK_EVENT AND BEV.STATUS IN ('a') AND EX.LINEST IN ('MARK SUCCEEDED', 'REVERSAL FAILED') ";
		logger.debug("LienListQuery : " + abortedLienListQuery);

		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			res = bStatement.executeQuery(abortedLienListQuery);
			int count = 1;
			while (res.next()) {
				LienAbortedDetails obj = new LienAbortedDetails();
				obj.setId(new BigDecimal(count++));
				// logger.debug(obj.getId());
				obj.setMasterreference(res.getString("MASTER_REF"));
				obj.setEventreference(res.getString("EVENT_REF"));
				obj.setAccountid(res.getString("ACCOUNT_NUMBER"));
				obj.setAmount(res.getString("LIENAMOUNT"));
				obj.setCurrency(res.getString("LIENCCY"));
				obj.setLienid(res.getString("LIENID"));
				obj.setLienstatus(res.getString("LIENSTATUS"));
				obj.setRemarks(res.getString("LIENREMARKS"));
				// obj.setTranamount(res.getString("TRANAMOUNT"));
				// obj.setRate(res.getString("RATE"));
				mapList.add(obj);
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

		return amount;
	}
}
