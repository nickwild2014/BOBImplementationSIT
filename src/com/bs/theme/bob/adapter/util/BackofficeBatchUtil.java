package com.bs.theme.bob.adapter.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.apache.log4j.Logger;

import com.bs.themebridge.entity.model.Transactionloghistory;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.misys.tiplus2.services.control.ServiceResponse.ResponseHeader;
import com.misys.tiplus2.services.control.ServiceResponse.ResponseHeader.Details;
import com.misys.tiplus2.services.control.StatusEnum;

public class BackofficeBatchUtil {

	private final static Logger logger = Logger.getLogger(BackofficeBatchUtil.class.getName());

	// /**
	// * NEW LOGGING
	// *
	// * @param aPosting
	// * @param tiRequest
	// * @return
	// */
	// public static boolean eodpostingLogging(Posting aPosting, String
	// tiRequest) {
	//
	// // logger.debug("Entering into the BackofficeUtil.eodposting method");
	//
	// boolean result = true;
	// Connection con = null;
	// PreparedStatement ps = null;
	// try {
	// con = DatabaseUtility.getThemebridgeConnection();
	// if (con != null) {
	// String query = "INSERT INTO EODPOSTING(ID, SERVICE, OPERATION, ZONE,
	// BRANCH, SOURCESYSTEM, "
	// + "TARGETSYSTEM, EVENTREFERENCE, TRANSACTIONID, TRANSACTIONSEQNO,
	// ACCOUNTNUMBER, "
	// + "POSTINGAMOUNT, POSTINGCCY, VALUEDATE, MASTERKEY, POSTINGBRANCH,
	// INPUTBRANCH, "
	// + "PRODUCTREFERENCE, BACKOFFICEACCOUNTNO, ACCOUNTTYPE, SPSKMNEMONIC,
	// SPSKCATEGORYCODE, "
	// + "DEBITCREDITFLAG, TRANSACTIONCODE, RELATEDPARTY, SETTLEMENTACCOUNTUSED,
	// "
	// + "SWIFTMESSAGETYPE, SERVICELEVEL, SWIFTCHARGESFOR, TIREQUEST, STATUS, "
	// + "PROCESSTIME, MASTERREFERENCE)"
	// + " VALUES (EODPOSTINGLOG_SEQ.nextval,
	// ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	//
	// // logger.debug("The query is " + query);
	// ps = con.prepareStatement(query);
	// // TODO
	// // ps.setString(1, ThemeBridgeUtil.getFormatLocalDateTime());
	// ps.setString(1, "BackOffice");
	// ps.setString(2, "Batch");
	// ps.setString(3, "ZONE1");
	// ps.setString(4, "");
	// ps.setString(5, "ZONE1");
	// ps.setString(6, "KOTAK");
	// ps.setString(7, aPosting.getEventReference());
	// ps.setString(8, aPosting.getTransactionId());
	// ps.setInt(9, aPosting.getTransactionSeqNo());
	// ps.setString(10, aPosting.getAccountNumber());
	// ps.setLong(11, aPosting.getPostingAmount());
	// ps.setString(12, aPosting.getPostingCcy());
	// // TODO DATE CONVESTION
	// ps.setDate(13,
	// ThemeBridgeUtil.getSqlDatefromGregorianCalendar(aPosting.getValueDate()));
	// ps.setString(14, aPosting.getMasterKey().toString());
	// ps.setString(15, aPosting.getPostingBranch());
	// ps.setString(16, aPosting.getInputBranch());
	// ps.setString(17, aPosting.getProductReference());
	// ps.setString(18, aPosting.getBackOfficeAccountNo());
	// ps.setString(19, aPosting.getAccountType());
	// ps.setString(20, aPosting.getSPSKMnemonic());
	// ps.setString(21, aPosting.getSPSKCategoryCode());
	// ps.setString(22, aPosting.getDebitCreditFlag());
	// ps.setString(23, aPosting.getTransactionCode());
	// ps.setString(24, aPosting.getRelatedParty());
	// ps.setString(25, aPosting.getSettlementAccountUsed());
	// ps.setString(26, aPosting.getSWIFTmessageType());
	// ps.setString(27, aPosting.getServiceLevel());
	// ps.setString(28, aPosting.getSWIFTChargesFor());
	// ps.setString(29, tiRequest);
	// ps.setString(30, "RECEIVED");
	// ps.setTimestamp(31, ThemeBridgeUtil.GetCurrentTimeStamp());
	// ps.setString(32, aPosting.getMasterReference());
	//
	// int insertedRows = ps.executeUpdate();
	// if (insertedRows > 0) {
	// logger.debug(insertedRows + " Row inserted successfully!");
	// } else {
	// logger.debug("EOD row inserted Failed");
	// }
	// }
	// } catch (Exception ex) {
	// logger.error("The Exception is :" + ex.getMessage());
	// ex.printStackTrace();
	// result = false;
	//
	// } finally {
	// DatabaseUtility.surrenderPrepdConnection(con, ps, null);
	// }
	//
	// return result;
	// }

