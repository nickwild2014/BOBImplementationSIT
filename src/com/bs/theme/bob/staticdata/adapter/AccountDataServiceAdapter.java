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
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.logging.StaticLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.StatusEnum;

public class AccountDataServiceAdapter {

	private final static Logger logger = Logger.getLogger(AccountDataServiceAdapter.class.getName());

	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String inputMessage = "";
	// private static String type = "StagingTable";

	public static void main(String ar[]) throws Exception {

		AccountDataServiceAdapter aAccountDataServiceAdapter = new AccountDataServiceAdapter();
		aAccountDataServiceAdapter.pushAccountDataService();
		// logger.debug(insertAccountTypeInC5PF("ggg","ggggggg"));
		// logger.debug(isContainAccountTypeInC5PF("ggg"));
		// updateAccountDataStatus("6500073", "01722560000002", "S");

		// String response =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\Garbage\\AccountDataResponse.xml");

		// String errorMessage = XPathParsing.getValue(response,
		// "ServiceResponse/ResponseHeader/Details/Error");
		// logger.debug(errorMessage);
	}

	public String pushAccountDataService() {

		// logger.debug(" ************ Account.Data adapter new process started
		// ************ ");
		String result = "";
		String errorMessage = "";
		Timestamp reqReceivedTime = null;
		boolean accountDataPushStatus = true;
		InputStream anInputStream = null;
		try {
			List<Map<String, String>> mapList = getAccountDataList();
			// logger.debug(" ====== \n" + mapList);
			logger.debug("AccountDataList map size : " + mapList.size());

			anInputStream = AccountDataServiceAdapter.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_ACCOUNT_DATA_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			if (mapList.size() > 0) {

				for (Map<String, String> map : mapList) {

					String status = "";
					String currency = "";
					String customerCif = "";
					String accountNumber = "";
					String tagRemovedXML = "";
					try {
						accountNumber = map.get("BACKOFFICEACCOUNT");
						currency = map.get("CURRENCY");
						customerCif = map.get("CUSTOMER");

						tagRemovedXML = getTirequestMsg(map, requestTemplate);
						reqReceivedTime = DateTimeUtil.getTimestamp();
						tiRequest = tagRemovedXML;
						logger.debug("\n\nAccont Data TI RequestXML :- " + tiRequest);

						/********************************************************/
						String tiResponseXML = TIPlusEJBClient.process(tagRemovedXML);
						tiResponse = tiResponseXML;
						logger.debug("Accont Data TI ResponseXML :- " + tiResponse);

						// StatusEnum statusEnum =
						// ResponseHeaderUtil.processEJBClientResponse(tiResponse);
						// status = statusEnum.toString();
						status = XPathParsing.getValue(tiResponseXML, "ServiceResponse/ResponseHeader/Status");
						errorMessage = XPathParsing.getValue(tiResponse,
								"ServiceResponse/ResponseHeader/Details/Error");

						int updatedCount = updateAccountDataStatus(customerCif, accountNumber, status);

					} catch (Exception e) {
						accountDataPushStatus = false;
						errorMessage = errorMessage + e.getMessage();
						logger.error("AccontData Exceptions! " + e.getMessage());
						e.printStackTrace();

					} finally {
						StaticLogging.pushLogData("TI", "AccountData", "ZONE1", "ALL", "IDBEXT", "TIPlus", status,
								reqReceivedTime, inputMessage, tiRequest, tiResponse, accountNumber, currency,
								customerCif, false, "0", errorMessage);
					}
				}

			}

		} catch (Exception e) {
			logger.error("AccountData Exception! " + e.getMessage(), e);
			e.printStackTrace();
			return result = StatusEnum.SUCCEEDED.toString();

		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		// logger.debug(" ************ Account.Data adapter new process ended
		// ************ ");
		return result = StatusEnum.SUCCEEDED.toString();
	}

	/**
	 * 
	 * @param map
	 * @param requestTemplate
	 * @return
	 */
	public static String getTirequestMsg(Map<String, String> map, String requestTemplate) {

		String result = "";
		String remarks = "";
		String currency = "";
		String customerCif = "";
		String accountNumber = "";
		String tokenReplacedXML = "";

		try {
			Map<String, String> tokens = new HashMap<String, String>();
			accountNumber = map.get("BACKOFFICEACCOUNT");
			currency = map.get("CURRENCY");
			customerCif = map.get("CUSTOMER");
			tokens.put("Name", StaticDataConstant.CredentialName);
			tokens.put("Password", "");
			tokens.put("Certificate", "");
			tokens.put("Digest", "");
			tokens.put("CorrelationId", UUID.randomUUID().toString());
			tokens.put("MaintType", "F");
			tokens.put("MaintainedInBackOffice", "N");
			tokens.put("BackOfficeAccount", accountNumber);
			tokens.put("Branch", map.get("BRANCH"));
			tokens.put("SourceBankingBusiness", StaticDataConstant.SourceBankingBusiness);
			tokens.put("Mnemonic", customerCif);
			// TODO
			tokens.put("CategoryCode", "");
			// TODO ACCOUNT TYPE, 2017-09-09
			// String accType = geTitAccountType(map.get("ACCOUNTTYPE"));
			String accType = map.get("ACCOUNTTYPE");
			String schm_code = map.get("SCHM_CODE").trim();
			accType = accType.trim() + schm_code;
			String schm_desc = map.get("SCHM_DESC").trim();

			/**
			 * 
			 */
			int count = isContainAccountTypeInC5PF(accType);

			if (count == 0) {
				insertAccountTypeInC5PF(accType, schm_desc);
			}

			tokens.put("AccountType", accType);
			logger.debug("Mapped Acc Type : " + map.get("ACCOUNTTYPE") + " == " + accType);

			tokens.put("Currency", currency);
			// TODO
			tokens.put("OtherCurrency", "");
			tokens.put("ExternalAccount", accountNumber);
			tokens.put("IBAN", map.get("IBAN"));
			String shortName = map.get("SHORTNAME");// 15 Max
			if (shortName.length() > 15)
				tokens.put("ShortName", shortName.substring(0, 15));
			else
				tokens.put("ShortName", map.get("SHORTNAME"));
			tokens.put("Country", map.get("COUNTRY"));
			tokens.put("ContingentAccount", map.get("CONTINGENTACCOUNT"));

			// TODO
			// if (accType.equals("CA") || accType.equals("OD") ||
			// accType.equals("SB") || accType.equals("LA"))
			// tokens.put("InternalAccount", "N");
			// else
			// tokens.put("InternalAccount", "Y");
			tokens.put("InternalAccount", "N");

			tokens.put("DateOpened", map.get("DATEOPENED").substring(0, 10));

			String dateMaintained = "";
			// if (ValidationsUtil.isValidString(map.get("DATECLOSED"))) {
			// dateMaintained = map.get("DATECLOSED").substring(0, 10);
			// }
			tokens.put("DateMaintained", dateMaintained);

			// TODO
			String dateClosed = "";
			// if (ValidationsUtil.isValidString(map.get("DATECLOSED"))) {
			// dateClosed = map.get("DATECLOSED").substring(0, 10);
			// logger.debug("dateClosed loop" + dateClosed);
			// }
			// logger.debug("dateClosed " + dateClosed);
			tokens.put("DateClosed", dateClosed);

			if (map.get("DESCRIPTION1").length() > 35) {
				tokens.put("Description1", map.get("DESCRIPTION1").substring(0, 35));
			} else {
				tokens.put("Description1", map.get("DESCRIPTION1"));
			}

			if (map.get("DESCRIPTION2").length() > 35) {
				tokens.put("Description2", map.get("DESCRIPTION2").substring(0, 35));
			} else {
				tokens.put("Description2", map.get("DESCRIPTION2"));
			}

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			// logger.debug("tokenReplacedXML : " + tokenReplacedXML);
			reader.close();

			tokenReplacedXML = tokenReplacedXML.replace("&", "&amp;");
			// logger.debug("& >>-->> " + tokenReplacedXML);

			String tagRemovedXML = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			result = tagRemovedXML;
			// logger.debug("RequestXML to TIPlus :\n" + tagRemovedXML);

		} catch (IOException e2) {
			logger.error("AccountData Exceptions " + e2.getMessage(), e2);
			e2.printStackTrace();
			remarks = "IO Exception";

		} catch (Exception e) {
			logger.error("AccountData Exceptions " + e.getMessage(), e);
			e.printStackTrace();
			remarks = "EJB Exception";

		}
		// logger.debug("Test : " + result);
		return result;
	}

	/**
	 * 
	 * @param finacleAccountType
	 * @return
	 */
	public static String geTitAccountType(String finacleAccountType) {

		// logger.debug("finacleAccountType : " + finacleAccountType);
		String tiAccType = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection
					.prepareStatement("SELECT TIACCOUNTYPE FROM LOOKUP_ACCOUNT_TYPE WHERE FIACCOUNTTYPE = ? ");
			aPreparedStatement.setString(1, finacleAccountType);
			aResultset = aPreparedStatement.executeQuery();
			if (aResultset.next()) {
				tiAccType = aResultset.getString("TIACCOUNTYPE");
			} else
				tiAccType = finacleAccountType;

		} catch (Exception e) {
			logger.error("AccountData Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return tiAccType;
	}

	/**
	 * 
	 * @return
	 */
	public static List<Map<String, String>> getAccountDataList() {

		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;
		List<Map<String, String>> returnMaplist = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbTiplusConnection();
			if (aConnection != null) {
				aStatement = aConnection.createStatement();
				// String query = "select BACKOFFICEACCOUNT, BRANCH, CUSTOMER,
				// SOURCEBANKINGBUSINESS, ACCOUNTTYPE, CURRENCY, IBAN,
				// SHORTNAME, COUNTRY, CONTINGENTACCOUNT, DATEOPENED,
				// DATEMAINTAINED, DATECLOSED, DESCRIPTION1, DESCRIPTION2,
				// REC_TIME, schm_code, schm_desc from c_misys_acct_interface
				// where schm_code is not null";

				String accountFlag = "N";
				String flag = ConfigurationUtil.getValueFromKey("AccountDataFlag");
				// logger.debug("Config StaticQuery : " + flag);
				if (!accountFlag.isEmpty() || accountFlag != null) {
					flag = flag.trim();
					accountFlag = flag;
				}
				// logger.debug("AccountStaticQuery StaticQuery : " +
				// accountFlag);

				String query = "SELECT BACKOFFICEACCOUNT, BRANCH, CUSTOMER, SOURCEBANKINGBUSINESS, ACCOUNTTYPE, CURRENCY, IBAN, SHORTNAME, COUNTRY, CONTINGENTACCOUNT, DATEOPENED, DATEMAINTAINED, DATECLOSED, DESCRIPTION1, DESCRIPTION2, REC_TIME, schm_code, schm_desc  FROM C_MISYS_ACCT_INTERFACE "
						+ " WHERE FLAG_PROCESSED IS null OR FLAG_PROCESSED IN ('" + accountFlag + "','U') ";
				// + " and schm_code is not null " ;// FLAG_PROCESSED <> 'S'
				// logger.debug("AccountStaticQuery : \n" + query);

				aResultset = aStatement.executeQuery(query);
				ResultSetMetaData rsmd = aResultset.getMetaData();
				int columnCount = rsmd.getColumnCount();
				returnMaplist = new ArrayList();

				while (aResultset.next()) {
					Map maplist = new HashMap();
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
			logger.error("Accountdata exceptions! " + ex.getMessage());

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}
		// logger.debug("returnMaplist : " + returnMaplist);
		return returnMaplist;
	}

	public static int isContainAccountTypeInC5PF(String schemeType) {
		int count = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		ResultSet rs = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {
				String selectQuery = "select trim(C5ATP) C5ATP from c5pf where c5atp = '" + schemeType + "'";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug("ACCOUNT update query is :\n" + updateQuery);
				aPreParedStatement = aConnection.prepareStatement(selectQuery);
				rs = aPreParedStatement.executeQuery();
				while (rs.next()) {
					count = count + 1;
				}

			}
		} catch (Exception ex) {
			logger.error("AccountData exception! " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, rs);
		}
		return count;
	}

	public static int insertAccountTypeInC5PF(String schemeType, String schemeDesc) {
		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {
				String updateQuery = "Insert into c5pf (C5ATP,C5ATD,C5IDBC,C5IDBD,C5SC46,C5SC47,C5VTE,MNT_IN_BO) values (?,?,?,?,?,?,?,?)";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug("ACCOUNT update query is :\n" + updateQuery);
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, schemeType);
				aPreParedStatement.setString(2, schemeDesc);
				aPreParedStatement.setString(3, " ");
				aPreParedStatement.setString(4, " ");
				aPreParedStatement.setString(5, "N");
				aPreParedStatement.setString(6, "N");
				aPreParedStatement.setString(7, "Y");
				aPreParedStatement.setString(8, "N");

				updatedRowCount = aPreParedStatement.executeUpdate();
				// logger.debug("AccountData UpdatedRowCount >>> " +
				// updatedRowCount);
			}
		} catch (Exception ex) {
			logger.error("AccountData exception! " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

	/**
	 * 
	 * @param customerMnemonic
	 * @param extnlAccNum
	 * @param status
	 * @return
	 */
	public static int updateAccountDataStatus(String customerMnemonic, String extnlAccNum, String status) {

		// logger.debug("customerMnemonic : " + customerMnemonic);
		// logger.debug("extnlAccNum : " + extnlAccNum);
		// logger.debug("status : " + status);

		if (status.equals("SUCCEEDED"))
			status = "S";
		else if (status.equals("FAILED"))
			status = "F";
		else if (status.equals("UNAVAILABLE"))
			status = "U";
		else if (status.isEmpty())
			status = "U";

		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbTiplusConnection();
			if (aConnection != null) {
				String updateQuery = "UPDATE C_MISYS_ACCT_INTERFACE SET REC_TIME = ? , FLAG_PROCESSED = ? WHERE BACKOFFICEACCOUNT = ? AND CUSTOMER = ? ";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug("ACCOUNT update query is :\n" + updateQuery);
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setTimestamp(1, DateTimeUtil.getSqlLocalTimestamp());
				aPreParedStatement.setString(2, status);
				aPreParedStatement.setString(3, extnlAccNum);
				aPreParedStatement.setString(4, customerMnemonic);

				updatedRowCount = aPreParedStatement.executeUpdate();
				// logger.debug("AccountData UpdatedRowCount >>> " +
				// updatedRowCount);
			}
		} catch (Exception ex) {
			logger.error("AccountData exception! " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

}
