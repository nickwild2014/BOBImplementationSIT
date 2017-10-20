package com.bs.theme.bob.staticdata.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
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
import java.util.UUID;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.incoming.util.StaticDataConstant;
import com.bs.themebridge.logging.StaticLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.StatusEnum;

public class BaseRateServiceAdapter {

	private final static Logger logger = Logger.getLogger(BaseRateServiceAdapter.class.getName());

	private static String misc3 = "N/A";
	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String inputMessage = "";
	private static String type = "StagingTable";

	public static void main(String[] args) throws Exception {

		BaseRateServiceAdapter baseRateAdapter = new BaseRateServiceAdapter();
		baseRateAdapter.pushTIBaseRateService();
		// int s = 1;
		// baseRateAdapter.updateBaseRateStatus("EUR", s);

		// updateBaseRateStatus("USD_3M", "S", "2016-12-09", "2013-10-17");
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public String pushTIBaseRateService() {

		// logger.debug(" ************ BaseRate adapter process started
		// ************ ");
		String result = "";
		InputStream anInputStream = null;
		try {
			List<Map<String, String>> mapList = getBaseRateList();
			// logger.debug("BaseRate MapList size : " + mapList.size());

			if (mapList.size() > 0) {
				anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.TI_BASE_RATE_REQUEST_TEMPLATE);
				String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
				// logger.debug("requestTemplate : " + requestTemplate);

				for (Map<String, String> map : mapList) {
					String rateStatus = push(map, requestTemplate);
				}
			}
			// logger.debug(" ************ BaseRate adapter process ended
			// ************ ");
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();

		} finally {
			try {
				if (anInputStream != null)
					anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		return result = StatusEnum.SUCCEEDED.toString();
	}

	/**
	 * 
	 * @param map
	 * @param requestTemplate
	 * @return
	 */
	public String push(Map<String, String> map, String requestTemplate) {

		String status = "FAILED";
		String rateType = "";
		String errorMessage = "";
		String rate = map.get("RATE");
		boolean baseRatePushStatus = true;
		Timestamp reqReceivedTime = null;
		String rateType_orig = map.get("RATE_TYPE");
		// MODIFIED_DATE
		String modifiedDate = map.get("MODIFIED_DATE").substring(0, 10);
		String quoteDate = map.get("QUOTE_DATE").substring(0, 10);
		int updatedRow = 0;
		// StatusEnum statusEnum = StatusEnum.FAILED;
		String rateDate = map.get("QUOTE_DATE").substring(0, 10);
		logger.debug("RATE_TYPE : " + rateType + "\t RATE : " + rate + "\tQUOTE_DATE : " + rateDate);

		try {
			rateType = rateType_orig.replace("_", "");
			// logger.debug("rateType after removal '_' : " + rateType);
			String tokenReplacedbaseXML = null;
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Name", StaticDataConstant.CredentialName);
			tokens.put("Password", "");
			tokens.put("Certificate", "");
			tokens.put("Digest", "");
			tokens.put("CorrelationId", UUID.randomUUID().toString());
			tokens.put("MaintType", "F");
			tokens.put("MaintainedInBackOffice", "N");
			tokens.put("Branch", "0001");
			tokens.put("RateCode", rateType);
			tokens.put("RateDate", "");
			tokens.put("DateFlag", "Z");
			tokens.put("Rate", rate);
			tokens.put("Historical", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedbaseXML = reader.toString();
			reader.close();

			String tagRemovedXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedbaseXML);
			reqReceivedTime = DateTimeUtil.getTimestamp();
			tiRequest = tagRemovedXML;
			logger.debug("BASE Rate TI RequestXML :- " + tiRequest);
			/********************************************************/
			String tiResponseXML = TIPlusEJBClient.process(tiRequest);
			tiResponseXML = tiResponse;
			logger.debug("BASE Rate TI ResponseXML :- " + tiResponse);

			// statusEnum =
			// ResponseHeaderUtil.processEJBClientResponse(tiResponse);
			// status = statusEnum.toString();
			status = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Status");
			errorMessage = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Details/Error");

		} catch (Exception e) {
			baseRatePushStatus = false;
			errorMessage = errorMessage + e.getMessage();
			logger.error("FX Rate Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// logger.debug("Calling update method - BaseRate");
			updatedRow = updateBaseRateStatus(rateType_orig, status, modifiedDate, quoteDate);

			StaticLogging.pushLogData("TI", "BaseRate", "ZONE1", "0001", "IDBEXT", "TIPlus", status, reqReceivedTime,
					inputMessage, tiRequest, tiResponse, rateType, rate, misc3, false, "0", type);
		}
		return status;
	}

	/**
	 * 
	 * @return
	 */
	public static List<Map<String, String>> getBaseRateList() {

		List<Map<String, String>> returnmaplist = null;
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		try {
			// aConnection = DBConnection.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();

				String query = "SELECT QUOTE_DATE, RATE_TYPE, RATE, PROCESSED_FLAG, PROCESSED_DATE, PROC_REMARKS, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE FROM BASE_RATE WHERE PROCESSED_FLAG IS null OR PROCESSED_FLAG <> 'S' ORDER BY MODIFIED_DATE ASC";
				// logger.debug("BaseRateStaticQuery : \n" + query);
				aResultset = aStatement.executeQuery(query);
				ResultSetMetaData rsmd = aResultset.getMetaData();
				int columncount = rsmd.getColumnCount();
				returnmaplist = new ArrayList();
				while (aResultset.next()) {
					Map maplist = new HashMap();
					for (int i = 1; i < columncount + 1; i++) {
						String key = rsmd.getColumnName(i);
						String value = ValidationsUtil.checkIsNull(aResultset.getString(key));
						// logger.debug(key + "\t" + value);
						maplist.put(key, value);
					}
					returnmaplist.add(maplist);
				}
			}

		} catch (Exception ex) {
			logger.error("The exception is " + ex.getMessage());

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
	public static int updateBaseRateStatus(String rateCode, String status, String modifiedDate, String quoteDate) {

		// String status = "";
		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;

		String modifiedDateString = DateTimeUtil.getStringDateInFormat(modifiedDate, "yyyy-MM-dd", "dd-MMM-yy");
		logger.debug("modifiedDateString : " + modifiedDateString);
		String quoteDateString = DateTimeUtil.getStringDateInFormat(quoteDate, "yyyy-MM-dd", "dd-MMM-yy");
		logger.debug("quoteDateString : " + quoteDateString);

		if (status.equalsIgnoreCase("SUCCEEDED"))
			status = "S";
		else if (status.equalsIgnoreCase("FAILED"))
			status = "F";
		else if (status.equalsIgnoreCase("UNAVAILABLE"))
			status = "U";
		else if (status.isEmpty())
			status = "U";

		// logger.debug("RateCode : " + rateCode + " Status : " + status);
		try {
			// aConnection = DBConnection.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbFcConnection();

			if (aConnection != null) {
				String updateQuery = "UPDATE BASE_RATE SET PROCESSED_FLAG = ?, PROCESSED_DATE = ? WHERE RATE_TYPE = ? AND to_char(MODIFIED_DATE) = '"
						+ modifiedDateString.toUpperCase() + "' AND to_char(QUOTE_DATE) = '"
						+ quoteDateString.toUpperCase() + "'";

				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, status);
				aPreParedStatement.setDate(2, DateTimeUtil.getSqlLocalDate());
				aPreParedStatement.setString(3, rateCode);

				// logger.debug("The customer update query is " + updateQuery);
				updatedRowCount = aPreParedStatement.executeUpdate();
				logger.debug("BASERATE updatedRowCount >>> " + updatedRowCount + "\t " + status);
			}

		} catch (Exception ex) {
			logger.error("The exception is " + ex.getMessage());
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

}
