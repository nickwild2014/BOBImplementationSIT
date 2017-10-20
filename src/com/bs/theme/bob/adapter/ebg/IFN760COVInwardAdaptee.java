package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSIN_760COV;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_TI;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;

import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AlphaNumericSegregation;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.misys.tiplus2.services.control.StatusEnum;

public class IFN760COVInwardAdaptee {

	private final static Logger logger = Logger.getLogger(IFN760COVInwardAdaptee.class);

	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private static Timestamp tiReqTime = null;
	private static Timestamp tiResTime = null;
	private static Timestamp bankReqTime = null;
	private static Timestamp bankResTime = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// String x = getDupIFSC("KKBK0000201") ;
		// System.out.println(x);

		logger.debug("760COV Inward message test");
		IFN760COVInwardAdaptee bn = new IFN760COVInwardAdaptee();
		try {
			String sfmsInMsg = ThemeBridgeUtil.readFile(
					"D:\\_Prasath\\00_TASK\\SFMSCoverInward760-767\\LKG\\ISS\\01_760COV_Inward02Actual - Copy.txt");
					// logger.debug("760COV Inward : " + sfmsInMsg);

			// bn.getMessageFieldsMap(sfmsInMsg);
			bn.processInwardCoverMsg(sfmsInMsg, "DEF_MQ");

		} catch (Exception e) {
			logger.error("SfmsOutMsg : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public String processInwardCoverMsg(String ifn760covMsg, String queueName) {

		String status = "";
		String errorMsg = "";
		String theirRef = "";
		StatusEnum statusEnum = null;
		tiReqTime = DateTimeUtil.getSqlLocalDateTime();
		logger.debug("Inward 760COV message is :- " + ifn760covMsg);
		try {
			bankRequest = ifn760covMsg;
			status = ThemeBridgeStatusEnum.RECEIVED.toString();
			HashMap<String, String> mapListValue = getMessageFieldsMap(ifn760covMsg);
			theirRef = mapListValue.get("transactionNumber"); // :7020:

			tiRequest = generateTIRequest(mapListValue);
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();

			// EJB CLient
			tiResponse = TIPlusEJBClient.process(tiRequest);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			// EJB Response
			statusEnum = ResponseHeaderUtil.processEJBClientResponse(tiResponse);
			logger.debug("SFMS760CovIn TIResponse status : " + statusEnum.toString());
			status = statusEnum.toString();
			bankResponse = status;
			tiResTime = DateTimeUtil.getSqlLocalDateTime();

		} catch (Exception e) {
			errorMsg = e.getMessage();
			logger.error("Exception!!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			boolean res = ServiceLogging.pushLogData(SERVICE_TI, OPERATION_SFMSIN_760COV, SOURCE_SYSTEM, "", queueName,
					"TI", "IFN760COV", "Cover", status, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime,
					bankReqTime, bankResTime, tiResTime, theirRef, "TIPLUS", "760COV", "1/1", false, "0", errorMsg);
		}

		return tiResponse;
	}

	private static HashMap<String, String> getMessageFieldsMap(String ifn760covMsg) {

		// String EBGmessageLines[] =
		// tiGwRequestXML.split(System.lineSeparator());
		bankReqTime = DateTimeUtil.getSqlLocalDateTime();
		String eBGmessageLines[] = ifn760covMsg.split(System.getProperty("line.separator"));
		HashMap<String, String> mapList = new HashMap<String, String>();

		for (String lines : eBGmessageLines) {

			if (lines.contains(":7020:"))
				mapList.put("transactionNumber", lines.replace(":7020:", ""));// M
			else if (lines.contains(":7021:"))
				mapList.put("relatedReference", lines.replace(":7021:", ""));
			else if (lines.contains(":7022:"))
				mapList.put("guaranteeFormNum", lines.replace(":7022:", ""));
			else if (lines.contains(":7024:"))
				mapList.put("guaranteeType", lines.replace(":7024:", ""));// M
			else if (lines.contains(":7025:"))
				mapList.put("guaranteeAmount", lines.replace(":7025:", ""));
			else if (lines.contains(":7026:"))
				mapList.put("fromToValidityDate", lines.replace(":7026:", ""));
			else if (lines.contains(":7027:"))
				mapList.put("effectiveDate", lines.replace(":7027:", ""));
			else if (lines.contains(":7029:"))
				mapList.put("lodgementClaimDate", lines.replace(":7029:", ""));
			else if (lines.contains(":7030:"))
				mapList.put("placeOfLogdement", lines.replace(":7030:", ""));

			else if (lines.contains(":7031:"))
				mapList.put("issuingBranchIfsc", lines.replace(":7031:", ""));
			else if (lines.contains(":7032:"))
				mapList.put("issuingBranchNameAddr", lines.replace(":7032:", ""));
			else if (lines.contains(":7033:"))
				mapList.put("applicantDetails", lines.replace(":7033:", ""));
			else if (lines.contains(":7034:"))
				mapList.put("beneficiaryDetails", lines.replace(":7034:", ""));
			else if (lines.contains(":7035:"))
				mapList.put("beneficiaryIFSC", lines.replace(":7035:", ""));
			else if (lines.contains(":7036:"))
				mapList.put("beneficiaryBranchNameAddr", lines.replace(":7036:", ""));
			else if (lines.contains(":7037:"))
				mapList.put("senderReceiverInfo", lines.replace(":7037:", ""));
			else if (lines.contains(":7038:"))
				mapList.put("purposeOfGuarantee", lines.replace(":7038:", ""));
			else if (lines.contains(":7039:"))
				mapList.put("contractDescription", lines.replace(":7039:", ""));

			else if (lines.contains(":7040:"))
				mapList.put("electronicallyPaid", lines.replace(":7040:", ""));
			else if (lines.contains(":7041:"))
				mapList.put("eStampCertificateNum", lines.replace(":7041:", ""));
			else if (lines.contains(":7042:"))
				mapList.put("eStampDateTime", lines.replace(":7042:", ""));
			else if (lines.contains(":7043:"))
				mapList.put("amountPaid", lines.replace(":7043:", ""));
			else if (lines.contains(":7044:"))
				mapList.put("StateCode", lines.replace(":7044:", ""));
			else if (lines.contains(":7045:"))
				mapList.put("articleNumber", lines.replace(":7045:", ""));
			else if (lines.contains(":7046:"))
				mapList.put("dateofPayment", lines.replace(":7046:", ""));
			else if (lines.contains(":7047:"))
				mapList.put("placeOfPayment", lines.replace(":7047:", ""));
			else if (lines.contains(":7048:"))
				mapList.put("dematForm", lines.replace(":7048:", ""));
			else if (lines.contains(":7049:"))
				mapList.put("dematAccountNo", lines.replace(":7049:", ""));
			else if (lines.contains(":7050:"))
				mapList.put("custodianService", lines.replace(":7050:", ""));
		}

		return mapList;
	}

	public String generateTIRequest(HashMap mapListValue) {

		String tirequest = "";
		// logger.debug("Inward message is :- " + mapListValue);
		try {
			InputStream anInputStream = IFN760COVInwardAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_760COV_TFEGTNEW_REQUEST_TEMPLATE);
			String tiInReqXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			String correlationId = ThemeBridgeUtil.randomCorrelationId();
			tokens.put("CorrelationId", correlationId);
			String branch = "";
			// branch = ConfigurationUtil.getValueFromKey("760CoverBranch");
			if (!branch.isEmpty() && branch != null)
				tokens.put("Branch", branch);// TODO DUMMY

			String bhealfOfBranch = "";
			// bhealfOfBranch =
			// ConfigurationUtil.getValueFromKey("760CoverBehalfOfBranch");
			if (!bhealfOfBranch.isEmpty() && bhealfOfBranch != null)
				tokens.put("BehalfOfBranch", bhealfOfBranch);// TODO DUMMY

			tokens.put("PricipalReference", (String) mapListValue.get("guaranteeFormNum"));// TODO
			tokens.put("GuranteeNumber", (String) mapListValue.get("guaranteeFormNum"));// TODO
			tokens.put("TheirReference", (String) mapListValue.get("transactionNumber"));
			tokens.put("ApplicantNameAddress", (String) mapListValue.get("applicantDetails"));
			tokens.put("BeneficiaryNameAddress", (String) mapListValue.get("beneficiaryDetails"));

			String tiDate = DateTimeUtil.getTISystemDate();
			tokens.put("AdviseDate", tiDate);

			String validityDates = (String) mapListValue.get("fromToValidityDate");
			String startDate = validityDates.substring(0, 8);
			// System.out.println(startDate);
			startDate = DateTimeUtil.dateStrformatChange(startDate, "yyyymmdd", "yyyy-mm-dd");
			String expiryDate = validityDates.substring(8, 16);
			// System.out.println(expiryDate);
			expiryDate = DateTimeUtil.dateStrformatChange(expiryDate, "yyyymmdd", "yyyy-mm-dd");
			tokens.put("IssueDate", startDate);
			tokens.put("ExpiryDate", expiryDate);
			tokens.put("ExpiryPlace", (String) mapListValue.get("placeOfLogdement"));// TODO

			tokens.put("AdviseBy", "4");// always SWIFT
			tokens.put("InstructionsReceived", (String) mapListValue.get("senderReceiverInfo"));
			// tokens.put("InstructionsToNextAdvisingBank", "");// always SWIFT
			// tokens.put("InstructionsToNextAdvisingBank", (String)
			// mapListValue.get("senderReceiverInfo"));

			String amountWithCcy = (String) mapListValue.get("guaranteeAmount");
			// AlphaNumericSegregation.getAmountFromEventField(amountWithCcy);
			String amount = AlphaNumericSegregation.getSwiftToRegularAmount(amountWithCcy);
			String ccy = AlphaNumericSegregation.getCcyFromEventField(amountWithCcy);

			String prodSubType = (String) mapListValue.get("guaranteeType");
			if (prodSubType.equalsIgnoreCase("FINANCIAL"))
				tokens.put("ProductType", "IGF");
			else if (prodSubType.equalsIgnoreCase("FINACIAL"))
				tokens.put("ProductType", "IGP");
			else
				tokens.put("ProductType", "BCR");

			tokens.put("Amount", amount);
			tokens.put("Currency", ccy);
			tokens.put("Qualifier", "E");// always
			tokens.put("Operative", "N");// always

			tokens.put("ReceivedFromBankNameAddress", (String) mapListValue.get("issuingBranchNameAddr"));
			tokens.put("IssuingBankNameAddress", (String) mapListValue.get("issuingBranchNameAddr"));
			// System.out.println("Milestone 02");
			// String beneficiaryIFSC = (String)
			// mapListValue.get("beneficiaryIFSC");
			String issuingIFSC = (String) mapListValue.get("issuingBranchIfsc");

			String issuingCountry = issuingIFSC.substring(5, 7);
			logger.debug("issuingCountry : " + issuingCountry);

			if (issuingCountry.equals("00")) {
				String customerAndSwift = getDupIFSC(issuingIFSC);
				tokens.put("ReceivedFromCustomer", customerAndSwift);
				tokens.put("ReceivedFromSwiftAddress", customerAndSwift);
				tokens.put("IssuingBankCustomer", customerAndSwift);
				tokens.put("IssuingSwiftAddress", customerAndSwift);
			} else {
				tokens.put("ReceivedFromCustomer", "");
				tokens.put("ReceivedFromSwiftAddress", "");
				tokens.put("IssuingSwiftAddress", "");
			}

			tokens.put("Financial", "Y");// always
			tokens.put("Trade", "N");// always

			tokens.put("AdviseDirect", "N");// always
			tokens.put("UseFreeFormat", "N");// always
			tokens.put("BillNumber", "");// always
			tokens.put("FreeFormatInstructionsForSWIFT", (String) mapListValue.get("contractDescription"));
			// EXTRA DATA
			tokens.put("RCIFSCIN", (String) mapListValue.get("beneficiaryIFSC"));
			tokens.put("SNIFSCIN", (String) mapListValue.get("issuingBranchIfsc"));
			// System.out.println("Milestone 03");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiInReqXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tirequest = reader.toString();
			reader.close();
			anInputStream.close();

			logger.debug("TIRequestXML: " + tirequest);

		} catch (Exception e) {
			logger.error("Exception!!! " + e.getMessage());
			e.printStackTrace();
		}
		return tirequest;
	}

	private static String getDupIFSC(String origIfsc) {// KKBK0000201

		String swiftCompatible = null;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		// 02-OCT-16
		String query = "SELECT * FROM ETTIFSCMAP WHERE REALIFSC = ? ";
		logger.debug("IFSC : " + query + "\t" + origIfsc);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			ps.setString(1, origIfsc);
			rs = ps.executeQuery();

			while (rs.next()) {
				swiftCompatible = rs.getString("TIIFSC");
			}

		} catch (SQLException e) {
			logger.error("SQL Exceptions! Fince_Pst Failed to insert. " + e.getMessage(), e);
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exception! Fince_Pst Failed to insert " + e.getMessage(), e);
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		logger.debug(swiftCompatible);
		return swiftCompatible;
	}

	public String getTIResponse(String ifn760cov) {

		logger.debug("Inward message is :- " + ifn760cov);
		try {

		} catch (Exception e) {
			logger.error("SfmsOutMsg : " + e.getMessage());
			e.printStackTrace();
		}

		return "";
	}

}
