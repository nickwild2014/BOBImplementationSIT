package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.bs.theme.bob.template.util.StepNameConstants;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.test.CustomisationQueryUtil;

public class FXDealUtilization {

	private final static Logger logger = Logger.getLogger(FXDealUtilization.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * IV
	 * 
	 * @since 2017-JAN-12
	 * @author Prasath Ravichandran
	 * 
	 * @param stepId
	 * @param stepStatus
	 * @param aMasterRefNo
	 * @param aEventRefNo
	 * @return
	 */
	public String processRETDealRef(String stepId, String stepStatus, String aMasterRefNo, String aEventRefNo) {

		logger.debug("******* 1.FX DEAL CSM / CBS TRXN ************");
		String result = "";

		try {
			if ((stepId.equalsIgnoreCase(StepNameConstants.INPUT_STEP)
					|| stepId.equalsIgnoreCase(StepNameConstants.CSM_STEP)
					|| stepId.equalsIgnoreCase(StepNameConstants.CBSMAKER_STEP))
					&& stepStatus.equalsIgnoreCase("Completed")) {
				logger.debug("Milestone 01 : " + stepId + "-" + stepStatus);
				// TODO insert if none, update if existing
				getAllMasterEventContractdeal(aMasterRefNo, aEventRefNo);

			} else if ((stepId.equalsIgnoreCase(StepNameConstants.AUTHORISE_STEP)
					|| stepId.equalsIgnoreCase(StepNameConstants.CBSAUTHORIZER_STEP))
					&& stepStatus.equalsIgnoreCase("Completed")) {
				logger.debug("Milestone 02 : " + stepId + "-" + stepStatus);
				// TODO update flag 'A' instead of 'I' existing
				updateDealMaster(aMasterRefNo, aEventRefNo);

			} else if (stepStatus.equalsIgnoreCase("Rejected") || stepStatus.equalsIgnoreCase("Aborted")) {
				logger.debug("Milestone 03 : " + stepId + "-" + stepStatus);
				deleteDealMaster(aMasterRefNo, aEventRefNo);

			} else {
				logger.debug(stepId + "-" + stepStatus + " No action required");
			}

		} catch (Exception e) {
			logger.error("FXDealUtilization exception..!" + e.getMessage());
		}
		return result;
	}

	/**
	 * IV.1
	 * 
	 * @param aMasterReference
	 * @param eventRef
	 */
	private void getAllMasterEventContractdeal(String aMasterReference, String eventReference) {

		String customer = "";
		String retAmount = "";
		String ti_sysdate = "";
		String contrct_ref = "";

		List<String> list_contrctref = new ArrayList<String>();
		// IV.1.1
		List<Map<String, String>> returnmaplist = fxdealprocess(aMasterReference, eventReference);

		for (Map<String, String> map : returnmaplist) {
			contrct_ref = map.get("DEALREF");
			ti_sysdate = map.get("TI_SYSDATE");
			customer = map.get("CUSTOMER");
			if (ValidationsUtil.isValidString(map.get("BUYSELL")) && map.get("BUYSELL").equalsIgnoreCase("P")) {
				retAmount = map.get("PURCHASEAMOUNT");

			} else if (ValidationsUtil.isValidString(map.get("BUYSELL")) && map.get("BUYSELL").equalsIgnoreCase("S")) {
				retAmount = map.get("SALEAMOUNT");
			}
			list_contrctref.add(contrct_ref + "|" + retAmount);
		}

		// IV.1.2
		fxUtilizationInsertProcess(aMasterReference, eventReference, customer, list_contrctref, ti_sysdate, retAmount);
	}

	/**
	 * IV.1.2
	 * 
	 * @since 2017-JAN-12
	 * @param masterref
	 * @param eventref
	 * @param customer
	 * @param listcontractref
	 * @param ti_sysdate
	 * @param retAmount
	 */
	public static void fxUtilizationInsertProcess(String masterref, String eventref, String customer,
			List<String> listcontractref, String ti_sysdate, String retAmount) {

		for (String cntrctno : listcontractref) {
			String[] L_cntrctno = cntrctno.split("\\|");
			cntrctno = L_cntrctno[0];
			retAmount = L_cntrctno[1];

			// Check existing or not
			boolean isExistingValues = isExisting(customer, cntrctno, masterref, eventref);
			logger.debug("isExistingValues : " + isExistingValues);

			if (!isExistingValues) {
				// IV.1.2.0
				List<Map<String, String>> mapList = getFXDealDetails(customer, cntrctno);
				// IV.1.2.1
				getinsertconrtctdealtftrt_details(mapList, ti_sysdate, masterref, eventref, retAmount);
			}
		}
	}

	public static boolean isExisting(String customer, String fxContractReference, String masterref, String eventref) {

		// logger.debug("********* Backoffice FXContract drawdown fxutilization
		// count *********");

		boolean result = false;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {
				String fxOptionSearchQuery = "SELECT COUNT(*) AS COUNT FROM FTRT_DETAILS_UTILIZE WHERE APPLICATION_ID = 'TIP' AND CIF_ID = ? AND TR_REF_NUM = ? AND TI_MST_EVT_REF = ? ";
				// logger.debug("FXUtilizationCountQuery : " +
				// fxOptionSearchQuery);

				ps = con.prepareStatement(fxOptionSearchQuery);
				ps.setString(1, customer);
				ps.setString(2, fxContractReference);
				ps.setString(3, masterref + eventref);
				rs = ps.executeQuery();

				while (rs.next()) {
					int count = rs.getInt("COUNT");
					logger.debug("Existing count : " + count);
					if (count > 0)
						result = true;
				}
			}
		} catch (Exception e) {
			logger.error("Exception checking existing record count : " + e.getMessage());
			result = false;
		}

		return result;
	}

