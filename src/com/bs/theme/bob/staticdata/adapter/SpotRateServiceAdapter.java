package com.bs.theme.bob.staticdata.adapter;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.staticdata.util.TISpotRate;
import com.bs.themebridge.logging.StaticLogging;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;

public class SpotRateServiceAdapter {

	private final static Logger logger = Logger.getLogger(SpotRateServiceAdapter.class.getName());

	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String inputMessage = "";

	static String filePath = new File("").getAbsolutePath();

	public static void main(String a[]) throws Exception {

		getTIFSpotRateRequest("42.32", "0001", "ZAR");

	}

	/**
	 * 
	 * @param sellRate
	 * @param buyRate
	 * @param bankEntity
	 * @param currency
	 * @param rateCode
	 * @return
	 */
	public static boolean getTIFSpotRateRequest(String spotRate, String bankEntity, String currency) {

		// logger.debug("Currency : " + currency + ";\t spotRate :" + spotRate);

		String status = "";
		String errorMessage = "";
		boolean spotRatePushStatus = true;
		Timestamp reqReceivedTime = null;
		// StatusEnum statusEnum = StatusEnum.FAILED;

		try {
			if (ValidationsUtil.isValidString(spotRate) && ValidationsUtil.isValidString(bankEntity)
					&& ValidationsUtil.isValidString(currency)) {

				TISpotRate tiSpotRate = new TISpotRate();
				tiSpotRate.generateHeader();
				tiSpotRate.generateStaticDataConstants();
				tiSpotRate.generateTokenMap();
				tiSpotRate.generateSetProperty("Currency", currency);
				tiSpotRate.generateSetProperty("SpotRate", spotRate);
				// if (currency.equalsIgnoreCase("JPY")) {
				// logger.debug("Currency : JPY");
				// String convertedRate =
				// FXRateServiceAdapter.getUnitRate(spotRate);
				// tiSpotRate.generateSetProperty("SpotRate", convertedRate);
				// } else {
				// tiSpotRate.generateSetProperty("SpotRate", spotRate);
				// }
				tiSpotRate.generateSetProperty("BankingEntity", bankEntity);
				// This will impact on FX DEAL displaying treasury rate
				tiSpotRate.generateSetProperty("Reciprocal", "Y"); // always

				tiSpotRate.generateTokenMap();
				String tokenReplacedXML = null;
				tokenReplacedXML = tiSpotRate.getXMLString();
				String tagRemovedXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);

				tiRequest = tagRemovedXML;
				reqReceivedTime = DateTimeUtil.getTimestamp();
				logger.debug("\n\nSpot Rate TI RequestXML :- " + tiRequest);

				String tiResponseXML = TIPlusEJBClient.process(tiRequest);
				tiResponse = tiResponseXML;
				logger.debug("Spot Rate TI ResponseXML :- " + tiResponse);

				// Get Xpath Status
				// statusEnum =
				// ResponseHeaderUtil.processEJBClientResponse(tiResponse);
				// status = statusEnum.toString();
				status = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Status");
				errorMessage = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Details/Error");

			}

		} catch (Exception e) {
			spotRatePushStatus = false;
			errorMessage = errorMessage + e.getMessage();
			logger.error("SpotRate Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// updatedRow = updateBaseRateStatus(rateCode, currency, status);
			StaticLogging.pushLogData("TI", "SpotRate", "ZONE1", "All", "IDBEXT", "TIPlus", status, reqReceivedTime,
					inputMessage, tiRequest, tiResponse, currency, spotRate, spotRate, false, "0", errorMessage);

		}
		// logger.info("FXRate Push Response XML-->" + fxRateTIRes);
		return spotRatePushStatus;
	}

	/**
	 * 
	 * @return
	 */
	public static List<Map<String, String>> getFXRateList() {

		List<Map<String, String>> returnmaplist = null;
		ResultSet aResultset = null;
		Connection aConnection = null;
		Statement aStatement = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();

				String fxRateQuery = "SELECT C1.CCY AS CCY,SUM(DECODE(C1.RATE_TYPE,'REVAL_RATE',C1.RATE)) AS REVAL_RATE FROM CARD_RATE_FX C1 WHERE C1.CREATED_BY = 'IFC' AND C1.PROCESSED_FLAG NOT IN ('N') AND C1.QUOTE_DATE = TRUNC(SYSDATE) GROUP BY C1.CCY ORDER BY C1.CCY  ";
				logger.debug("SpotRateStaticQuery is :- " + fxRateQuery);
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

}
