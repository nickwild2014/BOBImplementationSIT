package com.bs.theme.bob.unused;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.xpath.GatewayFXNonPostionDealXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class GatewayFXNonPositionDealProcessor {

	private final static Logger logger = Logger.getLogger(GatewayFXNonPositionDealProcessor.class.getName());

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

	public String processGatewayXML(String tiGatewayRequestXml) {

		String errorMessage = "";
		String tiResponse = "";

		try {
			tiRequest = tiGatewayRequestXml;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();

			// get gateway xml data
			Map<String, String> fxnpMapList = getMasterData(tiGatewayRequestXml);
			logger.debug("inprocessmethod");

		} catch (Exception e) {

		}

		logger.debug("tigatewayrequest" + tiGatewayRequestXml);
		return tiResponse;
	}

	public Map<String, String> getMasterData(String tirequestXML) {

		Map<String, String> fxnpMapList = new HashMap<String, String>();

		try {

			service = XPathParsing.getValue(tirequestXML, GatewayFXNonPostionDealXpath.SERVICE);
			operation = XPathParsing.getValue(tirequestXML, GatewayFXNonPostionDealXpath.OPERATION);
			targetSystem = XPathParsing.getValue(tirequestXML, GatewayFXNonPostionDealXpath.TARGETSYSTEM);
			correlationId = XPathParsing.getValue(tirequestXML, GatewayFXNonPostionDealXpath.CORRELATIONID);
			String sourceSystem = XPathParsing.getValue(tirequestXML, GatewayFXNonPostionDealXpath.SOUCESYSTEM);

			String operationName = operation.toLowerCase();
			String valuesXPath = "/ServiceRequest/" + operationName;

			String masterRef = XPathParsing.getValue(tirequestXML,
					valuesXPath + GatewayFXNonPostionDealXpath.MASTERREFERENCE);
			String eventRef = XPathParsing.getValue(tirequestXML,
					valuesXPath + GatewayFXNonPostionDealXpath.EVENTREFERENCE);
			String dealerRef = XPathParsing.getValue(tirequestXML,
					valuesXPath + GatewayFXNonPostionDealXpath.DEALREFERENCE);
			String customer = XPathParsing.getValue(tirequestXML, valuesXPath + GatewayFXNonPostionDealXpath.CUSTOMER);
			String fromCurrency = XPathParsing.getValue(tirequestXML,
					valuesXPath + GatewayFXNonPostionDealXpath.FROMCURRENCY);
			String toCurrency = XPathParsing.getValue(tirequestXML,
					valuesXPath + GatewayFXNonPostionDealXpath.TOCURRENCY);
			eventReference = eventRef;
			masterReference = masterRef;

			fxnpMapList.put("service", service);
			fxnpMapList.put("operation", operation);
			fxnpMapList.put("targetSystem", targetSystem);
			fxnpMapList.put("correlationId", correlationId);
			fxnpMapList.put("sourceSystem", sourceSystem);
			fxnpMapList.put("eventReference", eventRef);
			fxnpMapList.put("masterReference", masterRef);
			fxnpMapList.put("dealerRef", dealerRef);
			fxnpMapList.put("customer", customer);
			fxnpMapList.put("fromCurrency", fromCurrency);
			fxnpMapList.put("toCurrency", toCurrency);

			logger.debug("fxnpMapList:" + fxnpMapList);

		} catch (Exception e) {
			logger.error("Gateway XML Parsing Exception..!" + e.getMessage());
		}

		return fxnpMapList;
	}

}
