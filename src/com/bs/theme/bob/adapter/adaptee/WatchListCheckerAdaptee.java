package com.bs.theme.bob.adapter.adaptee;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bob.client.finacle.ExternalHttpClient;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ResponseHeaderUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.TIPlusEJBClient;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.GatewayDocumentsXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.services.control.StatusEnum;
import com.test.XmlSpecialCharacterEncoding;

/**
 * End system communication implementation for WatchList Checker services is
 * handled in this class.
 * 
 * @author Bluescope
 * 
 */
public class WatchListCheckerAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(WatchListCheckerAdaptee.class.getName());

	public WatchListCheckerAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public WatchListCheckerAdaptee() {
	}

	private boolean isClean = true;
	private String branch = "";
	private String service = "";
	private String operation = "";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String sourceSystem = "";
	private String targetSystem = "";
	private String bankResponse = "";
	private String correlationId = "";
	private String behalfOfBranch = "";
	private String eventReference = "";
	private String masterReference = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	/**
	 * <p>
	 * Process the Watchlist checker Service XML from the TI
	 * </p>
	 * 
	 * @throws JAXBException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String process(String requestXML)
			throws ParserConfigurationException, SAXException, IOException, JAXBException, ServiceException {

		logger.info(" ************ NCIF / WatchList / TI-TFWLCRSP adaptee process started ************ ");

		// String responseXML = null;
		String errorMsg = null;
		StatusEnum statusEnum = null;
		try {
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = requestXML;
			logger.debug("WatchList Bank Request:\n" + bankRequest);

			tiRequest = doNCIFWatchListProcess(requestXML);
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("WatchList TI Request:\n" + tiRequest);

			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			tiResponse = getTIResponseFromTIRequest(tiRequest);
			logger.debug("WatchList TI Response:\n" + tiResponse);

			statusEnum = ResponseHeaderUtil.processEJBClientResponse(tiResponse);
			logger.debug("TI Response status : " + statusEnum.toString());

		} catch (Exception e) {
			isClean = false;
			errorMsg = e.getMessage();
			e.printStackTrace();

		} finally {
			if (isClean) {
				// NEW LOGGING
				boolean res = ServiceLogging.pushLogData(getRequestHeader().getService(),
						getRequestHeader().getOperation(), sourceSystem, behalfOfBranch, sourceSystem, targetSystem,
						masterReference, eventReference, statusEnum.toString(), tiRequest, tiResponse, bankRequest,
						bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
						errorMsg);
			} else {
				// NEW LOGGING
				boolean res = ServiceLogging.pushLogData(getRequestHeader().getService(),
						getRequestHeader().getOperation(), sourceSystem, behalfOfBranch, sourceSystem, targetSystem,
						masterReference, eventReference, statusEnum.toString(), tiRequest, tiResponse, bankRequest,
						bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0",
						errorMsg);
			}
		}
		logger.info(" ************ NCIF / WatchList//TITFWLCRSP adaptee process ended ************ ");
		return tiResponse;
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 */
	public static Map<String, String> getWatchListMap(String requestXML) {

		Map<String, String> valueList = new HashMap<String, String>();
		try {
			SAXBuilder anBuilder = new SAXBuilder();
			Document anDocument = anBuilder.build(new InputSource(new ByteArrayInputStream(requestXML.getBytes())));

			logger.debug(anDocument.getRootElement().getName());
			Element anElement = anDocument.getRootElement();
			List<Element> listChild = anElement.getChildren();

			for (int temp = 0; temp < listChild.size(); temp++) {
				Element resp = listChild.get(temp);
				logger.debug("\nCurrent Element :" + resp.getName());

				// Operation name always start with NCIF for Gateway document
				if (resp.getName().startsWith("ncif")) {
					List<Element> listResp = resp.getChildren();

					for (int temp2 = 0; temp2 < listResp.size(); temp2++) {
						Element respChild = listResp.get(temp2);
						// Validation empty or null
						if (ValidationsUtil.isValidString(respChild.getText().trim()))
							valueList.put(respChild.getName(), respChild.getText().trim());
					}
				}
			}
		} catch (JDOMException e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
			return null;

		} catch (IOException e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		logger.debug("Map value-->" + valueList);
		return valueList;
	}

	/**
	 * 
	 * @param requestXML
	 * @return
	 */
	private String doNCIFWatchListProcess(String requestXML) {

		// logger.debug("Entering into doNCIFWatchListProcess : ");

		Map<String, String> ncifRequestList = new HashMap<String, String>();
		Map<String, String> ncifResponseList = new HashMap<String, String>();
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
			behalfOfBranch = XPathParsing.getValue(requestXML,
					"/ServiceRequest/" + operation.toLowerCase() + "/BehalfofBranch");

			ncifRequestList = getWatchListMap(requestXML);
			logger.debug("Final list of names for NCIF-->" + ncifRequestList);

			ncifResponseList = getNCIFBankResponse(ncifRequestList);
			logger.debug("ncifResponseList : " + ncifResponseList);

			if (ncifResponseList != null)
				requestXML = getTIRequestFromBankResponse(ncifResponseList);
			else
				requestXML = getFailedTIRequest();

		} catch (XPathExpressionException e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error("Exceptions!!! " + e.getMessage());
			e.printStackTrace();
		}

		return requestXML;
	}

	/**
	 * 
	 * @param bankRequest
	 * @return
	 */
	private String getTIResponseFromTIRequest(String tiEJBRequest) {

		String result = "";
		try {
			result = TIPlusEJBClient.process(tiEJBRequest);

		} catch (Exception e) {
			logger.error("WatchList EJB exceptions!", e);
			result = "";
		}
		return result;
	}

	/**
	 * 
	 * @param bankResponse
	 * @return
	 */
	private String getTIRequestFromBankResponse(Map<String, String> ncifResponseList) {

		String result = "";
		// logger.debug("ncifResponseList : " + ncifResponseList);
		try {
			InputStream anInputStream = WatchListCheckerAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_WATCHLIST_REQUEST_TEMPLATE);

			String watchListTemplate = ThemeBridgeUtil.readFile(anInputStream);
			String correlationId = ThemeBridgeUtil.randomCorrelationId();

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("CorrelationId", correlationId);
			tokens.put("Name", ConfigurationUtil.getValueFromKey("SwiftInUser"));
			tokens.put("targetSystem", ConfigurationUtil.getValueFromKey("TIZONEID"));// ZONEID
			tokens.put("Branch", behalfOfBranch);
			tokens.put("OurReference", masterReference);
			tokens.put("TheirReference", masterReference);
			tokens.put("BehalfOfBranch", behalfOfBranch);
			tokens.put("MasterRef", masterReference);
			tokens.put("EventReference", eventReference);
			tokens.put("WatchListCheckerRef", eventReference);

			Map<String, String> responseParser = ncifResponseParser(ncifResponseList);

			if (responseParser != null) {
				tokens.put("Status", responseParser.get("Status"));
				tokens.put("FailedFields", responseParser.get("FailedFields"));
			}

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(watchListTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();
			// logger.debug("Before Removed empty tag : \n" + result);

			String replacedSpecialChar = XmlSpecialCharacterEncoding.watchListXmlEscapeText(result);
			// logger.debug("ReplacedSpecialChar : " + replacedSpecialChar);

			// result = CSVToMapping.RemoveEmptyTagXML(result);
			// logger.debug("After Removed empty tag Original : \n" + result);
			// String result2 = CSVToMapping.RemoveEmptyTagXML(result);
			// logger.debug("After Removed empty tag Original : \n" + result2);

			result = CSVToMapping.RemoveEmptyTagXML(replacedSpecialChar);
			// logger.debug("After Removed empty tag Special char : \n" +
			// result);

		} catch (IOException e) {
			logger.error("NCIF Request exceptions! " + e.getMessage());
			e.printStackTrace();
		}
		// logger.debug("NCIFresult : " + result);
		return result;
	}

	/**
	 * 
	 * @param tirequest
	 *            for WatchList
	 * @return
	 */
	private String getFailedTIRequest() {

		// logger.info("Enter into getFailedTIRequest....!");
		String result = "";
		try {
			InputStream anInputStream = WatchListCheckerAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.TI_WATCHLIST_REQUEST_TEMPLATE);
			String watchListTemplate = ThemeBridgeUtil.readFile(anInputStream);
			// logger.debug("swiftInTiRequestTemplate : "
			// + swiftInTiRequestTemplate);
			String correlationId = ThemeBridgeUtil.randomCorrelationId();
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("CorrelationId", correlationId);
			tokens.put("Name", ConfigurationUtil.getValueFromKey("SwiftInUser"));
			tokens.put("targetSystem", ConfigurationUtil.getValueFromKey("TIZONEID"));// ZONEID
			tokens.put("Branch", behalfOfBranch);
			tokens.put("OurReference", masterReference);
			tokens.put("TheirReference", masterReference);
			tokens.put("BehalfOfBranch", behalfOfBranch);
			tokens.put("MasterRef", masterReference);
			tokens.put("EventReference", eventReference);
			tokens.put("WatchListCheckerRef", eventReference);
			tokens.put("Status", "F");

			StringBuilder failedFiled = new StringBuilder();
			failedFiled.append("<ns2:FailedField>");
			failedFiled.append("<ns2:Code>Service Unavailable</ns2:Code>");
			failedFiled.append("<ns2:Reason>Bank NCIF Service not Available[IM]</ns2:Reason>");
			failedFiled.append("</ns2:FailedField>");

			tokens.put("FailedFields", failedFiled.toString());

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(watchListTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();
			// logger.debug("Before Removed empty tag : \n" + result);

			result = CSVToMapping.RemoveEmptyTagXML(result);
			// logger.debug("After Removed empty tag : \n" + result);

		} catch (IOException e) {
			logger.error("TI Request exceptions! " + e.getMessage());
			e.printStackTrace();

		}
		return result;
	}

	/**
	 * 
	 * @param ncifNameList
	 * @return
	 */
	private Map<String, String> getNCIFBankResponse(Map<String, String> ncifNameList) {

		// logger.info("Enter into the generateNCIFBankRequest method");
		Map<String, String> tokens = new HashMap<String, String>();

		Map<String, String> finalBankResponse = new HashMap<String, String>();
		Reader reader = null;
		try {
			String httpClientUrl = ConfigurationUtil.getValueFromKey("WatchListUrl");
			// logger.debug("WatchListUrl : " + httpClientUrl);

			InputStream anInputStream = AccountAvailBalAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.NCIF_BANK_REQUEST_TEMPLATE);
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			if (ncifNameList != null && ncifNameList.size() > 0) {
				for (Map.Entry<String, String> name : ncifNameList.entrySet()) {
					// TODO Validate empty string
					if (ValidationsUtil.isValidString(name.getValue())) {
						if (!name.getKey().equalsIgnoreCase("MasterReference")
								&& !name.getKey().equalsIgnoreCase("EventReference")
								&& !name.getKey().equalsIgnoreCase("BehalfofBranch")) {
							// logger.debug("valid String " + name.getKey());

							/*********************/
							tokens.put("P_FULL_NAME", name.getValue());

							MapTokenResolver resolver = new MapTokenResolver(tokens);
							Reader fileValue = new StringReader(requestTemplate);
							reader = new TokenReplacingReader(fileValue, resolver);
							String ncifRequest = reader.toString();
							logger.debug("NcifRequest : \n" + ncifRequest);
							bankReqTime = DateTimeUtil.getSqlLocalDateTime();

							String status = "SUCCEEDED";
							String ncifResponse = "";
							try {
								ncifResponse = ExternalHttpClient.postXML(ncifRequest, httpClientUrl);

							} catch (Exception e) {
								e.getMessage();
								status = "FAILED";
							}
							bankResTime = DateTimeUtil.getSqlLocalDateTime();
							logger.debug("NcifResponse : " + ncifResponse);

							if (!ValidationsUtil.isValidString(ncifResponse))
								return null;

							finalBankResponse.put(name.getKey() + "~" + name.getValue(), ncifResponse);
							bankResponse = bankResponse + "\n" + name.getKey() + ">>-->> " + name.getValue() + "\t\t : "
									+ ncifResponse;

						} else {
							logger.debug("Not valid String " + name.getKey() + "\t" + name.getValue());
						}
					}

				}

			}
		} catch (Exception e) {
			logger.error("" + e.getMessage());
			e.printStackTrace();

		} finally {
			try {
				if (reader != null)
					reader.close();

			} catch (IOException e) {
				logger.error("" + e.getMessage());
				e.printStackTrace();
			}
		}

		logger.info("Final value of generateNCIFBankRequest method is-->" + finalBankResponse);
		return finalBankResponse;
	}

	/**
	 * 
	 * @param ncifResult
	 * @return
	 */
	private static Map<String, String> ncifResponseParser(Map<String, String> ncifResult) {

		// logger.debug("Enter into the ncifResponseParser method");
		Map<String, String> parserResult = new HashMap<String, String>();
		StringBuilder failedFiled = new StringBuilder();
		boolean status = true;
		String result = "";
		for (Map.Entry<String, String> ncif : ncifResult.entrySet()) {

			if (ValidationsUtil.isValidString(ncif.getValue()) && ncif.getValue().contains("Return value:")) {
				result = ncif.getValue().substring(ncif.getValue().indexOf(":") + 1, ncif.getValue().length()).trim();
			} else {
				result = ncif.getValue();
			}
			logger.debug("Watclist response for " + ncif.getKey() + " is " + result);

			String[] ncifKeypair = ncif.getKey().split("~");
			String keyName = ncifKeypair[0];
			String searchName = ncifKeypair[1];

			// If Y -> not present
			// else is present
			if (result.equalsIgnoreCase("Y")) {
				status = false;
				failedFiled.append("<ns2:FailedField>");
				failedFiled.append("<ns2:Code>" + keyName + "-" + searchName + "</ns2:Code>");
				failedFiled
						.append("<ns2:Reason>This name " + searchName + " found in the Bank NCIF List.</ns2:Reason>");
				failedFiled.append("</ns2:FailedField>");
			}
			// }
		}

		if (status) {
			parserResult.put("Status", "P");
			parserResult.put("FailedFields", "");
		} else {
			parserResult.put("Status", "F");
			parserResult.put("FailedFields", failedFiled.toString());
		}

		logger.info("Final value of ncifResponseParser method is-->" + parserResult);
		return parserResult;
	}

	public static void main(String rags[]) {

		String requestXML = null;
		try {
			requestXML = ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\Garbage\\NCIF123LT.xml");
			// logger.debug(requestXML);
			WatchListCheckerAdaptee aWL = new WatchListCheckerAdaptee();
			aWL.process(requestXML);

			// getWatchListMap(requestXML);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
