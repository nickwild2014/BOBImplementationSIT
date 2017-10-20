package com.bs.theme.bob.adapter.adaptee;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_HIGHVALUE;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_WORKFLOW;
import static com.bs.theme.bob.template.util.KotakConstant.SERVICE_EMAIL;
import static com.bs.themebridge.util.ThemeConstant.OPERATION_EVENT_STEP;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bob.client.finacle.FinacleServiceException;
import com.bs.theme.bob.adapter.email.EmailAlertHighValueTrxnUtil;
import com.bs.theme.bob.adapter.email.EmailAlertWorkFlowStatusUtil;
import com.bs.theme.bob.adapter.util.CheckListDataUtil;
import com.bs.theme.bob.adapter.util.FXDealUtilization;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.theme.bob.template.util.StepNameConstants;
import com.bs.themebridge.entity.model.EventstepNotification;
import com.bs.themebridge.logging.NotificationsLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.AmountConversion;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.JAXBInstanceInitialiser;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.XPathParsing;
import com.misys.tiplus2.apps.ti.service.messages.Notifications;
import com.misys.tiplus2.apps.ti.service.messages.Notifications.Notification;
import com.misys.tiplus2.apps.ti.service.messages.Notifications.Notification.Payload.Entry;

public class NotificationsEventStepAdaptee extends ServiceProcessorUtil implements AdapteeInterface {

	private final static Logger logger = Logger.getLogger(NotificationsEventStepAdaptee.class.getName());

	private String branch = "N/A";
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String eventReference = "N/A";
	private String masterReference = "N/A";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;

