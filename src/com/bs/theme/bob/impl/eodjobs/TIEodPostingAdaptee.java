package com.bs.theme.bob.impl.eodjobs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;

public class TIEodPostingAdaptee {

	private final static Logger logger = Logger.getLogger(TIEodPostingAdaptee.class.getName());
	// static String tiLastEODBusinessDate = "03-FEB-17";
	// static String tiSODBusinessDate = "04-FEB-17";

	public static void main(String[] args) {

		// List<TIEodPostingBean> beanobj = new
		// TIEodPostingAdaptee().getEodPostingDetails("25-AUG-17", "28-AUG-17",
		// "Y");

		// logger.debug("" + beanobj.size());
		// for (TIEodPostingBean tiEodPostingBean : beanobj) {
		// logger.debug(">>" + tiEodPostingBean.getHdr_process_date());
		// logger.debug(tiEodPostingBean.getUniqueId());
		// logger.debug(tiEodPostingBean.getError_code());
		// logger.debug(tiEodPostingBean.getProcessedflag());
		// logger.debug(tiEodPostingBean.getError_code());
		// }

		// getEodPostingDetails("02-MAY-17", "09-MAY-17", "Y");
	}

	public List<TIEodPostingBean> getEodPostingDetails(String fromDate, String toDate, String status) {

		// Y - YES (SUCCESS)
		// E - ERROR (FAILED)
		// N - NEW (NOT PROCESSED)
		// I - INITIATED (PROCESSING)
		logger.debug("Status : " + status + ",\t From(DD-MON-YY): " + fromDate + ",\t To(DD-MON-YY): " + toDate);

		String query = "";
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement ps = null;
		List<TIEodPostingBean> postingStatusList = new LinkedList<TIEodPostingBean>();
		query = "SELECT DTL.APPLICATION_ID, DTL.AP_TRAN_ID, to_char(DTL.AP_TRAN_DATE, 'YYYY-MM-DD') as AP_TRAN_DATE, DTL.AP_TRAN_PARTICULAR, DTL.TRAN_SR_NO, "
				+ " DTL.AP_DRCR_FLAG as DRCR_FLAG, DTL.AP_TRAN_AMOUNT as TRAN_AMOUNT, DTL.CURRENCY_CD as TRAN_CCY, DTL.FIN_ACCT_ID, DTL.ACCT_CURRENCY, DTL.RATE_CODE, "
				+ " DTL.CONVERSION_RATE, to_char(DTL.CREATED_DATE, 'YYYY-MM-DD') as CREATED_DATE, HDR.PROCESSED_FLAG as HDR_STATUS, "
				+ " to_char(HDR.PROCESSED_DATE, 'YYYY-MM-DD') as HDR_PROCESS_DATE, DTL.FIN_TRAN_ID, to_char(DTL.FIN_TRAN_DATE, 'YYYY-MM-DD') as FIN_TRAN_DATE, "
				+ " HDR.ERROR_CODE AS HDR_ERROR, HDR.PROC_REMARKS AS HDR_REMARKS, DTL.ERROR_REASON AS DTL_REASON"
				+ " from FIN_TRANS_DTL DTL, FIN_TRANS_HDR HDR where DTL.AP_TRAN_ID = HDR.AP_TRAN_ID  AND HDR.PROCESSED_FLAG like '%"
				+ status + "%' AND HDR.APPLICATION_ID = 'TIPS' AND " + " HDR.CREATED_DATE BETWEEN '" + fromDate
				+ "' AND '" + toDate + "' ORDER BY HDR.AP_TRAN_ID, DTL.TRAN_SR_NO ASC ";
		logger.debug("EODPostingStatusQuery : " + query);

		try {
			// conn = DatabaseUtility.getIdbTiplusConnection();
			conn = DatabaseUtility.getIdbFcConnection(); // 2017-08-28
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				TIEodPostingBean beanObj = new TIEodPostingBean();
				beanObj.setApplication_id(rs.getString("APPLICATION_ID"));
				beanObj.setAp_tran_id(rs.getString("AP_TRAN_ID"));
				beanObj.setAp_tran_date(rs.getString("AP_TRAN_DATE"));
				beanObj.setAp_tran_particular(rs.getString("AP_TRAN_PARTICULAR"));
				beanObj.setTran_sr_no(rs.getString("TRAN_SR_NO"));
				beanObj.setDrcr_flag(rs.getString("DRCR_FLAG"));
				beanObj.setTran_amount(rs.getString("TRAN_AMOUNT"));
				beanObj.setTran_ccy(rs.getString("TRAN_CCY"));
				beanObj.setFin_acct_id(rs.getString("FIN_ACCT_ID"));
				beanObj.setAcct_currency(rs.getString("ACCT_CURRENCY"));
				beanObj.setRate_code(rs.getString("RATE_CODE"));
				beanObj.setConversion_rate(rs.getString("CONVERSION_RATE"));
				beanObj.setCreated_date(rs.getString("CREATED_DATE"));
				beanObj.setHdr_status(rs.getString("HDR_STATUS"));
				// beanObj.setHdr_process_date(rs.getString("HDR_PROCESS_DATE"));
				beanObj.setHdr_process_date(
						rs.getString("HDR_PROCESS_DATE") == null ? "" : rs.getString("HDR_PROCESS_DATE"));
				beanObj.setFin_tran_id(rs.getString("FIN_TRAN_ID") == null ? "" : rs.getString("FIN_TRAN_ID"));
				beanObj.setFin_tran_date(rs.getString("FIN_TRAN_DATE") == null ? "" : rs.getString("FIN_TRAN_DATE"));
				beanObj.setHdr_error(rs.getString("HDR_ERROR") == null ? "" : rs.getString("HDR_ERROR"));
				beanObj.setHdr_remarks(rs.getString("HDR_REMARKS") == null ? "" : rs.getString("HDR_REMARKS"));
				beanObj.setError_reason(rs.getString("DTL_REASON") == null ? "" : rs.getString("DTL_REASON"));
				postingStatusList.add(beanObj);
			}
			logger.debug("EodPostingListSize : " + postingStatusList.size());

		} catch (SQLException e) {
			logger.error("SQLException-----> " + e.getMessage());
			e.printStackTrace();

		} catch (Exception ex) {
			logger.error("Exception -->" + ex.getMessage());
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, rs);
		}
		return postingStatusList;
	}

	/**
	 * 
	 * @return
	 */
	public String getTiLastEodDate() {

		String tiLastEodDate = "";
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		// 02-OCT-16
		String query = "select BS_SUBTRACT_EOD_DAYS_FUNC((SELECT to_char(PROCDATE,'dd-MON-yy') FROM DLYPRCCYCL ), 1) from dual ";
		logger.debug("TI EOD DATE QUERY : " + query);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				Date tiDate = rs.getDate(1);
				// logger.debug(tiLastDate);
				tiLastEodDate = DateTimeUtil.getStringDateByDateInFormat(tiDate, "dd-MMM-yy");
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
		logger.debug("TI Last Eod Date : " + tiLastEodDate);
		return tiLastEodDate;
	}

	/**
	 * 
	 * @return
	 */
	public String getTICurrentDate() {

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

}
