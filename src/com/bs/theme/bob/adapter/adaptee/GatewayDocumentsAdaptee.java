package com.bs.theme.bob.adapter.adaptee;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bs.theme.bob.adapter.util.GatewayElcPyrProcessor;
import com.bs.theme.bob.adapter.util.GatewayExtCustProcessor;
import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.PostingStagingLogging;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.GatewayDocumentsXpath;
import com.bs.themebridge.xpath.IFN760COVGatewayXpath;
import com.bs.themebridge.xpath.IFN767COVGatewayXpath;
import com.bs.themebridge.xpath.IFNSFMSGatewayXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * End system communication implementation for LOCALIZATION services are handled
 * in this class.
 * 
 * @author Bluescope
 */
public class GatewayDocumentsAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(GatewayDocumentsAdaptee.class.getName());

	// public static URL resource =
	// AccountAvailBalAdaptee.class.getResource(".");
	// String filePath = new File("").getAbsolutePath();

	private String service = "";
	private String operation = "";
	private String errorMsg = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String correlationId = "";
	private static String branch = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public GatewayDocumentsAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public GatewayDocumentsAdaptee() {
	}

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

		String errorMsg = "";
		String status = "SUCCEEDED";
		try {
			tiRequest = requestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("GatewayDocuments TI Request:\n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			tiResponse = processTIGatewayRequest(requestXML);
			// logger.debug("GatewayDocuments Bank Request:\n" + bankRequest);

			// bankResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("GatewayDocuments Bank Response:\n" + bankResponse);

			// tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("GatewayDocuments TI Response:\n" + tiResponse);

		} catch (XPathExpressionException e) {
			errorMsg = e.getMessage();
			status = "FAILED";
			e.printStackTrace();

		} catch (SAXException e) {
			errorMsg = e.getMessage();
			status = "FAILED";
			e.printStackTrace();

		} catch (IOException e) {
			errorMsg = e.getMessage();
			status = "FAILED";
			e.printStackTrace();

		} finally {

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
	private String processTIGatewayRequest(String tirequestXML)
			throws XPathExpressionException, SAXException, IOException {

		String result = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		try {
			correlationId = XPathParsing.getValue(tirequestXML, GatewayDocumentsXpath.CorrelationId);
			operation = XPathParsing.getValue(tirequestXML, GatewayDocumentsXpath.Operation);
			service = XPathParsing.getValue(tirequestXML, GatewayDocumentsXpath.Service);
			sourceSystem = XPathParsing.getValue(tirequestXML, GatewayDocumentsXpath.SourceSystem);
			targetSystem = XPathParsing.getValue(tirequestXML, GatewayDocumentsXpath.TargetSystem);

			eventReference = XPathParsing.getValue(tirequestXML,
					"/ServiceRequest/" + operation.toLowerCase() + "/EventReference");
			masterReference = XPathParsing.getValue(tirequestXML,
					"/ServiceRequest/" + operation.toLowerCase() + "/MasterReference");

			String response = gatewayDocumentRouter(service, operation, tirequestXML);
			result = response;

		} catch (Exception e) {
			logger.error("Gateway Document Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 
	 * @param service
	 * @param operation
	 * @param tiGatewayRequestXml
	 * @return
	 */
	private String gatewayDocumentRouter(String service, String operation, String tiGatewayRequestXml) {

		logger.info("GATEWAY Documents Service Router : Service : " + service + "\tOperation : " + operation);

		String responseXML = "";
		bankResTime = DateTimeUtil.getSqlLocalDateTime();
		try {
			if (operation.startsWith("EXT") || operation.startsWith("EXC") || operation.startsWith("EXX")
					|| operation.startsWith("PRESHIP") || operation.startsWith("EDPMS")) {
				// External customization process
				GatewayExtCustProcessor anGatewayExtCustObj = new GatewayExtCustProcessor();
				anGatewayExtCustObj.process(tiGatewayRequestXml);

			}  else if (operation.startsWith("LCBDELCPYR")) {
				// Own BANK LCBD - Create new ELC events
				GatewayElcPyrProcessor anGatewayElcPyrObj = new GatewayElcPyrProcessor();
				responseXML = anGatewayElcPyrObj.processGatewayXML(tiGatewayRequestXml);

				/**
				 * queue enabled 16-05-17
				 */
			} else if (operation.startsWith("EBGISSUE")) {
				String masterRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFNSFMSGatewayXpath.MASTER_REFERENCE);
				String eventRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFNSFMSGatewayXpath.EVENT_REFERENCE);
				String serviceDuplicate = KotakConstant.SERVICE_SWIFT;// "SWIFT";
				String operationDuplicate = KotakConstant.OPERATION_SFMSOUT_298REQ; // BGIFN298PIn
				// String serviceDuplicate = "SWIFTIFN298";
				// String operationDuplicate = "SFMSOut_EBGIFN298R" ;
				String StatusDuplicate = "QUEUED";
				String tiresponseXMLDuplicate = getDefaultTIResponse("QUEUED");
				PostingStagingLogging.pushLogData(serviceDuplicate, operationDuplicate, masterRefDuplicate,
						eventRefDuplicate, tiGatewayRequestXml, tiresponseXMLDuplicate, StatusDuplicate);

			} else if (operation.startsWith("BGIFN760CV")) {
				String masterRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFN760COVGatewayXpath.MASTERREFERENCE);
				String eventRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFN760COVGatewayXpath.EVENTREFERENCE);
				String serviceDuplicate = KotakConstant.SERVICE_SWIFT;// "SWIFT";
				String operationDuplicate = KotakConstant.OPERATION_SFMSOUT_760COV; // "EBGIFN760CVOut";
				// String serviceDuplicate = "SWIFTIFN760";
				// String operationDuplicate = "BGIFN760CV" ;
				String StatusDuplicate = "QUEUED";
				String tiresponseXMLDuplicate = getDefaultTIResponse("QUEUED");
				PostingStagingLogging.pushLogData(serviceDuplicate, operationDuplicate, masterRefDuplicate,
						eventRefDuplicate, tiGatewayRequestXml, tiresponseXMLDuplicate, StatusDuplicate);

			} else if (operation.startsWith("BGIFN767CV")) {
				String masterRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFN767COVGatewayXpath.MASTERREFERENCE);
				String eventRefDuplicate = XPathParsing.getValue(tiGatewayRequestXml,
						IFN767COVGatewayXpath.EVENTREFERENCE);
				String serviceDuplicate = KotakConstant.SERVICE_SWIFT;// "SWIFT";
				String operationDuplicate = KotakConstant.OPERATION_SFMSOUT_767COV; // "EBGIFN767CVOut";
				// String serviceDuplicate = "SWIFTIFN767";
				// String operationDuplicate = "BGIFN767CV" ;
				String StatusDuplicate = "QUEUED";
				String tiresponseXMLDuplicate = getDefaultTIResponse("QUEUED");
				PostingStagingLogging.pushLogData(serviceDuplicate, operationDuplicate, masterRefDuplicate,
						eventRefDuplicate, tiGatewayRequestXml, tiresponseXMLDuplicate, StatusDuplicate);

			} else if (operation.startsWith("BGIFN769CV")) {
				logger.error("BGIFN769CV has not been developed. Please contact Themebridge administrator..!!! ");
				// IFN769OutMessageGenerator ifn769Out = new
				// IFN769OutMessageGenerator();
				// responseXML =
				// ifn769Out.processIFN298SDP(tiGatewayRequestXml);

			} else {
				logger.debug(service + "-" + operation + " is not defined. Please contact THEMEBRIDGE administrator.");
				logger.debug("GatewayDocument Received: " + tiGatewayRequestXml);
				tiResTime = DateTimeUtil.getSqlLocalDateTime();

				boolean res = ServiceLogging.pushLogData(service, operation, sourceSystem, branch, sourceSystem,
						targetSystem, masterReference, eventReference, "SUPPRESSED", tiRequest, tiResponse, bankRequest,
						bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
						errorMsg);
			}

			// else if (operation.startsWith("FXNP")) {
			// logger.debug("Transaction FXNP");
			// GatewayFXNonPositionDealProcessor fxnpProcessor = new
			// GatewayFXNonPositionDealProcessor();
			// responseXML =
			// fxnpProcessor.processGatewayXML(tiGatewayRequestXml);
			// }

		} catch (Exception e) {
			logger.error("Exception in Gateway Document parsing " + e.getMessage());
			e.printStackTrace();

		} finally {

		}
		return responseXML;
	}

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

	public static void main(String a[]) throws Exception {

		String requestXML = ThemeBridgeUtil.readFile("D:\\_Prasath\\Filezilla\\gateway\\LCBDEXOTPR-FCY2.xml");
		GatewayDocumentsAdaptee as = new GatewayDocumentsAdaptee();

		// logger.debug(as.getBankRequestFromTiRequest(requestXML));
		as.process(requestXML);

		// String responseXML = as.process(requestXML);
		// logger.debug("GatewayDocumentsAdaptee : " + responseXML);

	}

}
