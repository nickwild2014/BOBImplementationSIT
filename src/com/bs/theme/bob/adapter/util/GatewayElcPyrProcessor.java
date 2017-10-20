package com.bs.theme.bob.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.adapter.adaptee.GatewayDocumentsAdaptee;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.GatewayTfelcpyrXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class GatewayElcPyrProcessor {

	private final static Logger logger = Logger.getLogger(GatewayElcPyrProcessor.class.getName());

	private String zone = "";
	private String branch = "";
	private String service = "";
	private String operation = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public static void main(String[] args) throws Exception {

		GatewayElcPyrProcessor an = new GatewayElcPyrProcessor();
		// String tiGatewayRequestXml = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task
		// GatewayLCBD\\Gateway.tfelcpyr.tirequest-2.xml");

		// String tiGatewayRequestXml = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task
		// GatewayLCBD\\GATEWAY.0958ILD160100216.XML");

		// String tiGatewayRequestXml = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task
		// GatewayLCBD\\xml\\0958ILD160100217.Gatewaymessage.xml");

		// an.processGatewayXML(tiGatewayRequestXml);

		an.getELCMasterDetailsList("ELD170200956D001");

		// an.getPayEventDetails("0958ELD160100109");
	}

	/**
	 * @since 2017-JAN-13
	 * @param tiGatewayRequestXml
	 * @return
	 */
	public String processGatewayXML(String tiGatewayRequestXml) {

		String errorMessage = "";
		String ejbStatus = "FAILED";
		try {
			tiRequest = tiGatewayRequestXml;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("\n\nAvailBal TI Request:\n" + tiRequest);

			// get gateway xml data
			Map<String, String> elcMapList = getMasterData(tiGatewayRequestXml);

			// generate TI Request - XML
			String tfElcPyrXMLrequest = generateTFELCPYRxml(elcMapList);
			bankRequest = tfElcPyrXMLrequest;
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("TFELCPYR Bank Request:\n" + bankRequest);

			// Push into EJB Client
			String tfElcPyrXMLresponse = TIPlusEJBClient.process(tfElcPyrXMLrequest);
			bankResponse = tfElcPyrXMLresponse;
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("TFELCPYR Bank Response:\n" + bankResponse);

			ejbStatus = XPathParsing.getValue(tfElcPyrXMLresponse, GatewayTfelcpyrXpath.EJB_STATUS);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();

			// status = bankRequest;
			tiResponse = getTIResponseFromBankResponse(ejbStatus);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GatewayDocuments TI Response:\n" + tiResponse);

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
			errorMessage = e.getMessage();
			ejbStatus = "FAILED";

		} finally {
			ServiceLogging.pushLogData(service, operation, zone, branch, sourceSystem, targetSystem, masterReference,
					eventReference, ejbStatus, tiRequest, tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime,
					bankResTime, tiResTime, "", "", "", "", false, "0", errorMessage);
		}

		return tiResponse;
	}

	public Map<String, String> getMasterData(String tirequestXML) {

		Map<String, String> elcMapList = new HashMap<String, String>();

		try {
			service = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.SERVICE);
			operation = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.OPERATION);
			zone = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.ZONE);
			targetSystem = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.TARGETSYSTEM);
			correlationId = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.CORRELATIONID);
			String masterRef = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.MASTERREFERENCE);
			String eventRef = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.EVENTREFERENCE);
			String behalfOfBranch = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.BEHALFOFBRANCH);
			String postingAmount = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.AMOUNT);
			String postingCurrency = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.CURRENCY);
			String customer = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.CUSTOMER);
			String ourReference = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.OURREFERENCE);
			String theirreference = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.THEIRREFERENCE);
			String presentersReference = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.PRESENTERSREFERENCE);
			String claimId = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.CLAIMID);
			String productcode = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.PRODUCTCODE);
			String productname = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.PRODUCTNAME);
			String country = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.COUNTRY);
			String applicantILC = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.APPLICANT);
			String beneficiaryILC = XPathParsing.getValue(tirequestXML, GatewayTfelcpyrXpath.BENEFICIARY);

			// logger.debug("Milestone 10 : " + service + operation +
			// masterRef);
			branch = behalfOfBranch;
			eventReference = eventRef;
			masterReference = masterRef;
			elcMapList.put("service", service);
			elcMapList.put("operation", operation);
			elcMapList.put("masterReference", masterRef);
			elcMapList.put("eventReference", eventRef);

			elcMapList.put("behalfOfBranch", behalfOfBranch);
			elcMapList.put("postingAmount", postingAmount);
			elcMapList.put("postingCurrency", postingCurrency);
			elcMapList.put("customer", customer);
			elcMapList.put("ourReference", ourReference);
			elcMapList.put("theirReference", theirreference);
			elcMapList.put("presentersReference", presentersReference);
			elcMapList.put("claimId", claimId);
			elcMapList.put("productcode", productcode);
			elcMapList.put("productname", productname);
			elcMapList.put("country", country);
			// 2017-06-30
			elcMapList.put("applicantILC", applicantILC);
			elcMapList.put("beneficiaryILC", beneficiaryILC);

			Map<String, String> elcmap = getELCMasterDetailsList(presentersReference);
			String elccustomer = elcmap.get("elccustomer");
			// logger.debug(elccustomer);
			String paycount = elcmap.get("paycount");
			// logger.debug(paycount);
			String elcBehalfOfBranch = elcmap.get("behalfBranch");
			String elcMaster = elcmap.get("elcmaster");
			String issuingBank = elcmap.get("issuingBank");

			// String paycount = getPayEventDetails(presentersReference);
			elcMapList.put("elccustomer", elccustomer);
			elcMapList.put("paycount", paycount);
			elcMapList.put("elcBehalfOfBranch", elcBehalfOfBranch);
			elcMapList.put("elcMaster", elcMaster);
			elcMapList.put("elcissuingBank", issuingBank);

			logger.debug("elcMapList : " + elcMapList);

		} catch (Exception e) {
			logger.error("Gateway XML Parsing Exception..!" + e.getMessage());

		}

		return elcMapList;
	}

	public String generateTFELCPYRxml(Map<String, String> mapList) {

		String tfElcPyrXML = "";
		InputStream anInputStream = null;
		try {
			anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_TFELCPYR_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			// Map<String, String> event = getMasterDetails(masterref) ;

			String requestId = ThemeBridgeUtil.randomCorrelationId();
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("CredentialsName", "SUPERVISOR");
			tokens.put("CorrelationId", requestId);

			tokens.put("Branch", mapList.get("elcBehalfOfBranch")); // Q

			// ILC Applicant CRN
			tokens.put("Customer", mapList.get("customer"));// Q
			// tokens.put("Customer", mapList.get("elccustomer"));

			tokens.put("Product", "ELC");// Q, mapList.get("productcode")
			tokens.put("Event", "PAY");// always

			// mapList.get("masterReference")
			tokens.put("OurReference", mapList.get("ourReference"));// Q,

			tokens.put("TheirReference", mapList.get("theirReference"));// Q
			tokens.put("Team", "All Trade Team");// Q
			tokens.put("BehalfOfBranch", mapList.get("elcBehalfOfBranch"));// Q,
																			// behalfOfBranch

			// if (mapList.get("paycount") != null)
			// tokens.put("EventReference", "POD" + mapList.get("paycount"));
			// else
			tokens.put("EventReference", "");

			tokens.put("Step", "");
			// tokens.put("Step", "Input");

			tokens.put("MasterPrefix", "");
			tokens.put("MasterSerialNumber", "");

			tokens.put("DocID", "");// always
			tokens.put("DMSID", "");// always
			tokens.put("DocType", "");// always
			tokens.put("Description", "");// always
			tokens.put("ReceivedDate", "");// always
			tokens.put("ReceivedTime", "");// always

			tokens.put("MessageData", "");// always
			tokens.put("MessageDescription", "");// always
			tokens.put("MessageInfo", "");// always
			tokens.put("MessageNumber", "");// always
			tokens.put("Actioned", "");// always
			tokens.put("ID", "");// always
			tokens.put("AttachType", "");// always
			tokens.put("DocType", "");// always
			tokens.put("Description", "");// always
			tokens.put("DataStream", "");// always
			tokens.put("MimeType", "");// always

			tokens.put("MasterRef", mapList.get("elcMaster"));// Q,presentersReference
			// mapList.get("masterReference")
			// tokens.put("ClaimId", mapList.get("claimId"));// Q
			tokens.put("ClaimId", "");// Q

			// ILC Beneficiary CRN
			// tokens.put("SenderCustomer", mapList.get("elcissuingBank"));
			tokens.put("SenderCustomer", mapList.get("beneficiaryILC"));// always

			tokens.put("SenderNameAddress", "");// always
			tokens.put("SenderSwiftAddress", "");// always
			tokens.put("SenderReference", "");// always
			tokens.put("SenderContact", "");// always
			tokens.put("SenderZipCode", "");// always
			tokens.put("SenderTelephone", "");// always
			tokens.put("SenderFaxNumber", "");// always
			tokens.put("SenderTelexNumber", "");// always
			tokens.put("SenderCountry", mapList.get("country"));// Q
			tokens.put("SenderTelexAnswerBack", "");// always
			tokens.put("SenderEmail", "");// always

			tokens.put("ResponseType", "");// always
			tokens.put("ResponseDetails", "");// always
			tokens.put("RefusalDetails", "");// always
			tokens.put("eBankMasterRef", "");// always
			tokens.put("eBankEventRef", "");// always
			tokens.put("Name", "");// always
			tokens.put("Value", "");// always

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			tfElcPyrXML = reader.toString();
			reader.close();

			// TODO
			tfElcPyrXML = CSVToMapping.RemoveEmptyTagXML(tfElcPyrXML);
			// logger.debug("Removed empty tag : " + tfElcPyrXML);

		} catch (Exception e) {
			logger.error("TFELCPYR Exceptions! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		return tfElcPyrXML;

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
			anInputStream = GatewayDocumentsAdaptee.class.getClassLoader()
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
			logger.error("Exceptions while TIResponse " + e.getMessage());
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		return result;

	}

	public Map<String, String> getELCMasterDetailsList(String elcPresentersReference) {

		boolean result = true;
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		Map<String, String> mapList = new HashMap<String, String>();
		try {
			con = DatabaseUtility.getTizoneConnection();
			if (con != null) {

				// String query = "SELECT trim(MASTER_REF) as MASTER_REF,
				// trim(NPRCUSTMNM) as NPRCUSTMNM, mas.WORKGROUP,
				// lpad(max(bev.REFNO_SERL)+1, 3, 0) as PAYCOUNT FROM MASTER
				// mas, BASEEVENT bev WHERE trim(MASTER_REF) = ? "
				// + " and mas.KEY97 = bev.MASTER_KEY and bev.REFNO_PFIX = 'POD'
				// group by MASTER_REF, NPRCUSTMNM, mas.WORKGROUP ";

				// 2017-07-11
				// String query = "SELECT trim(MASTER_REF) as MASTER_REF,
				// trim(Bhalf_Brn) as Bhalf_Brn, trim(NPRCUSTMNM) as NPRCUSTMNM,
				// trim(pricustmnm) as issuing_bank, trim(mas.WORKGROUP) as
				// WORKGROUP, lpad(max(bev.REFNO_SERL)+1, 3, 0) as PAYCOUNT "
				// + " FROM MASTER mas, BASEEVENT bev , EXTEVENT exte WHERE
				// mas.KEY97 = bev.MASTER_KEY AND bev.KEY97 = exte.EVENT "
				// // + " and bev.REFNO_PFIX = 'POD' "
				// + " and exte.BLLREFNO = ? "
				// // + " and trim(MASTER_REF) = '0958ELD170201031' "
				// + " group by MASTER_REF, BHALF_BRN, NPRCUSTMNM, PRICUSTMNM,
				// mas.WORKGROUP ";

				// 2017-07-11
				String query = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(MAS.INPUT_BRN) AS INPUTBRANCH, TRIM(MAS.BHALF_BRN) AS BHALF_BRN, TRIM(MAS.NPRCUSTMNM) AS NPRCUSTMNM, TRIM(MAS.PRICUSTMNM) AS ISSUING_BANK, TRIM(MAS.WORKGROUP) AS WORKGROUP, LPAD(MAX(BEV.REFNO_SERL)+1, 3, 0) AS PAYCOUNT  FROM MASTER MAS, BASEEVENT BEV , EXTEVENT EXTE "
						+ " WHERE MAS.KEY97 =  BEV.MASTER_KEY AND BEV.KEY97 = EXTE.EVENT AND EXTE.BLLREFNO = ? "
						// + " and bev.REFNO_PFIX = 'POD' "
						+ " group by TRIM(MAS.MASTER_REF), TRIM(MAS.INPUT_BRN), TRIM(MAS.BHALF_BRN), TRIM(MAS.NPRCUSTMNM), TRIM(MAS.PRICUSTMNM), TRIM(MAS.WORKGROUP) ";

				logger.debug("ELC Master : " + query);
				ps = con.prepareStatement(query);
				ps.setString(1, elcPresentersReference);
				rs = ps.executeQuery();

				while (rs.next()) {
					mapList.put("elcmaster", rs.getString("MASTER_REF"));
					mapList.put("behalfBranch", rs.getString("BHALF_BRN"));
					mapList.put("issuingBank", rs.getString("ISSUING_BANK"));
					mapList.put("team", rs.getString("WORKGROUP"));
					mapList.put("eventcount", rs.getString("PAYCOUNT"));
					mapList.put("elccustomer", rs.getString("NPRCUSTMNM"));
					mapList.put("paycount", rs.getString("PAYCOUNT"));
				}
				logger.debug("ELC mapList " + mapList);

			} else {
				// logger.debug("");
			}
		} catch (Exception ex) {
			logger.error("The Exception is :" + ex.getMessage());
			ex.printStackTrace();
			result = false;

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}

		return mapList;
	}

	public String getPayEventDetails(String masterref) {

		// logger.debug("Entering into the BackofficeUtil.eodposting method" +
		// masterref);

		boolean result = true;
		int payEventCount = 000;
		Connection con = null;
		ResultSet rs = null;
		String payEventCountStr = "";
		PreparedStatement ps = null;

		try {
			con = DatabaseUtility.getTizoneConnection();
			if (con != null) {
				String query = "SELECT trim(MASTER_REF) as MASTER_REF, mas.WORKGROUP, lpad(max(bev.REFNO_SERL)+1, 3, 0) as PAYCOUNT FROM MASTER mas, BASEEVENT bev WHERE trim(MASTER_REF) = ? "
						+ " and mas.KEY97 =  bev.MASTER_KEY and bev.REFNO_PFIX = 'POD' group by MASTER_REF, mas.WORKGROUP ";
				// String query = "SELECT trim(MASTER_REF) as MASTER_REF,
				// mas.WORKGROUP FROM MASTER mas, BASEEVENT bev WHERE
				// trim(MASTER_REF) = ? ";

				logger.debug("ELC Master : " + query);

				ps = con.prepareStatement(query);
				ps.setString(1, masterref);
				rs = ps.executeQuery();
				while (rs.next()) {
					// payEventCount = rs.getInt("PAYCOUNT");
					payEventCountStr = rs.getString("PAYCOUNT");
				}
				// logger.debug("ALREADY CREATED : " + payEventCountStr);

				// payEventCount = payEventCount + 11
				// logger.debug("TO BE CREATE : " + payEventCountStr);

			}
		} catch (Exception ex) {
			logger.error("The Exception is :" + ex.getMessage());
			ex.printStackTrace();
			result = false;

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}

		return payEventCountStr;
	}

	public boolean eodpostingLogging(String masterref) {

		// logger.debug("Entering into the BackofficeUtil.eodposting method");

		boolean result = true;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseUtility.getThemebridgeConnection();
			if (con != null) {
				String query = "SELECT m.input_brn as Input_branch, m.bhalf_brn as  behalf_Branch,m.pri_ref as Our_Reference,"
						+ " m.nprcustmnm as Customer,m.workgroup as Team,m.master_ref as Master_reference,bv.their_ref as Their_Reference,"
						+ " pad.country as Country,bv.amount/power(10,C82.C8CED) as Amount,bv.ccy as Currency,ex.code79 as Product,"
						+ " concat((bv.refno_pfix),'00') || (TO_CHAR(bv.REFNO_SERL)) as claimID  "
						+ " FROM baseevent bv,extevent e,master m,partydtls pad,exempl30 ex,c8pf c82 "
						+ " WHERE e.event=bv.key97 AND bv.master_key = m.key97  AND BV.STATUS ='c' "
						+ " AND m.primarycus= pad.key97 AND m.exemplar=ex.key97 AND trim(C82.C8CCY) = trim(bv.ccy)"
						+ " AND trim(e.BLLREFNO) = '" + masterref + "'";
				;

				logger.debug("The query is " + query);
				ps = con.prepareStatement(query);

				ps.setString(1, "BackOffice");

				int insertedRows = ps.executeUpdate();
				if (insertedRows > 0) {
					logger.debug(insertedRows + " Row inserted successfully!");
				} else {
					logger.debug("EOD row inserted Failed");
				}
			}
		} catch (Exception ex) {
			logger.error("The Exception is :" + ex.getMessage());
			ex.printStackTrace();
			result = false;

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, null);
		}

		return result;
	}

}
