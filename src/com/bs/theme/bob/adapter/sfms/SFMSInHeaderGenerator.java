/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bs.theme.bob.adapter.sfms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.DatabaseUtility;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.mt.mt1xx.*;
import com.prowidesoftware.swift.model.mt.mt2xx.*;
import com.prowidesoftware.swift.model.mt.mt4xx.*;
import com.prowidesoftware.swift.model.mt.mt7xx.*;
import com.prowidesoftware.swift.model.mt.mt9xx.*;

/**
 * 
 * @author BSIT_Periyasamy
 */
public class SFMSInHeaderGenerator {

	private final static Logger logger = Logger.getLogger(SFMSInHeaderGenerator.class);

	public static void main(String[] args) {

		// String header = getSWIFTHeader("ICICINBBXXX", "KKBKINBBCSM", "760");
		String header = getSWIFTHeader("ICICINBBXXX", "KKBKINBBCSM", "495");
		System.out.println(header);
	}

	/**
	 * 2.5.1.3
	 * 
	 * @param senderBIC
	 * @param receiverBIC
	 * @param messageType
	 * @return
	 */
	public static String getSWIFTHeader(String senderBIC, String receiverBIC, String messageType) {

		// logger.debug("SFMSInHeaderGenerator:- senderBIC(IFSC Compatible w/o):
		// " + senderBIC
		// + " receiverBIC(IFSC Compatible w/o): " + receiverBIC);

		String finalMTMessage = "";
		if (senderBIC.isEmpty() || senderBIC == null) {
			logger.debug("Sender Bic Code is Empty!" + senderBIC);
			senderBIC = "KKBKIN00AXXX";
		}

		logger.debug("Message Type : " + messageType);
		logger.debug("SenderBIC(IFSC Compatible) :->>>" + senderBIC + "<<<");
		if (senderBIC.length() == 11) {
			String prefixBIC = senderBIC.substring(0, 8);
			String terminalCode = "A";
			String suffixBIC = senderBIC.substring(8, 11);
			senderBIC = prefixBIC + terminalCode + suffixBIC;
		} else {
			senderBIC = "SXXXXXXXAXXX";
		}
		logger.debug("SenderBIC wit terminator:- " + senderBIC);

		logger.debug("ReceiverBIC(IFSC Compatible) :->>>" + receiverBIC + "<<<");
		if (receiverBIC.length() == 11) {
			String prefixBIC = receiverBIC.substring(0, 8);
			String terminalCode = "A";
			String suffixBIC = receiverBIC.substring(8, 11);
			receiverBIC = prefixBIC + terminalCode + suffixBIC;
		} else {
			receiverBIC = "RXXXXXXXAXXX";
		}
		logger.debug("ReceiverBIC wit terminator:- " + receiverBIC);

		if (messageType.equals("101")) {
			MT101 mthder = new MT101(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			mthder.getSwiftMessage().getBlock2().setInput(true);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("102")) {
			MT102 mthder = new MT102(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			mthder.getSwiftMessage().getBlock2().setInput(true);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("103")) {
			MT103 mthder = new MT103(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			mthder.getSwiftMessage().getBlock2().setInput(true);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("110")) {
			MT110 mthder = new MT110(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("111")) {
			MT111 mthder = new MT111(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("112")) {
			MT112 mthder = new MT112(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("192")) {
			MT192 mthder = new MT192(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("200")) {
			MT200 mthder = new MT200(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("202")) {
			MT202 mthder = new MT202(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("203")) {
			MT203 mthder = new MT203(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("205")) {
			MT205 mthder = new MT205(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("210")) {
			MT210 mthder = new MT210(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("292")) {
			MT292 mthder = new MT292(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("400")) {
			MT400 mthder = new MT400(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("410")) {
			MT410 mthder = new MT410(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("412")) {
			MT412 mthder = new MT412(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("416")) {
			MT416 mthder = new MT416(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("420")) {
			MT420 mthder = new MT420(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("422")) {
			MT422 mthder = new MT422(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("430")) {
			MT430 mthder = new MT430(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("495")) {
			MT495 mthder = new MT495(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("496")) {
			MT496 mthder = new MT496(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("499")) {
			MT499 mthder = new MT499(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("700")) {
			MT700 mthder = new MT700(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("701")) {
			MT701 mthder = new MT701(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("705")) {
			MT705 mthder = new MT705(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("707")) {
			MT707 mthder = new MT707(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("710")) {
			MT710 mthder = new MT710(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("711")) {
			MT711 mthder = new MT711(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("720")) {
			MT720 mthder = new MT720(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("721")) {
			MT721 mthder = new MT721(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("730")) {
			MT730 mthder = new MT730(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("732")) {
			MT732 mthder = new MT732(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("734")) {
			MT734 mthder = new MT734(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("740")) {
			MT740 mthder = new MT740(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("742")) {
			MT742 mthder = new MT742(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("747")) {
			MT747 mthder = new MT747(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("750")) {
			MT750 mthder = new MT750(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("752")) {
			MT752 mthder = new MT752(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("754")) {
			MT754 mthder = new MT754(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("756")) {
			MT756 mthder = new MT756(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("760")) {
			MT760 mthder = new MT760(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("767")) {
			MT767 mthder = new MT767(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("768")) {
			MT768 mthder = new MT768(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("769")) {
			MT769 mthder = new MT769(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("790")) {
			MT790 mthder = new MT790(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("791")) {
			MT791 mthder = new MT791(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("792")) {
			MT792 mthder = new MT792(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("795")) {
			MT795 mthder = new MT795(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("796")) {
			MT796 mthder = new MT796(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("798")) {
			MT798 mthder = new MT798(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("799")) {
			MT799 mthder = new MT799(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("900")) {
			MT900 mthder = new MT900(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("910")) {
			MT910 mthder = new MT910(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("940")) {
			MT940 mthder = new MT940(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		} else if (messageType.equals("999")) {
			MT999 mthder = new MT999(new SwiftMessage(true));
			/** Outgoing **/
			// mthder.setSender(senderBIC);
			// mthder.setReceiver(receiverBIC);
			/** Incoming **/
			mthder.setSender(receiverBIC);
			mthder.setReceiver(senderBIC);
			mthder.getSwiftMessage().getBlock2().setMessageType(messageType);
			finalMTMessage = mthder.FIN();
			// logger.debug("MTTYPE - >" + messageType);

		}

		/*******************************************************/

		String date = getCurrentDateofTISystem();
		String hhmmStr = getCurentformatTime("HHMM");

		/*******************************************************/

		// logger.debug("Before Converting SFMS header : " + finalMTMessage);

		StringBuffer buf = new StringBuffer(finalMTMessage);
		int startindex = 18;
		int endindex = 28;
		buf = buf.replace(startindex, endindex, "9999999999");
		finalMTMessage = buf.toString();

		// finalMTMessage = finalMTMessage.replace("0000000000", "9999999999");
		// logger.debug("Stp1 : >>->> " + finalMTMessage);

		finalMTMessage = finalMTMessage.replaceAll("2:I" + messageType, "2:O" + messageType + hhmmStr + date);
		// logger.debug("Stp2 : >>->> " + finalMTMessage);

		// finalMTMessage = finalMTMessage.replaceAll("N\\}", "0" +
		// generateRandom(10) + date + hhmmStr + "N}");
		finalMTMessage = finalMTMessage.replaceAll("N\\}", generateRandom(10) + date + hhmmStr + "N}");
		logger.debug("After Converting SFMS header  : " + finalMTMessage);

		// validateSwiftMessage(finalMTMessage);

		return finalMTMessage;
	}

	/**
	 * 
	 * @param foramt
	 * @return
	 */
	public static String getCurentformatTime(String foramt) {

		String result = new Date() + "";
		Date today = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat(foramt);
		result = formatter.format(today);
		return result;
	}

	/**
	 * 
	 * @param sourceSys
	 * @return
	 */
	public static String getCurrentDateofTISystem() {

		Date result;
		result = null;
		String tidate = "";
		ResultSet res = null;
		Connection con = null;
		Statement stmt = null;

		try {
			String query = "SELECT PROCDATE AS TI_CURRENTDATE FROM DLYPRCCYCL";
			// logger.debug("Query fro TI Sysdate process:>" + query);

			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(query);
			while (res.next()) {
				result = res.getDate("TI_CURRENTDATE");
			}
			DateFormat df = new SimpleDateFormat("yyMMdd");
			tidate = df.format(result);

		} catch (Exception ex) {
			logger.error((Object) "Exception! Check the logs for detail", (Throwable) ex);

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}

		return tidate;
	}

	public static long generateRandom(int length) {
		Random random = new Random();
		char[] digits = new char[length];
		digits[0] = (char) (random.nextInt(9) + '1');
		for (int i = 1; i < length; i++) {
			digits[i] = (char) (random.nextInt(10) + '0');
		}
		return Long.parseLong(new String(digits));
	}

	/**
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static boolean validateSwiftMessage(String swiftMessage) {

		boolean flag = false;
		SwiftParser parser;
		parser = new SwiftParser(swiftMessage);
		try {
			SwiftMessage message = parser.message();
			flag = message.isCOV();

		} catch (IOException ex) {
			logger.debug("Swift validations exception!" + ex.getMessage());
			ex.printStackTrace();
		}

		return flag;
	}

}
