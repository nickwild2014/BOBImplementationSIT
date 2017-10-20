package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.bs.themebridge.entity.adapter.FtrtdetailsUtilizeAdapter;
import com.bs.themebridge.entity.model.FtrtDetailsUtilizeBean;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;

public class RETSyncAdapterUtil {

	private final static Logger logger = Logger.getLogger(RETSyncAdapterUtil.class.getName());

	public static void main(String args[]) {

		// List<Map<String, String>> getFTRTRetList = bu.getFTRTRetList("N");
		// logger.debug(getFTRTRetList.size());

		// List<FtrtDetails> getFTRTRetList = bu.getFTRTRetList("N");
		// logger.debug(getFTRTRetList.size());

		// addListInThemebridge(getFTRTRetList);
		// processFtrtDeals();

		getRETDealsIdList();

		// processFtrtDeals();
	}

	/**
	 * 
	 * @return
	 */
	public static boolean processFtrtDeals() {
		// System.out.println("T");
		List<FtrtDetailsUtilizeBean> getFTRTRetList = getRETDealsIdList();
		// logger.debug(getFTRTRetList.size());

		// Insert into themebridge
		if (getFTRTRetList.size() > 0) {

			for (FtrtDetailsUtilizeBean ftrtdetails : getFTRTRetList) {
				boolean status = addListInThemebridge(ftrtdetails);
				// logger.debug("FTRT insert Themebridge status : " + status);
				if (status)
					updateListInRET(ftrtdetails.getApplicationId(), ftrtdetails.getApRefNo(),
							ftrtdetails.getTrRefNum());
			}
		} else {
			// logger.debug("FTRT new deal list size 0 ");
		}

		return true;
	}

	/**
	 * 
	 * @param ftrtdetails
	 * @return
	 */
	public static boolean addListInThemebridge(FtrtDetailsUtilizeBean ftrtdetails) {

		FtrtdetailsUtilizeAdapter ftrtAdapterObj = new FtrtdetailsUtilizeAdapter();
		boolean insertStatus = ftrtAdapterObj.addProperty(ftrtdetails);

		return insertStatus;
	}

