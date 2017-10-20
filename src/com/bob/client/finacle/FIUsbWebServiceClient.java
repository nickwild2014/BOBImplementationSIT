package com.bob.client.finacle;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.bs.themebridge.token.util.ConfigurationUtil;
import com.infosys.ci.fiusb.webservice.ExecuteService;
import com.infosys.ci.fiusb.webservice.ExecuteServiceResponse;
import com.infosys.ci.fiusb.webservice.FIUsbWebServiceService;

public class FIUsbWebServiceClient {

	private final static Logger logger = Logger
			.getLogger(FIUsbWebServiceClient.class.getName());

	public static void main(String[] args) throws MalformedURLException {

		// String responseXml = processFIUsbService("s");
		// if (responseXml != null) {

		// }
	}

	// Webservice Client
	// public static String processFIUsbService(String requestXML)
	// throws MalformedURLException {
	//
	// logger.debug("processFIUsbService request: \n" + requestXML);
	// String responseXML = "";
	// try {
	// logger.debug("Calling  finacle webservice via URL");
	// String serviceUrl = ConfigurationUtil
	// .getValueFromKey("FIUsbWebService");
	// logger.debug("FIUsbServiceUrl " + serviceUrl);
	// FIUsbWebServiceService anContext = new FIUsbWebServiceService(
	// new URL(serviceUrl));
	// ExecuteService parameters = new ExecuteService();
	// parameters.setArg00(requestXML);
	// ExecuteServiceResponse anResponse = anContext.getFIUsbWebService()
	// .executeService(parameters);
	// if (anResponse != null) {
	// responseXML = anResponse.getExecuteServiceReturn();
	// }
	// logger.debug("processFIUsbService response: \n" + responseXML);
	// } catch (MalformedURLException e) {
	// e.printStackTrace();
	// logger.error(e.getMessage());
	// }
	// return responseXML;
	// }
}
