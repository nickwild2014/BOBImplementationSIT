package com.bs.theme.bob.staticdata.adapter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.staticdata.util.TIFxRate;
import com.bs.themebridge.logging.StaticLogging;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;

public class FXRateServiceAdapter {

	private final static Logger logger = Logger.getLogger(FXRateServiceAdapter.class.getName());

	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String inputMessage = "";
	// private static String type = "StagingTable";

	// public static URL resource =
	// GatewaySmsAdapteeStaging.class.getResource(".");
	// static String filePath = new File("").getAbsolutePath();

	public static void main(String a[]) throws Exception {

		// pushTIFxRateService();
		// logger.debug(updateBaseRateStatus("AED", "SUCCEEDED"));
		// Map<String, String> TES = getFXRateListNew("JPY");
		// logger.debug(TES.get("CCY"));
		// logger.debug(TES.get("TTB"));
		// logger.debug(TES.get("TTS"));
		// List<Map<String, String>> bs = getFXRateList();

		// logger.debug("resource : " + resource);
		// logger.debug("filePath : " + filePath);
		// String fxRateQuery = ThemeBridgeUtil
		// .readFile(filePath +
		// "\\src\\com\\bs\\themebridge\\query\\FXRateService.sql");
		// logger.debug(fxRateQuery);
		FXRateServiceAdapter an = new FXRateServiceAdapter();
		// List<Map<String, String>> fxRateList = getFXRateList();
		// logger.debug(fxRateList.size());
		String x = an.pushTIFxRateService();

	}

	/**
	 * 
	 * @return
	 */
	public String pushTIFxRateService() {

		// logger.debug(" ************ FxRate adapter process started
		// ************ ");

		String result = "";
		final String TTRTCD = "TTRTCD";
		final String BLRTCD = "BLRTCD";
		final String CNRTCD = "CNRTCD";
		final String DDRTCD = "DDRTCD";
		final String TCRTCD = "TCRTCD";
		final String BARTCD = "BARTCD";
		final String CHRTCD = "CHRTCD";
		final String RVRTCD = "RVRTCD";// RVR == NRR == MDR
		final String MDRTCD = "MDRTCD";// RVR == NRR == MDR
		final String NRRTCD = "NRRTCD";// RVR == NRR == MDR
		final String RFRTCD = "RFRTCD";// RET FX Live

		// Get the List of currency
		List<Map<String, String>> fxRateList = getFXRateList();
		// logger.debug("CardRate/FXRate MapList size : " + fxRateList.size());

		String sellRate = null;
		String buyRate = null;
		String currency = null;
		String rateCode = null;
		String bankEntity = null;

		if (fxRateList.size() > 0) {

			for (Map<String, String> fxRate : fxRateList) {

				bankEntity = "0001";
				currency = fxRate.get("CCY");

				String modifiedDate = "";
				// String modifiedDate =
				// fxRate.get("MODIFIED_DATE").substring(0, 10);
				// logger.debug(fxRate.get("MODIFIED_DATE"));
				String quoteDate = fxRate.get("QUOTE_DATE").substring(0, 10);
				logger.debug(fxRate.get("QUOTE_DATE"));

				/*** To process TTS-TTB Rates ***/
				rateCode = TTRTCD;
				buyRate = fxRate.get("TT_BUY");// TTB
				sellRate = fxRate.get("TT_SELL"); // TTs
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("TTRTCD FxRate pushed successfully.");
				}

				/*** To process BLS-BLB Rates ***/
				rateCode = BLRTCD;
				buyRate = fxRate.get("BL_BUY");// BLB
				sellRate = fxRate.get("BL_SELL");// BLS
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("BLRTCD FxRate pushed successfully.");
				}

				/*** To process CN_BUY-CN_SELL Rates ***/
				rateCode = CNRTCD;
				buyRate = fxRate.get("CN_BUY");
				sellRate = fxRate.get("CN_SELL");
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("CNRTCD FxRate pushed successfully.");
				}