	public NotificationsEventStepAdaptee(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public NotificationsEventStepAdaptee() {

	}

	public static void main(String[] args) throws Exception {

		NotificationsEventStepAdaptee eventStepAdaptee = new NotificationsEventStepAdaptee();
		// String reqXML = ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\task
		// RET\\Notifications.insert.error.xml");

		String reqXML = ThemeBridgeUtil
				.readFile("D:\\_Prasath\\00_TASK\\CheckListDelete\\01CheckListAbortOrigMaster.xml");

		// List<Map<String, String>> ts =
		// eventStepAdaptee.fxdealprocess("0958ICF160100195", "PAY001");
		// List<Map<String, String>> ts =
		// eventStepAdaptee.fxdealprocess("0958ELF160100123", "POD003");
		// System.out.println(ts);

		// String reqXML = ThemeBridgeUtil.readFile(
		// "D:\\_Prasath\\Filezilla\\task\\task notification email-high
		// value\\Amount-CCy-HighValueTrxn.xml");

		// String reqXML =
		// ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\Notifications2.xml");

		// String reqXML = ThemeBridgeUtil.readFile(
		// "D:\\_Prasath\\Filezilla\\task\\task notification email-high
		// value\\Without-Amount-CCy-HighValueTrxn.xml");

		// String reqXML = ThemeBridgeUtil.readFile(
		// "D:\\_Prasath\\Filezilla\\task\\task notification email-high
		// value\\Amount-FCY-HighValueTrxn.xml");

		eventStepAdaptee.process(reqXML);

	}

	// public static boolean threadProcess(String requestXML) {
	//
	// Thread thread = new Thread() {
	// public void run() {
	// notificationsAlert(requestXML);
	// }
	// };
	// thread.start();
	// }

	@Override
	public String process(String requestXML) throws Exception {

		String user = "";
		String stepId = "";
		// String branch = "";
		String errorMsg = "";
		String stepStatus = "";

		try {
			tiRequest = requestXML;
			requestXML = removeSpecialChars(requestXML); // II
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();

			String zone = XPathParsing.getValue(requestXML, "/ServiceRequest/RequestHeader/SourceSystem");
			String sourceSys = XPathParsing.getValue(requestXML, "/ServiceRequest/RequestHeader/SourceSystem");
			String targetSys = XPathParsing.getValue(requestXML, "/ServiceRequest/RequestHeader/TargetSystem");

			String aEventref = "";
			String trxnAmount = "";
			String eventlongname = "";
			String productlongname = "";
			String aMasterReference = "";
			String tiMasterEventRef = "";
			String customerMnemonicName = "";

			List<Map<String, String>> arrayList = processNotificationMarshals(requestXML); // III

			for (Map<String, String> hashMap : arrayList) {
				bankRequest = "";
				user = (String) hashMap.get("user");
				stepId = (String) hashMap.get("step-id");
				stepStatus = (String) hashMap.get("status");
				tiMasterEventRef = (String) hashMap.get("ti-event-reference");
				productlongname = (String) hashMap.get("product-long-name");
				customerMnemonicName = (String) hashMap.get("customer");
				trxnAmount = (String) hashMap.get("amount-and-currency");
				eventlongname = (String) hashMap.get("event-long-name");

				// TODO Migrated Master Event will be in different length
				int tiEventRefLength = tiMasterEventRef.length();
				branch = tiMasterEventRef.substring(0, 4);
				eventReference = aEventref = tiMasterEventRef.substring(tiEventRefLength - 6);
				masterReference = aMasterReference = tiMasterEventRef.substring(0, tiEventRefLength - 6);
				bankReqTime = DateTimeUtil.getSqlLocalDateTime();
				logger.debug("StepID-Status : <(-_-)> : " + stepId + "-" + stepStatus);
				bankRequest = stepId + "-" + stepStatus;

				/** 1.Fxdeal utilization **/
				if ((stepId.equalsIgnoreCase(StepNameConstants.INPUT_STEP)
						|| stepId.equalsIgnoreCase(StepNameConstants.CSM_STEP)
						|| stepId.equalsIgnoreCase(StepNameConstants.CBSMAKER_STEP)
						|| stepId.equalsIgnoreCase(StepNameConstants.AUTHORISE_STEP)
						|| stepId.equalsIgnoreCase(StepNameConstants.CBSAUTHORIZER_STEP))
						&& (stepStatus.equalsIgnoreCase("Completed") || stepStatus.equalsIgnoreCase("Rejected")
								|| stepStatus.equalsIgnoreCase("Aborted"))) {
					// TODO
					FXDealUtilization fXDealUtilObj = new FXDealUtilization();
					String processResult = fXDealUtilObj.processRETDealRef(stepId, stepStatus, aMasterReference,
							aEventref); // IV
					bankRequest = bankRequest + ", " + " FXDeal : " + processResult;
				}

				/** 2.CheckList records delete from Table **/
				boolean deleteCheckListDataStatus = false;
				if (stepStatus.equalsIgnoreCase("Aborted")) {
					// logger.debug("CheckListData Deleting initiated");
					deleteCheckListDataStatus = CheckListDataUtil.processCheckListData(stepId, stepStatus,
							aMasterReference, aEventref);
					bankRequest = bankRequest + ", " + " CheckListDataDelete status : " + deleteCheckListDataStatus;
				}

				/** 3.Workflow email notification **/
				boolean mailSendingStatus = false;
				boolean isEligible = EmailAlertWorkFlowStatusUtil.isEligibleWorkFlowEAlert(stepId, stepStatus);
				if (isEligible) {
					mailSendingStatus = EmailAlertWorkFlowStatusUtil.sendWorkFlowAlertMail(SERVICE_EMAIL,
							OPERATION_WORKFLOW, zone, branch, sourceSys, targetSys, aMasterReference, aEventref, user,
							stepId, stepStatus, productlongname, eventlongname, customerMnemonicName);
					bankRequest = bankRequest + ", " + " WorkflowEMail status : " + mailSendingStatus;
				}

				/** 4.HighValue email notification **/
				boolean highValueMailSendingStatus = false;
				highValueMailSendingStatus = EmailAlertHighValueTrxnUtil.processHighValueTrxn(SERVICE_EMAIL,
						OPERATION_HIGHVALUE, zone, branch, sourceSys, targetSys, aMasterReference, aEventref, user,
						stepId, stepStatus, productlongname, eventlongname, customerMnemonicName, trxnAmount);
				bankRequest = bankRequest + ", " + " HighValueEmail status : " + highValueMailSendingStatus;
			}

			// logger.debug("Notifications Bank Request: " + bankRequest);
			bankResponse = "";
			// logger.debug("Notifications Bank Response: " + bankResponse);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();

			tiResponse = getTIResponse("SUCCEEDED");
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("Notifications TI Response:\n" + tiResponse);

		} catch (XPathExpressionException e) {
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (SAXException e) {
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (IOException e) {
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (FinacleServiceException e) {
			errorMsg = e.getMessage();
			e.printStackTrace();

		} catch (Exception e) {
			errorMsg = e.getMessage();
			e.printStackTrace();

		} finally {
			String status = XPathParsing.getValue(tiResponse, "ServiceResponse/ResponseHeader/Status");
			NotificationsLogging.pushLogData(getRequestHeader().getService(), getRequestHeader().getOperation(),
					getRequestHeader().getSourceSystem(), branch, getRequestHeader().getSourceSystem(),
					getRequestHeader().getTargetSystem(), masterReference, eventReference, status, tiRequest,
					tiResponse, bankRequest, bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "",
					stepId, stepStatus, user, "", false, "0", errorMsg);
		}

		// logger.debug(" ************ Notifications.EventStep adaptee process
		// ended ************ ");

		return tiResponse;
	}

	/**
	 * 
	 * @param amt
	 * @return
	 */
	public static String getAmountFromEventField(String amt) {
		String result = amt;
		if (result == null) {
			return result;
		}
		result = result.replaceAll("[^0-9, .]", "");
		// logger.debug("result : " + result);
		return result;
	}

	/**
	 * II
	 * 
	 * @since 2017-JAN-12
	 * @param tiRequestXML
	 * @return
	 */
	private String removeSpecialChars(String tiRequestXML) {

		// logger.debug("RemoveSpecialChars initiated..!");
		String result = "";
		try {
			tiRequestXML = ThemeBridgeUtil.stringReplaceCommonUtil(tiRequestXML, "<None>", "");
			tiRequestXML = ThemeBridgeUtil.stringReplaceCommonUtil(tiRequestXML, "&gt;", "");
			tiRequestXML = ThemeBridgeUtil.stringReplaceCommonUtil(tiRequestXML, "&lt;", "");
			result = tiRequestXML;

		} catch (Exception e) {
			logger.debug("Notifications Exception..!" + e.getMessage());
			result = tiRequestXML;
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param inputXML
	 * @return
	 * @throws Exception
	 */
	public ArrayList<HashMap> getBankRequestFromTIRequest(String inputXML) throws Exception {

		ArrayList<HashMap> entryMapList = new ArrayList<HashMap>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document document = null;
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(new ByteArrayInputStream(inputXML.getBytes()));

			JAXBContext jaxbContext = JAXBContext.newInstance(
					com.misys.tiplus2.apps.ti.service.messages.Notifications.class,
					com.misys.tiplus2.services.control.ServiceRequest.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			NodeList nodeList = document.getElementsByTagNameNS("*", "NotificationsType");

			for (int i = 0; i < nodeList.getLength(); i++) {
				JAXBElement<Notifications> notificationJAXB = (JAXBElement<Notifications>) unmarshaller
						.unmarshal(nodeList.item(i), Notifications.class);
				Notifications notification = notificationJAXB.getValue();
				List<Notification> notificationList = notification.getNotification();
				boolean rootElementSet = false;
				for (Notification aNotification : notificationList) {
					List<Notifications.Notification.Payload.Entry> payloadEntryLst = aNotification.getPayload()
							.getEntry();
					HashMap<String, String> entryMap = new HashMap<String, String>();
					for (Notifications.Notification.Payload.Entry aEntry : payloadEntryLst) {
						entryMap.put(aEntry.getField(), aEntry.getValue());
					}
					entryMapList.add(entryMap);

					// TODO
					String masterEventRef = (String) entryMapList.get(i).get("ti-event-reference");
					masterReference = masterEventRef.substring(0, masterEventRef.length() - 6);
					eventReference = masterEventRef.substring(masterEventRef.length() - 6);
					// logger.debug("MasterReference : " + masterReference + "\t
					// EventReference : " + eventReference);
				}
			}

		} catch (JAXBException e) {
			logger.error(e.getMessage());
			// throw new Exception(e);

		}
		return entryMapList;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getTIResponse(String status) throws Exception {

		String result = "";
		try {
			InputStream anInputStream = GatewayRtgsNeftAdaptee.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.NOTIFICATIONS_TI_RESPONSE_TEMPLATE);
			String notificationTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("operation", OPERATION_EVENT_STEP);
			tokens.put("status", status);
			tokens.put("correlationId", ThemeBridgeUtil.randomCorrelationId());
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(notificationTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();
			reader.close();

		} catch (Exception e) {
			logger.debug("Exceptions.!" + e.getMessage());
		}

		// logger.debug("The NeftRtgs Response is: " + result);
		return result;

	}

	/**
	 * 
	 * @param document
	 * @return
	 */
	public String getBankRequestFromTIRequest(Document document) {

		String result = "";
		// ArrayList<HashMap> entryMapList = new ArrayList<HashMap>();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(
					com.misys.tiplus2.apps.ti.service.messages.Notifications.class,
					com.misys.tiplus2.services.control.ServiceRequest.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			NodeList nodeList = document.getElementsByTagNameNS("*", "NotificationsType");

			List<LinkedHashMap> entryMapList = new ArrayList<LinkedHashMap>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				JAXBElement<Notifications> notificationJAXB = (JAXBElement<Notifications>) unmarshaller
						.unmarshal(nodeList.item(i), Notifications.class);
				Notifications notification = notificationJAXB.getValue();
				List<Notification> notificationList = notification.getNotification();
				for (Notification aNotification : notificationList) {
					List<Notifications.Notification.Payload.Entry> payloadEntryLst = aNotification.getPayload()
							.getEntry();
					LinkedHashMap<String, Object> entryMap = new LinkedHashMap<String, Object>();
					for (Notifications.Notification.Payload.Entry aEntry : payloadEntryLst) {
						entryMap.put(aEntry.getField(), aEntry.getValue());
					}
					entryMap.put("user", aNotification.getUser());
					entryMapList.add(entryMap);
				}
			}
			EventstepNotification eventNotification = getEventStepNotification(entryMapList);
			LinkedHashMap<String, Object> resultMap = new LinkedHashMap<String, Object>();
			resultMap.put("requestId", ValidationsUtil.getValidStringValue(eventNotification.getCorrelationID(),
					ThemeBridgeUtil.randomCorrelationId()));
			resultMap.put("sourceSystem", "ZONE1");// BRIDGEPROPERTIES
			resultMap.put("requestTime",
					DateTimeUtil.xmlGregorianCalendarToString(DateTimeUtil.getLocalDateInXMLGregorian()));
			resultMap.put("data", eventNotification);
			result = ThemeBridgeUtil.convertObjectToJSON(resultMap);

		} catch (Exception ex) {
			logger.error(ex);
			// ex.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param entryMapList
	 * @return
	 */
	public EventstepNotification getEventStepNotification(List<LinkedHashMap> entryMapList) {
		EventstepNotification esNotification = new EventstepNotification();
		for (HashMap<String, String> aMap : entryMapList) {
			if (ValidationsUtil.isValidObject(aMap)) {
				String amountAndCurrency = "";
				if (ValidationsUtil.isValidObject(aMap.get("amount-and-currency"))) {
					amountAndCurrency = ValidationsUtil.getValidStringValue(aMap.get("amount-and-currency").toString());
				}

				String masterEventReference = aMap.get("ti-event-reference").toString();
				masterReference = masterEventReference.substring(0, masterEventReference.length() - 6);
				eventReference = masterEventReference.substring(masterEventReference.length() - 6);
				// logger.debug("Master-Event Reference : " + masterReference +
				// "\t " + eventReference);

				// COMMENTED BELOW LINE
				// long timeValue = ThemeBridgeUtil.getDateByDateAndFormat(
				// aMap.get("deadline-timestamp"), "dd/MM/yy HH:mm:ss")
				// .getTime();

				// GETTING SERVER TIME FOR BPMS AUDIT HISTORY
				long timeValue = DateTimeUtil.getLocalTime().getTime();

				esNotification.setAmount(AmountConversion.convertAmountStringToNumberStringFormat(
						ThemeBridgeUtil.getSubStringData(amountAndCurrency, 0, amountAndCurrency.indexOf(" "))));
				esNotification.setCategory("event-step");
				esNotification.setCorrelationID(ThemeBridgeUtil.randomCorrelationId());
				esNotification.setCreatedBy(aMap.get("user"));
				esNotification.setCurrency(ThemeBridgeUtil.getSubStringData(amountAndCurrency,
						amountAndCurrency.indexOf(" ") + 1, amountAndCurrency.length()));
				esNotification.setCustomerName(aMap.get("customer-name-and-address"));
				esNotification.setDeploymentId("TI1");
				String masterReference = ThemeBridgeUtil.getSubStringData(masterEventReference, 0,
						masterEventReference.length() - 6);
				String eventReference = ThemeBridgeUtil.getSubStringData(masterEventReference,
						masterEventReference.length() - 6, masterEventReference.length());
				esNotification.setTradeTxnReference(masterReference);// "ACILC1300143ISS001"
				esNotification.setTiEventReference(eventReference);// "ACILC1300143ISS001"

				// To get the sourcesystem from transaction branch

				String behalfOfBranch = ThemeBridgeUtil.getSubStringData(masterEventReference, 0, 2);
				esNotification.setProductLongName(aMap.get("product-long-name"));

				String productCode = ThemeBridgeUtil.getSubStringData(masterReference, 2, 5);
				// commented to test the product code i.e.'ILC/ELC/IDC'
				// in BPMS system
				esNotification.setProductShortName(productCode);
				// ends here

				esNotification.setEventLongName(aMap.get("event-long-name"));

				// commented to test the event code i.e.'CRE/ISS/ADV'
				// in BPMS system
				String prodEvent = productCode + ThemeBridgeUtil.getSubStringData(eventReference, 0, 3);

				// CHANGED THE EVENT SHORTNAME LOGIC
				// String eventCode =
				// ThemeBridgeUtil.getProductEventOrigCodeFromProperties(prodEvent);
				// if (ValidationsUtil.isValidString(eventCode)) {
				// esNotification.setEventShortName(eventCode);
				// } else {
				esNotification.setEventShortName(ThemeBridgeUtil.getSubStringData(eventReference, 0, 3));
				// }
				// ends here

				String customerId = "";
				String customer = "";
				if (ValidationsUtil.isValidObject(aMap.get("customer"))) {
					customer = ValidationsUtil.getValidStringValue(aMap.get("customer").toString());
				}
				esNotification.setCustomerId(ValidationsUtil.getValidStringValue(customerId, customer));

				esNotification.setSourceSystem("TIPLUS");
				esNotification.setCreatedBy(aMap.get("user"));
				esNotification.setSubcategory(aMap.get("product-short-name")); // "ILC"
				esNotification.setSystem("ZONE1");

				esNotification.setTime(timeValue);
				esNotification.setType("Transactions");
				esNotification.setUser(ValidationsUtil.getValidStringValue(aMap.get("user"), "SUPERVISOR"));
				esNotification.setZone(aMap.get("zone"));

				if (esNotification.getNotifications() == null)
					esNotification.setNotifications(new EventstepNotification.Notifications());

				List<EventstepNotification.Notifications.Notification> notificationList = esNotification
						.getNotifications().getNotification();
				EventstepNotification.Notifications.Notification notification = new EventstepNotification.Notifications.Notification();
				notification.setSla(aMap.get("sla"));
				notification.setStepStatus(aMap.get("status"));
				notification.setStepDescription(aMap.get("step-description"));
				notification.setStepId(aMap.get("step-id"));
				notification.setTeam(ValidationsUtil.getValidStringValue(aMap.get("team"), "ALL TRADE TEAM"));
				notification.setUser(ValidationsUtil.getValidStringValue(aMap.get("user"), "SUPERVISOR"));
				notification.setDeadlineTimestamp(timeValue + "");
				notificationList.add(notification);
			}
		}
		return esNotification;
	}

	/**
	 * III
	 * 
	 * @since 2016-JAN-12
	 * @author Prasath Ravichandran
	 * @param inputXML
	 * @return
	 */
	public List<Map<String, String>> processNotificationMarshals(String inputXML) {

		Unmarshaller unmarshaller;
		NodeList nodeList;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder builder;
		List<Map<String, String>> anList = new ArrayList<Map<String, String>>();

		try {
			JAXBContext jaxbContext = JAXBInstanceInitialiser.getNotificationRequestContext();
			builder = dbf.newDocumentBuilder();
			unmarshaller = jaxbContext.createUnmarshaller();
			// nodeList = getDocument().getElementsByTagNameNS("*",
			// "FacilitiesResponse");
			Document document = null;
			InputSource inputSource = new InputSource(new StringReader(inputXML));
			document = builder.parse(inputSource);
			nodeList = document.getElementsByTagNameNS("*", "NotificationsType");

			JAXBElement<Notifications> bulkServiceJAXB = (JAXBElement<Notifications>) unmarshaller
					.unmarshal(nodeList.item(0), Notifications.class);
			Notifications bsr = bulkServiceJAXB.getValue();

			// NotificationsType
			List<Notification> sNotification = bsr.getNotification();

			for (Notification notification2 : sNotification) {
				Map<String, String> map = new HashMap<String, String>();
				String Category = notification2.getCategory();
				String subCategory = notification2.getSubcategory();
				String deploymentid = notification2.getDeploymentId();
				String system = notification2.getSystem();
				String type = notification2.getType();
				String user = notification2.getUser();
				long time = notification2.getTime();
				map.put("Category", Category);
				map.put("subCategory", subCategory);
				map.put("system", system);
				map.put("deploymentid", deploymentid);
				map.put("type", type);
				map.put("user", user);
				List<Entry> entries = notification2.getPayload().getEntry();

				for (Entry entry : entries) {
					String field = entry.getField();
					String value = entry.getValue();
					if (field.equals("ti-event-reference")) {
						map.put("masterEventRefer", value);
					}
					if (field.equalsIgnoreCase("customer")) {
						map.put(field, value);
					}
					if (field.equalsIgnoreCase("created-by")) {
						map.put(field, value);
					}
					map.put(field, value);
				}
				anList.add(map);
			}

		} catch (ParserConfigurationException e) {
			logger.error(" ParserConfigurationException-->" + e.getMessage());
			e.printStackTrace();

		} catch (JAXBException e) {
			logger.error(" JAXBException-->" + e.getMessage());
			e.printStackTrace();

		} catch (SAXException e) {
			logger.error(" SAXException-->" + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			logger.error(" IOEXception-->" + e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {
			logger.error(" EXception-->" + e.getMessage());
			e.printStackTrace();
		}

		return anList;
	}

}
