package com.bs.theme.bob.adapter.adaptee;

//import static com.bs.theme.kmbl.template.util.KotakConstant.OPERATION_EXPOSURERESERVATION;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_LIMIT;
import static com.bs.theme.bob.template.util.KotakConstant.TARGET_SYSTEM;
import static com.bs.theme.bob.template.util.KotakConstant.ZONE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleHttpClient;
import com.bs.theme.bob.adapter.email.EmailAlertServiceFailureUtil;
import com.bs.theme.bob.adapter.util.LimitServicesUtil;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.LimitExposureXpath;
import com.bs.themebridge.xpath.LimitReservationsXpath;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.messages.Exposure;

// com.misys.tiplus2.apps.ti.service.messages.EQ3Exposure
// SIT

/**
 * End system communication implementation for Limit Exposure services is
 * handled in this class.
 * 
 * @since 2016-DEC-16
 * @version v.1.0.1
 * @author <b><i><font color=blue>Prasath Ravichandran</font></i></b>,
 *         <font color=green>Software Analyst, </font>
 *         <font color=violet>Bluescope</font>
 */
public class LimitExposureAdaptee {

	private final static Logger logger = Logger.getLogger(LimitExposureAdaptee.class.getName());

	private String status = "";
	private String branch = "N/A";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	// private String creditDebitExposureStatus = "";

