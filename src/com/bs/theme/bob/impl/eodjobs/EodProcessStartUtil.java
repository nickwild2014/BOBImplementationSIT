package com.bs.theme.bob.impl.eodjobs;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ValidationsUtil;

/**
 * 
 * @since 2016-09-07
 * @version 1.0.2
 * @author KXT51472, Prasath Ravichandran
 */
public class EodProcessStartUtil {

	private final static Logger logger = Logger.getLogger(EodProcessStartUtil.class.getName());

	/**
	 * 
	 * @return
	 */
	public static String eodProcessStart() {

		/**
		 * 2017-APR-04, Nothing is required here. All are added as separate
		 * custom job in EodCustomJobs project. By Prasath.
		 **/

		String result = "";
		try {

			/** Below jobs are configured diectly in TI custom jobs **/

			// Date tiSysDtae = getTISystemDate();
			// int procedureCurrentDay = pushToOutstandingHistory(tiSysDtae);
			// logger.debug("Procedure call 1-PROC_DAILY_OS_UPD end");

			// Date tiNxtDtae = getTINxtDate();
			// int procedureNextDay = pushToOutstandingHistory(tiNxtDtae);
			// logger.debug("Procedure call 2-PROC_DAILY_OS_UPD end");

			// int procedure2 = pushToBillsUpdateHistory();
			// logger.debug("Procedure call 3-PROC_DAILY_BILLS_UPD end");

			// String spotRateStatus = spotRateHistory();
			// logger.debug("SPOT rate history process completed");

			// String fxRateStatus = fxRateHistory();
			// logger.debug("FX rate history process completed");

			// String baseRateStatus = baseRateHistory();
			// logger.debug("Base rate history process completed");

		} catch (Exception e) {
			logger.debug("Exception while processing start eod processes..!" + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 
	 * @return
	 */
	public static String fxRateHistory() {

		logger.debug("CARD Rate History backup process initiated");
		int x = 0;
		int size = 0;
		String status = "";
		String result = "";

		try {
			List<HashMap<String, String>> cardRateMapList = getCardRateList();
			logger.debug("CARD RATE MAP SIZE : " + cardRateMapList.size());
			size = cardRateMapList.size();

			for (HashMap<String, String> map : cardRateMapList) {
				x = insertCardRateHistory(map);
			}

			if (x == 0) {
				status = ThemeBridgeStatusEnum.FAILED.toString();
			} else {
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			}

		} finally {
			ServiceLogging.pushLogData("TIEODJOB", "FXRATE", "ZONE1", "", "ZONE1", "BOB", "HISTORY", "BACKUP", status,
					String.valueOf(size), "TI_RESPONSE", "BANK_REQUEST", "BANK_RESPONSE", null, null, null, null, "",
					"", "", "", false, "0", "");
		}
		logger.debug("CARD RATE History backup process finished");
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public static String baseRateHistory() {

		logger.debug("BASE Rate History backup process initiated");
		boolean x = true;
		int size = 0;
		String status = "";
		String result = "";
		try {
			List<HashMap<String, String>> baseRateMapList = getBaseRateList();
			logger.debug("BASE RATE MAP SIZE : " + baseRateMapList.size());

			if (baseRateMapList != null) {
				size = baseRateMapList.size();

				for (HashMap<String, String> map : baseRateMapList) {
					x = insertBaseRateHistory(map);
				}
			}

			if (x) {
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			} else {
				status = ThemeBridgeStatusEnum.FAILED.toString();

			}

		} finally {
			ServiceLogging.pushLogData("TIEODJOB", "BASERATE", "ZONE1", "", "ZONE1", "BOB", "HISTORY", "BACKUP",
					status, String.valueOf(size), "TI_RESPONSE", "BANK_REQUEST", "BANK_RESPONSE", null, null, null,
					null, "", "", "", "", false, "0", "");

		}
		logger.debug("BASE Rate History backup process finished");
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public static String spotRateHistory() {

		logger.debug("Spot Rate History backup process initiated");
		int x = 0;
		int size = 0;
		String status = "";
		String result = "";

		try {
			List<HashMap<String, String>> spotRateMapList = getSpotRateList();
			logger.debug("SPOT RATE MAP SIZE : " + spotRateMapList.size());
			size = spotRateMapList.size();

			for (HashMap<String, String> map : spotRateMapList) {
				x = insertSpotRateHistory(map);
			}

			if (x == 0) {
				status = ThemeBridgeStatusEnum.FAILED.toString();
			} else {
				status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
			}

		} finally {
			ServiceLogging.pushLogData("TIEODJOB", "SPOTRATE", "ZONE1", "", "ZONE1", "BOB", "HISTORY", "BACKUP",
					status, String.valueOf(size), "TI_RESPONSE", "BANK_REQUEST", "BANK_RESPONSE", null, null, null,
					null, "", "", "", "", false, "0", "");
		}
		logger.debug("Spot RATE History backup process finished");
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public static List<HashMap<String, String>> getSpotRateList() {

		logger.debug("Get SPOT Rate List initiated");
		ResultSet aResultset = null;
		Statement bStatement = null;
		Connection bConnection = null;
		List<HashMap<String, String>> returnmaplist = null;
		// String postingStatusQuery = "SELECT QUOTE_DATE, RATE_TYPE, CCY, RATE,
		// PROCESSED_FLAG, PROCESSED_DATE AS PROCESSED_DATE, PROC_REMARKS,
		// CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM
		// CARD_RATE_FX ";
		String getSpotRateQuery = "SELECT CURRENCY, SPOTRATE FROM SPOTRATE WHERE SPOTRATE <> 1";
		logger.debug("ToMailListQuery : " + getSpotRateQuery);

		try {
			bConnection = DatabaseUtility.getTizoneConnection();
			bStatement = bConnection.createStatement();
			aResultset = bStatement.executeQuery(getSpotRateQuery);
			ResultSetMetaData rsmd = aResultset.getMetaData();
			int columncount = rsmd.getColumnCount();
			returnmaplist = new ArrayList();

			while (aResultset.next()) {
				HashMap<String, String> spotRateMapList = new HashMap<String, String>();
				for (int i = 1; i < columncount + 1; i++) {
					String key = rsmd.getColumnName(i);
					String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
					// logger.debug(key + "\t" + value);
					spotRateMapList.put(key, value);
				}
				returnmaplist.add(spotRateMapList);
			}

		} catch (Exception e) {
			logger.debug("SpotRate History Backup Exceptions!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, aResultset);
		}
		// logger.debug("postingStatus : " + cardRateMapList);
		return returnmaplist;
	}

	/**
	 * 
	 * @param cardRateMapList
	 * @return
	 */
	public static int insertSpotRateHistory(HashMap<String, String> cardRateMapList) {

		// logger.debug("Insert CARD Rate initiated");
		int insertRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {

				String updateQuery = "INSERT INTO SPOTRATEHISTORY (ID, QUOTE_DATE, CCY, RATE, PROCESS_TIME) VALUES (SPOTRATEHISTORY_SEQ.nextval, SYSDATE, ?, ?, CURRENT_TIMESTAMP) ";
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, cardRateMapList.get("CURRENCY"));
				aPreParedStatement.setString(2, cardRateMapList.get("SPOTRATE"));
				// aPreParedStatement.setTimestamp(12,
				// DateTimeUtil.getSqlLocalTimestamp());

				insertRowCount = aPreParedStatement.executeUpdate();
			}

		} catch (Exception ex) {
			logger.error("Inserting spot rate exception " + ex.getMessage());
			ex.printStackTrace();
			return insertRowCount;

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return insertRowCount;
	}

	/**
	 * 
	 * @param cardRateMapList
	 * @return
	 */
	public static List<HashMap<String, String>> getCardRateList() {

		logger.debug("Get CARD Rate List initiated");
		ResultSet aResultset = null;
		Statement bStatement = null;
		Connection bConnection = null;
		List<HashMap<String, String>> returnmaplist = null;
		// String postingStatusQuery = "SELECT QUOTE_DATE, RATE_TYPE, CCY, RATE,
		// PROCESSED_FLAG, PROCESSED_DATE AS PROCESSED_DATE, PROC_REMARKS,
		// CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM
		// CARD_RATE_FX ";
		String getCardRateQuery = "SELECT QUOTE_DATE, RATE_TYPE, CCY, RATE, PROCESSED_FLAG, PROCESSED_DATE AS PROCESSED_DATE, PROC_REMARKS, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM CARD_RATE_FX WHERE TO_CHAR(MODIFIED_DATE) = (SELECT TO_CHAR(SYSDATE, 'DD-MON-YY') FROM DUAL) ";
		logger.debug("ToMailListQuery : " + getCardRateQuery);
		try {
			bConnection = DatabaseUtility.getIdbFcConnection();
			bStatement = bConnection.createStatement();
			aResultset = bStatement.executeQuery(getCardRateQuery);
			ResultSetMetaData rsmd = aResultset.getMetaData();
			int columncount = rsmd.getColumnCount();
			returnmaplist = new ArrayList();

			while (aResultset.next()) {
				HashMap<String, String> cardRateMapList = new HashMap<String, String>();
				for (int i = 1; i < columncount + 1; i++) {
					String key = rsmd.getColumnName(i);
					String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
					// logger.debug(key + "\t" + value);
					cardRateMapList.put(key, value);
				}
				returnmaplist.add(cardRateMapList);
			}

		} catch (Exception e) {
			logger.debug("CardRate History Backup Exceptions!" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, aResultset);
		}
		// logger.debug("postingStatus : " + cardRateMapList);
		return returnmaplist;
	}

	/**
	 * 
	 * @param cardRateMapList
	 * @return
	 */
	public static int insertCardRateHistory(HashMap<String, String> cardRateMapList) {

		// logger.debug("Insert CARD Rate initiated");
		int insertRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {

				String updateQuery = "INSERT INTO CardRateFxHistory (ID, RATE_TYPE, CCY, RATE, PROCESSED_FLAG, PROC_REMARKS, CREATED_BY, MODIFIED_BY,  QUOTE_DATE, PROCESSED_DATE, CREATED_DATE, MODIFIED_DATE, PROCESS_TIME ) VALUES (CARDRATEHISTORY_SEQ.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, cardRateMapList.get("RATE_TYPE"));
				aPreParedStatement.setString(2, cardRateMapList.get("CCY"));
				aPreParedStatement.setString(3, cardRateMapList.get("RATE"));
				aPreParedStatement.setString(4, cardRateMapList.get("PROCESSED_FLAG"));
				aPreParedStatement.setString(5, cardRateMapList.get("PROC_REMARKS"));
				aPreParedStatement.setString(6, cardRateMapList.get("CREATED_BY"));
				aPreParedStatement.setString(7, cardRateMapList.get("MODIFIED_BY"));
				aPreParedStatement.setDate(8,
						DateTimeUtil.getSqlDateByStringDateInFormat(cardRateMapList.get("QUOTE_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setDate(9, DateTimeUtil
						.getSqlDateByStringDateInFormat(cardRateMapList.get("PROCESSED_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setDate(10,
						DateTimeUtil.getSqlDateByStringDateInFormat(cardRateMapList.get("CREATED_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setDate(11, DateTimeUtil
						.getSqlDateByStringDateInFormat(cardRateMapList.get("MODIFIED_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setTimestamp(12, DateTimeUtil.getSqlLocalTimestamp());

				insertRowCount = aPreParedStatement.executeUpdate();

			}

		} catch (Exception ex) {
			logger.error("Inser card rate exception " + ex.getMessage());
			ex.printStackTrace();
			return insertRowCount;

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return insertRowCount;
	}

	public static List<HashMap<String, String>> getBaseRateList() {

		logger.debug("Get BaseRate List initiated");
		ResultSet aResultset = null;
		Statement bStatement = null;
		Connection bConnection = null;
		List<HashMap<String, String>> returnmaplist = null;
		// String postingStatusQuery = "SELECT QUOTE_DATE, RATE_TYPE, RATE,
		// PROCESSED_FLAG, PROCESSED_DATE, PROC_REMARKS, CREATED_BY,
		// CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM BASE_RATE ";
		String getBaseRateQuery = "SELECT QUOTE_DATE, RATE_TYPE, RATE, PROCESSED_FLAG, PROCESSED_DATE, PROC_REMARKS, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM BASE_RATE WHERE TO_CHAR(MODIFIED_DATE) = (SELECT TO_CHAR(SYSDATE, 'DD-MON-YY') FROM DUAL)";
		logger.debug("ToMailListQuery : " + getBaseRateQuery);
		try {
			bConnection = DatabaseUtility.getIdbFcConnection();
			bStatement = bConnection.createStatement();
			aResultset = bStatement.executeQuery(getBaseRateQuery);
			ResultSetMetaData rsmd = aResultset.getMetaData();
			int columncount = rsmd.getColumnCount();
			returnmaplist = new ArrayList();

			while (aResultset.next()) {
				HashMap<String, String> cardRateMapList = new HashMap<String, String>();
				for (int i = 1; i < columncount + 1; i++) {
					String key = rsmd.getColumnName(i);
					String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
					// logger.debug(key + "\t" + value);
					cardRateMapList.put(key, value);
				}
				returnmaplist.add(cardRateMapList);
			}

		} catch (Exception e) {
			returnmaplist = null;
			logger.debug("Exceptions!" + e.getMessage());

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, aResultset);
		}
		// logger.debug("postingStatus : " + cardRateMapList);
		return returnmaplist;
	}

	/**
	 * 
	 * @param baseRateMapList
	 * @return
	 */
	public static boolean insertBaseRateHistory(HashMap<String, String> baseRateMapList) {

		logger.debug("Insert BASE Rate initiated");
		boolean result = false;
		int insertRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			if (aConnection != null) {
				String updateQuery = "INSERT INTO BASERATEHISTORY (ID, QUOTE_DATE, RATE_TYPE, RATE, PROCESSED_FLAG, PROCESSED_DATE, PROC_REMARKS, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE, PROCESS_TIME ) VALUES (CARDRATEHISTORY_SEQ.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ";
				aPreParedStatement = aConnection.prepareStatement(updateQuery);

				aPreParedStatement.setDate(1,
						DateTimeUtil.getSqlDateByStringDateInFormat(baseRateMapList.get("QUOTE_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setString(2, baseRateMapList.get("RATE_TYPE"));
				aPreParedStatement.setString(3, baseRateMapList.get("RATE"));
				aPreParedStatement.setString(4, baseRateMapList.get("PROCESSED_FLAG"));
				aPreParedStatement.setDate(5, DateTimeUtil
						.getSqlDateByStringDateInFormat(baseRateMapList.get("PROCESSED_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setString(6, baseRateMapList.get("PROC_REMARKS"));
				aPreParedStatement.setString(7, baseRateMapList.get("CREATED_BY"));
				aPreParedStatement.setDate(8,
						DateTimeUtil.getSqlDateByStringDateInFormat(baseRateMapList.get("CREATED_DATE"), "yyyy-MM-dd"));
				aPreParedStatement.setString(9, baseRateMapList.get("MODIFIED_BY"));
				aPreParedStatement.setDate(10, DateTimeUtil
						.getSqlDateByStringDateInFormat(baseRateMapList.get("MODIFIED_DATE"), "yyyy-MM-dd"));
				// aPreParedStatement.setTimestamp(12,
				// DateTimeUtil.getSqlLocalTimestamp());

				insertRowCount = aPreParedStatement.executeUpdate();
				result = true;
			}

		} catch (Exception ex) {
			logger.error("Insert Base rate exception " + ex.getMessage());
			ex.printStackTrace();
			result = false;

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return result;
	}

	/**
	 * KRIOS - REPORTS TEAM
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static int pushToOutstandingHistory(Date tiSysDtae) {

		logger.debug("Entering into pushToOutstandingHistory Procedure call / REPORTS TEAM");

		String processDate = "";
		String status = "FAILED";
		int insertedRowCount = 0;
		String errorMesssage = "";
		ResultSet aResultSet = null;
		Connection aConnection = null;
		CallableStatement aCallableStatement = null;

		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {

				// FUNCTION RETURN NOTHING. NO INPUT & NO OUTPUT FOR FUNCTION
				// String procedureQuery = "{call PROC_DAILY_OS_UPD()}";
				String procedureQuery = "{call PROC_DAILY_OS_UPD(?)}";
				// SELECT * FROM ETT_DAILY_OS; --APPLICATION DATE

				aCallableStatement = aConnection.prepareCall(procedureQuery);
				aCallableStatement.setDate(1, tiSysDtae);

				insertedRowCount = aCallableStatement.executeUpdate();
				logger.debug(insertedRowCount + " rows are inserted successfully");

				if (insertedRowCount == 1) {
					status = "SUCCEEDED";
				}
			}

			Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			processDate = formatter.format(tiSysDtae);

		} catch (SQLException ex) {
			logger.error("PushToExtDataBase exception is: " + ex.getMessage());
			errorMesssage = ex.getMessage();
			ex.printStackTrace();
			return insertedRowCount;

		} finally {
			DatabaseUtility.surrenderCallableConnection(aConnection, aCallableStatement, aResultSet);

			// New Logging
			ServiceLogging.pushLogData("TIEODJOB", "REPORTS", "ZONE1", "", "ZONE1", "BOB", "DLY_OS_AMT", "PROCEDURE",
					status, "TI_REQUEST", "TI_RESPONSE", "BANK_REQUEST", "BANK_RESPONSE", null, null, null, null, "",
					"", processDate, "", false, "0", errorMesssage);
		}
		return insertedRowCount;
	}

	/**
	 * KRIOS - REPORTS TEAM
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static int pushToBillsUpdateHistory() {

		logger.debug("Entering into pushToBillsUpdateHistory Procedure call / REPORTS TEAM");

		String status = "FAILED";
		int insertedRowCount = 0;
		String errorMesssage = "";
		ResultSet aResultSet = null;
		Connection aConnection = null;
		CallableStatement aCallableStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {
				Timestamp bankReqTime = DateTimeUtil.getTimestamp();

				// FUNCTION RETURN NOTHING. NO INPUT & NO OUTPUT FOR FUNCTION
				String procedureQuery = "{call PROC_DAILY_BILLS_UPD()}";
				// SELECT * FROM ETT_BILLS_HISTROY; --APPLICATION DATE

				aCallableStatement = aConnection.prepareCall(procedureQuery);

				insertedRowCount = aCallableStatement.executeUpdate();
				logger.debug(insertedRowCount + " rows are inserted successfully");

				Timestamp bankResTime = DateTimeUtil.getTimestamp();
				if (insertedRowCount == 1) {
					status = "SUCCEEDED";
				}
			}
		} catch (SQLException ex) {
			logger.error("pushToExtDataBase exception is: " + ex.getMessage());
			errorMesssage = ex.getMessage();
			ex.printStackTrace();
			return insertedRowCount;

		} finally {
			DatabaseUtility.surrenderCallableConnection(aConnection, aCallableStatement, aResultSet);
			// New Logging
			ServiceLogging.pushLogData("TIEODJOB", "REPORTS", "ZONE1", "", "ZONE1", "BOB", "DLY_BILLS_UPD",
					"PROCEDURE", status, "TI_REQUEST", "TI_RESPONSE", "BANK_REQUEST", "BANK_RESPONSE", null, null, null,
					null, "", "", "", "", false, "0", errorMesssage);
		}
		return insertedRowCount;
	}

	private static Date getTISystemDate() {

		Date tiCurrDate = null;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		// 02-OCT-16
		String query = "SELECT to_date(PROCDATE,'dd-MON-yy') as PROCDATE FROM DLYPRCCYCL ";
		logger.debug("TI CURRENT DATE QUERY : " + query);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				tiCurrDate = rs.getDate(1);
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

		return tiCurrDate;
	}

	private static Date getTINxtDate() {

		Date tiCurrDate = null;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		// 02-OCT-16
		String query = "SELECT to_date(NEXTDATE,'dd-MON-yy') as PROCDATE FROM DLYPRCCYCL ";
		logger.debug("TI CURRENT DATE QUERY : " + query);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				tiCurrDate = rs.getDate(1);
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

		return tiCurrDate;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		EodProcessStartUtil anAdaptee = new EodProcessStartUtil();
		// String request =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\task
		// eod\\EOD.Start.xml");
		// String request =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\task
		// eod\\EOD.Stop.xml");

		// logger.debug(anAdaptee.process(request));

		// HashMap<String, String> list = getpostingStatus();
		// logger.debug(pushToBillsUpdateHistory());
		anAdaptee.spotRateHistory();

		// Date d = getTISystemDate();
		// logger.debug(d);

		// pushToOutstandingHistory(d);

	}

}
