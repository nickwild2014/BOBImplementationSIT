package com.bs.theme.bob.adapter.adaptee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.ebg.GatewayIFNMessageRouter;
import com.bs.theme.bob.adapter.email.GatewayEmailAdapteeStaging;
import com.bs.themebridge.entity.model.Postingstaging;
import com.bs.themebridge.logging.PostingStagingLogging;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;

public class PostingStagingAdaptee {

	private final static Logger logger = Logger.getLogger(PostingStagingAdaptee.class.getName());

	final static PostingStagingAdaptee POSTSTAGOBJ = new PostingStagingAdaptee();
	public static final String STATUS_XPATH = "/ServiceResponse/ResponseHeader/Status";
	public static final String QUEUED_STAGING_QUERY = "SELECT * FROM POSTINGSTAGING WHERE PROCESSTIME >= SYSDATE-1 and PROCESSTIME <= SYSDATE-(1/(24*60)) and STATUS = 'QUEUED' ";
	public static final String POSTING_STATUS_QUERY = "SELECT COUNT(*) AS FAILED FROM TRANSACTIONLOG WHERE MASTERREFERENCE=? AND EVENTREFERENCE=? AND SERVICE='BackOffice' AND OPERATION='Batch' AND STATUS='FAILED' AND PROCESSTIME >= SYSDATE-1";

	public static void main(String a[]) throws Exception {
		PostingStagingAdaptee obj = new PostingStagingAdaptee();
		obj.process();
		// List<Postingstaging> s = obj.getStagingQueueDetails();
		// System.out.println(s.size());
	}

	public boolean process() {

		List<Postingstaging> stagingList = getStagingQueueDetails();
		// logger.debug("PostingStaging ListSize -->" + stagingList.size());

		if (stagingList.size() > 0)
			PostingStagingAdaptee.checkStagingPostingStatus(stagingList);
		// else {
		// logger.debug("Posting Staging list empty...!");
		// }

		return true;
	}