	public String processBankRequestDetails(List<Exposure> anExposureList, String requestXML, String masterRef,
			String eventRef) {

		logger.debug(" ************ Limit.Exposure adaptee process started ************ ");

		String errorMsg = "";
		String expoReqStatus = "SUCCEEDED";
		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
			// logger.debug("\nLimit Exposure TI Request :- " + requestXML);

			masterReference = XPathParsing.getValue(requestXML, LimitExposureXpath.MasterReferenceXPath);
			eventReference = XPathParsing.getValue(requestXML, LimitExposureXpath.EventReferenceXPath);
			correlationId = XPathParsing.getValue(requestXML, LimitExposureXpath.correlationIdXPath);

			/** Calling Finacle **/
			// String tiRespXML = exposureLimitRouter(anExposureList, masterRef,
			// eventRef);
			// logger.debug("ExposuereRespXML : " + tiRespXML);
			// expoReqStatus = XPathParsing.getValue(requestXML,
			// "/ServiceResponse/ResponseHeader/Status");

			expoReqStatus = exposureLimitRouter(anExposureList, masterRef, eventRef);
			logger.debug("ExposuereRespStatus : " + expoReqStatus);

		} catch (Exception e) {
			expoReqStatus = "FAILED";
			errorMsg = e.getMessage();
			logger.error("Exceptions! Limit exposure..! " + e.getMessage());

		} finally {
			logger.debug("Exposure status " + expoReqStatus);
			if (expoReqStatus.equals("FAILED"))
				EmailAlertServiceFailureUtil.sendFailureAlertMail("Limit", "Exposure", masterReference, eventReference,
						sourceSystem, targetSystem);

		}
		logger.debug("ExpoReqStatus(return) : " + expoReqStatus);
		logger.debug(" ************ Limit.Exposure adaptee process ended ************ ");
		return expoReqStatus;
	}

	/**
	 * 
	 * @param anExposureList
	 *            {@code allows } {@link String}
	 * @param masterRef
	 *            {@code allows } {@link String}
	 * @param eventRef
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String exposureLimitRouter(List<Exposure> anExposureList, String masterRef, String eventRef) {

		String tiResponseXML = "";
		String expoReqStatus = "PROCESSED";
		try {
			logger.debug("AnExposureList : " + anExposureList.size());
			if (anExposureList != null && anExposureList.size() > 0) {
				Iterator<Exposure> sourceListItr = anExposureList.iterator();
				while (sourceListItr.hasNext()) {
					Exposure anExposure = (Exposure) sourceListItr.next();

					if (!anExposure.getFacilityIdentifier().equalsIgnoreCase("*NOFACILITYID*")) {
						logger.debug("IF Facility ID " + anExposure.getFacilityIdentifier());

							logger.debug("Regular exposure Debit MS02");
							// expoReqStatus = "PROCESSED";
							tiResponseXML = exposureLimitReservations(anExposure);
							expoReqStatus = XPathParsing.getValue(tiResponseXML,
									"/ServiceResponse/ResponseHeader/Status");

					} else {
						operation = "Exposure";
						// expoReqStatus = "PROCESSED";
						tiResponseXML = getErrorTIResponse("No FacilityID [IM]");
						expoReqStatus = XPathParsing.getValue(tiResponseXML, "/ServiceResponse/ResponseHeader/Status");
						logger.debug("ELSE Facility ID " + anExposure.getFacilityIdentifier());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception!!! " + e.getMessage());
			e.printStackTrace();
		}
		return expoReqStatus;
	}

	/**
	 * <p>
	 * 1-B Exposure Reservations NEW API
	 * </p>
	 * 
	 * @since 2017-FEB-15
	 * @version v1.0
	 * 
	 * @param anExposure
	 * @return
	 */
	public String exposureLimitReservations(Exposure anExposure) {

		bankRequest = "";
		String bankResp = "";
		String rate = "";
		String branch = "";
		String eventRef = "";
		String masterRef = "";
		String narrative1 = "";
		String productType = "";
		String subProductType = "";
		String errorMsg = null;
		double loggingRate = 0;
		String titrxnamount = "";
		String liabilityFlag = "";
		String txnCrncyCode = "";
		String loggingTranCcy = "";
		String exposureINRAmount = "";
		String reversaltiResponse = "";
		String reservationBankRequest = "";
		String reversalBankResponse = "";
		String reservationstiResponseStatus = "";
		String operationProcess = "ExposureReservation";

		try {
			logger.debug("Milestone Dr Reservations");
			masterRef = anExposure.getMasterReference();
			eventRef = anExposure.getEventReference();
			/** UAT **/
			// narrative1 = anExposure.getCustomer();
			/** SIT **/
			narrative1 = anExposure.getRelatedParty();
			branch = anExposure.getBehalfOfBranch();
			txnCrncyCode = anExposure.getCurrency();
			productType = anExposure.getProduct();
			subProductType = anExposure.getProductSubType();
			String facilityIdentifier = anExposure.getFacilityIdentifier();
			XMLGregorianCalendar valueDateGregorian = anExposure.getValueDate();
			java.sql.Date valueSqlDates = DateTimeUtil.getSqlDateByXMLGregorianCalendar(valueDateGregorian);
			// String transactionID = ThemeBridgeUtil.randomCorrelationId()+
			// anExposure.getTransactionId();
			String transactionID = anExposure.getTransactionId() + anExposure.getTransactionSeqNo();

			// BankRequest_LimitExposureReservations_Template.xml
			InputStream anInputStream = LimitExposureAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.LIMIT_EXPOSURE_RESERVATIONS_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			String[] parts = facilityIdentifier.split("/");
			String limitPrefix = parts[0].trim(); // TODO
			String limitSuffix = parts[1].trim(); // TODO

			long titrxnamountLong = anExposure.getAmount();
			String titrxnamountStr = String.valueOf(titrxnamountLong);
			titrxnamount = LimitServicesUtil.getTransactionAmount(titrxnamountStr, txnCrncyCode);
			// logger.debug("XML tranAmount :-) " + titrxnamount);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("RequestUUID", transactionID);
			tokens.put("ChannelId", KotakConstant.CHANNELID);
			//tokens.put("BankId", KotakConstant.BANKID);
			tokens.put("ServiceRequestVersion", KotakConstant.SERVICEREQUESTVERSION);
			tokens.put("MessageDateTime", DateTimeUtil.getDateAsEndSystemFormat());

			logger.debug("Milestone 04");

			// tokens.put("REQUEST_ID", masterRef);
			//tokens.put("REQUEST_ID", transactionID);
			tokens.put("LIMIT_PREFIX", limitPrefix);
			tokens.put("LIMIT_SUFFIX", limitSuffix);
			tokens.put("FUND_LIAB", titrxnamount);
			tokens.put("txnCrncyCode", txnCrncyCode);
			//tokens.put("UML", "0.0");
			//tokens.put("REMARKS", masterRef);// TODO

			//boolean isEligible = isFundedLiability(productType, subProductType);
			//logger.debug("isEligible : " + isEligible);

			// logger.debug("Milestone 05");
			// logger.debug("CCY : " + txnCrncyCode);
			//double fcySpotRate = 0;
//			if (!txnCrncyCode.equals("INR")) {
//				fcySpotRate = LimitServicesUtil.getSpotRateFCY(txnCrncyCode);
//				String inrEquivalentAmount = LimitServicesUtil.getINRAmount(titrxnamount, txnCrncyCode, fcySpotRate);
//				if (isEligible) {
//					liabilityFlag = "FL";
//					tokens.put("FUND_LIAB", inrEquivalentAmount);
//					tokens.put("NON_FUND_LIAB", "0.0");
//				} else {
//					liabilityFlag = "NFL";
//					tokens.put("FUND_LIAB", "0.0");
//					tokens.put("NON_FUND_LIAB", inrEquivalentAmount);
//				}
//				exposureINRAmount = inrEquivalentAmount;
//				loggingRate = fcySpotRate;
//				rate = Double.toString(fcySpotRate);
//
//			} else {
//				logger.debug("INR currency");
//				if (isEligible) {
//					liabilityFlag = "FL";
//					tokens.put("FUND_LIAB", titrxnamount);
//					tokens.put("NON_FUND_LIAB", "0.0");
//				} else {
//					liabilityFlag = "NFL";
//					tokens.put("FUND_LIAB", "0.0");
//					tokens.put("NON_FUND_LIAB", titrxnamount);
//				}
//				exposureINRAmount = titrxnamount;
//				loggingRate = 1;
//				rate = Double.toString(1);
//				// list.put("amount", tranAmount);
//				// list.put("fcyRate", "1.0");
//			}
			// logger.debug("Milestone 08 b");
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			reservationBankRequest = reader.toString();
			reader.close();

			// logger.debug("Milestone 09");
			bankReqTime = DateTimeUtil.getTimestamp();
			bankRequest = reservationBankRequest;
			logger.debug("Limit Exposure Reservations BankRequest :- " + reservationBankRequest);

			// logger.debug("Milestone 10");
			if (reservationBankRequest!=null && !reservationBankRequest.isEmpty()) {
				reversalBankResponse = getBankResponseFromBankRequest(reservationBankRequest);
				bankResponse = reversalBankResponse;
				bankResTime = DateTimeUtil.getTimestamp();
				logger.debug("Limit Exposure Reservations BankResponse :- " + reversalBankResponse);

				if (reversalBankResponse!=null && !reversalBankResponse.isEmpty()) {
					reversaltiResponse = getTIResponseFromBankResponse(reversalBankResponse);
					tiResponse = reversaltiResponse;
					tiResTime = DateTimeUtil.getTimestamp();
					logger.debug("Limit Exposure Reservations TIResponse :- " + reversaltiResponse);
					reservationstiResponseStatus = XPathParsing.getValue(tiResponse,
							"/ServiceResponse/ResponseHeader/Status");
					if (reservationstiResponseStatus.equals("SUCCEEDED")) {
						logger.debug("Limit Exposure Reservations TIResponse status :- " + reversaltiResponse);

						boolean limitValue = limitExposureLogging(masterRef, eventRef, limitPrefix, limitSuffix,
								masterRef, titrxnamount, txnCrncyCode, rate, exposureINRAmount, "INR", "D",
								valueSqlDates, liabilityFlag, null);

						// CustomerLimitDetailsLogging.pushCustomerLimitData(masterReference,
						// eventReference,
						// anExposure.getReservationIdentifier(),
						// anExposure.getFacilityIdentifier(), tranAmount,
						// anExposure.getCurrency(), rate, loggingTranAmount,
						// "ExposureReversal", "ERES");
					}

				} else {
					reservationstiResponseStatus = "FAILED";
					logger.error("Bank Response is empty..!");
					tiResponse = getErrorTIResponse("HTTP - 404 Finacle Host Unavailable [IM]");
				}

			} else {
				reservationstiResponseStatus = "FAILED";
				logger.error("BankRequest is empty..!");
				tiResponse = getErrorTIResponse("Unexpected error while parsing [IM]");
			}

		} catch (Exception e) {
			errorMsg = "Nothing to do for exposure debit leg / Hard Block Reservation not required..!";

		} finally {
			ServiceLogging.pushLogData(SERVICE_LIMIT, operationProcess, ZONE, branch, ZONE, TARGET_SYSTEM, masterRef,
					eventRef, reservationstiResponseStatus, tiRequest, reversaltiResponse, reservationBankRequest,
					reversalBankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", narrative1, "", false,
					"0", errorMsg);
		}
		return bankResp;
	}

	
	/**
	 * <p>
	 * 2-A Exposure Reservations soft Block release NEW API
	 * </p>
	 * 
	 * @since 2017-FEB-15
	 * @version v1.0
	 * 
	 * @param anExposure
	 * @return
	 */

	/**
	 * 
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventRef
	 *            {@code allows }{@link String}
	 * @param limitPrefix
	 *            {@code allows }{@link String}
	 * @param limitSuffix
	 *            {@code allows }{@link String}
	 * @param reservationIdentifier
	 *            {@code allows }{@link String}
	 * @param loggingTranAmount
	 *            {@code allows }{@link String}
	 * @param txnCrncyCode
	 *            {@code allows }{@link String}
	 * @param rate
	 *            {@code allows }{@link String}
	 * @param exposureAmount
	 *            {@code allows }{@link long}
	 * @param drCrFlag
	 *            {@code allows }{@link String}
	 * @param valueDateGregorian
	 *            {@code allows }{@link XMLGregorianCalendar}
	 * @return
	 */
	public boolean limitExposureLogging(String masterRef, String eventRef, String limitPrefix, String limitSuffix,
			String reservationIdentifier, String loggingTranAmount, String txnCrncyCode, String rate,
			String exposureAmount, String exposureCcy, String drCrFlag, java.sql.Date valueDate, String liabilityFlag,
			String narrative) {

		boolean result = true;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {

				String query = "INSERT INTO limitExposure(ID, MASTERREFERENCE, EVENTREFERENCE, LIMITPREFIX, LIMITSUFFIX, RESERVATIONID, "
						+ "TRXNAMOUNT, TRXNCURRENCY, RATE, EXPOSUREAMOUNT, EXPOSURECURRENCY, EXPOSUREFLAG, VALUEDATE, PROCESSTIME, LIABTYPE, NARRATIVE1 )"
						+ " VALUES (LIMITEXPOSURE_SEQ.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

				ps = con.prepareStatement(query);
				ps.setString(1, masterRef);
				ps.setString(2, eventRef);
				ps.setString(3, limitPrefix);
				ps.setString(4, limitSuffix);
				ps.setString(5, reservationIdentifier);

				// logger.debug(">>> \n\n" + new
				// BigDecimal(loggingTranAmount));
				// ps.setBigDecimal(6, new BigDecimal(loggingTranAmount));
				// loggingTranAmount = "55555555555551.5";
				// loggingTranAmount = "98999999999999.22556";

				double d = Double.parseDouble(loggingTranAmount);
				ps.setDouble(6, d); // exposure

				ps.setString(7, txnCrncyCode);
				ps.setString(8, rate);
				// TODO DATE CONVESTION
				// ps.setLong(9, exposureAmount);
				ps.setBigDecimal(9, new BigDecimal(exposureAmount));
				ps.setString(10, exposureCcy);
				ps.setString(11, drCrFlag);// Flag

				// if (valueDateGregorian != null) {
				// java.sql.Date valueSqlDates =
				// DateTimeUtil.getSqlDateByXMLGregorianCalendar(valueDateGregorian);
				// ps.setDate(11, valueSqlDates);// valuedate
				// } else {
				ps.setDate(12, valueDate);// valuedate
				// }

				// ps.setTimestamp(13, ThemeBridgeUtil.GetCurrentTimeStamp());
				ps.setTimestamp(13, DateTimeUtil.GetLocalTimeStamp());
				ps.setString(14, liabilityFlag);
				ps.setString(15, narrative);// NARRATIVE1

				int insertedRows = ps.executeUpdate();

				if (insertedRows > 0) {
					logger.debug(insertedRows + "(Limit Exposure) Row inserted successfully!!! ");
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
	 * 
	 * @param productType
	 * @param subProductType
	 * @return
	 */
	public static boolean isFundedLiability(String productType, String subProductType) {

		boolean isEligible = false;

		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		String fundLiabQuery = "SELECT * FROM LOOKUP_FUNDED_LIABILITIES WHERE TRIM(PRODUCTTYPE) = ? AND SUBPRODUCTTYPE = ? ";
		// logger.debug("fundLiabQuery : " + fundLiabQuery);

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(fundLiabQuery);
			aPreparedStatement.setString(1, productType);
			aPreparedStatement.setString(2, subProductType);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				// String productCode = aResultset.getString("PRODUCTTYPE");
				isEligible = true;
			}

		} catch (Exception e) {
			logger.error("Exceptions! while getting Fundliab..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}

		return isEligible;
	}

	/**
	 * 
	 * @param bankRequest
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	private String getBankResponseFromBankRequest(String bankRequest) {

		String result = "";
		try {
			/******* Finacle http client call *******/
			result = FinacleHttpClient.postXML(bankRequest);

		} catch (Exception e) {
			logger.error("Limit ReservationsReversal Finacle exceptions! " + e.getMessage());
			result = "";
		}
		return result;
	}

	public static void main(String a[]) throws Exception {

		LimitExposureAdaptee limitExposureObj = new LimitExposureAdaptee();

		// String tiGatewayRequestXML = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\00_TASK\\task GatewayLCBD\\LCBD
		// 2017-03-01.xml");
		// limitExposureObj.gatewayLcbdLimitReversal("Limit",
		// "ExposureReversalLcbd", tiGatewayRequestXML);

		// getLimitFacility("0958ELD160001173");

		limitExposureObj.limitExposureLogging("masterRef", "eventRef", "limitPrefix", "limitSuffix", "reservation",
				"123.23", "INR", "1.0", "123.23", "INR", "D", null, "R", "narrative");
	}



	/**
	 * 
	 * @param productType
	 * @param subProductType
	 * @return
	 */
	public static String getLimitFacility(String masterRefe) {

		String facilityID = "";
		ResultSet aResultset = null;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		String FundLiabQuery = "SELECT trim(LIMITPREFIX||'/'||LIMITSUFFIX) AS FACILITYID FROM LIMITEXPOSURE WHERE TRIM(MASTERREFERENCE) = ? AND EXPOSUREFLAG = 'D' ";

		try {
			aConnection = DatabaseUtility.getThemebridgeConnection();
			aPreparedStatement = aConnection.prepareStatement(FundLiabQuery);
			aPreparedStatement.setString(1, masterRefe);
			aResultset = aPreparedStatement.executeQuery();
			while (aResultset.next()) {
				facilityID = aResultset.getString("FACILITYID");
			}

		} catch (Exception e) {
			logger.error("Exceptions! while getting FACILITYID..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}

		return facilityID;
	}

	/**
	 * 
	 * @param responseXML
	 *            {@code allows }{@link String}
	 * @return {@code allows } {@link String}
	 */
	private String getTIResponseFromBankResponse(String responseXML) {

		logger.debug("Enterd into buildTIResponseFromBankResponse method 1111 ");

		String result = "";
		String errorMessage = "";
		String tokenReplacedXML = "";

		try {
			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_EXPOSURE_TI_RESPONSE_TEMPLATE);

			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			String hostStatus = XPathParsing.getValue(responseXML, LimitReservationsXpath.HostStatausXpath);
			logger.debug("hostStatus " + hostStatus);
			String exposureStataus = "";
			//String exposureStataus = XPathParsing.getValue(responseXML, LimitReservationsXpath.FacilityStatausXpath);
			//logger.debug("exposureStataus " + exposureStataus);

			errorMessage = getBankResponseErrorMessage(responseXML);

			Map<String, String> tokens = new HashMap<String, String>();
			if (hostStatus.equalsIgnoreCase("FAILURE") ) {
				exposureStataus = "FAILED";
				tokens.put("Status", "FAILED");
				tokens.put("Error", errorMessage + " [IM]");
				tokens.put("Info", "");
				tokens.put("Warning", "");
				tokens.put("CorrelationId", correlationId);
				tokens.put("ResponseHeader", "");

			} else if (hostStatus.equalsIgnoreCase("SUCCESS") ) {
				exposureStataus = "SUCCEEDED";
				tokens.put("Status", "SUCCEEDED");
				tokens.put("Error", "");
				tokens.put("Info", "");
				tokens.put("Warning", "");
				tokens.put("CorrelationId", correlationId);
				tokens.put("ResponseHeader", "");

				// } else {
				// // creditDebitExposureStatus =
				// exposureStataus = "RECEIVED";
				// tokens.put("Status", "RECEIVED");
				// tokens.put("Error", "");
				// tokens.put("Info", "");
				// tokens.put("Warning", "");
				// tokens.put("CorrelationId", correlationId);
				// tokens.put("ResponseHeader", "");
			}

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();

			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeExposure Exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("TIResponse : " + result);
		return result;
	}

	/**
	 * 
	 * @param responseXML
	 *            {@code allows }{@link String}
	 * @return {@code allows } {@link String}
	 */
	private String getErrorTIResponse(String errorMesage) {

		logger.debug("Enterd into buildTIResponseFromBankResponse method 222 ");

		String result = "";
		String tokenReplacedXML = "";

		try {
			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.BACKOFFICE_EXPOSURE_TI_RESPONSE_TEMPLATE);
			String responseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			// exposureStataus = "FAILED";
			tokens.put("Status", "RECEIVED");
			tokens.put("Error", errorMesage);
			tokens.put("Info", "");
			tokens.put("Warning", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("ResponseHeader", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tokenReplacedXML = reader.toString();
			reader.close();

			result = CSVToMapping.RemoveEmptyTagXML(tokenReplacedXML);
			// logger.debug("Result tag removed ti response xml : \n" + result);

		} catch (Exception e) {
			logger.error("BackOfficeExposure Exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		logger.debug("TIResponse : " + result);
		return result;

	}

	/**
	 * 
	 * @param bankResponseXml
	 *            {@code allows }{@link String}
	 * @return {@code returns }{@link String}
	 */
	public static String getBankResponseErrorMessage(String bankResponseXml) {

		String allerrorMessages = "";

		try {
			String scriptResponseException = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FIScriptResponseExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml, LimitReservationsXpath.FIScriptResponseExMsgDescXpath);

			String fiBusinessErrorMsg = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FIBusinessExCodeXpath) + " "
					+ XPathParsing.getValue(bankResponseXml, LimitReservationsXpath.FIBusinessExMsgDescXpath);

			String fiSystemErrorDesc = "";
			String fiSystemErrorCode = XPathParsing.getValue(bankResponseXml,
					LimitReservationsXpath.FISystemExCodeXpath);

			if (fiSystemErrorCode.equalsIgnoreCase("60012")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Service Not Available. Internal Finlistval Error occured";

			} else if (fiSystemErrorCode.equalsIgnoreCase("60023")) {
				fiSystemErrorDesc = fiSystemErrorCode
						+ " System Error in Finacle Core. Could Not Get Response from Server";

			} else if (fiSystemErrorCode.equalsIgnoreCase("60024")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Fatal Error in Finacle Core";

			} else if (fiSystemErrorCode.equalsIgnoreCase("3009")) {
				fiSystemErrorDesc = fiSystemErrorCode + " Runtime error has occured. Internal Finlistval Error";

			} else {
				fiSystemErrorDesc = fiSystemErrorCode;
			}

			allerrorMessages = "Finacle exception " + scriptResponseException + fiBusinessErrorMsg + fiSystemErrorDesc;
			logger.debug("Limit Reservations BankResponse error : " + allerrorMessages);

		} catch (XPathExpressionException e) {
			logger.error("XPathExpressionException! " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.error("SAXException! " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error("IOException! " + e.getMessage());
			e.printStackTrace();
		}

		return allerrorMessages;
	}

	
	/**
	 * 
	 * @param amount
	 *            {@code allows } {@link String}
	 * @param currency
	 *            {@code allows } {@link String}
	 * @return {@code allows } {@link String}
	 */
	public String getTransactionAmount(String amount, String currency) {

		String result = "";

		BigDecimal transAmount = new BigDecimal(amount);

		if (currency.equals("OMR") || currency.equals("BHD") || currency.equals("KWD") || currency.equals("JOD")) {
			result = transAmount.divide(new BigDecimal(1000), 3, RoundingMode.CEILING).toString();
		} else if (currency.equals("JPY")) {
			result = amount;
		} else {
			result = transAmount.divide(new BigDecimal(100), 2, RoundingMode.CEILING).toString();
		}
		return result.trim();
	}

}