	/**
	 * IV.2
	 * 
	 * @since 2017-JAN-12
	 * @param masterRefNo
	 * @param eventRefNo
	 * @return
	 */
	private int updateDealMaster(String masterRefNo, String eventRefNo) {

		logger.debug("*********** Authorize step process ************");

		int insertedRows = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		conn = DatabaseUtility.getThemebridgeConnection();

		String FXDealUtilizationUpdateQuery = "update FTRT_DETAILS_UTILIZE set PROCESS_DATE = to_date(?,'yyyy-MM-dd'), TI_CONTCT_STATUS = 'A' where TI_MST_EVT_REF = ? and TI_CONTCT_STATUS ='I' ";
		try {
			// logger.debug("FXDealUtilizationUpdateQuery : " +
			// FXDealUtilizationUpdateQuery);

			ps = conn.prepareStatement(FXDealUtilizationUpdateQuery);
			String tisysDate = CustomisationQueryUtil.getCurrentDateofTISystem();
			String dateString = DateTimeUtil.getDateTimeChangeFormat(tisysDate, "dd-MM-yyyy", "yyyy-MM-dd");
			logger.debug(dateString + "," + masterRefNo + eventRefNo);
			ps.setString(1, dateString);
			ps.setString(2, masterRefNo + eventRefNo);
			insertedRows = ps.executeUpdate();

			if (insertedRows > 0) {
				logger.debug(insertedRows + " deal(s) updated successfully.!" + masterRefNo + eventRefNo);
			}

		} catch (SQLException e) {
			logger.error("SQLException:\t" + e.getMessage(), e);
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exception:\t" + e.getMessage(), e);
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, null);
		}
		return insertedRows;
	}

	/**
	 * IV.3
	 * 
	 * @since 2017-JAN-12
	 * @param masterRefNo
	 * @param eventRefNo
	 * @return
	 */
	private int deleteDealMaster(String masterRefNo, String eventRefNo) {

		logger.debug("*********** Master event deals all auth step process ************");

		int insertedRows = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		conn = DatabaseUtility.getThemebridgeConnection();

		String dealUtilzDeleteQuery = "delete FROM FTRT_DETAILS_UTILIZE WHERE APPLICATION_ID = 'TIP'  and TI_MST_EVT_REF = ?";
		try {
			// logger.debug("dealUtilzDeleteQuery : " + dealUtilzDeleteQuery);
			ps = conn.prepareStatement(dealUtilzDeleteQuery);
			ps.setString(1, masterRefNo + eventRefNo);
			insertedRows = ps.executeUpdate();

			if (insertedRows > 0) {
				logger.debug(insertedRows + " deal(s) deleted successfully! " + masterRefNo + eventRefNo);
			}

		} catch (SQLException e) {
			logger.error("SQLException:\t" + e.getMessage(), e);
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exception:\t" + e.getMessage(), e);
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, null);
		}
		return insertedRows;

	}

	/**
	 * IV.1.1
	 * 
	 * @since 2017-JAN-12
	 * @param masterRefer
	 * @param eventRef
	 * @return
	 */
	public static List<Map<String, String>> fxdealprocess(String masterRef, String eventRef) {

		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement ps = null;
		List<Map<String, String>> returnmaplist = null;

		String query = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL,3,0)) AS EVENTREFERENCE,"
				+ "  MAS.REFNO_PFIX , FXB.PURCH_AMT/POWER(10,(SELECT C8CED FROM C8PF WHERE C8CCY= FXB.PURCH_CCY)) AS PURCHASEAMOUNT, FXC.CUSTOMER,"
				+ "  FXB.PURCH_CCY AS PURCHASECURRENCY, FXC.REFERENCE AS DEALREF , CASE WHEN FXB.FIXEDSIDE = 'B' OR FXB.FIXEDSIDE = 'P'  THEN ROUND(1/FXB.EXCH_RATE,5) "
				+ "  ELSE ROUND(FXB.EXCH_RATE,5) END FXCONTRACTRATE, FXB.FIXEDSIDE ,FXB.FXHOSTTYPE,  FXB.SALE_AMT/POWER(10,(SELECT C8CED FROM C8PF"
				+ "  WHERE C8CCY= FXB.SALE_CCY)) AS SALEAMOUNT, FXB.SALE_CCY AS SALECURRENCY ,"
				+ "  FXB.SALE_DATE , FXB.FXCONTRACT, FXHOSTTCKT, CASE WHEN TRIM(FXB.FIXEDSIDE)  = 'B' THEN 'P' ELSE FXB.FIXEDSIDE  END AS BUYSELL,"
				+ "  FXB.DIRECTION , T2.DEAL_PTY, T2.DEAL_TYPE       AS DEALTYPE, TRIM(T2.REFERENCE) AS DRAWDOWNREFERENCE,"
				+ "  TRIM(T2.DEALBRANCH)      AS BRANCH, MAS.KEY97 MSTKEY97, BEV.KEY97 BEVKEY97, (SELECT  PROCDATE FROM DLYPRCCYCL) TI_SYSDATE"
				+ "  FROM MASTER MAS, BASEEVENT BEV, RELITEM REL, FXBASEDEAL FXB, DEAL T2, DOCRELITEM T1 , FXCONTRACT FXC"
				+ "  WHERE MAS.KEY97          = BEV.MASTER_KEY AND BEV.KEY97            = REL.EVENT_KEY"
				+ "  AND FXB.KEY97            = REL.KEY97 AND T2.KEY97             = REL.KEY97"
				+ "  AND T1.KEY97             = REL.KEY97 AND TRIM(FXC.KEY97)      = TRIM(FXB.FXCONTRACT) "
				+ "  AND TRIM(MAS.MASTER_REF) = '" + masterRef
				+ "' AND (TRIM(BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL,3,0))) = '" + eventRef + "' ";
		// logger.debug("FXdeal : " + query);

		try {
			conn = DatabaseUtility.getTizoneConnection();
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery(query);

			ResultSetMetaData rsmd = ps.getMetaData();
			int columncount = rsmd.getColumnCount();
			returnmaplist = new ArrayList();
			while (rs.next()) {
				// logger.debug("whilelooping FxOptions");
				Map<String, String> maplist = new HashMap<String, String>();
				for (int i = 1; i < columncount + 1; i++) {
					// logger.debug("Forlooping FxOptions");
					String key = rsmd.getColumnLabel(i);
					String value = ValidationsUtil.checkIsNull(rs.getString(key));
					// logger.debug("The key:" + key + " & the value: "
					// + value);
					maplist.put(key, value);
				}
				returnmaplist.add(maplist);
				// logger.debug("FxOptions list added");
			}
			logger.debug("FXDealprocessQuery executed");

		} catch (SQLException e) {
			logger.error("SQLException " + e.getMessage());
			e.printStackTrace();

		} catch (Exception ex) {
			logger.debug("Exception " + ex.getMessage());
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, rs);

		}
		// logger.debug("returnmaplist " + returnmaplist);
		return returnmaplist;
	}

	/**
	 * IV.1.2.0
	 * 
	 * @param aFxContractDrawdownAPI
	 * @return
	 */
	public static List<Map<String, String>> getFXDealDetails(String customer, String fxContractReference) {

		List<Map<String, String>> returnmaplist = null;
		boolean result = false;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {
				String fxOptionSearchQuery = "SELECT * FROM FTRT_DETAILS_UTILIZE WHERE APPLICATION_ID = 'RET' and CIF_ID = ? AND TR_REF_NUM = ? ";
				// logger.debug("FxOptionSearchQuery : " + fxOptionSearchQuery);

				ps = con.prepareStatement(fxOptionSearchQuery);
				ps.setString(1, customer);
				ps.setString(2, fxContractReference);
				rs = ps.executeQuery();

				ResultSetMetaData rsmd = ps.getMetaData();
				int columncount = rsmd.getColumnCount();
				returnmaplist = new ArrayList();
				while (rs.next()) {
					// logger.debug("whilelooping FxOptions");
					Map<String, String> maplist = new HashMap<String, String>();
					for (int i = 1; i < columncount + 1; i++) {
						// logger.debug("Forlooping FxOptions");
						String key = rsmd.getColumnLabel(i);
						String value = ValidationsUtil.checkIsNull(rs.getString(key));
						// logger.debug("The key:" + key + " & the value: "
						// + value);
						maplist.put(key, value);
					}
					returnmaplist.add(maplist);
					// logger.debug("FxOptions list added");
				}
			}
		} catch (Exception ex) {
			logger.error("Exception! " + ex.getMessage());
			ex.printStackTrace();
			result = false;

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		// logger.debug("********* Backoffice FXContract drawdown
		// request/fxutilization ended *********");
		return returnmaplist;
	}

	/**
	 * IV.1.2.0
	 * 
	 * @param mapList
	 * @param ti_sysdate
	 * @param masterref
	 * @param eventRef
	 * @param retAmount
	 */
	private static void getinsertconrtctdealtftrt_details(List<Map<String, String>> mapList, String ti_sysdate,
			String masterref, String eventRef, String retAmount) {

		String insertCoulmns = "";
		for (Map<String, String> map : mapList) {
			// to itreate the list of the records

			Set<String> propertiesKey = map.keySet();
			int i = 0;
			for (String keys : propertiesKey) {
				// to set all the coulmns of a single record
				i++;
				String key = (String) keys;
				// logger.debug("The key is:" + key);
				String value = map.get(keys);
				// logger.debug("The value of that key is :" + value);
				// logger.debug("Count:" + i + ",Key:" + key);

				// if(!ValidationsUtil.isValidString(insertCoulmns)){
				if (insertCoulmns.isEmpty()) {
					insertCoulmns = insertCoulmns + key;
				} // else if(!insertCoulmns.isEmpty()){
				else {
					insertCoulmns = insertCoulmns + "," + key;
				}
			}
			// logger.debug("Insert Coulmns Print ---------> " + insertCoulmns);
		}

		boolean result = false;
		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;
		con = DatabaseUtility.getThemebridgeConnection();
		String query = "INSERT INTO FTRT_DETAILS_UTILIZE " + "(" + insertCoulmns + ") "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		// logger.debug("The query is " + query);
		try {
			ps = con.prepareStatement(query);
			logger.debug("Tracer 2 MapList size >>->> " + mapList.size());
			for (Map<String, String> map : mapList) {
				// to itreate the list of the records
				Set<String> propertiesKey = map.keySet();
				int i = 0;
				for (String keys : propertiesKey) {
					// to set all the coulmns of a single record
					i++;
					String key = (String) keys;
					// logger.debug("The key is:" + key);
					String value = map.get(keys);
					// logger.debug("The value of that key is :" + value);
					// logger.debug("Count:" + i + ",Key:" + key + ",value:" +
					// value);

					if (key.equalsIgnoreCase("BUY_OR_SELL") || key.equalsIgnoreCase("PROCESS_FLG")
							|| key.equalsIgnoreCase("STATUS") || key.equalsIgnoreCase("CREATED_BY")
							|| key.equalsIgnoreCase("FREE_CODE_1") || key.equalsIgnoreCase("FREE_CODE_2")
							|| key.equalsIgnoreCase("FREE_CODE_3") || key.equalsIgnoreCase("MODIFIED_BY")
							|| key.equalsIgnoreCase("ACCOUNT_ID") || key.equalsIgnoreCase("FW_CONTRACT_NO")
							|| key.equalsIgnoreCase("RELATED_TR_REF_NUM") || key.equalsIgnoreCase("TR_REF_NUM")
							|| key.equalsIgnoreCase("AP_REF_NO") || key.equalsIgnoreCase("APPLICATION_ID")
							|| key.equalsIgnoreCase("TRADE_REF_NO") || key.equalsIgnoreCase("ERROR_REASON")
							|| key.equalsIgnoreCase("FROM_CRNCY_CODE") || key.equalsIgnoreCase("TO_CRNCY_CODE")
							|| key.equalsIgnoreCase("REV_PROC_REMARKS") || key.equalsIgnoreCase("EVENT_ID")
							|| key.equalsIgnoreCase("FWC_TYPE") || key.equalsIgnoreCase("LOB_CODE")
							|| key.equalsIgnoreCase("RATECODE") || key.equalsIgnoreCase("CIF_ID")
							|| key.equalsIgnoreCase("REMARKS") || key.equalsIgnoreCase("SOL_ID")
							|| key.equalsIgnoreCase("FUNCTION_CODE") || key.equalsIgnoreCase("REV_PROCESSED_FLAG")) {
						if (ValidationsUtil.isValidString(value)) {
							ps.setString(i, value);
						} else {
							ps.setString(i, null);
						}

						if (ValidationsUtil.isValidString(value) && key.equalsIgnoreCase("APPLICATION_ID")) {
							// ps.setString(i, "TRDE");
							ps.setString(i, "TIP");
						}

						if (ValidationsUtil.isValidString(value) && key.equalsIgnoreCase("AP_REF_NO")) {
							ps.setString(i, ThemeBridgeUtil.generateRandom(10) + "");
						}

						if (ValidationsUtil.isValidString(value) && key.equalsIgnoreCase("PROCESS_FLG")) {
							ps.setString(i, "N"); // hard Coded to N per
													// document
						}

						// logger.debug("Count:" + i + ",Key:" + key + ",value:"
						// + value);
					}
					if (key.equalsIgnoreCase("CUST_RATE") || key.equalsIgnoreCase("SWAP_CHARGE_RATE")
							|| key.equalsIgnoreCase("SWAP_RATE") || key.equalsIgnoreCase("TREASURY_RATE")
							|| key.equalsIgnoreCase("REF_AMT") || key.equalsIgnoreCase("UTILIZED_AMOUNT")) {

						if (ValidationsUtil.isValidString(value)) {
							ps.setString(i, value);
						} else {
							ps.setNull(i, java.sql.Types.INTEGER);
						}

						if (ValidationsUtil.isValidString(value) && key.equalsIgnoreCase("REF_AMT")) {
							// ps.setString(i, value);
							ps.setString(i, retAmount);
						}
						// logger.debug("Count:" + i + ",Key:" + key + ",value:"
						// + value);
					}

					if (key.equalsIgnoreCase("TI_MST_EVT_REF") || key.equalsIgnoreCase("TI_CONTCT_STATUS")) {

						if (key.equalsIgnoreCase("TI_CONTCT_STATUS")) {
							ps.setString(i, "I"); // hard Coded to N per
													// document
						}

						if (key.equalsIgnoreCase("TI_MST_EVT_REF")) {
							ps.setString(i, masterref + eventRef); // hard Coded
																	// to N per
							// document
						}
						// logger.debug("Count:" + i + ",Key:" + key + ",value:"
						// + value);
					}

					if (key.equalsIgnoreCase("APPL_REQ_DATE") || key.equalsIgnoreCase("CREATED_DATE")
							|| key.equalsIgnoreCase("FUNDS_DELIVERY_DATE") || key.equalsIgnoreCase("FUNDS_START_DATE")
							|| key.equalsIgnoreCase("MODIFIED_DATE") || key.equalsIgnoreCase("PROCESS_DATE")
							|| key.equalsIgnoreCase("REQUEST_DATE") || key.equalsIgnoreCase("REV_PROCESSED_DATE")) {

						if (ValidationsUtil.isValidString(value)) {
							try {
								ps.setDate(i, DateTimeUtil
										.getSqlDateByXMLGregorianCalendar(DateTimeUtil.getXmlGregorianDate(value)));
								// ps.setDate(i, null);

							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							try {
								ps.setDate(i, null);
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						if (ValidationsUtil.isValidString(ti_sysdate) && key.equalsIgnoreCase("PROCESS_DATE")) {
							// ps.setDate(i,ThemeBridgeUtil.getSqlDatefromGregorianCalendar(ThemeBridgeUtil.getDateStringInXMLGregorian(tiSystemDate,
							// "yyyy-MM-dd")));ps.setDate(i,ThemeBridgeUtil.getSqlDatefromGregorianCalendar(requestheader.getCreationDate()));
							try {
								ps.setDate(i, DateTimeUtil.getSqlDateByXMLGregorianCalendar(DateTimeUtil
										.getDateInXMLGregorianByStringDateInFormat(ti_sysdate, "yyyy-MM-dd")));
							} catch (SQLException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						// logger.debug("Count:" + i + ",Key:" + key + ",value:"
						// + value);
					}
				}

				// logger.debug("PS ---------------------------- " +
				// ps.getParameterMetaData().toString());

				/*
				 * ps.setString(1, "TRDE"); ps.setString(1, "TIP");
				 * ps.setDate(35,ThemeBridgeUtil.getSqlDatefromGregorianCalendar
				 * (requestheader.getCreationDate())); ps.setString(4, "N");
				 * //hard Coded to N per document ps.setString(5,
				 * aFxContractDrawdownAPI.getPurchaseAmount().toString());
				 */
				// logger.debug("Tracer 3" + ps.getUpdateCount());
				int insertedRows = ps.executeUpdate();
				// logger.debug("Tracer 4");
				if (insertedRows > 0) {
					logger.debug(insertedRows + " Row inserted successfully");
					result = true;
				} else {
					result = false;
					logger.debug("Fx Drawdown row inserted Failed");
				}
				// logger.debug("Tracer 5");
			}
		} catch (SQLException e1) {
			logger.error("SQLExceptiopns! " + e1.getMessage());
			e1.printStackTrace();

		} catch (Exception e1) {
			logger.error("Exceptiopns! " + e1.getMessage());
			e1.printStackTrace();
		}
	}

}