	/**
	 * NEW LOGGING
	 * 
	 * @param aPosting
	 * @param tiRequest
	 * @return
	 */
	public static boolean eodpostingLogging(String tiRequest, String master, String event) {

		logger.debug("Entering into the BackofficeUtil.eodposting method");
		boolean result = true;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {

				// String query = "INSERT INTO EODPOSTING(ID, SERVICE,
				// OPERATION, ZONE, BRANCH, SOURCESYSTEM, "
				// + "TARGETSYSTEM, MASTERREFERENCE, EVENTREFERENCE, TIREQUEST,
				// STATUS, PROCESSTIME )"
				// + " VALUES (EODPOSTINGLOG_SEQ.nextval,
				// ?,?,?,?,?,?,?,?,?,?,?)";

				String query = "INSERT INTO EODPOSTING(ID, SERVICE, OPERATION, ZONE, BRANCH, SOURCESYSTEM, "
						+ "TARGETSYSTEM, MASTERREFERENCE, EVENTREFERENCE, TIREQUEST, STATUS, PROCESSTIME )"
						+ " VALUES (EODPOSTINGLOG_SEQ.nextval, ?,?,?,?,?,?,?,?,?,?,?)";

				ps = con.prepareStatement(query);
				ps.setString(1, "BackOffice");
				ps.setString(2, "Batch");
				ps.setString(3, "ZONE1");
				ps.setString(4, "");
				ps.setString(5, "ZONE1");
				ps.setString(6, "BOB");
				ps.setString(7, master);
				ps.setString(8, event);
				// TODO DATE CONVESTION
				ps.setString(9, tiRequest);
				ps.setString(10, "RECEIVED");
				// ps.setTimestamp(11, ThemeBridgeUtil.GetCurrentTimeStamp());
				ps.setTimestamp(11, DateTimeUtil.GetLocalTimeStamp());

				int insertedRows = ps.executeUpdate();
				if (insertedRows > 0) {
					logger.debug(insertedRows + " Row inserted successfully!!! ");
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
	 * Get currency code
	 * 
	 * @param ccy
	 * @return
	 */
	public static String getCcyCode(String ccy) {

		String ccyCode = "";
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		ResultSet aResultset = null;
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"SELECT CURRENCY_MNEMONIC_CODE FROM LOOKUP_CURRENCY_CODE WHERE CURRENCY_CODE = ?");
			aPreparedStatement.setString(1, ccy);
			aResultset = aPreparedStatement.executeQuery();

			if (aResultset.next()) {
				ccyCode = aResultset.getString(1);
			}

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		logger.debug("Ccy mnemonic code : " + ccyCode);
		return ccyCode;
	}

	/**
	 * Get currency code
	 * 
	 * @param ccy
	 * @return
	 */
	// public static String getCcyCode(String ccy) {
	//
	// String ccyCode = "";
	// Connection aConnection = null;
	// PreparedStatement aPreparedStatement = null;
	// ResultSet aResultset = null;
	// try {
	// aConnection = DBConnection.getTizoneConnection();
	// aPreparedStatement = aConnection.prepareStatement("SELECT C8CCYN FROM
	// C8PF WHERE C8CCY = ?");
	// aPreparedStatement.setString(1, ccy);
	// aResultset = aPreparedStatement.executeQuery();
	//
	// if (aResultset.next()) {
	// ccyCode = aResultset.getString(1);
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	//
	// } finally {
	// DBConnection.surrenderPrepdConnection(aConnection, aPreparedStatement,
	// aResultset);
	//
	// }
	// return ccyCode;
	// }

	/**
	 * 
	 * @param category
	 * @return
	 */
	public static String getTransChargeParticulars(String category) {

		String chargeType = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection
					.prepareStatement("SELECT trim(DHDIA) as DHDIA FROM DHPF WHERE trim(CATEGORY) = ?");
			// SELECT C5ATD FROM C5PF WHERE C5ATP = 'CA';
			aPreparedStatement.setString(1, category);
			aResultset = aPreparedStatement.executeQuery();

			// logger.debug(" SELECT trim(DHDIA) as DHDIA FROM DHPF WHERE
			// trim(CATEGORY) = '" + category + "'");

			if (aResultset.next()) {
				chargeType = aResultset.getString(1);
			}

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return chargeType;
	}

	/**
	 * 
	 * @param masterReference
	 * @param eventReference
	 * @return
	 */
	public static String getBillReferenceNum(String masterReference, String eventReference) {

		String billRefNo = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"SELECT BLLREFNO, TRIM(MAS.MASTER_REF) MASTER_REF, (BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 000)) AS EVENT_REF FROM MASTER MAS, BASEEVENT BEV, EXTEVENT EXT WHERE MAS.KEY97 = BEV.MASTER_KEY AND BEV.KEY97 = EXT.EVENT AND TRIM(MAS.MASTER_REF)= ? AND (BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 000)) = ? ");
			aPreparedStatement.setString(1, masterReference);
			aPreparedStatement.setString(2, eventReference);
			aResultset = aPreparedStatement.executeQuery();
			if (aResultset.next()) {
				billRefNo = aResultset.getString("BLLREFNO");
			}

		} catch (Exception e) {
			logger.debug("Bill reference Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return billRefNo;
	}

	/**
	 * updateUTRReferenceNum
	 * 
	 * @param masterReference
	 * @param eventReference
	 * @return
	 */
	public static String updateUTRReferenceNum(String masterReference, String eventReference) {

		String billRefNo = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"SELECT BLLREFNO, TRIM(MAS.MASTER_REF) MASTER_REF, (BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 000)) AS EVENT_REF FROM MASTER MAS, BASEEVENT BEV, EXTEVENT EXT WHERE MAS.KEY97 = BEV.MASTER_KEY AND BEV.KEY97 = EXT.EVENT AND TRIM(MAS.MASTER_REF)= ? AND (BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL, 3, 000)) = ? ");
			aPreparedStatement.setString(1, masterReference);
			aPreparedStatement.setString(2, eventReference);
			aResultset = aPreparedStatement.executeQuery();
			if (aResultset.next()) {
				billRefNo = aResultset.getString("BLLREFNO");
			}

		} catch (Exception e) {
			logger.debug("Update reference Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return billRefNo;
	}

	/**
	 * 
	 * @param accountType
	 * @param creditDebit
	 * @param productType
	 * @return
	 */
	public static String getGLAccount(String accountType, String creditDebit, String tiproductType) {

		logger.debug("accountType : " + accountType);
		// logger.debug("creditDebit : " + creditDebit);
		// logger.debug("productType : " + tiproductType);

		String glAccountNum = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(
					"SELECT TRIM(GLACCOUNT) AS GLACCOUNT FROM LOOKUP_GL_ACCOUNT WHERE TRIM(ACCOUNTTYPE) = ?");
			// .prepareStatement("SELECT GLACCOUNT FROM
			// LOOKUP_GL_ACCOUNT WHERE ACCOUNTTYPE = ? ");
			// AND CREDIT_DEBIT = ? AND TIPRODTYPE = ?");

			aPreparedStatement.setString(1, accountType);
			// aPreparedStatement.setString(2, creditDebit);
			// aPreparedStatement.setString(3, tiproductType);
			aResultset = aPreparedStatement.executeQuery();

			if (aResultset.next()) {
				glAccountNum = aResultset.getString("GLACCOUNT");
			}

		} catch (Exception e) {
			logger.error("Acc Type Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		logger.debug("chargeType : " + glAccountNum);
		return glAccountNum;
	}

	/**
	 * 
	 * @param category
	 * @return
	 */
	public static Transactionloghistory getTransactioLog(String id) {

		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		Transactionloghistory transLog = new Transactionloghistory();
		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement("SELECT * FROM TRANSACTIONLOG WHERE ID = ?");
			aPreparedStatement.setString(1, id);
			aResultset = aPreparedStatement.executeQuery();

			if (aResultset.next()) {
				transLog.setTransactionlogid(aResultset.getBigDecimal("ID").toBigInteger());
				transLog.setZone(aResultset.getString("ZONE"));
				transLog.setService(aResultset.getString("SERVICE"));
				transLog.setOperation(aResultset.getString("OPERATION"));
				transLog.setBranch(aResultset.getString("BRANCH"));
				transLog.setSourcesystem(aResultset.getString("SOURCESYSTEM"));
				transLog.setTargetsystem(aResultset.getString("TARGETSYSTEM"));

				transLog.setMasterreference(aResultset.getString("MASTERREFERENCE"));
				transLog.setEventreference(aResultset.getString("EVENTREFERENCE"));
				transLog.setProcesstime(aResultset.getTimestamp("PROCESSTIME"));
				transLog.setStatus(aResultset.getString("STATUS"));

				transLog.setTirequest(aResultset.getString("TIREQUEST"));
				transLog.setTiresponse(aResultset.getString("TIRESPONSE"));
				transLog.setBankrequest(aResultset.getString("BANKREQUEST"));
				transLog.setBankresponse(aResultset.getString("BANKRESPONSE"));

				transLog.setTireqtime(aResultset.getTimestamp("TIREQTIME"));
				transLog.setTirestime(aResultset.getTimestamp("TIRESTIME"));
				transLog.setBankreqtime(aResultset.getTimestamp("BANKREQTIME"));
				transLog.setBankrestime(aResultset.getTimestamp("BANKRESTIME"));

				// transLog.setRequestid(aResultset.getString("REQUESTID"));
				transLog.setServicekey1(aResultset.getString("SERVICEKEY1"));
				// transLog.setServicekey2(aResultset.getString("SERVICEKEY2"));
				transLog.setStatickey1(aResultset.getString("STATICKEY1"));
				// transLog.setStatickey2(aResultset.getString("STATICKEY2"));
				transLog.setNarrative1(aResultset.getString("NARRATIVE1"));
				transLog.setNarrative2(aResultset.getString("NARRATIVE1"));
				// transLog.setNarrative3(aResultset.getString("NARRATIVE3"));
				// transLog.setNarrative4(aResultset.getString("NARRATIVE4"));

				// Get as String & Set as Character
				char isResubmitted = aResultset.getString("ISRESUBMITTED").charAt(0);
				transLog.setIsresubmitted(isResubmitted);

				transLog.setResubmittedcount(aResultset.getShort("RESUBMITTEDCOUNT"));
				transLog.setResubmittedtime(aResultset.getTimestamp("RESUBMITTEDTIME"));
				// transLog.setIsresolved(aResultset.getShort("ISRESOLVED"));
				transLog.setDescription(aResultset.getString("DESCRIPTION"));

				// Get as String & Set as Character
				// char typeFlag = aResultset.getString("TYPEFLAG").charAt(0);
				transLog.setTypeflag(null);
			}

		} catch (Exception e) {
			logger.error("Transaction history log : " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}
		return transLog;
	}

	/**
	 * DELETE TRANSACTIONLOG RECORD
	 * 
	 * @param id
	 * @return
	 */
	public static int deleteTransactionLog(String id) {

		int deletedCount = 0;
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement("DELETE FROM TRANSACTIONLOG WHERE ID = ?");

			aPreparedStatement.setString(1, id);
			deletedCount = aPreparedStatement.executeUpdate();
			logger.debug("DeletedCount : " + deletedCount);

		} catch (Exception e) {
			logger.debug("Delete TransactionLog Exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);

		}

		return deletedCount;
	}

	/**
	 * 
	 * @param branch
	 * @param postingCcy
	 * @param accountType
	 * @param creditDebit
	 * @return
	 */
	// public static String getAccountMapping(String branch, String postingCcy,
	// String accountType, String creditDebit ) {
	//
	//
	// if(accountType.startsWith("T") || accountType.startsWith("R")){
	// branch + getCcyCode(postingCcy) +
	// }
	//
	// String chargeType = "";
	// Connection aConnection = null;
	// PreparedStatement aPreparedStatement = null;
	// ResultSet aResultset = null;
	// try {
	// aConnection = DBConnection.getTizoneConnection();
	// aPreparedStatement = aConnection.prepareStatement("SELECT DHDIA FROM DHPF
	// WHERE CATEGORY = ?");
	// aPreparedStatement.setString(1, category);
	// aResultset = aPreparedStatement.executeQuery();
	//
	// if (aResultset.next()) {
	// chargeType = aResultset.getString(1);
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	//
	// } finally {
	// DBConnection.surrenderPrepdConnection(aConnection, aPreparedStatement,
	// aResultset);
	//
	// }
	// return chargeType;
	// }

	/**
	 * 
	 * @param length
	 * @return
	 */
	public static String generateRandom(int length) {
		// int length =12;
		Random random = new Random();
		char[] digits = new char[length];
		digits[0] = (char) (random.nextInt(9) + '1');
		for (int i = 1; i < length; i++) {
			digits[i] = (char) (random.nextInt(10) + '0');
		}
		return new String(digits);
	}

	/**
	 * 
	 * @param pstAma
	 * @param pstCCY
	 * @return
	 */
	public static String amountConversion(String pstAma, String pstCCY) {
		String amount = "";
		BigDecimal bg = null;
		if (ValidationsUtil.isValidString(pstAma) && ValidationsUtil.isValidString(pstCCY)
				&& (pstCCY.equals("OMR") || pstCCY.equals("BHD") || pstCCY.equals("KWD"))) {
			bg = new BigDecimal(pstAma);
			bg = bg.divide(new BigDecimal(1000));
		} else if (ValidationsUtil.isValidString(pstAma) && ValidationsUtil.isValidString(pstCCY)
				&& pstCCY.equals("JPY")) {
			bg = new BigDecimal(pstAma);
		} else if (ValidationsUtil.isValidString(pstAma) && ValidationsUtil.isValidString(pstCCY)) {
			bg = new BigDecimal(pstAma);
			bg = bg.divide(new BigDecimal(100));
		}
		if (bg != null)
			amount = bg.toString();

		return amount;
	}

	/**
	 * 
	 * @return
	 */
	private String getTICurrentDate() {

		String tiCurrDate = "";
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		// 02-OCT-16
		String query = "SELECT to_char(PROCDATE,'dd-MON-yy') as PROCDATE FROM DLYPRCCYCL ";
		logger.debug("TI CURRENT DATE QUERY : " + query);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				tiCurrDate = rs.getString(1);
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
	 * @param correlationId
	 *            {@code allows }{@link String}
	 * @param Operation
	 *            {@code allows }{@link String}
	 * @param service
	 *            {@code allows }{@link String}
	 * @param failureMsg
	 *            {@code allows }{@link String}
	 * @param status
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static ResponseHeader getHeader(String correlationId, String service, String operation, String status,
			String errorMsg, String warningMsg) {

		ResponseHeader header = new ResponseHeader();
		header.setCorrelationId(correlationId);
		header.setOperation(operation);
		header.setService(service);
		if (status.equals("SUCCEEDED")) {
			header.setStatus(StatusEnum.SUCCEEDED);
		} else {
			header.setStatus(StatusEnum.FAILED);
			Details aDetail = new Details();
			if (!errorMsg.isEmpty()) {
				aDetail.getError().add(errorMsg);
			}
			if (!warningMsg.isEmpty()) {
				aDetail.getWarning().add(warningMsg);
			}
			header.setDetails(aDetail);
		}
		return header;
	}

}