				/*** To process DD_BUY-DD_SELL Rates ***/
				rateCode = DDRTCD;
				buyRate = fxRate.get("DD_BUY");
				sellRate = fxRate.get("DD_SELL");
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("DDRTCD FxRate pushed successfully.");
				}

				/*** To process TC_BUY-TC_SELL Rates ***/
				rateCode = TCRTCD;
				buyRate = fxRate.get("TC_BUY");
				sellRate = fxRate.get("TC_SELL");
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("TCRTCD FxRate pushed successfully.");
				}

				/*** To process BASE_BUY_RATE-BASE_SELL_RATE Rates ***/
				rateCode = BARTCD;
				buyRate = fxRate.get("BASE_BUY_RATE"); // ?
				sellRate = fxRate.get("BASE_SELL_RATE"); // ?
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("BARTCD FxRate pushed successfully.");
				}

				/*** To process CH_BUY-CH_BUY Rates ***/
				rateCode = CHRTCD;
				buyRate = fxRate.get("CH_BUY");
				sellRate = fxRate.get("CH_BUY"); // DUMMY VALUE, BUY == SELL
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("CHRTCD FxRate pushed successfully.");
				}

				/*** To process REVAL_RATE-REVAL_RATE Rates ***/
				rateCode = RVRTCD;
				buyRate = fxRate.get("REVAL_RATE");
				sellRate = fxRate.get("REVAL_RATE"); // DUMMY VALUE, BUY == SELL
				if (getTIFXRateRequest(sellRate, buyRate, bankEntity, currency, rateCode, modifiedDate, quoteDate)) {
					logger.debug("RVRTCD FxRate pushed successfully.");
					boolean spotRateSts = SpotRateServiceAdapter.getTIFSpotRateRequest(sellRate, bankEntity, currency);
					logger.debug("RVRTCD Spot Rate pushing status " + spotRateSts);
				}

				/*** To process NRRTCD Rates ***/
				// rateCode = NRRTCD;
				// buyRate = fxRate.get("REVAL_RATE"); // ?
				// sellRate = fxRate.get("REVAL_RATE"); // ?
				// if (getTIFXRateRequest(sellRate, buyRate, bankEntity,
				// currency, rateCode)) {
				// logger.info("NRRTCD NORTIONAL RATE FxRate pushed
				// successfully.");
				// boolean spotRateStatus =
				// SpotRateServiceAdapter.getTIFSpotRateRequest(sellRate,
				// bankEntity,
				// currency);
				// logger.debug("RVRTCD FxRate pushed successfully.");
				// }
				//
				/*** To process MIDRTCD Rates ***/
				// rateCode = MDRTCD;
				// buyRate = fxRate.get("REVAL_RATE"); // ?
				// sellRate = fxRate.get("REVAL_RATE"); // ?
				// if (getTIFXRateRequest(sellRate, buyRate, bankEntity,
				// currency, rateCode)) {
				// logger.info("MDRTCD FxRate pushed successfully.");
				// boolean spotRateStatus =
				// SpotRateServiceAdapter.getTIFSpotRateRequest(sellRate,
				// bankEntity,
				// currency);
				// logger.debug("RVRTCD FxRate pushed successfully.");
				// }

			}
		} else {
			logger.debug("Number of FX CCY zero records :- " + fxRateList.size());
		}

		// logger.debug(" ************ FxRate adapter process ended ************
		// ");
		return result;
	}

	/**
	 * 
	 * @param sellRate
	 *            {@code allows }{@link String}
	 * @param buyRate
	 *            {@code allows }{@link String}
	 * @param bankEntity
	 *            {@code allows }{@link String}
	 * @param currency
	 *            {@code allows }{@link String}
	 * @param rateCode
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static boolean getTIFXRateRequest(String sellRate, String buyRate, String bankEntity, String currency,
			String rateCode, String modifiedDate, String quoteDate) {

		logger.debug("RateCode : " + rateCode + ";\t currency : " + currency + ";\t buyRate :" + buyRate
				+ ";\t sellRatec :" + sellRate);

		int updatedRow = 0;
		String status = "FAILED";// FAILED
		String errorMessage = "";
		boolean fxRatePushStatus = true;
		Timestamp reqReceivedTime = null;
		// StatusEnum statusEnum = StatusEnum.FAILED;
		try {
			if (ValidationsUtil.isValidString(sellRate) && ValidationsUtil.isValidString(buyRate)
					&& ValidationsUtil.isValidString(bankEntity) && ValidationsUtil.isValidString(currency)
					&& ValidationsUtil.isValidString(rateCode)) {

				TIFxRate tiFxRate = new TIFxRate();
				tiFxRate.generateHeader();
				tiFxRate.generateStaticDataConstants();
				tiFxRate.generateTokenMap();
				tiFxRate.generateSetProperty("Currency", currency);
				/** 2017-09-28 **/
				tiFxRate.generateSetProperty("BuyExchangeRate", buyRate);
				tiFxRate.generateSetProperty("SellExchangeRate", sellRate);
				// if (currency.equals("JPY")) {
				// logger.debug("Currency : JPY");
				// tiFxRate.generateSetProperty("BuyExchangeRate",
				// getUnitRate(buyRate));
				// tiFxRate.generateSetProperty("SellExchangeRate",
				// getUnitRate(sellRate));
				// } else {
				// tiFxRate.generateSetProperty("BuyExchangeRate", buyRate);
				// tiFxRate.generateSetProperty("SellExchangeRate", sellRate);
				// }
				tiFxRate.generateSetProperty("BankingEntity", bankEntity);
				tiFxRate.generateSetProperty("FxRateCode", rateCode);
				tiFxRate.generateTokenMap();
				String tokenReplacedXML = null;
				tokenReplacedXML = tiFxRate.getXMLString();
				String tagRemovedXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);

				tiRequest = tagRemovedXML;
				reqReceivedTime = DateTimeUtil.getTimestamp();
				logger.debug("FX Rate TI RequestXML :- " + tiRequest);
				/********************************************************/
				String tiResponseXML = TIPlusEJBClient.process(tiRequest);
				tiResponse = tiResponseXML;
				logger.debug("FX Rate TI ResponseXML :- " + tiResponse);

				// Get Xpath Status
				// statusEnum =
				// ResponseHeaderUtil.processEJBClientResponse(tiResponse);
				// status = statusEnum.toString();
				status = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Status");
				errorMessage = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Details/Error");

			}
		} catch (Exception e) {
			fxRatePushStatus = false;
			errorMessage = errorMessage + e.getMessage();
			logger.error("FX Rate Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			updatedRow = updateFXRateStatus(rateCode, currency, status, modifiedDate, quoteDate);

			StaticLogging.pushLogData("TI", "FxRate", "ZONE1", "All", "IDBEXT", "TIPlus", status, reqReceivedTime,
					inputMessage, tiRequest, tiResponse, rateCode + "_" + currency, "BUY " + buyRate,
					"SELL " + sellRate, false, "0", errorMessage);
		}
		// logger.info("FXRate Push Response XML-->" + fxRateTIRes);
		return fxRatePushStatus;
	}

	public static String getUnitRate(String rate) {

		String unitRate = "";
		try {
			BigDecimal inwardRate = new BigDecimal(rate);
			// logger.debug("100 UnitRate : " + inwardRate);
			BigDecimal divRate = new BigDecimal(100);
			// logger.debug("DivideBy : " + divRate);
			BigDecimal unitRateBD = inwardRate.divide(divRate);
			logger.debug("1 UnitRate : " + unitRateBD);
			unitRate = unitRateBD.toString();
			// logger.debug("ConvertedDate : " + unitRate);
			logger.debug("JPYUnitRate : " + inwardRate + " / " + divRate + " >>-->> " + unitRate);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return unitRate;
	}

	/**
	 * 
	 * @return
	 */
	public static List<Map<String, String>> getFXRateList() {

		List<Map<String, String>> returnmaplist = null;
		Statement aStatement = null;
		ResultSet aResultset = null;
		Connection aConnection = null;
		// String fxRateQuery = "SELECT C1.CCY AS CCY, C1.QUOTE_DATE,
		// SUM(DECODE(C1.RATE_TYPE,'TT_BUY',C1.RATE)) AS TT_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'TT_SELL',C1.RATE)) AS TT_SELL,
		// SUM(DECODE(C1.RATE_TYPE,'BL_BUY',C1.RATE)) AS BL_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'BL_SELL',C1.RATE)) AS BL_SELL,
		// SUM(DECODE(C1.RATE_TYPE,'CN_BUY',C1.RATE)) AS CN_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'CN_SELL',C1.RATE)) AS CN_SELL,
		// SUM(DECODE(C1.RATE_TYPE,'DD_BUY',C1.RATE)) AS DD_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'DD_SELL',C1.RATE)) AS DD_SELL,
		// SUM(DECODE(C1.RATE_TYPE,'TC_BUY',C1.RATE)) AS TC_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'TC_SELL',C1.RATE)) AS TC_SELL,
		// SUM(DECODE(C1.RATE_TYPE,'BASE_BUY_RATE',C1.RATE)) AS BASE_BUY_RATE,
		// SUM(DECODE(C1.RATE_TYPE,'BASE_SELL_RATE',C1.RATE)) AS BASE_SELL_RATE,
		// SUM(DECODE(C1.RATE_TYPE,'CH_BUY',C1.RATE)) AS CH_BUY,
		// SUM(DECODE(C1.RATE_TYPE,'REVAL_RATE',C1.RATE)) AS REVAL_RATE FROM
		// CARD_RATE_FX C1 WHERE C1.CREATED_BY = 'IFC' AND C1.PROCESSED_FLAG =
		// 'N' GROUP BY C1.CCY, C1.QUOTE_DATE ORDER BY C1.CCY ";
		/** 2017-SEP-29 **/
		String fxRateQuery = "SELECT C1.CCY AS CCY, C1.QUOTE_DATE, SUM(DECODE(C1.RATE_TYPE,'TT_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS TT_BUY, SUM(DECODE(C1.RATE_TYPE,'TT_SELL',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS TT_SELL, SUM(DECODE(C1.RATE_TYPE,'BL_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS BL_BUY, SUM(DECODE(C1.RATE_TYPE,'BL_SELL',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS BL_SELL, SUM(DECODE(C1.RATE_TYPE,'CN_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS CN_BUY, SUM(DECODE(C1.RATE_TYPE,'CN_SELL',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS CN_SELL, SUM(DECODE(C1.RATE_TYPE,'DD_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS DD_BUY, SUM(DECODE(C1.RATE_TYPE,'DD_SELL',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS DD_SELL, SUM(DECODE(C1.RATE_TYPE,'TC_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS TC_BUY, SUM(DECODE(C1.RATE_TYPE,'TC_SELL',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS TC_SELL, SUM(DECODE(C1.RATE_TYPE,'BASE_BUY_RATE',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS BASE_BUY_RATE, SUM(DECODE(C1.RATE_TYPE,'BASE_SELL_RATE',C1.RATE))/ DECODE(C1.CCY,'JPY',100,1) AS BASE_SELL_RATE, SUM(DECODE(C1.RATE_TYPE,'CH_BUY',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS CH_BUY, SUM(DECODE(C1.RATE_TYPE,'REVAL_RATE',C1.RATE)) / DECODE(C1.CCY,'JPY',100,1) AS REVAL_RATE FROM CARD_RATE_FX C1 WHERE C1.CREATED_BY = 'IFC' AND C1.PROCESSED_FLAG = 'N' GROUP BY C1.CCY, C1.QUOTE_DATE ORDER BY C1.CCY";
		// logger.debug("FxRateStaticQuery is :- " + fxRateQuery);
		try {
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();
				aResultset = aStatement.executeQuery(fxRateQuery);

				ResultSetMetaData rsmd = aResultset.getMetaData();
				int columncount = rsmd.getColumnCount();
				// logger.debug("columncount " + columncount);
				returnmaplist = new ArrayList();

				while (aResultset.next()) {
					// logger.debug("FX RATE while loop");
					Map maplist = new HashMap();
					for (int i = 1; i < columncount + 1; i++) {
						String key = rsmd.getColumnLabel(i);
						String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
						// logger.debug("The key:" + key + " & the value: " +
						// value);
						maplist.put(key, value);
					}
					returnmaplist.add(maplist);
				}
			}

		} catch (Exception ex) {
			logger.error("FXRate exception is " + ex.getMessage());

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}
		return returnmaplist;
	}

	/**
	 * 
	 * @param rateCode
	 * @param status
	 * @return
	 */
	public static int updateFXRateStatus(String rateCode, String currency, String status, String modifiedDate,
			String quoteDate) {

		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		if (status.equalsIgnoreCase("SUCCEEDED"))
			status = "S";
		else if (status.equalsIgnoreCase("FAILED"))
			status = "F";
		else if (status.equalsIgnoreCase("UNAVAILABLE"))
			status = "U";
		else if (status.isEmpty())
			status = "U";

		logger.debug("RateCode : " + rateCode + " Status : " + status);
		// String modifiedDateString =
		// DateTimeUtil.getStringDateInFormat(modifiedDate, "yyyy-MM-dd",
		// "dd-MMM-yy");
		// logger.debug("modifiedDateString : " + modifiedDateString);
		String quoteDateString = DateTimeUtil.getStringDateInFormat(quoteDate, "yyyy-MM-dd", "dd-MMM-yy");
		logger.debug("quoteDateString : " + quoteDateString);

		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {
				// String updateQuery = "UPDATE CARD_RATE_FX SET PROCESSED_FLAG
				// = ?, PROCESSED_DATE = ?, PROC_REMARKS = ?, MODIFIED_BY = ?,
				// MODIFIED_DATE = ? WHERE CCY = ? ";
				String updateQuery = "UPDATE CARD_RATE_FX SET PROCESSED_FLAG = ?, PROCESSED_DATE = ?, PROC_REMARKS = ? WHERE CCY = ? AND TO_CHAR(QUOTE_DATE) = '"
						+ quoteDateString.toUpperCase() + "'";
				// AND TO_CHAR(MODIFIED_DATE) = '"+
				// modifiedDateString.toUpperCase() + "'
				// logger.debug("FX Rate update query is : " + updateQuery);

				String whereQuery = "";
				if (rateCode.equalsIgnoreCase("TTRTCD"))
					whereQuery = " AND RATE_TYPE IN ('TT_BUY', 'TT_SELL')";
				// TODO
				else if (rateCode.equalsIgnoreCase("BLRTCD"))
					whereQuery = " AND RATE_TYPE IN ('BL_BUY', 'BL_SELL') AND CREATED_BY = 'IFC'";

				else if (rateCode.equalsIgnoreCase("CNRTCD"))
					whereQuery = " AND RATE_TYPE IN ('CN_BUY', 'CN_SELL')";

				else if (rateCode.equalsIgnoreCase("DDRTCD"))
					whereQuery = " AND RATE_TYPE IN ('DD_BUY', 'DD_SELL')";

				else if (rateCode.equalsIgnoreCase("TCRTCD"))
					whereQuery = " AND RATE_TYPE IN ('TC_BUY', 'TC_SELL')";

				else if (rateCode.equalsIgnoreCase("BARTCD"))
					whereQuery = " AND RATE_TYPE IN ('BASE_BUY_RATE', 'BASE_SELL_RATE')";

				else if (rateCode.equalsIgnoreCase("CHRTCD"))
					whereQuery = " AND RATE_TYPE IN ('CH_BUY')";

				else if (rateCode.equalsIgnoreCase("RVRTCD"))
					whereQuery = " AND RATE_TYPE IN ('REVAL_RATE')";

				// else if (rateCode.equalsIgnoreCase("NRRTCD"))
				// whereQuery = " AND RATE_TYPE IN ('REVAL_RATE')"; // ? todo

				// else if (rateCode.equalsIgnoreCase("MDRTCD"))
				// whereQuery = " AND RATE_TYPE IN ('REVAL_RATE')";// todo

				// TODO add some more update ratecode here

				// logger.debug("FX Rate update where clause query : " +
				// updateQuery);

				aPreParedStatement = aConnection.prepareStatement(updateQuery + whereQuery);
				aPreParedStatement.setString(1, status);
				aPreParedStatement.setDate(2, DateTimeUtil.getSqlLocalDate());
				aPreParedStatement.setString(3, status);
				// aPreParedStatement.setString(4, "TI");
				// aPreParedStatement.setDate(5,
				// ThemeBridgeUtil.getSQLLocalDate());
				aPreParedStatement.setString(4, currency);
				updatedRowCount = aPreParedStatement.executeUpdate();
				logger.debug("FXRate UpdatedRowCount >>> " + updatedRowCount + "\t " + status);
			}

		} catch (Exception ex) {
			logger.error("FX Rate update exception! " + ex.getMessage());
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

}