	public static synchronized void checkStagingPostingStatus(List<Postingstaging> stagingList) {
		synchronized (POSTSTAGOBJ) {
			// logger.debug("Enter into checkStagingPostingStatus method...");
			try {
				for (final Postingstaging aPostingstaging : stagingList) {
					String masterRef = aPostingstaging.getMasterreference().trim();
					String eventRef = aPostingstaging.getEventreference().trim();
					String service = aPostingstaging.getService().trim();
					String operation = aPostingstaging.getOperation().trim();
					String status = aPostingstaging.getStatus().trim();
					String tiRequest = aPostingstaging.getTirequest();

					if (ValidationsUtil.isValidString(masterRef) && ValidationsUtil.isValidString(eventRef)
							&& ValidationsUtil.isValidString(service) && ValidationsUtil.isValidString(operation)
							&& ValidationsUtil.isValidString(tiRequest) && status.equalsIgnoreCase("QUEUED")) {
						boolean transactionPostingStatus = checkTransactionPostingStatus(masterRef, eventRef);
						// logger.debug(masterRef + eventRef + " : " +
						// transactionPostingStatus + ", service : " + service
						// + ", operation :>>>" + operation + "<<<");

						if (transactionPostingStatus) {

							if (service.equalsIgnoreCase("SWIFT")) {
								// logger.debug("SWIFT");

								if (operation.equalsIgnoreCase("SwiftOut") || operation.equalsIgnoreCase("SFMSOut")) {
									// logger.debug("SwiftOut");
									SWIFTSwiftOutAdapteeStaging anSWIFTSwiftOutAdapteeStaging = new SWIFTSwiftOutAdapteeStaging();
									String swiftTIResponse = anSWIFTSwiftOutAdapteeStaging
											.process(aPostingstaging.getTirequest());
									String swiftServiceLogStatus = XPathParsing.getValue(swiftTIResponse, STATUS_XPATH);
									logger.debug("PostingStagingServiceLogStatus(SWIFT) : " + swiftServiceLogStatus);
									if (ValidationsUtil.isValidString(swiftTIResponse)) {
										aPostingstaging.setTiresponse(swiftTIResponse);
										postingStagingUpdate(aPostingstaging, swiftServiceLogStatus);
									}
								} else if (operation.startsWith("EBGIFN")) {
									// logger.debug("EBGIFN");
									GatewayIFNMessageRouter gatewayInfMessagesStaging = new GatewayIFNMessageRouter();
									String swiftTIResponse = gatewayInfMessagesStaging
											.processOutwardMessages(aPostingstaging.getTirequest(), service, operation);
									String swiftServiceLogStatus = XPathParsing.getValue(swiftTIResponse, STATUS_XPATH);
									logger.debug("PostingStagingServiceLogStatus(EBG) : " + swiftServiceLogStatus);
									if (ValidationsUtil.isValidString(swiftTIResponse)) {
										aPostingstaging.setTiresponse(swiftTIResponse);
										postingStagingUpdate(aPostingstaging, swiftServiceLogStatus);
									}
								}

							} else if (service.equalsIgnoreCase("EMAIL")) {

								if (operation.equalsIgnoreCase("Advice") || operation.equalsIgnoreCase("Tracer")) {
									logger.debug("PostingStaging (*_*) :>>->> " + service + operation);
									Thread alertMailThread = new Thread() {
										public void run() {
											GatewayEmailAdapteeStaging anGatewayEmailAdapteeStaging = new GatewayEmailAdapteeStaging();
											String emailServiceLogStatus = anGatewayEmailAdapteeStaging
													.process(aPostingstaging);
											logger.debug("PstgStgServiceLogStatus(EMAIL) : " + emailServiceLogStatus);
											postingStagingUpdate(aPostingstaging, emailServiceLogStatus);
										}
									};
									alertMailThread.setName("EMAILThread");
									alertMailThread.start();
								}

							} else if (service.equalsIgnoreCase("GATEWAY")) {
								logger.debug("GATEWAY");
								if (operation.startsWith("NEFT") || operation.startsWith("RTGS")
										|| operation.startsWith("NFT") || operation.startsWith("RTG")) {
									logger.debug("GATEWAY RTGS/NEFT Message update Posting Staging Queue status");
									GatewayRtgsNeftAdapteeStaging anGatewayRtgsNeftAdapteeStaging = new GatewayRtgsNeftAdapteeStaging();
									String paymentGwyResponse = anGatewayRtgsNeftAdapteeStaging
											.process(aPostingstaging.getTirequest());
									String paymentGwyServiceLogStatus = XPathParsing.getValue(paymentGwyResponse,
											STATUS_XPATH);
									logger.debug("PostingServiceLogStatus(NEFT/RTGS) : " + paymentGwyServiceLogStatus);
									if (ValidationsUtil.isValidString(paymentGwyResponse)) {
										aPostingstaging.setTiresponse(paymentGwyResponse);
										postingStagingUpdate(aPostingstaging, paymentGwyServiceLogStatus);
									}

								} 
								else if (operation.startsWith("SMS")) {}

							}
						}
					}
					// }
				}
			} catch (Exception e) {
				logger.error("Exception..!!! PostingStaging " + e.getMessage());
				e.printStackTrace();
			}
		}

		// logger.debug("End of postingStaging Queue Adaptee process");
	}

	private static String getQueuedStatus(String masterRef, String eventRef, String service, String operation) {
		String queueStatus = "";
		ResultSet rs = null;
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		String queryDetails = "SELECT trim(status) STATUS FROM POSTINGSTAGING WHERE masterreference " + "LIKE '%"
				+ masterRef + "%' AND eventreference LIKE '%" + eventRef + "%' and service='" + service + "' "
				+ "and operation='" + operation + "'";
		logger.debug("queuedDetailsQuery : " + queryDetails);
		try {
			dbConnection = DatabaseUtility.getThemebridgeConnection();
			if (ValidationsUtil.isValidObject(dbConnection)) {
				preparedStatement = dbConnection.prepareStatement(queryDetails);
				rs = preparedStatement.executeQuery();
				while (rs.next()) {
					queueStatus = rs.getString("STATUS");
				}
			}
		}

		catch (SQLException e) {
			logger.error("Posting staging get queued List exceptions! " + e.getMessage());

		} finally {
			DatabaseUtility.surrenderPrepdConnection(dbConnection, preparedStatement, rs);

		}
		return queueStatus;
	}

