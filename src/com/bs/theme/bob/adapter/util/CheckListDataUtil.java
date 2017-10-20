package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.DatabaseUtility;

public class CheckListDataUtil {

	private final static Logger logger = Logger.getLogger(CheckListDataUtil.class.getName());

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static boolean processCheckListData(String stepId, String stepStatus, String masterRef, String eventRef) {

		logger.debug("******* 2.CheckList data delete ABRT TRXN ************");

		boolean deleteProcess = false;
		deleteProcess = deleteCheckListData(stepId, stepStatus, masterRef, eventRef);

		// logger.debug("*********** ProcessCheckListData finished
		// ************");
		return deleteProcess;
	}

	/**
	 * 
	 * @param stepId
	 *            {@code allows }{@link String}
	 * @param stepStatus
	 *            {@code allows }{@link String}
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventRef
	 *            {@code allows }{@link String}
	 * @return {@code returns }{@link String}
	 */
	public static boolean deleteCheckListData(String stepId, String stepStatus, String masterRef, String eventRef) {

		boolean deleteStatus = false;
		int resultSet = 0;
		Connection conn = null;
		PreparedStatement ps = null;

		String checkListDeleteQuery = "DELETE FROM ETT_WF_CHKLST_TRACKING WHERE MASTER_REF = ? and EVENTREF = ? ";
		// logger.debug("CheckListDeleteQuery : " + checkListDeleteQuery);
		// logger.debug("Master : " + masterRef + "\t Event : " + eventRef + "\t
		// StepStatus : " + stepStatus);
		try {
			conn = DatabaseUtility.getTizoneConnection();
			ps = conn.prepareStatement(checkListDeleteQuery);
			ps.setString(1, masterRef);
			ps.setString(2, eventRef);
			resultSet = ps.executeUpdate();

			if (resultSet > 0) {
				deleteStatus = true;
			}

		} catch (SQLException e) {
			logger.error("SQLException! " + e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exception! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, ps, null);
		}
		logger.debug("DeleteStatus : " + deleteStatus);
		return deleteStatus;
	}
}
