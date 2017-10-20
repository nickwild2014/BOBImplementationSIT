package com.bs.theme.bob.adapter.adaptee;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.util.PostingStatusCheck;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.PostingStagingLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.NeftRtgsXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class GatewayRtgsNeftAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(GatewayRtgsNeftAdaptee.class.getName());

	private String service = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String status = "QUEUED";
	private String correlationId = "";

	public String process(String tirequestXML) throws Exception {

		logger.info(" ************ GATEWAY.NEFTRTGS adaptee process started ************ ");

		String tiresponseXML = "";
		String eventReference = "";
		String masterReference = "";
		try {
			tiRequest = tirequestXML;
			logger.debug("GATEWAY.NEFTRTGS TI Request : \n" + tiRequest);

			service = getRequestHeader().getService();
			operation = getRequestHeader().getOperation();
			correlationId = XPathParsing.getValue(tirequestXML, NeftRtgsXpath.correlationIdXPath);

			String requestOperation = "/ServiceRequest/" + operation.toLowerCase();
			masterReference = XPathParsing.getValue(tirequestXML, requestOperation + "/MasterReference");
			eventReference = XPathParsing.getValue(tirequestXML, requestOperation + "/EventReference");

			String postingStatus = PostingStatusCheck.postingStatus(masterReference, eventReference);
			logger.debug("Posting status : " + postingStatus);
			if (postingStatus.equalsIgnoreCase("SUCCEEDED")) {
				GatewayRtgsNeftAdapteeStaging paymentAdapteeObj = new GatewayRtgsNeftAdapteeStaging();
				tiresponseXML = paymentAdapteeObj.process(tirequestXML);
			} else {
				tiresponseXML = getDefaultTIResponse(status);
				PostingStagingLogging.pushLogData(service, operation, masterReference, eventReference, tirequestXML,
						tiresponseXML, status);
			}

			tiResponse = tiresponseXML;
			logger.debug("GATEWAY.NEFTRTGS TI Response : \n" + tiResponse);

		} catch (Exception e) {
			logger.error("Exception while parse request XML..!! " + e.getMessage());
			e.printStackTrace();

		} finally {
			// PostingStagingLogging.pushLogData(service, operation,
			// masterReference, eventReference, tiRequest,
			// tiResponse, status);

		}
		logger.info(" ************ GATEWAY.NEFTRTGS adaptee process started ************ ");
		return tiResponse;

	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getDefaultTIResponse(String status) throws Exception {

		String result = "";
		try {
			InputStream anInputStream = GatewayRtgsNeftAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.GATEWAY_NEFTRTGS_TI_RESPONSE_TEMPLATE);

			String swiftTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("operation", operation);
			tokens.put("status", status);
			tokens.put("correlationId", correlationId);
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(swiftTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.error("Exceptions..! " + e.getMessage());
		}

		// logger.debug("The NeftRtgs Response is: " + result);
		return result;
	}
}
