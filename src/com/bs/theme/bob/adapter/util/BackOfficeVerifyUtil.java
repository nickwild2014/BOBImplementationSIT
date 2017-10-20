package com.bs.theme.bob.adapter.util;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.lang.StringEscapeUtils;

import com.bs.themebridge.util.JAXBInstanceInitialiser;
import com.misys.tiplus2.apps.ti.service.messages.BulkServiceRequest;
import com.misys.tiplus2.apps.ti.service.messages.BulkServiceResponse;
import com.misys.tiplus2.services.control.ServiceRequest;
import com.misys.tiplus2.services.control.ServiceResponse;
import com.misys.tiplus2.services.control.ServiceResponse.ResponseHeader;

public class BackOfficeVerifyUtil {

	private final static Logger logger = Logger.getLogger(BackOfficeVerifyUtil.class.getName());

	/**
	 * 
	 * @param tiRequestXML
	 * @param headerStatus
	 * @param headerErrorMsg
	 * @param exposureStatus
	 * @param exposureError
	 * @param postingStatus
	 * @param postingError
	 * @param dealStatus
	 * @param dealError
	 * @return
	 */
	public static String getTIResponseXML(String tiRequestXML, String headerStatus, String headerErrorMsg,
			String headerWarningMsg, String exposureStatus, String exposureError, String postingStatus,
			String postingError, String dealStatus, String dealError) {

		String convertedString = StringEscapeUtils.unescapeHtml(tiRequestXML);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			tiRequestXML = convertedString.replaceAll("&", "&amp;");
			String innerfailureMsg = "";
			InputStream inStream = new ByteArrayInputStream(tiRequestXML.getBytes());
			JAXBContext context = JAXBInstanceInitialiser.getBackOfficeBatchRequestContext();
			Unmarshaller unmarshaller;
			ServiceRequest serviceRequest = new ServiceRequest();
			unmarshaller = context.createUnmarshaller();
			serviceRequest = (ServiceRequest) unmarshaller.unmarshal(inStream);
			com.misys.tiplus2.apps.ti.service.messages.ObjectFactory of = new com.misys.tiplus2.apps.ti.service.messages.ObjectFactory();
			ServiceResponse serviceResponse = new ServiceResponse();

			// Get Header
			// headerWarnMsg
			serviceResponse.setResponseHeader(BackofficeBatchUtil.getHeader(
					serviceRequest.getRequestHeader().getCorrelationId(),
					serviceRequest.getRequestHeader().getService(), serviceRequest.getRequestHeader().getOperation(),
					headerStatus, headerErrorMsg, headerWarningMsg));
			// "", "", ""));

			List<JAXBElement<?>> batchRequestList = serviceRequest.getRequest();
			JAXBElement<BulkServiceRequest> bsr = (JAXBElement<BulkServiceRequest>) batchRequestList.get(0);
			List<ServiceRequest> bsrlist = bsr.getValue().getServiceRequest();
			List<JAXBElement<?>> sres = serviceResponse.getResponse();
			BulkServiceResponse localResponse = new BulkServiceResponse();
			List<ServiceResponse> batchResponse = localResponse.getServiceResponse();
			String RespResult = "";
			// logger.debug("Posting status & Error--->" + postingStatus + " : "
			// + postingError);
			// logger.debug("Exposure Status & Error-->" + exposureStatus + " :
			// " + exposureError);
			// logger.debug("FXDeal Status & Error-->" + dealStatus + " : " +
			// dealError);

			for (ServiceRequest sr : bsrlist) {
				ServiceResponse bulkServiceResponse = new ServiceResponse();
				String service = sr.getRequestHeader().getService();
				String operation = sr.getRequestHeader().getOperation();
				String serviceName = sr.getRequestHeader().getService() + sr.getRequestHeader().getOperation();
				if (serviceName.equals("BackOfficeExposure")) {
					RespResult = exposureStatus;
					operation = sr.getRequestHeader().getOperation();
					innerfailureMsg = exposureError;
				} else if (serviceName.equals("BackOfficeFXDeal")) {
					RespResult = dealStatus;
					operation = sr.getRequestHeader().getOperation();
					innerfailureMsg = dealError;
				} else {
					RespResult = postingStatus;
					operation = sr.getRequestHeader().getOperation();
					innerfailureMsg = postingError;
				}

				ResponseHeader responseHeader = BackofficeBatchUtil.getHeader(
						serviceRequest.getRequestHeader().getCorrelationId(),
						serviceRequest.getRequestHeader().getService(),
						serviceRequest.getRequestHeader().getOperation(), RespResult, innerfailureMsg, "");

				bulkServiceResponse.setResponseHeader(responseHeader);
				// logger.info(bulkServiceResponse.toString());
				batchResponse.add(bulkServiceResponse);
				RespResult = "";
				service = "";
				operation = "";
			}
			sres.add(of.createBatchResponse(localResponse));
			// logger.info("create marshalling process started");
			JAXBContext jaxbContext;
			Marshaller jaxbMarshaller;

			jaxbContext = JAXBContext.newInstance("com.misys.tiplus2.apps.ti.service.messages");
			jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			outStream = new ByteArrayOutputStream();
			jaxbMarshaller.marshal(serviceResponse, outStream);

		} catch (JAXBException e) {
			logger.error("JAXBException Exceptions! " + e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {
			logger.error("Exceptions! " + e.getMessage());
			e.printStackTrace();
		}

		return outStream.toString();

	}
}
