package com.bs.theme.bob.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.server.gateway.in.BridgeGateway;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.GatewayDocumentsXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.StatusEnum;

/**
 * End system communication implementation for LOCALIZATION services is handled
 * in this class.
 * 
 * @author Bluescope
 */
public class GatewayExtCustProcessor extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(GatewayExtCustProcessor.class.getName());

	// public static URL resource =
	// AccountAvailBalAdaptee.class.getResource(".");
	// String filePath = new File("").getAbsolutePath();

	private String service = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private static String branch = "";
	private String eventReference = "";
	private String customizationID = "";
	private String masterReference = "";
	private static String errorMsg = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private static String bankRequest = "";
	private static String bankResponse = "";
	private static String description = "";

	/**
	 * <p>
	 * Process the incoming Account available balance Service XML from the TI
	 * </p>
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@link String}
	 * 
	 */
	public String process(String requestXML) {

		logger.info(" ************ Gateway.Documents adaptee process started ************ ");

		String status = "SUCCEEDED";
		boolean isClean = true;
		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GatewayDocuments TI Request:\n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			processTIGatewayRequest(requestXML);
			logger.debug("GatewayDocuments Bank Request:\n" + bankRequest);

			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GatewayDocuments Bank Response:\n" + bankResponse);

			tiResponse = getTIResponseFromBankResponse(status);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GatewayDocuments TI Response:\n" + tiResponse);

		} catch (XPathExpressionException e) {
			isClean = false;
			status = "FAILED";
			errorMsg = e.getMessage();

		} catch (SAXException e) {
			isClean = false;
			status = "FAILED";
			errorMsg = e.getMessage();

		} catch (IOException e) {
			isClean = false;
			status = "FAILED";
			errorMsg = e.getMessage();

		} finally {
			// NEW LOGGING
			boolean res = ServiceLogging.pushLogData(service, operation, sourceSystem, branch, sourceSystem,
					targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,
					bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);
		}

		logger.info(" ************ Gateway.Documents adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param requestXML
	 *            {@code allows }{@link String}
	 * @return
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws IOException
	 */
	private String processTIGatewayRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		int count = 0;
		String result = ThemeBridgeStatusEnum.SUCCEEDED.toString();

		try {
			correlationId = XPathParsing.getValue(requestXML, GatewayDocumentsXpath.CorrelationId);
			operation = XPathParsing.getValue(requestXML, GatewayDocumentsXpath.Operation);
			service = XPathParsing.getValue(requestXML, GatewayDocumentsXpath.Service);
			sourceSystem = XPathParsing.getValue(requestXML, GatewayDocumentsXpath.SourceSystem);
			targetSystem = XPathParsing.getValue(requestXML, GatewayDocumentsXpath.TargetSystem);

			eventReference = XPathParsing.getValue(requestXML,
					"/ServiceRequest/" + operation.toLowerCase() + "/EventReference");
			masterReference = XPathParsing.getValue(requestXML,
					"/ServiceRequest/" + operation.toLowerCase() + "/MasterReference");
			customizationID = XPathParsing.getValue(requestXML, "/ServiceRequest/" + operation.toLowerCase() + "/ID");
			logger.debug("customization Document ID : " + customizationID);

			count = gatewayDocumentRouter(requestXML, customizationID);
			if (count == 0)
				result = ThemeBridgeStatusEnum.FAILED.toString();

		} catch (Exception e) {
			logger.error("Gateway Document Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	private int gatewayDocumentRouter(String requestXML, String customizationID) {

		int count = 0;

		try {
			if (customizationID.equals("PRR")) {
				// TODO Call customisation Code / Procedure / Jar
				count = preshipment(masterReference, eventReference);

			} else if (customizationID.equals("DWF")) {
				// TODO Call customisation Code / Procedure / Jar
				count = edpmsProcedureCall(masterReference, eventReference, customizationID);

			} else if (customizationID.equals("ROD")) {
				// TODO Call customisation Code / Procedure / Jar
				count = edpmsProcedureCall(masterReference, eventReference, customizationID);

			} else if (customizationID.equals("PRN")) {
				// TODO Call customisation Code / Procedure / Jar
				count = edpmsProcedureCall(masterReference, eventReference, customizationID);

			} else if (customizationID.equals("ARR")) {
				// TODO Call customisation Code / Procedure / Jar
				count = edpmsProcedureCall(masterReference, eventReference, customizationID);
			}

			// if (count == 0)
			// result = ThemeBridgeStatusEnum.FAILED.toString();

		} catch (Exception e) {
			logger.error("Gateway Document Exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		return count;
	}

	private int preshipment(String master, String event) {

		logger.info("Method Preshipment GATEWAY is Called " + master + "\t" + event);

		String loanRef = "";
		String bulkRequest = "";
		String bulkResponse = "";
		String tiEjbRequestXML = "";
		String limitExposure = "";
		int limitExpoStatus = 0;
		int procedureCallCount = 0;
		Connection con = null;
		ResultSet result = null;
		PreparedStatement ps = null;

		// String query = "select REQUEST, LOAN_REF from
		// ETT_PRESHIPMENT_APISERVER where MASREF='" + master
		// + "' AND EVENTREF='" + event + "'";
		try {
			con = DatabaseUtility.getTizoneConnection();
			String query = "select REQUEST, LOAN_REF, RESERVATION_REQUEST from ETT_PRESHIPMENT_APISERVER where MASREF= ? AND EVENTREF= ? ";
			logger.debug("PreshipmentQuery : " + query);
			ps = con.prepareStatement(query);
			ps.setString(1, master);
			ps.setString(2, event);
			result = ps.executeQuery();
			while (result.next()) {
				logger.debug("Milestone 02");
				tiEjbRequestXML = result.getString("REQUEST");
				loanRef = result.getString("LOAN_REF");
				limitExposure = result.getString("RESERVATION_REQUEST"); // TODO
				// bankRequest = bankRequest + " $ " + request;

				/** ATDOC PUSH **/
				if (tiEjbRequestXML != null) {
					String ejbRespXML = TIPlusEJBClient.process(tiEjbRequestXML);
					logger.debug("Preship EJB Response : " + ejbRespXML);

					StatusEnum statusEnum = ResponseHeaderUtil.processEJBClientResponse(ejbRespXML);
					logger.debug("Preship EJB Response status : " + master + "\t" + statusEnum.toString());
					String repStatus = statusEnum.toString();
					int update = updateAtdocStatus(master, event, loanRef, ejbRespXML, repStatus);

					/** PROCEDURE CALL **/
					if (statusEnum.equals(StatusEnum.SUCCEEDED)) {
						logger.debug("ATDOC SUCCEEDED");
						procedureCallCount = pushToExtDataBase(master, event, loanRef);

						logger.debug("ProcedureCallCount : " + procedureCallCount);
						if (procedureCallCount > 0) {
							/** Limit reversal **/
							if (limitExposure != null) {
								BridgeGateway bg = new BridgeGateway();
								String bridgeGatewayRespXML = bg.process(limitExposure);
								logger.debug("Limit response(Preship) : " + bridgeGatewayRespXML);

								/** Limit Exposure Reversal **/
								limitExpoStatus = updatePreshipLimitStatus(master, event, loanRef,
										bridgeGatewayRespXML);
							} else {
								logger.debug("Exposure is empty! ");
							}
						}
					} else if (statusEnum.equals(StatusEnum.FAILED) || statusEnum.equals(StatusEnum.UNAVAILABLE)) {
						logger.debug("FAILED : Procedure call is failed ");
					}

					bulkRequest = bulkRequest + "$\n" + tiEjbRequestXML;
					bulkResponse = bulkResponse + "$\n" + ejbRespXML;

					logger.debug("S : ATDOC RepStatus " + repStatus);
					logger.debug("S : Procedure " + procedureCallCount);
					logger.debug("S : LimitExpoStatus " + limitExpoStatus);

				} else {
					logger.error("requestTIXML is null ");
				}
			}
		} catch (Exception e) {
			logger.error("Exception in preshipment " + e.getMessage());
			errorMsg = e.getMessage();

		} finally {
			bankRequest = bulkRequest;
			bankResponse = bulkResponse;
			DatabaseUtility.surrenderPrepdConnection(con, ps, result);
		}
		return procedureCallCount;
	}

	/**
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @param loanRef
	 * @return
	 * @throws SQLException
	 */
	public static int pushToExtDataBase(String masterRef, String eventRef, String loanRef) {

		logger.debug("Entering into ETT_PRESHIPMENT_CLOSURE Documents Procedure call");
		logger.debug("MasterRef : " + masterRef + "\tEventRef : " + eventRef + "\tLoanRef : " + loanRef);
		bankRequest = "ETT_PRESHIPMENT_CLOSURE(3)";
		int insertedRowCount = 0;
		ResultSet aResultSet = null;
		Connection aConnection = null;
		CallableStatement aCallableStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {
				String procedureQuery = "{call ETT_PRESHIPMENT_CLOSURE(?,?,?)}";
				aCallableStatement = aConnection.prepareCall(procedureQuery);
				aCallableStatement.setString(1, masterRef);
				aCallableStatement.setString(2, eventRef);
				aCallableStatement.setString(3, loanRef);

				insertedRowCount = aCallableStatement.executeUpdate();
				bankResponse = "SUCCEEDED";
				logger.debug(insertedRowCount + " ETT_PRESHIPMENT_CLOSURE called successfully");
			}
		} catch (SQLException ex) {
			logger.error("ETT_PRESHIPMENT_CLOSURE procedure SQLException! " + ex.getMessage());
			ex.printStackTrace();
			bankResponse = "FAILED";
			insertedRowCount = 0;

		} catch (Exception e) {
			logger.error("ETT_PRESHIPMENT_CLOSURE procedure Exception! " + e.getMessage());
			bankResponse = "FAILED";
			errorMsg = e.getMessage();
			e.printStackTrace();
			insertedRowCount = 0;

		} finally {
			DatabaseUtility.surrenderCallableConnection(aConnection, aCallableStatement, aResultSet);
		}
		return insertedRowCount;
	}

	/**
	 * Provided by Mohanraj.B, 2017-08-26
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @param loanRef
	 * @param responseXML
	 * @param status
	 * @return
	 */
	public int updateAtdocStatus(String masterRef, String eventRef, String loanRef, String responseXML, String status) {
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		try {
			String query = "UPDATE ETT_PRESHIPMENT_APISERVER SET RESPONSE = ?, STATUS = ? WHERE MASREF = ? AND EVENTREF = ? AND LOAN_REF = ? ";
			logger.debug("PreShipRespUpdateQuery:-" + query);

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			// Set clob data
			Clob clobData = con.createClob();
			clobData.setString(1, responseXML);
			ps.setClob(1, clobData);
			ps.setString(2, status);
			ps.setString(3, masterRef);
			ps.setString(4, eventRef);
			ps.setString(5, loanRef);
			result = ps.executeUpdate();

		} catch (Exception e) {
			logger.error("Update exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}
		return result;
	}

	/**
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @param loanRef
	 * @param responseXML
	 * @param status
	 * @return
	 */
	public int updatePreshipLimitStatus(String masterRef, String eventRef, String loanRef, String responseXML) {
		Connection con = null;
		PreparedStatement ps = null;
		int result = 0;
		try {
			String limitUpdateQuery = "UPDATE ETT_PRESHIPMENT_APISERVER SET RESERVATION_RESPONSE = ? WHERE MASREF = ? and EVENTREF = ? AND LOAN_REF = ? ";
			logger.debug("PreShipLimitUpdateQuery:-" + limitUpdateQuery);

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(limitUpdateQuery);
			// Set clob data
			Clob clobData = con.createClob();
			clobData.setString(1, responseXML);
			ps.setClob(1, clobData);
			ps.setString(2, masterRef);
			ps.setString(3, eventRef);
			ps.setString(4, loanRef);
			result = ps.executeUpdate();

		} catch (Exception e) {
			logger.error("Update exceptions! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}
		return result;
	}

	/**
	 * 
	 * @param master
	 * @param event
	 * @return
	 */
	private int edpmsProcedureCall(String master, String event, String actionCodeCode) {

		logger.info("Method edpmsProcedureCall GATEWAY is Called ");
		int procedureCallCount = 0;
		try {
			procedureCallCount = pushCustomizationProcedure(master, event, actionCodeCode);

		} catch (SQLException e) {
			logger.error("ETT_CUSTOMIZATION_PKG Exception! " + e.getMessage());
			e.printStackTrace();
		}

		return procedureCallCount;
	}

	/**
	 * 
	 * @param masterRef
	 * @param eventRef
	 * @param loanRef
	 * @return
	 * @throws SQLException
	 */
	public static int pushCustomizationProcedure(String masterRef, String eventRef, String actionCode)
			throws SQLException {

		logger.debug("Entering into pushCustomizationProcedure Documents Procedure call");
		logger.debug("MasterRef : " + masterRef + "\t EventRef : " + eventRef + "\t ActionCode : " + actionCode);
		bankRequest = "ETT_CUSTOMIZATION_PKG.ETT_EXE_CUSTOMIZATION(3)";
		int insertedRowCount = 0;
		ResultSet aResultSet = null;
		Connection aConnection = null;
		CallableStatement aCallableStatement = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			if (aConnection != null) {

				// String procedureQuery = "{call
				// ETT_CUSTOMIZATION_PKG(?,?,?)}";
				String procedureQuery = "{call ETT_CUSTOMIZATION_PKG.ETT_EXE_CUSTOMIZATION(?,?,?)}";
				aCallableStatement = aConnection.prepareCall(procedureQuery);
				aCallableStatement.setString(1, masterRef);
				aCallableStatement.setString(2, eventRef);
				aCallableStatement.setString(3, actionCode);

				insertedRowCount = aCallableStatement.executeUpdate();
				bankResponse = "SUCCEEDED";
				logger.debug(insertedRowCount + " ETT_CUSTOMIZATION_PKG called successfully");
			}
		} catch (Exception ex) {
			logger.error("ETT_CUSTOMIZATION_PKG Exception! " + ex.getMessage());
			errorMsg = ex.getMessage();
			bankResponse = "FAILED";
			ex.printStackTrace();

		} finally {
			DatabaseUtility.surrenderCallableConnection(aConnection, aCallableStatement, aResultSet);
		}
		return insertedRowCount;
	}

	/**
	 * 
	 * @param bankResponse
	 *            {@code allows object is }{@link String}
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws XPathExpressionException
	 * @throws Exception
	 */
	private String getTIResponseFromBankResponse(String status)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		InputStream anInputStream = null;
		try {
			anInputStream = GatewayExtCustProcessor.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.GATEWAY_DOCUMENTS_TI_RESPONSE_TEMPLATE);
			String tiResponseXMLTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			// TODO Mapping based on condition check
			tokens.put("operation", operation);
			tokens.put("status", status);
			tokens.put("correlationId", correlationId);

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(tiResponseXMLTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();
			// TODO Test
			result = CSVToMapping.RemoveEmptyTagXML(result);
			// logger.debug("Removed empty tag : " + result);

		} catch (Exception e) {
			logger.error("Exception while generating TIResponse..! " + e.getMessage());
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		// logger.debug("Before giving bank to tiResponse : " + result);
		return result;

	}

	public static void main(String a[]) throws Exception {

		// String requestXML =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\preship01.xml");
		// String requestXML =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\exxcust_04.xml");

		// String requestXML =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\EXTCUST.XML");
		GatewayExtCustProcessor as = new GatewayExtCustProcessor();
		// as.process(requestXML);

		// pushToExtDataBase("0958FCA170200701", "CRE001", "0958PCF170200214");

		// as.preshipment("0958OCF170201064", "FEC001");

		as.updateAtdocStatus("0958ELF170201277", "POD001", "0958PCF170200270", "XML", "FAILED");

		// logger.debug(as.getBankRequestFromTiRequest(requestXML));

		// String responseXML = as.process(requestXML);
		// logger.debug("GatewayDocumentsAdaptee : " + responseXML);

		// int s = pushToExtDataBase("123", "12", "8777");
		// logger.debug("s " + s);
	}

}
