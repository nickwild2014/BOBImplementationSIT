package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;
import com.bs.themebridge.util.DatabaseUtility;

public class PostingStatusCheck {

	private final static Logger logger = Logger.getLogger(PostingStatusCheck.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		logger.debug(postingStatus("0958IGF1702003795", "ISS001"));
	}

	/**
	 * 
	 * @param mastRefnc
	 * @return
	 */
	public static String postingStatus(String mastRefnc, String eventRefnc) {

		ResultSet res = null;
		Connection conn = null;
		String postingStatus = "";
		PreparedStatement preStatmt = null;

		String postingStatusQuery = "SELECT STATUS FROM TRANSACTIONLOG WHERE MASTERREFERENCE = ? AND EVENTREFERENCE = ? ORDER BY ID DESC ";
		logger.debug("TrxnPostingStatusQuery : " + postingStatusQuery);

		try {
			conn = DatabaseUtility.getThemebridgeConnection();
			preStatmt = conn.prepareStatement(postingStatusQuery);
			preStatmt.setString(1, mastRefnc);
			preStatmt.setString(2, eventRefnc);
			res = preStatmt.executeQuery();
			while (res.next()) {
				postingStatus = res.getString("STATUS");
			}
			logger.debug("TrxnPostingStatus : >>-->>" + postingStatus + "<<--<<");

		} catch (Exception e) {
			logger.error("PostingStatus exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(conn, preStatmt, res);
		}
		return postingStatus;
	}

	/**
	 * 
	 * @param mastRefnc
	 * @return
	 */
	public static String postingStatusStmt(String mastRefnc, String eventRefnc) {

		ResultSet res = null;
		Statement bStatement = null;
		Connection bConnection = null;
		String postingStatus = "FAILED";

		String postingStatusQuery = "SELECT STATUS FROM TRANSACTIONLOG WHERE MASTERREFERENCE = '" + mastRefnc
				+ "' AND EVENTREFERENCE = '" + eventRefnc + "' ORDER BY ID DESC ";
		logger.debug("TrxnPostingStatusQuery : " + postingStatusQuery);

		try {
			bConnection = DatabaseUtility.getThemebridgeConnection();
			bStatement = bConnection.createStatement();
			res = bStatement.executeQuery(postingStatusQuery);
			while (res.next()) {
				postingStatus = res.getString("STATUS");
			}
			logger.debug("TrxnPostingStatus : " + postingStatus);

		} catch (Exception e) {
			logger.error("PostingStatus exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(bConnection, bStatement, res);
		}
		return postingStatus;
	}
}
