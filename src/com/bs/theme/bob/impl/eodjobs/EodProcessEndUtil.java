package com.bs.theme.bob.impl.eodjobs;

import java.sql.Timestamp;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.util.ThemeBridgeUtil;

/**
 * 
 * @since 2016-09-07
 * @version 1.0.2
 * @author KXT51472, Prasath Ravichandran
 */
public class EodProcessEndUtil {

	private final static Logger logger = Logger.getLogger(EodProcessEndUtil.class.getName());

	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	/**
	 * 
	 * @return
	 */
	public static String eodProcessStop(String tiLastEODBusinessDate, String tiEodBusinessDate) {

		String result = "";
		EodProcessEndUtil ce = new EodProcessEndUtil();
		ce.eodBatchProcess(tiLastEODBusinessDate, tiEodBusinessDate);

		return result;
	}

	public String eodBatchProcess(String tiLastEODBusinessDate, String tiEodBusinessDate) {

		String result = "";
		String errorMsg = "";
		logger.debug("Entering into the BackofficeUtil.bulk posting processing method");

		try {
			// EODFincePeriodicPosting bu = new EODFincePeriodicPosting();
			// boolean eodprocess = bu.bulkPostingProcess(tiLastEODBusinessDate,
			// tiEodBusinessDate);
			// logger.debug("eodprocess status : " + eodprocess);

		} catch (Exception e) {
			logger.error("" + e.getMessage());
			errorMsg = e.getMessage();

		} finally {
			ServiceLogging.pushLogData("TIEODJOB", "BULK_POSTING", "ZONE1", null, "ZONE1", "BOB", "", "", "PROCESSED",
					"", "", "", "", tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
					errorMsg);
		}

		return result;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		EodProcessEndUtil anAdaptee = new EodProcessEndUtil();
		String request = ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\task eod\\EOD.Start.xml");
		// String request =
		// ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\task\\task
		// eod\\EOD.Stop.xml");

		// logger.debug(anAdaptee.process(request));

		// HashMap<String, String> list = getpostingStatus();

	}

}
