package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSOUT_298REQ;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSOUT_760COV;
import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSOUT_767COV;
import static com.bs.theme.bob.template.util.KotakConstant.TARGET_SYSTEM;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.ebg.IFN298SDROutwardAdaptee;
import com.bs.theme.bob.adapter.ebg.IFN760COVOutwardAdaptee;
import com.bs.theme.bob.adapter.ebg.IFN767COVOutwardAdaptee;
import com.bs.theme.bob.template.util.KotakConstant;

public class GatewayIFNMessageRouter {

	private final static Logger logger = Logger.getLogger(GatewayIFNMessageRouter.class.getName());

	/**
	 * 
	 * @param tiGatewayRequestXml
	 * @param service
	 * @param operation
	 * @return
	 */
	public String processOutwardMessages(String tiGatewayRequestXml, String service, String operation) {

		String responseXML = "";
		/** Outward **/
		if (operation.startsWith(KotakConstant.OPERATION_SFMSOUT_298REQ)) {// Outward-EBGISSUE
			IFN298SDROutwardAdaptee ifn298Out = new IFN298SDROutwardAdaptee();
			responseXML = ifn298Out.processIFN298SDP(tiGatewayRequestXml, service, operation);// BGIFN298SDROut

		} else if (operation.startsWith(KotakConstant.OPERATION_SFMSOUT_760COV)) {// Outward-BGIFN760CV
			IFN760COVOutwardAdaptee ifn760Out = new IFN760COVOutwardAdaptee();
			responseXML = ifn760Out.processIFN760COV(tiGatewayRequestXml, service, operation);// EBGIFN760CVOut

		} else if (operation.startsWith(KotakConstant.OPERATION_SFMSOUT_767COV)) {// Outward-BGIFN767CV,
			IFN767COVOutwardAdaptee ifn767Out = new IFN767COVOutwardAdaptee();
			responseXML = ifn767Out.processIFN767COV(tiGatewayRequestXml, service, operation);// EBGIFN767CVOut
		}

		return responseXML;
	}

	/**
	 * 
	 * @param inwardMessage
	 * @param service
	 * @param operation
	 * @return
	 */
	public String processInwardMessage(String inwardMessage, String ifnType, String queueName) {

		String responseStatus = "";
		/** Inward **/
		if (ifnType.startsWith("IFN298SDP")) {// Outward-EBGISSUE
			logger.debug("Inward process of EBG298SDP");
			IFN298SDPInwardAdaptee ifn298 = new IFN298SDPInwardAdaptee();
			responseStatus = ifn298.processIFN298SDP(inwardMessage, queueName);

		} else if (ifnType.startsWith("IFN760COV")) {// Outward-EBGISSUE
			logger.debug("Inward process of BGIFN760 under development");
			IFN760COVInwardAdaptee ifn760cov = new IFN760COVInwardAdaptee();
			responseStatus = ifn760cov.processInwardCoverMsg(inwardMessage, queueName);

		} else if (ifnType.startsWith("IFN767COV")) {// Outward-EBGISSUE
			logger.debug("Inward process of BGIFN767 under development");
			IFN767COVInwardAdaptee ifn767cov = new IFN767COVInwardAdaptee();
			responseStatus = ifn767cov.processInwardCoverMsg(inwardMessage, queueName);
		}

		return responseStatus;
	}

}
