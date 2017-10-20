package com.bs.theme.bob.impl.eodjobs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.LimitReservationsXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class EodLimitExposureRevaluation {

	private final static Logger logger = Logger.getLogger(EodLimitExposureRevaluation.class.getName());

	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String correlationId = "";
	private static Timestamp tiReqTime = null;
	private static Timestamp tiResTime = null;
	private static Timestamp bankReqTime = null;
	private static Timestamp bankResTime = null;

	public static void main(String[] args) {

		EodLimitExposureRevaluation anAdaptee = new EodLimitExposureRevaluation();
		String result = anAdaptee.limitRevaluationProcess("");
		logger.debug(result);

	}

	public String limitRevaluationProcess(String requestXML) {

		String result = "SUCCEEDED";

		EodLimitExposureRevaluation limitrevalobj = new EodLimitExposureRevaluation();
		List<String> distinctCcy = limitrevalobj.getDistntTranCcyFromLimitRevaluationTbl();

		for (String ccy : distinctCcy) {
			double spotRate = getSpotRateFCY(ccy);

			List<Map<String, String>> mapList = limitrevalobj.getLimitRevaluationList(ccy);

			if (mapList != null && mapList.size() > 0) {

				for (Map<String, String> map : mapList) {

					double limetRevalRate = Double.parseDouble(map.get("RATE"));
					logger.debug("limetRevalRate " + limetRevalRate);

					double newRate = spotRate - limetRevalRate;
					logger.debug("spotRate " + spotRate);
					logger.debug("newRate " + newRate);

					if (newRate != 0) {
						double tranAmount = Double.parseDouble(map.get("TRXNAMOUNT"));
						tranAmount = tranAmount * newRate;
						logger.debug("tranAmount " + tranAmount);
						BigDecimal roundAmount = new BigDecimal(tranAmount);
						logger.debug("roundAmount " + roundAmount);
						if (ccy.equals("OMR") || ccy.equals("BHD") || ccy.equals("KWD") || ccy.equals("JOD")) {
							tranAmount = roundAmount.setScale(3, RoundingMode.CEILING).doubleValue();
						} else if (ccy.equals("JPY")) {
							tranAmount = roundAmount.setScale(2, RoundingMode.CEILING).doubleValue();
						} else {
							tranAmount = roundAmount.setScale(2, RoundingMode.CEILING).doubleValue();
						}
						logger.debug("tranAmount " + tranAmount);
						result = limitrevalobj.exposureLimitReservations(tranAmount, map);
						logger.debug("----------------------------------------------------------------------------");
					}
				}
			}
		}

		logger.debug(limitrevalobj.getDistntTranCcyFromLimitRevaluationTbl());

		return result;
	}

	public List<Map<String, String>> getLimitRevaluationList(String ccy) {

		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		List<Map<String, String>> returnMaplist = new ArrayList<Map<String, String>>();
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();

				String query = "select trim(LIMITPREFIX) LIMITPREFIX,trim(LIMITSUFFIX) LIMITSUFFIX,trim(TRXNAMOUNT) TRXNAMOUNT,trim(TRXNCURRENCY) TRXNCURRENCY, trim(RATE) RATE,trim(VALUEDATE) VALUEDATE,trim(PROCESSEDFLAG) PROCESSEDFLAG,trim(NARRATIVE1) NARRATIVE1,trim(LIABTYPE) LIABTYPE from LIMITREVALUATION where trxncurrency='"
						+ ccy + "' AND processedflag in ('N','X')";

				aResultset = aStatement.executeQuery(query);
				ResultSetMetaData rsmd = aResultset.getMetaData();
				int columnCount = rsmd.getColumnCount();

				while (aResultset.next()) {
					Map<String, String> maplist = new HashMap<String, String>();
					for (int i = 1; i < columnCount + 1; i++) {
						String key = rsmd.getColumnLabel(i);
						String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
						// logger.debug(key + "\t" + value);
						maplist.put(key, value);
					}
					returnMaplist.add(maplist);
				}
			}
		} catch (Exception ex) {
			returnMaplist = null;
			logger.debug("CustomerData Exceptions! " + ex.getMessage());
			ex.getMessage();
			return returnMaplist;

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		return returnMaplist;
	}

	public List<String> getDistntTranCcyFromLimitRevaluationTbl() {

		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		List<String> returnList = new ArrayList<String>();
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();

				String query = "select distinct trim(TRXNCURRENCY) TRXNCURRENCY from LIMITREVALUATION";
				aResultset = aStatement.executeQuery(query);

				while (aResultset.next()) {
					returnList.add(aResultset.getString("TRXNCURRENCY"));
				}
			}
		} catch (Exception ex) {
			logger.debug("CustomerData Exceptions! " + ex.getMessage());
			ex.getMessage();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		return returnList;
	}

	public String exposureLimitReservations(double tranAmount, Map<String, String> map) {

		String status = "SUCCEEDED";
		String branch = "";
		String narrative1 = "";
		String errorMsg = null;
		String reversaltiResponse = "";
		String reversalBankRequest = "";
		String reversalBankResponse = "";
		String reservationstiResponseStatus = "";
		String limitPrefix = "";
		String limitSuffix = "";
		InputStream anInputStream = null;
		try {
			logger.debug("Milestone Dr Reservations");

			String transactionID = ThemeBridgeUtil.randomCorrelationId();
			anInputStream = EodLimitExposureRevaluation.class.getClassLoader()
					.getResourceAsStream("BankRequest_LimitExposure_Reservations_Template.xml");
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
			logger.debug(requestTemplate);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("RequestUUID", transactionID);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			// tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceRequestVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("MessageDateTime", DateTimeUtil.getDateAsEndSystemFormat());

			logger.debug("Milestone 04");

			// tokens.put("REQUEST_ID", masterRef);
			tokens.put("REQUEST_ID", transactionID);
			tokens.put("LIMIT_PREFIX", map.get("LIMITPREFIX"));
			tokens.put("LIMIT_SUFFIX", map.get("LIMITSUFFIX"));
			tokens.put("UML", "0.0");////
			// tokens.put("REMARKS", "REVALUATION " +
			// ThemeBridgeUtil.getCurrentDateAsTreasury());// TODO
			tokens.put("REMARKS", DateTimeUtil.getCurrentDateAsTreasury());

			if (map.get("LIABTYPE").contains("FL")) {
				tokens.put("FUND_LIAB", Double.toString(tranAmount));
				tokens.put("NON_FUND_LIAB", "0.0");
			} else {
				tokens.put("FUND_LIAB", "0.0");
				tokens.put("NON_FUND_LIAB", Double.toString(tranAmount));
			}

			logger.debug("Milestone 08 b");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			reversalBankRequest = reader.toString();
			reader.close();

			logger.debug("Milestone 09");
			logger.debug("\nLimit Exposure Reservations BankRequest :- " + reversalBankRequest);

			logger.debug("Milestone 10");
			if (!reversalBankRequest.isEmpty()) {
				reversalBankResponse = getBankResponseFromBankRequest(reversalBankRequest);
				logger.debug("reversalBankResponse " + reversalBankResponse);
				bankResTime = DateTimeUtil.getTimestamp();
				logger.debug("\nLimit Exposure Reservations BankResponse :- " + reversalBankResponse);

				if (!reversalBankResponse.isEmpty()) {
					reversaltiResponse = getTIResponseFromBankResponse(reversalBankResponse);
					tiResponse = reversaltiResponse;
					tiResTime = DateTimeUtil.getTimestamp();
					logger.debug("\nLimit Exposure Reservations TIResponse :- " + reversaltiResponse);

					reservationstiResponseStatus = XPathParsing.getValue(tiResponse,
							"/ServiceResponse/ResponseHeader/Status");

					logger.debug("tiResponse tiResponse ====> " + tiResponse);

					if (reservationstiResponseStatus.equals("SUCCEEDED")) {
						logger.debug("\nLimit Exposure Reservations TIResponse status :- " + reversaltiResponse);

						limitPrefix = map.get("LIMITPREFIX");
						limitSuffix = map.get("LIMITSUFFIX");
						/**
						 * update the table if success
						 */
						LimitRevaluationTblUpdate(map.get("LIMITPREFIX"), map.get("LIMITSUFFIX"), map.get("TRXNAMOUNT"),
								map.get("TRXNCURRENCY"), map.get("RATE"), map.get("VALUEDATE"),
								map.get("PROCESSEDFLAG"), map.get("LIABTYPE"));

					} else {
						status = "FAILED";
					}

				} else {
					status = "FAILED";
					reservationstiResponseStatus = "FAILED";
					logger.error("Bank Response is empty..!");
					tiResponse = getErrorTIResponse("HTTP - 404 Finacle Host Unavailable [IM]");
				}

			} else {
				status = "FAILED";
				reservationstiResponseStatus = "FAILED";
				logger.error("BankRequest is empty..!");
				tiResponse = getErrorTIResponse("Unexpected error while parsing [IM]");
			}

		} catch (Exception e) {
			status = "FAILED";
			errorMsg = "Nothing to do for exposure debit leg / Hard Block Reservation not required..!";

		} finally {
			ServiceLogging.pushLogData("Limit", "Revaluation", "ZONE1", branch, "ZONE1", "BOB", limitPrefix,
					limitSuffix, reservationstiResponseStatus, tiRequest, reversaltiResponse, reversalBankRequest,
					reversalBankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", narrative1, "", false,
					"0", errorMsg);
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		return status;
	}

	private int LimitRevaluationTblUpdate(String LimitPrefix, String LimitSuffix, String TrxnAmount,
			String TrxnCurrency, String Rate, String ValueDate, String ProcessedFlag, String LiabType) {

		int updatedCount = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		String updateUTRQuery = "update LIMITREVALUATION set processedflag=? where limitprefix=? and limitsuffix=? and trxnamount=? and trxncurrency=? and rate=? and valuedate=? and processedflag=? and liabtype=?";

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(updateUTRQuery);
			aPreparedStatement.setString(1, "Y");
			aPreparedStatement.setString(2, LimitPrefix);
			aPreparedStatement.setString(3, LimitSuffix);
			aPreparedStatement.setString(4, TrxnAmount);
			aPreparedStatement.setString(5, TrxnCurrency);
			aPreparedStatement.setString(6, Rate);
			aPreparedStatement.setString(7, ValueDate);
			aPreparedStatement.setString(8, ProcessedFlag);
			aPreparedStatement.setString(9, LiabType);

			updatedCount = aPreparedStatement.executeUpdate();

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return updatedCount;

	}

	public static double getSpotRateFCY(String currency) {

		double buyRate = 0;
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;

		String SpotRateQuery = "SELECT SPOTRATE FROM SPOTRATE WHERE CURRENCY = '" + currency + "'";
		// logger.debug("SpotRateQuery : " + SpotRateQuery);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(SpotRateQuery);
			while (aResultset.next()) {
				buyRate = aResultset.getDouble("SPOTRATE");
			}
		} catch (Exception e) {
			logger.debug("Exceptions! while getting spotrate..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		logger.debug("SPOTRATE For Ccy : " + buyRate);
		return buyRate;
	}

	/**
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String getBankResponseFromBankRequest(String bankRequest) {

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.debug("resu " + result);
			logger.error("Limit ReservationsReversal Finacle exceptions! " + e.getMessage());
			result = "";
		}
		return result;
	}

	/**
	 * 
	 * @param responseXML
	 *            {@code allows }{@link String}
	 * @return {@code allows } {@link String}
	 */
	private static String getTIResponseFromBankResponse(String responseXML) {

		logger.debug("Enterd into buildTIResponseFromBankResponse method 1111 ");

		String result = "";
		String errorMessage = "";
		String tokenReplacedXML = "";
		InputStream anInputStream = null;
		try {
			anInputStream = EodLimitExposureRevaluation.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_EXPOSURE_TI_RESPONSE_TEMPLATE);

			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			String hostStatus = XPathParsing.getValue(responseXML, LimitReservationsXpath.HostStatausXpath);
			logger.debug("hostStatus " + hostStatus);
			String exposureStataus = XPathParsing.getValue(responseXML, LimitReservationsXpath.FacilityStatausXpath);
			logger.debug("exposureStataus " + exposureStataus);

			errorMessage = getBankResponseErrorMessage(responseXML);

			Map<String, String> tokens = new HashMap<String, String>();
			if (hostStatus.equalsIgnoreCase("FAILURE") || exposureStataus.equalsIgnoreCase("FAILURE")) {
				exposureStataus = "FAILED";
				tokens.put("Status", "FAILED");
				tokens.put("Error", errorMessage + " [IM]");
				tokens.put("Info", "");
				tokens.put("Warning", "");
				tokens.put("CorrelationId", correlationId);
				tokens.put("ResponseHeader", "");

			} else if (hostStatus.equalsIgnoreCase("SUCCESS") && exposureStataus.equalsIgnoreCase("SUCCESS")) {
				exposureStataus = "SUCCEEDED";
				tokens.put("Status", "SUCCEEDED");
				tokens.put("Error", "");
				tokens.put("Info", "");
				tokens.put("Warning", "");
				tokens.put("CorrelationId", correlationId);
				tokens.put("ResponseHeader", "");

				// } else {
				// // creditDebitExposureStatus =
				// exposureStataus = "RECEIVED";
				// tokens.put("Status", "RECEIVED");
				// tokens.put("Error", "");
				// tokens.put("Info", "");
				// tokens.put("Warning", "");
				// tokens.put("CorrelationId", correlationId);
				// tokens.put("ResponseHeader", "");
			}

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();

			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeExposure Exceptions! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		logger.debug("TIResponse : " + result);
		return result;
	}

	/**
	 * 
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventRef
	 *            {@code allows }{@link String}
	 * @param limitPrefix
	 *            {@code allows }{@link String}
	 * @param limitSuffix
	 *            {@code allows }{@link String}
	 * @param reservationIdentifier
	 *            {@code allows }{@link String}
	 * @param loggingTranAmount
	 *            {@code allows }{@link String}
	 * @param txnCrncyCode
	 *            {@code allows }{@link String}
	 * @param rate
	 *            {@code allows }{@link String}
	 * @param exposureAmount
	 *            {@code allows }{@link long}
	 * @param drCrFlag
	 *            {@code allows }{@link String}
	 * @param valueDateGregorian
	 *            {@code allows }{@link XMLGregorianCalendar}
	 * @return
	 */
	public boolean limitExposureLogging(String masterRef, String eventRef, String limitPrefix, String limitSuffix,
			String reservationIdentifier, String loggingTranAmount, String txnCrncyCode, String rate,
			String exposureAmount, String drCrFlag, java.sql.Date valueDate) {

		boolean result = true;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {

				String query = "INSERT INTO limitExposure(ID, MASTERREFERENCE, EVENTREFERENCE, LIMITPREFIX, LIMITSUFFIX, RESERVATIONID, "
						+ "TRXNAMOUNT, TRXNCURRENCY, RATE, EXPOSUREAMOUNT, EXPOSUREFLAG, VALUEDATE, PROCESSTIME, NARRATIVE1 )"
						+ " VALUES (LIMITEXPOSURE_SEQ.nextval, ?,?,?,?,?,?,?,?,?,?,?,?,?)";

				ps = con.prepareStatement(query);
				ps.setString(1, masterRef);
				ps.setString(2, eventRef);
				ps.setString(3, limitPrefix);
				ps.setString(4, limitSuffix);
				ps.setString(5, reservationIdentifier);

				// logger.debug(">>> \n\n" + new
				// BigDecimal(loggingTranAmount));
				// ps.setBigDecimal(6, new BigDecimal(loggingTranAmount));
				// loggingTranAmount = "55555555555551.5";
				// loggingTranAmount = "98999999999999.22556";

				double d = Double.parseDouble(loggingTranAmount);
				ps.setDouble(6, d); // exposure

				ps.setString(7, txnCrncyCode);
				ps.setString(8, rate);
				// TODO DATE CONVESTION
				// ps.setLong(9, exposureAmount);
				ps.setBigDecimal(9, new BigDecimal(exposureAmount));
				ps.setString(10, drCrFlag);// Flag

				// if (valueDateGregorian != null) {
				// java.sql.Date valueSqlDates =
				// DateTimeUtil.getSqlDateByXMLGregorianCalendar(valueDateGregorian);
				// ps.setDate(11, valueSqlDates);// valuedate
				// } else {
				ps.setDate(11, valueDate);// valuedate
				// }

				// ps.setTimestamp(12, ThemeBridgeUtil.GetCurrentTimeStamp());
				ps.setTimestamp(12, DateTimeUtil.GetLocalTimeStamp());
				ps.setString(13, null);// NARRATIVE1

				int insertedRows = ps.executeUpdate();

				if (insertedRows > 0) {
					logger.debug(insertedRows + "(Limit Exposure) Row inserted successfully!!! ");
				} else {
					logger.debug("EOD row inserted Failed");
				}
			}
		} catch (Exception ex) {
			logger.error("The Exception is :" + ex.getMessage());
			ex.printStackTrace();
			result = true;

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}

		return result;
	}

	/**
	 * 
	 * @param responseXML
	 *            {@code allows }{@link String}
	 * @return {@code allows } {@link String}
	 */
	private static String getErrorTIResponse(String errorMesage) {

		logger.debug("Enterd into buildTIResponseFromBankResponse method 222 ");

		String result = "";
		String tokenReplacedXML = "";
		InputStream anInputStream = null;
		try {
			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_EXPOSURE_TI_RESPONSE_TEMPLATE);
			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			// exposureStataus = "FAILED";
			tokens.put("Status", "RECEIVED");
			tokens.put("Error", errorMesage);
			tokens.put("Info", "");
			tokens.put("Warning", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("ResponseHeader", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();

			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeExposure Exceptions! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		logger.debug("TIResponse : " + result);
		return result;

	}

	/**
	 * 
	 * @param bankResponseXml
	 *            {@code allows }{@link String}
	 * @return {@code returns }{@link String}
	 */
	public static String getBankResponseErrorMessage(String bankResponseXml) {

		String allerrorMessages = "";

		try {
			String scriptResponseException = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FIScriptResponseExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml, LimitReservationsXpath.FIScriptResponseExMsgDescXpath);

			String fiBusinessErrorMsg = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FIBusinessExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml, LimitReservationsXpath.FIBusinessExMsgDescXpath);

			String fiSystemErrorDesc = "";
			String fiSystemErrorCode = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FISystemExCodeXpath);

			if (fiSystemErrorCode.equalsIgnoreCase("60012")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Service Not Available. Internal Finlistval Error occured";

			} else if (fiSystemErrorCode.equalsIgnoreCase("60023")) {
				fiSystemErrorDesc = fiSystemErrorCode
						+ " System Error in Finacle Core. Could Not Get Response from Server";

			} else if (fiSystemErrorCode.equalsIgnoreCase("60024")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Fatal Error in Finacle Core";

			} else if (fiSystemErrorCode.equalsIgnoreCase("3009")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Runtime error has occured. Internal Finlistval Error";

			} else {
				fiSystemErrorDesc = fiSystemErrorCode;
			}

			allerrorMessages = "Finacle exception " + scriptResponseException + fiBusinessErrorMsg + fiSystemErrorDesc;
			logger.debug("Limit Reservations BankResponse error : " + allerrorMessages);

		} catch (XPathExpressionException e) {
			logger.error("XPathExpressionException! " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.error("SAXException! " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error("IOException! " + e.getMessage());
			e.printStackTrace();
		}

		return allerrorMessages;
	}

	/**
	 * below are data base column names
	 */
	// logger.debug(map.get("LIMITPREFIX"));
	// logger.debug(map.get("LIMITSUFFIX"));
	// logger.debug(map.get("TRXNAMOUNT"));
	// logger.debug(map.get("TRXNCURRENCY"));
	// logger.debug(map.get("RATE"));
	// logger.debug(map.get("VALUEDATE"));
	// logger.debug(map.get("PROCESSEDFLAG"));
	// logger.debug(map.get("NARRATIVE1"));
	// logger.debug(map.get("LIABTYPE"));

}