	/**
	 * 
	 * @param processFlag
	 * @return
	 */
	public static List<FtrtDetailsUtilizeBean> getRETDealsIdList() {

		ResultSet rs = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		List<FtrtDetailsUtilizeBean> ftrtDetailsList = new ArrayList<FtrtDetailsUtilizeBean>();
		// + " PROCESS_FLG = 'N' AND TO_CHAR(CREATED_DATE) = (SELECT
		// TO_CHAR(SYSDATE, 'dd-MON-YY') FROM DUAL) ";
		String query = "SELECT APPLICATION_ID, AP_REF_NO, APPL_REQ_DATE, REQUEST_DATE, FROM_CRNCY_CODE, TO_CRNCY_CODE, REF_AMT, FUNDS_DELIVERY_DATE, BUY_OR_SELL, CIF_ID, EVENT_ID, RATECODE, TREASURY_RATE, CUST_RATE, SWAP_RATE, SWAP_CHARGE_RATE, TR_REF_NUM, RELATED_TR_REF_NUM, STATUS, REMARKS, FREE_CODE_1, FREE_CODE_2, FREE_CODE_3, PROCESS_FLG, ERROR_REASON, FWC_TYPE, ACCOUNT_ID, LOB_CODE, SOL_ID, FW_CONTRACT_NO, FUNCTION_CODE, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE, FUNDS_START_DATE FROM FTRT_DETAILS "
				+ " WHERE PROCESS_FLG = 'N' ";
		// logger.debug("Get RET Query : " + query);
		java.sql.Date tiSysDate = DateTimeUtil.getTISystemSqlDate();
		System.out.println("TISysDate : " + tiSysDate);

		try {
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {
				// System.out.println("T2");
				aPreparedStatement = aConnection.prepareStatement(query);
				// aPreparedStatement.setString(1, processFlag);
				rs = aPreparedStatement.executeQuery();

				while (rs.next()) {
					FtrtDetailsUtilizeBean ftrtModel = new FtrtDetailsUtilizeBean();

					ftrtModel.setApplicationId(rs.getString("APPLICATION_ID"));
					ftrtModel.setApRefNo(rs.getString("AP_REF_NO"));
					ftrtModel.setApplReqDate(rs.getDate("APPL_REQ_DATE"));
					ftrtModel.setRequestDate(rs.getDate("REQUEST_DATE"));
					ftrtModel.setFromCrncyCode(rs.getString("FROM_CRNCY_CODE"));
					ftrtModel.setToCrncyCode(rs.getString("TO_CRNCY_CODE"));
					ftrtModel.setRefAmt(rs.getBigDecimal("REF_AMT"));
					// TODO
					String retDate = ConfigurationUtil.getValueFromKey("FXToDateFlag");
					logger.debug("RET Fund Delivery Date : " + retDate);
					if (retDate.equals("Y")) {
						ftrtModel.setFundsDeliveryDate(rs.getDate("FUNDS_DELIVERY_DATE"));//
					} else {
						ftrtModel.setFundsDeliveryDate(tiSysDate);//
					}
					ftrtModel.setBuyOrSell(rs.getString("BUY_OR_SELL"));
					ftrtModel.setCifId(rs.getString("CIF_ID"));
					ftrtModel.setEventId(rs.getString("EVENT_ID"));
					ftrtModel.setRatecode(rs.getString("RATECODE"));

					if (rs.getBigDecimal("TREASURY_RATE") != null && !rs.getBigDecimal("TREASURY_RATE").equals(""))
						ftrtModel.setTreasuryRate(rs.getBigDecimal("TREASURY_RATE")); // .toBigInteger()
					// logger.debug("ftrtModel.setTreasuryRate " +
					// rs.getBigDecimal("TREASURY_RATE"));
					// logger.debug("ftrtModel.setTreasuryRate " +
					// ftrtModel.getTreasuryRate());

					if (rs.getBigDecimal("CUST_RATE") != null && !rs.getBigDecimal("CUST_RATE").equals(""))
						ftrtModel.setCustRate(rs.getBigDecimal("CUST_RATE"));// .toBigInteger()
					// logger.debug("ftrtModel.getCustRate " +
					// ftrtModel.getCustRate());

					if (rs.getBigDecimal("SWAP_RATE") != null && !rs.getBigDecimal("SWAP_RATE").equals(""))
						ftrtModel.setSwapRate(rs.getBigDecimal("SWAP_RATE"));// .toBigInteger()
					// logger.debug("ftrtModel.setSwapRate " +
					// ftrtModel.getSwapRate());

					if (rs.getBigDecimal("SWAP_CHARGE_RATE") != null
							&& !rs.getBigDecimal("SWAP_CHARGE_RATE").equals(""))
						ftrtModel.setSwapChargeRate(rs.getBigDecimal("SWAP_CHARGE_RATE"));// .toBigInteger()
					// logger.debug("ftrtModel.getSwapChargeRate " +
					// ftrtModel.getSwapChargeRate());

					ftrtModel.setTrRefNum(rs.getString("TR_REF_NUM"));
					ftrtModel.setRelatedTrRefNum(rs.getString("RELATED_TR_REF_NUM"));
					ftrtModel.setStatus(rs.getString("STATUS"));
					ftrtModel.setRemarks(rs.getString("REMARKS"));
					ftrtModel.setFreeCode1(rs.getString("FREE_CODE_1"));
					ftrtModel.setFreeCode2(rs.getString("FREE_CODE_2"));
					ftrtModel.setFreeCode3(rs.getString("FREE_CODE_3"));
					ftrtModel.setProcessFlg(rs.getString("PROCESS_FLG"));
					ftrtModel.setErrorReason(rs.getString("ERROR_REASON"));
					ftrtModel.setFwcType(rs.getString("FWC_TYPE"));
					ftrtModel.setAccountId(rs.getString("ACCOUNT_ID"));
					ftrtModel.setLobCode(rs.getString("LOB_CODE"));
					ftrtModel.setSolId(rs.getString("SOL_ID"));
					ftrtModel.setFwContractNo(rs.getString("FW_CONTRACT_NO"));

					if (rs.getString("FUNCTION_CODE") != null && !rs.getString("FUNCTION_CODE").isEmpty())
						ftrtModel.setFunctionCode(rs.getString("FUNCTION_CODE").charAt(0));

					ftrtModel.setCreatedBy(rs.getString("CREATED_BY"));
					ftrtModel.setCreatedDate(rs.getDate("CREATED_DATE"));
					ftrtModel.setModifiedBy(rs.getString("MODIFIED_BY"));
					ftrtModel.setModifiedDate(rs.getDate("MODIFIED_DATE"));
					ftrtModel.setFundsStartDate(rs.getDate("FUNDS_START_DATE"));

					ftrtDetailsList.add(ftrtModel);
				}
			}
		} catch (Exception ex) {
			logger.error("FTRT RET details exceptions! " + ex.getMessage());

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, rs);
		}
		return ftrtDetailsList;
	}

	/**
	 * 
	 * @param applicationId
	 * @param applicatopnRef
	 * @param trRefNum
	 * @return
	 */
	public static int updateListInRET(String applicationId, String applicatopnRef, String trRefNum) {

		// logger.debug(applicationId + "\t" + applicatopnRef + "\t" +
		// trRefNum);
		int updatedRowCount = 0;
		Connection aConnection = null;
		PreparedStatement aPreParedStatement = null;
		try {
			// aConnection = DatabaseUtility.getIdbinternalConnection();
			aConnection = DatabaseUtility.getIdbFcConnection();
			if (aConnection != null) {

				String updateQuery = "UPDATE FTRT_DETAILS SET PROCESS_FLG = 'Y' WHERE APPLICATION_ID = ? AND AP_REF_NO = ? AND TR_REF_NUM = ? ";
				// AND CREATED_DATE = (SELECT TO_CHAR(SYSDATE, 'dd-MON-YY') FROM
				// DUAL) ";
				// StaticDataConstant.AccountStatusUpdateQuery;
				// logger.debug(" IDB.FTRT update query is :\n" + updateQuery);
				aPreParedStatement = aConnection.prepareStatement(updateQuery);
				aPreParedStatement.setString(1, applicationId);
				aPreParedStatement.setString(2, applicatopnRef);
				aPreParedStatement.setString(3, trRefNum);

				updatedRowCount = aPreParedStatement.executeUpdate();
				// logger.debug("FX Deal UpdatedRowCount >>> " +
				// updatedRowCount);
			}
		} catch (Exception ex) {
			logger.error("FX Deal updation exception is " + ex.getMessage(), ex);
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aPreParedStatement, null);
		}
		return updatedRowCount;
	}

}