	/**
	 * 
	 * @param aPostingstaging
	 *            {@code allows }{@link List}
	 * @param status
	 *            {@code allows }{@link String}
	 */
	private static void postingStagingUpdate(Postingstaging aPostingstaging, String status) {

		boolean result = true;
		try {
			// logger.debug("aPostingStaging status : " +
			// aPostingstaging.getStatus());

			Timestamp tiReqTimestemp = (Timestamp) aPostingstaging.getTireqtime();
			// logger.debug("Update Posting log timestamp : " + tiReqTimestemp);
			logger.debug("Update Posting log status : " + status);

			result = PostingStagingLogging.updateLogData(aPostingstaging.getId(), aPostingstaging.getService(),
					aPostingstaging.getOperation(), aPostingstaging.getMasterreference(),
					aPostingstaging.getEventreference(), status, aPostingstaging.getTirequest(),
					aPostingstaging.getTiresponse(), tiReqTimestemp);

			/**
			 * below is written by supreme Sir[alternative for above code]. Need
			 * to implement
			 */
			// aPostingstaging.setStatus(status);
			// PostingStagingAdapter sAdapter = new PostingStagingAdapter();
			// result = sAdapter.updateProperty(aPostingstaging);

		} catch (Exception e) {
			logger.error("Posting statging update logging exception..!!" + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return
	 */
	private List<Postingstaging> getStagingQueueDetails() {

		ResultSet rs = null;
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		String queryDetails = QUEUED_STAGING_QUERY;
		List<Postingstaging> stagingQueueList = new ArrayList<Postingstaging>();
		// logger.info("Get QUEUED List from Posting Staging : " +
		// queryDetails);

		try {
			dbConnection = DatabaseUtility.getThemebridgeConnection();
			if (ValidationsUtil.isValidObject(dbConnection)) {
				preparedStatement = dbConnection.prepareStatement(queryDetails);
				rs = preparedStatement.executeQuery();
				while (rs.next()) {
					Postingstaging aPostingstaging = new Postingstaging();
					// System.out.println("TR");
					if (ValidationsUtil.isValidString(rs.getString("MASTERREFERENCE"))
							&& ValidationsUtil.isValidString(rs.getString("EVENTREFERENCE"))
							&& ValidationsUtil.isValidString(rs.getString("TIREQUEST"))) {

						aPostingstaging.setId(rs.getBigDecimal("ID"));
						aPostingstaging.setService(rs.getString("SERVICE"));
						aPostingstaging.setOperation(rs.getString("OPERATION"));
						aPostingstaging.setMasterreference(rs.getString("MASTERREFERENCE"));
						aPostingstaging.setEventreference(rs.getString("EVENTREFERENCE"));
						aPostingstaging.setStatus(rs.getString("STATUS"));
						aPostingstaging.setProcesstime(rs.getDate("PROCESSTIME"));
						aPostingstaging.setTirequest(rs.getString("TIREQUEST"));
						// PRASATH RAVICHANDRAN
						aPostingstaging.setTiresponse(rs.getString("TIRESPONSE"));
						aPostingstaging.setTireqtime(rs.getTimestamp("TIREQTIME"));
						aPostingstaging.setTirestime(rs.getTimestamp("TIRESTIME"));
						stagingQueueList.add(aPostingstaging);
					}
				}
			}
		}

		catch (SQLException e) {
			logger.error("Posting staging get queued List exceptions! " + e.getMessage());

		} finally {
			DatabaseUtility.surrenderPrepdConnection(dbConnection, preparedStatement, rs);

		}
		return stagingQueueList;
	}

	/**
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @return
	 */
	private static boolean checkTransactionPostingStatus(String masterRef, String eventRef) {

		// logger.info("Enter into checkSwiftTransactionStatus method..!");
		ResultSet rs = null;
		Connection dbConnection = null;
		boolean swiftTransResult = false;
		PreparedStatement preparedStatement = null;
		String queryDetails = POSTING_STATUS_QUERY;
		// logger.info("Posting Staging : " + queryDetails);

		try {
			dbConnection = DatabaseUtility.getThemebridgeConnection();
			if (ValidationsUtil.isValidObject(dbConnection)) {
				preparedStatement = dbConnection.prepareStatement(queryDetails);
				// logger.debug("MASTERREFERENCE >>" + masterRef + "<<");
				// logger.debug("EVENTREFERENCE >>" + eventRef + "<<");
				preparedStatement.setString(1, masterRef);
				preparedStatement.setString(2, eventRef);
				rs = preparedStatement.executeQuery();
				while (rs.next()) {
					// logger.debug("Posting status : " + rs.getInt(1));
					if (rs.getInt(1) == 0)
						swiftTransResult = true;
				}
			}
		} catch (SQLException e) {
			logger.error("Posting staging exceptions! " + e.getMessage());

		} finally {
			DatabaseUtility.surrenderPrepdConnection(dbConnection, preparedStatement, rs);

		}
		// logger.debug("checkTransactionPostingStatus " + swiftTransResult);
		return swiftTransResult;
	}
}
