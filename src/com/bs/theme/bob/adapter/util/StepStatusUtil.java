package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;
import com.bs.themebridge.util.DatabaseUtility;

public class StepStatusUtil {

	private final static Logger logger = Logger.getLogger(StepStatusUtil.class.getName());

	public static void main(String[] args) {

		getStepNameAndStatus("0958ILF170200627", "ISS001");
	}

	/**
	 * This is used for only Backoffice verify
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @return
	 */
	public static HashMap<String, String> getStepNameAndStatus(String masterRef, String eventRef) {

		ResultSet res = null;
		Statement aStatement = null;
		Connection aConnection = null;
		HashMap<String, String> stepHistoryMap = new HashMap<String, String>();

		try {
			// Earlier 2017-08-04
			// String statusUpdateQuery = "SELECT MASTER_REF, EVENT_REF,
			// STEPNAME, STEPSTATUS, FINALRVIEW, TIMESTART, STEPTYPE FROM "
			// + " ( SELECT TRIM(M.MASTER_REF) AS MASTER_REF,
			// TRIM(BE.REFNO_PFIX||lpad(BE.REFNO_SERL, 3, 0)) AS EVENT_REF,
			// TRIM(OS.ID) as STEPNAME, "
			// + " TRIM(SH.STATUS) AS STEPSTATUS, "
			// + " TRIM(OS.FINALRVIEW) AS FINALRVIEW, SH.TIMESTART AS TIMESTART,
			// "
			// + " TRIM(SH.TYPE) AS STEPTYPE FROM MASTER M,BASEEVENT BE,STEPHIST
			// SH,ORCH_MAP OM,ORCH_STEP OS "
			// + " WHERE M.KEY97 = BE.MASTER_KEY AND BE.KEY97 = SH.EVENT_KEY AND
			// SH.ORCH_MAP = OM.KEY97 AND"
			// + " OM.ORCH_STEP = OS.KEY97 AND TRIM(M.MASTER_REF) = '" +
			// masterRef
			// + "' AND TRIM(BE.REFNO_PFIX||LPAD(BE.REFNO_SERL, 3, 0)) = '" +
			// eventRef
			// + "' ORDER BY SH.TIMESTART DESC) WHERE ROWNUM = 1";

			String statusUpdateQuery = "SELECT MASTER_REF, EVENT_REF, STEPNAME, STEPSTATUS, FINALRVIEW, TIMESTART, STEPTYPE FROM "
					+ " (SELECT TRIM(M.MASTER_REF) AS MASTER_REF, TRIM(BE.REFNO_PFIX||lpad(BE.REFNO_SERL, 3,0)) AS EVENT_REF, TRIM(OS.ID) AS STEPNAME, "
					+ " TRIM(SH.STATUS) AS STEPSTATUS, TRIM(OS.FINALRVIEW) AS FINALRVIEW, SH.TIMESTART AS TIMESTART, TRIM(SH.TYPE) AS STEPTYPE "
					+ " FROM MASTER M, BASEEVENT BE, STEPHIST SH, ORCH_MAP OM, ORCH_STEP OS  WHERE M.KEY97 = BE.MASTER_KEY "
					+ " AND BE.KEY97 = SH.EVENT_KEY  AND SH.ORCH_MAP = OM.KEY97 AND OM.ORCH_STEP = OS.KEY97 "
					+ " AND M.MASTER_REF =  '" + masterRef + "' AND BE.REFNO_PFIX||LPAD(BE.REFNO_SERL,3,0) =  '"
					+ eventRef + "' ORDER BY SH.TIMESTART DESC ) WHERE ROWNUM =1 ";
			// logger.debug("BackOfficeVerify StatusUpdateQuery ->" +
			// statusUpdateQuery);

			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			res = aStatement.executeQuery(statusUpdateQuery);
			while (res.next()) {
				String stepType = res.getString("STEPTYPE");
				String stepName = res.getString("STEPNAME");
				stepHistoryMap.put("stepType", stepType);
				stepHistoryMap.put("stepName", stepName);
			}
		} catch (SQLException e) {
			logger.error("BOV exceptions..! " + e.getMessage(), e);

		} catch (Exception e) {
			logger.error("BOV exceptions..! " + e.getMessage(), e);

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, res);
		}
		logger.debug("Hashmap : " + stepHistoryMap);
		return stepHistoryMap;
	}

	// /**
	// * This is used for only Backoffice verify
	// *
	// * @param masterRef
	// * @param eventRef
	// * @return
	// */
	// public static String getStepStatusID(String masterRef, String eventRef) {
	//
	// String stepId = "";
	// ResultSet res = null;
	// Statement aStatement = null;
	// Connection aConnection = null;
	//
	// try {
	// String StatusUpdateQuery = "SELECT MASTER_REF, EVENT_REF, STEPNAME,
	// STEPSTATUS, FINALRVIEW, TIMESTART, STEPTYPE FROM "
	// + " ( SELECT M.MASTER_REF, BE.REFNO_PFIX||lpad(BE.REFNO_SERL, 3, 0) AS
	// EVENT_REF, OS.ID as STEPNAME, SH.STATUS AS STEPSTATUS, "
	// + " OS.FINALRVIEW,SH.TIMESTART AS TIMESTART, "
	// + " SH.TYPE AS STEPTYPE FROM MASTER M,BASEEVENT BE,STEPHIST SH,ORCH_MAP
	// OM,ORCH_STEP OS "
	// + " WHERE M.KEY97 = BE.MASTER_KEY AND BE.KEY97 = SH.EVENT_KEY AND
	// SH.ORCH_MAP = OM.KEY97 AND"
	// + " OM.ORCH_STEP = OS.KEY97 AND TRIM(M.MASTER_REF) = '" + masterRef
	// + "' AND BE.REFNO_PFIX||LPAD(BE.REFNO_SERL, 3, 0) = '" + eventRef
	// // + "' AND TRIM(BE.REFNO_PFIX) = '" + event + "' AND
	// // TRIM(BE.REFNO_SERL) = '" + eventSrlNo
	// + "' ORDER BY SH.TIMESTART DESC) WHERE ROWNUM = 1";
	// logger.info("StatusUpdateQuery ->" + StatusUpdateQuery);
	//
	// aConnection = DatabaseUtility.getTizoneConnection();
	// aStatement = aConnection.createStatement();
	// res = aStatement.executeQuery(StatusUpdateQuery);
	// while (res.next()) {
	// stepId = res.getString("STEPTYPE");
	// logger.info("step Id : " + stepId);
	// }
	// } catch (SQLException e) {
	// logger.error(e.getMessage(), e);
	//
	// } catch (Exception e) {
	// logger.error(e.getMessage(), e);
	//
	// } finally {
	// DatabaseUtility.surrenderConnection(aConnection, aStatement, res);
	// }
	// return stepId;
	// }

	// 0958ELF170200032 POD001 Complete c N 10-MAR-17 12.07.47.970000000 PM -
	// 0958ELF170200032 POD001 Complete i N 10-MAR-17 12.07.47.900000000 PM -
	// 0958ELF170200032 POD001 Release c N 10-MAR-17 12.07.42.373000000 PM r
	// 0958ELF170200032 POD001 Release i N 10-MAR-17 12.07.42.328000000 PM r
	// 0958ELF170200032 POD001 CBS Authoriser c Y 10-MAR-17 12.07.08.540000000
	// PM a1
	// 0958ELF170200032 POD001 CBS Authoriser i Y 10-MAR-17 12.06.58.825000000
	// PM a1
	// 0958ELF170200032 POD001 Watch list chk c N 10-MAR-17 12.06.46.398000000
	// PM w
	// 0958ELF170200032 POD001 Watch list chk w N 10-MAR-17 12.06.24.470000000
	// PM w
	// 0958ELF170200032 POD001 Watch list chk i N 10-MAR-17 12.06.24.170000000
	// PM w
	// 0958ELF170200032 POD001 CBS Maker c N 10-MAR-17 11.56.40.335000000 AM i
	// 0958ELF170200032 POD001 CBS Maker d N 10-MAR-17 11.56.29.772000000 AM i
	// 0958ELF170200032 POD001 CBS Maker i N 10-MAR-17 11.56.29.689000000 AM i
	// 0958ELF170200032 POD001 CSM c N 10-MAR-17 11.50.58.144000000 AM i

	// 0958ILD160000734 Complete c N 18-OCT-16 07.57.36.109000000 AM -
	// 0958ILD160000734 Complete i N 18-OCT-16 07.57.36.104000000 AM -
	// 0958ILD160000734 Release c N 18-OCT-16 07.57.32.784000000 AM r
	// 0958ILD160000734 Release i N 18-OCT-16 07.57.32.766000000 AM r
	// 0958ILD160000734 Authorise c Y 18-OCT-16 07.57.21.812000000 AM a1
	// 0958ILD160000734 Authorise i Y 18-OCT-16 07.57.15.478000000 AM a1
	// 0958ILD160000734 Review c N 18-OCT-16 07.57.11.991000000 AM a1
	// 0958ILD160000734 Review i N 18-OCT-16 07.56.58.562000000 AM a1
	// 0958ILD160000734 Final limit chk c N 18-OCT-16 07.56.52.869000000 AM c2
	// 0958ILD160000734 Final limit chk r N 18-OCT-16 07.56.48.827000000 AM c2
	// 0958ILD160000734 Final limit chk i N 18-OCT-16 07.56.48.821000000 AM c2
	// 0958ILD160000734 Watch list chk c N 18-OCT-16 07.56.37.143000000 AM w
	// 0958ILD160000734 Watch list chk q N 18-OCT-16 07.56.20.874000000 AM w
	// 0958ILD160000734 Watch list chk r N 18-OCT-16 07.56.19.993000000 AM w
	// 0958ILD160000734 Watch list chk i N 18-OCT-16 07.56.19.987000000 AM w
	// 0958ILD160000734 Input c N 18-OCT-16 07.54.03.270000000 AM i
	// 0958ILD160000734 Input i N 18-OCT-16 07.53.59.314000000 AM i
	// 0958ILD160000734 Log c N 18-OCT-16 07.53.17.776000000 AM l
	// 0958ILD160000734 Log i N 18-OCT-16 07.53.17.314000000 AM l
	// 0958ILD160000734 Create c N 18-OCT-16 07.53.17.221000000 AM c
	// 0958ILD160000734 Create i N 18-OCT-16 07.53.17.216000000 AM c
}
