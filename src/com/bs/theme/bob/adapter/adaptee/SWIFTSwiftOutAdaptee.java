package com.bs.theme.bob.adapter.adaptee;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bs.theme.bob.adapter.util.PostingStatusCheck;
import com.bs.theme.bob.adapter.util.SWIFTMessageUtil;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.PostingStagingLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.swift.model.ThemeSwiftModel;
import com.bs.themebridge.swift.parser.ThemeSwiftParser;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.JAXBInstanceInitialiser;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.SWIFTSwiftOutXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.messages.SWOPF;
import com.misys.tiplus2.apps.ti.service.messages.SWOPF.Messages;

/**
 * End system communication implementation for Swift Out services is handled in
 * this class.
 * 
 * @author Bluescope
 * 
 */
public class SWIFTSwiftOutAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(SWIFTSwiftOutAdaptee.class.getName());

	private String service = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String status = "QUEUED";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";

	public SWIFTSwiftOutAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public SWIFTSwiftOutAdaptee() {
	}

	public static void main(String[] args) throws Exception {

		SWIFTSwiftOutAdaptee anSwiftOut = new SWIFTSwiftOutAdaptee();

		// String inputXML = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task-sfms-e-BG\\760SWIFTOUT-SIT.xml");
		String inputXML = ThemeBridgeUtil.readFile("C:\\Users\\KXT51472.KBANK\\Desktop\\SwiftTransferLC.xml");
		anSwiftOut.process(inputXML);

		// Map<String, String> test = getSwiftDetails("23171");
		// logger.debug(test.get("MASTERREF"));
		// logger.debug(test.get("EVENTREF"));
		// logger.debug(test.get("MTTYPE"));
		// logger.debug(test.get("CURRENCY"));
		// logger.debug(test.get("PRODTYPE"));
		// logger.debug(test.get("SUBTYPE"));

		// logger.debug(test.get("IS_EBG"));
	}

	/**
	 * <p>
	 * Process the Swift out Service XML from the TI
	 * </p>
	 * 
	 * @throws Exception
	 */
	public String process(String tirequestXML) throws Exception {

		logger.info(" ************ SWIFT.SwiftOut/SFMSOut/IFN adaptee process started ************ ");
		String stagingResponse = "";
		try {
			tiRequest = tirequestXML;
			logger.debug("SwiftOut TI Request :\n" + tiRequest);

			service = XPathParsing.getValue(tirequestXML, SWIFTSwiftOutXpath.SERVICE);
			operation = XPathParsing.getValue(tirequestXML, SWIFTSwiftOutXpath.OPERATION);
			correlationId = XPathParsing.getValue(tirequestXML, SWIFTSwiftOutXpath.CORRELATION);
			// logger.debug("Milestone 01");

			String sourceMessage = XPathParsing.getValue(tirequestXML, SWIFTSwiftOutXpath.MESSAGE);
			logger.debug(sourceMessage);
			ThemeSwiftParser tsp = new ThemeSwiftParser();
			ThemeSwiftModel tsmBeanObj = tsp.parseSwiftMessage(sourceMessage);

			String masterReferenceNum = tsmBeanObj.getMasterReference();
			masterReference = masterReferenceNum;// TransferLC/Transaction_Master
			logger.debug("SwiftMasterReference : " + masterReferenceNum);

			Map<String, String> swiftDetailsMap = getSwiftDetails(correlationId);
			String masterRef = swiftDetailsMap.get("MASTERREF");
			// masterReference = masterRef; // TransferLC Main master
			String eventRef = swiftDetailsMap.get("EVENTREF");
			eventReference = eventRef;
			String mtType = swiftDetailsMap.get("MTTYPE");
			String trxnccy = swiftDetailsMap.get("CURRENCY");
			String prodType = swiftDetailsMap.get("PRODTYPE");
			String subType = swiftDetailsMap.get("SUBTYPE");
			logger.debug("MasterReference : " + masterReference);
			logger.debug("EventReference : " + eventReference);

			String sfmsFlag = SWIFTMessageUtil.checkinSFMSFlag(prodType, subType, subType);
			logger.debug("is SFMS ? : " + sfmsFlag);
			if (sfmsFlag.equals("YES") && trxnccy.equalsIgnoreCase("INR")) {
				service = "SWIFT";
				operation = "SFMSOut";
			}

			if (mtType.equals("760") || mtType.equals("761") || mtType.equals("767")) {
				String iscovmsg = isValidCOVMsg(correlationId);
				String isebg = isValidEBG(correlationId);

				if (iscovmsg.equals("Y")) {
					status = ThemeBridgeStatusEnum.SUPPRESSED.toString();
					// tiResponse = getDefaultTIResponse("SUPPRESSED");
					stagingResponse = getDefaultTIResponse("SUPPRESSED");

				} else if (iscovmsg.equals("N") && isebg.equals("Y")) {
					// status = ThemeBridgeStatusEnum.QUEUED.toString();
					status = ThemeBridgeStatusEnum.AWAITING.toString();
					// tiResponse = getDefaultTIResponse("AWAITING");
					stagingResponse = getDefaultTIResponse("AWAITING");

				} else if (iscovmsg.equals("N") && isebg.equals("N")) {
					status = ThemeBridgeStatusEnum.QUEUED.toString();
					// tiResponse = getDefaultTIResponse("QUEUED");
					stagingResponse = getDefaultTIResponse("QUEUED");
				}

			} else {
				status = ThemeBridgeStatusEnum.QUEUED.toString();
				stagingResponse = getDefaultTIResponse("QUEUED");
				// tiResponse = getDefaultTIResponse("QUEUED");
			}
			tiResponse = getDefaultTIResponse("SUCCEEDED"); // always to TI
			// logger.debug("SwiftOutAdaptee TI Response :\n" + tiResponse);

		} catch (Exception e) {
			logger.error("SWIFTSwiftOutAdaptee Exception..! " + e.getMessage());
			status = ThemeBridgeStatusEnum.FAILED.toString();
			tiResponse = getDefaultTIResponse("FAILED");
			e.printStackTrace();

		} finally {
			logger.debug("SWIFTOutwrdTIResponse:- " + tiResponse);
			PostingStagingLogging.pushLogData(service, operation, masterReference, eventReference, tiRequest,
					stagingResponse, status);

		}
		logger.info(" ************ SWIFT.SwiftOut/SFMSOut/IFN adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param inputXML
	 * @return
	 */
	public List<String> getSwiftOutMessageList(String inputXML) {

		// RequestHeader reqHeader = new RequestHeader();
		NodeList nodeList;
		Messages bsrList = null;
		JAXBContext jaxbContext = JAXBInstanceInitialiser.getSwiftOutRequestContext();
		Unmarshaller unmarshaller;
		List<String> swiftMsglist = new ArrayList<String>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document document = null;
			dbf.setNamespaceAware(true);
			unmarshaller = jaxbContext.createUnmarshaller();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(inputXML));
			document = builder.parse(inputSource);
			nodeList = document.getElementsByTagNameNS("*", "SwiftOut");
			JAXBElement<SWOPF> bulkServiceJAXB = (JAXBElement<SWOPF>) unmarshaller.unmarshal(nodeList.item(0),
					SWOPF.class);

			SWOPF swiftSourceMsg = bulkServiceJAXB.getValue();
			int swiftMessageSize = swiftSourceMsg.getMessages().getMessage().size();
			logger.debug("SwiftOut message total size : " + swiftMessageSize);

			bsrList = swiftSourceMsg.getMessages();
			swiftMsglist = bsrList.getMessage();

		} catch (Exception e) {
			logger.error("Get SwiftAdaptee <Messages> exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		return swiftMsglist;
	}

	/**
	 * With CCY details
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static Map<String, String> getSwiftDetails(String correlationId) {

		Connection con = null;
		// Statement stmt = null;
		ResultSet res = null;
		PreparedStatement ps = null;
		Map<String, String> swiftDetailsMapList = new HashMap<String, String>();

		// DON't USE this commented Query
		// String eventRefQuery = "SELECT DISTINCT TRIM(MAS.MASTER_REF) AS
		// MASTERREF, TRIM(BEV.REFNO_PFIX)||LPAD(TRIM(BEV.REFNO_SERL),3,'0') AS
		// EVENTREF, TRIM(SW.SWOTYP) AS MTTYPE, TRIM(EVT.GMTCOV) AS IS_EBG,
		// TRIM(MAS.CCY) AS CURRENCY, TRIM(SWORNO) AS CORRELATIONID,
		// TRIM(PRD.CODE79) AS PRODTYPE, TRIM(P.CODE) AS SUBTYPE,
		// TRIM(P.DESCRIP) AS SUBTYPEDESC FROM MASTER MAS, TIDATAITEM TID,
		// EXEMPL30 PRD, PRODTYPE P, BASEEVENT BEV , SWOPF SW, DOCRELITEM DRI,
		// RELITEM REL, EXTEVENT EVT WHERE MAS.EXEMPLAR = PRD.KEY97 AND
		// MAS.KEY97 = TID.MASTER_KEY AND MAS.KEY97 = BEV.MASTER_KEY AND
		// BEV.KEY97 = EVT.EVENT AND SW.OWNER = DRI.KEY97 AND DRI.KEY97 =
		// REL.KEY97 AND REL.EVENT_KEY = BEV.KEY97 AND BEV.MASTER_KEY =
		// MAS.KEY97 AND MAS.PRODTYPE = P.KEY97 AND SW.SWORNO = '"
		// + correlationId + "'";
		// String eventRefQuery = "SELECT TRIM(MAS.MASTER_REF) AS MASTERREF,
		// TRIM(BEV.REFNO_PFIX)||LPAD(TRIM(BEV.REFNO_SERL),3,'0') AS EVENTREF,
		// TRIM(SW.SWOTYP) AS MTTYPE, TRIM(MAS.CCY) AS CURRENCY,
		// TRIM(EVT.GMTCOV) AS IS_EBG FROM MASTER MAS, BASEEVENT BEV,SWOPF
		// SW,DOCRELITEM DRI,RELITEM REL,EXTEVENT EVT WHERE MAS.KEY97 =
		// BEV.MASTER_KEY AND BEV.KEY97 = EVT.EVENT AND SW.OWNER = DRI.KEY97 AND
		// DRI.KEY97 = REL.KEY97 AND REL.EVENT_KEY = BEV.KEY97 AND SW.SWORNO =
		// '"
		// + correlationId + "'";

		/** 2017-03-28 **/
		// String eventRefQuery = "SELECT MAS.MASTER_REF AS MASTERREF,
		// (TRIM(A.REFNO_PFIX)||LPAD(TRIM(A.REFNO_SERL),3,'0')) AS EVENTREF,
		// TRIM(d.swotyp) AS MTTYPE, TRIM(MAS.CCY) AS CURRENCY FROM BASEEVENT A,
		// RELITEM B, DOCRELITEM C, SWOPF D, MASTER MAS WHERE MAS.KEY97 =
		// A.MASTER_KEY AND A.KEY97 = B.EVENT_KEY AND B.KEY97 = C.KEY97 AND
		// C.KEY97 = D.OWNER AND D.SWORNO ='"
		// + correlationId + "'";

		String eventRefQuery = "SELECT TRIM(MAS.MASTER_REF) AS MASTERREF, (TRIM(A.REFNO_PFIX)||LPAD(TRIM(A.REFNO_SERL),3,'0')) AS EVENTREF, TRIM(d.swotyp) AS MTTYPE, TRIM(MAS.CCY) AS CURRENCY , trim(prd.code79) as ProdType, trim(p.code) as subType,trim(p.descrip) as subTYPEDesc FROM BASEEVENT A, RELITEM B, DOCRELITEM C, SWOPF D, MASTER MAS, exempl30 prd, prodtype p WHERE MAS.KEY97 = A.MASTER_KEY AND A.KEY97 = B.EVENT_KEY AND B.KEY97 = C.KEY97 AND C.KEY97 = D.OWNER AND mas.exemplar = prd.key97 AND mas.prodtype = p.key97 AND D.SWORNO ='"
				+ correlationId + "'";
		logger.debug("SwiftDetails : " + eventRefQuery);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(eventRefQuery);
			// ps.setString(1, correlationId);
			res = ps.executeQuery(eventRefQuery);
			while (res.next()) {
				swiftDetailsMapList.put("MASTERREF", res.getString("MASTERREF"));
				swiftDetailsMapList.put("EVENTREF", res.getString("EVENTREF"));
				swiftDetailsMapList.put("CURRENCY", res.getString("CURRENCY"));
				swiftDetailsMapList.put("MTTYPE", res.getString("MTTYPE"));
				swiftDetailsMapList.put("PRODTYPE", res.getString("PRODTYPE"));
				swiftDetailsMapList.put("SUBTYPE", res.getString("SUBTYPE"));
				// swiftDetailsMapList.put("IS_EBG", res.getString("IS_EBG"));
			}

		} catch (SQLException e) {
			logger.error("SWIFTSwiftOutAdaptee SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}

		return swiftDetailsMapList;
	}

	public static String isValidEBG(String correlationId) {

		String isEBG = "";
		Connection con = null;
		ResultSet res = null;
		PreparedStatement ps = null;

		// DON't USE this commented Query
		String eventRefQuery = "SELECT DISTINCT TRIM(MAS.MASTER_REF) AS MASTERREF, TRIM(BEV.REFNO_PFIX)||LPAD(TRIM(BEV.REFNO_SERL),3,'0') AS EVENTREF, TRIM(SW.SWOTYP) AS MTTYPE,  TRIM(EVT.EPAID) AS IS_EBG,  TRIM(EVT.GMTCOV) AS IS_COVMSG, TRIM(MAS.CCY) AS CURRENCY, TRIM(SWORNO) AS CORRELATIONID, TRIM(PRD.CODE79) AS PRODTYPE, TRIM(P.CODE) AS SUBTYPE, TRIM(P.DESCRIP) AS SUBTYPEDESC FROM MASTER MAS, TIDATAITEM TID, EXEMPL30 PRD, PRODTYPE P, BASEEVENT BEV , SWOPF SW, DOCRELITEM DRI, RELITEM REL, EXTEVENT EVT WHERE MAS.EXEMPLAR = PRD.KEY97 AND MAS.KEY97 = TID.MASTER_KEY AND MAS.KEY97 = BEV.MASTER_KEY AND BEV.KEY97 = EVT.EVENT AND SW.OWNER = DRI.KEY97 AND DRI.KEY97 =	 REL.KEY97 AND REL.EVENT_KEY = BEV.KEY97 AND BEV.MASTER_KEY = MAS.KEY97 AND MAS.PRODTYPE = P.KEY97 AND SW.SWORNO = '"
				+ correlationId + "'";
		logger.debug("SwiftDetails : " + eventRefQuery);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(eventRefQuery);
			// ps.setString(1, correlationId);
			res = ps.executeQuery(eventRefQuery);
			while (res.next()) {
				// swiftDetailsMapList.put("MASTERREF",
				// res.getString("MASTERREF"));
				// swiftDetailsMapList.put("EVENTREF",
				// res.getString("EVENTREF"));
				// swiftDetailsMapList.put("CURRENCY",
				// res.getString("CURRENCY"));
				// swiftDetailsMapList.put("MTTYPE", res.getString("MTTYPE"));
				// swiftDetailsMapList.put("IS_EBG", res.getString("IS_EBG"));

				isEBG = res.getString("IS_EBG");
				// IS_COVMSG
			}

		} catch (SQLException e) {
			logger.error("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}

		return isEBG;
	}

	public static String isValidCOVMsg(String correlationId) {

		String isCOVMsg = "";
		Connection con = null;
		ResultSet res = null;
		PreparedStatement ps = null;

		// DON't USE this commented Query
		String eventRefQuery = "SELECT DISTINCT TRIM(MAS.MASTER_REF) AS MASTERREF, TRIM(BEV.REFNO_PFIX)||LPAD(TRIM(BEV.REFNO_SERL),3,'0') AS EVENTREF, TRIM(SW.SWOTYP) AS MTTYPE,  TRIM(EVT.EPAID) AS IS_EBG,  TRIM(EVT.GMTCOV) AS IS_COVMSG, TRIM(MAS.CCY) AS CURRENCY, TRIM(SWORNO) AS CORRELATIONID, TRIM(PRD.CODE79) AS PRODTYPE, TRIM(P.CODE) AS SUBTYPE, TRIM(P.DESCRIP) AS SUBTYPEDESC FROM MASTER MAS, TIDATAITEM TID, EXEMPL30 PRD, PRODTYPE P, BASEEVENT BEV , SWOPF SW, DOCRELITEM DRI, RELITEM REL, EXTEVENT EVT WHERE MAS.EXEMPLAR = PRD.KEY97 AND MAS.KEY97 = TID.MASTER_KEY AND MAS.KEY97 = BEV.MASTER_KEY AND BEV.KEY97 = EVT.EVENT AND SW.OWNER = DRI.KEY97 AND DRI.KEY97 =	 REL.KEY97 AND REL.EVENT_KEY = BEV.KEY97 AND BEV.MASTER_KEY = MAS.KEY97 AND MAS.PRODTYPE = P.KEY97 AND SW.SWORNO = '"
				+ correlationId + "'";
		logger.debug("SwiftDetails : " + eventRefQuery);

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(eventRefQuery);
			// ps.setString(1, correlationId);
			res = ps.executeQuery(eventRefQuery);
			while (res.next()) {
				// swiftDetailsMapList.put("MASTERREF",
				// res.getString("MASTERREF"));
				// swiftDetailsMapList.put("EVENTREF",
				// res.getString("EVENTREF"));
				// swiftDetailsMapList.put("CURRENCY",
				// res.getString("CURRENCY"));
				// swiftDetailsMapList.put("MTTYPE", res.getString("MTTYPE"));
				// swiftDetailsMapList.put("IS_EBG", res.getString("IS_EBG"));

				isCOVMsg = res.getString("IS_COVMSG");
				// IS_COVMSG
			}

		} catch (SQLException e) {
			logger.error("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}

		return isCOVMsg;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getDefaultTIResponse(String status) {

		String result = "";
		try {
			InputStream anInputStream = SWIFTSwiftOutAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.SWIFT_SWIFTOUT_TI_RESPONSE_TEMPLATE);
			String swiftTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("status", status);
			tokens.put("correlationId", correlationId);
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(swiftTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("Generating TI Response Exception..!" + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

}
