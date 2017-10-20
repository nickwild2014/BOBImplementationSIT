package com.bs.theme.bob.adapter.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.sfms.SFMSInMessageGenerator;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ThemeConstant;
import com.bs.themebridge.util.ValidationsUtil;
import com.prowidesoftware.swift.io.ConversionService;
import com.prowidesoftware.swift.model.SwiftMessage;

public class SFMSAdaptee {

	private final static Logger logger = Logger.getLogger(SFMSAdaptee.class.getName());

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * Swift 1.0
	 * 
	 * @param prdType
	 * @param eventcode
	 * @param subType
	 * @return
	 */
	public static String checkinSFMSFlag(String prdType, String eventcode, String subType) {

		String flag = "";
		Connection con = null;
		ResultSet rs = null;
		Statement stmt = null;
		String query = "Select Count(*) as SFMSFLAG from EXTSFMSPRMA where PROTY = '" + prdType + "' and PRSUB = '"
				+ subType + "'  and EVENT = '" + eventcode + "' ";

		int count = 0;
		try {
			logger.info("Checkin SFMSFlag Query :>" + query);
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				count = rs.getInt("SFMSFLAG");
			}
			if (count > 0) {
				flag = "YES";
			} else {
				flag = "NO";
			}

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);

		} finally {
			logger.info("SFMS Flag :>" + flag);
			DatabaseUtility.surrenderConnection(con, null, rs);
		}
		return flag;
	}

	/**
	 * 
	 * @param masterRef
	 * @param sworno
	 * @return
	 */
	public static Map<String, String> getswiftMSGType(String masterRef, String sworno) {

		logger.debug("To getting Sender and recevier IFSC code process started....");
		Statement stmt = null;
		Connection connection = null;
		ResultSet resultSet = null;
		Map<String, String> ifscResp = new HashMap<String, String>();
		String subType = "";
		// 1. prd.CODE79 = product
		// 2. pad.role =
		// 3. bev.refno_pfix = event ref no
		// 4. mas.master_ref= master ref
		// 5. sw.SWORNO = corrlation id

		String query = "select distinct  p.code subType, p.descrip subTYPEDesc  , prd.code79 as ProdType, MAS.MASTER_REF, BEV.REFNO_PFIX AS eventcode,"
				+ " trim(BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL,3,0)) AS EventREFNO_PFIXSERL  "
				+ "  from master mas,   tidataitem tid, exempl30 prd, prodtype p , "
				+ "  baseevent bev , swopf sw, docrelitem dri, relitem rel where mas.exemplar = prd.key97  "
				+ "  and mas.key97 = tid.master_key " + "  and mas.key97 = bev.master_key     "
				// + " and mas.master_ref='" + masterRef + "' "
				+ "  and sw.owner = dri.key97  and dri.key97 = rel.key97  and rel.event_key = bev.key97   "
				+ "  and bev.master_key = mas.key97  " + "  and mas.prodtype = p.key97   " + "  and sw.sworno = '"
				+ sworno + "' ";
		try {

			// connection = DBUtility.getTIConnection();
			logger.info("query-->" + query);

			connection = DatabaseUtility.getTizoneConnection();
			stmt = connection.createStatement();

			resultSet = stmt.executeQuery(query);
			String CusRole = "";
			int i = 0;
			while (resultSet.next()) {

				logger.info("Count--" + i);
				subType = resultSet.getString("subType");
				ifscResp.put("Status", "SUCCESS");

				ifscResp.put("subType", subType.trim());
				String ProdType = resultSet.getString("ProdType");
				ifscResp.put("ProdType", ProdType.trim());

				logger.info("IFSC Code-->" + CusRole);

				String subTYPEDesc = resultSet.getString("subTYPEDesc");
				ifscResp.put("subTYPEDesc", subTYPEDesc.trim());

				String eventcode = resultSet.getString("eventcode");
				ifscResp.put("eventcode", eventcode.trim());

				String MASTER_REF = resultSet.getString("MASTER_REF");
				ifscResp.put("MASTER_REF", MASTER_REF.trim());

				String eventRef = resultSet.getString("EventREFNO_PFIXSERL");
				ifscResp.put("eventRef", eventRef.trim());

				i = i + 1;
			}

			if (i == 0) {
				logger.info("No Process in master refno");
				ifscResp.put("subType", "");
				ifscResp.put("subTYPEDesc", "");
				ifscResp.put("ProdType", "");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			ifscResp.put("subTYPEDesc", "");
			ifscResp.put("Status", "FILED");
			ifscResp.put("subType", "");
			ifscResp.put("ProdType", "");
			logger.info("Exception!!!!!!", e);
		} finally {
			logger.info("IFSC Code for sender and reciver-->" + ifscResp);
			DatabaseUtility.surrenderConnection(connection, stmt, resultSet);
		}
		logger.info("To getting Sender and recevier IFSC code process ended....");
		return ifscResp;
	}

	/**
	 * SFMS 1.1
	 * 
	 * @param bankReqSfmsMsg
	 * @param MT
	 * @param corr
	 * @param masterRef
	 * @param eventPriFix
	 * @param serl
	 * @return
	 */
	public String updateSFMSMsg(String bankReqSfmsMsg, String MT, String corr, String masterRef, String eventPriFix,
			String serl) {

		logger.debug("updateSFMSMsg Process Start Here");

		String status = "";
		Map<String, String> responseMap = new HashMap<String, String>();

		try {
			HashMap<String, String> sfmsincomingMap = SFMSAdaptee.updateSFMSHeader(bankReqSfmsMsg, corr, masterRef,
					eventPriFix, serl);

			status = sfmsincomingMap.get("STATUS");

			if (status.equals("0")) {
				logger.debug("status " + status);

				bankReqSfmsMsg = sfmsincomingMap.get("HEADERPART");
				logger.debug("bankReqSFMSMge : " + bankReqSfmsMsg);

				bankReqSfmsMsg = SFMSAdaptee.updateSFMSBodyDateFormat(bankReqSfmsMsg, MT);
				logger.debug("DateFormat : " + bankReqSfmsMsg);

				bankReqSfmsMsg = SFMSAdaptee.updateSFMSBodyBicIfscFormat(bankReqSfmsMsg, MT, masterRef, eventPriFix,
						serl);

			} else {
				logger.debug("status " + status);
				logger.debug("Update SFMS Message failed ");
			}

			responseMap.put("bankReqSFMSMge", bankReqSfmsMsg);
			logger.debug("Return bankReqSFMSMge >>> " + bankReqSfmsMsg);

		} catch (Exception e) {
			logger.error("Exception! " + e.getMessage(), e);
			e.printStackTrace();
			status = ThemeBridgeStatusEnum.FAILED.toString();
			responseMap.put("swiftMessage", bankReqSfmsMsg);
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : responseMap.entrySet()) {
			sb.append(entry.getValue());
		}
		logger.info("sb.toString() >>> " + sb.toString());
		return sb.toString();
	}

	/**
	 * SFMS 1.2
	 * 
	 * @param swiftMessage
	 * @param corrlID
	 * @param masterRef
	 * @param eventpfx
	 * @param eventserl
	 * @return
	 */
	public static HashMap<String, String> updateSFMSHeader(String swiftMessage, String corrlID, String masterRef,
			String eventpfx, String eventserl) {

		logger.debug("SFMS Header update process Started Here");
		String reSwiftMessage = "{A:APP";
		HashMap<String, String> sfmsincomingMap = new HashMap<String, String>();
		swiftMessage = swiftMessage.trim();
		boolean identifierFlag = false;
		SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);

		// Message Identifier
		String mT = "";
		String msgIdent = "F01";
		// MsgIdent = getMessageIdentifier(swiftMessage);
		reSwiftMessage = reSwiftMessage + msgIdent;
		// Input/output Identifier (either I or O)
		reSwiftMessage = reSwiftMessage + "O";
		// Message type
		mT = m.getType();
		logger.debug("Msg Type : " + mT);
		reSwiftMessage = reSwiftMessage + mT;
		// Sub Message type (For IFN 298C01, this field should be C01, for
		// IFN100.
		// Message, this field should be XXX)
		String subMT = "XXX";
		reSwiftMessage = reSwiftMessage + subMT;
		String receiverIFSC = "", senderIFSC = "";
		String transSwiftRef = m.getBlock4().getTagValue("20");
		// String transSwiftRef =
		// ServiceLookupUtil.getReferenceNo(swiftMessage);
		// logger.info("Sender and receiver ifsc Code :>" + ifscResp);
		SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
		// Sender IFSC Code-SBIN0001001.

		Map<String, String> map = getSenderInIFSCCode(transSwiftRef);

		String inStatus = map.get("status");
		Map<String, String> gridifscmap = new HashMap<String, String>();
		if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
			gridifscmap = generator.getSwiftBodyBicifscCode(masterRef, eventpfx, eventserl);
		}
		if (inStatus.equals("1")) {
			// REMBNK NXAB
			if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
				receiverIFSC = gridifscmap.get("OTHERBNK");
			} else {
				receiverIFSC = map.get("recIsfc");
				identifierFlag = true;
			}
			senderIFSC = map.get("sendserIFSC");
			reSwiftMessage = reSwiftMessage + receiverIFSC;
			reSwiftMessage = reSwiftMessage + senderIFSC;
			if (senderIFSC != null && !senderIFSC.isEmpty() && receiverIFSC != null && !receiverIFSC.isEmpty()) {
				identifierFlag = true;
			} else {
				identifierFlag = false;
			}

		} else {

			logger.info("outside the instatus loop**************************************" + inStatus);

			Map<String, String> senderRece = generator.getSwiftBicifscCode(masterRef, eventpfx, eventserl);

			if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
				receiverIFSC = gridifscmap.get("OTHERBNK");
			} else {
				receiverIFSC = senderRece.get("receiverIFSC");
			}
			senderIFSC = senderRece.get("sendserIFSC");
			logger.debug("senderRece Mappp :>" + senderRece);
			logger.debug("senderIFSC  :>" + senderIFSC);
			logger.debug("receiverIFSC  :>" + receiverIFSC);
			if (senderIFSC != null && !senderIFSC.isEmpty() && receiverIFSC != null && !receiverIFSC.isEmpty()) {
				reSwiftMessage = reSwiftMessage + senderIFSC;
				reSwiftMessage = reSwiftMessage + receiverIFSC;
				identifierFlag = true;
			} else {
				identifierFlag = false;
			}
		}
		// End receiver IFSC Code
		logger.info("identifierFlag :>" + identifierFlag);
		// receiverIFSC = getReceiverIFSC(swiftMessage);
		// Delivery notification flag
		if (identifierFlag) {
			reSwiftMessage = reSwiftMessage + "1";
			// Open Notification flag
			reSwiftMessage = reSwiftMessage + "1";
			// Non-delivery Warning flag
			reSwiftMessage = reSwiftMessage + "2";
			// Obsolescence Period
			reSwiftMessage = reSwiftMessage + "000";
			// Message User Reference (MUR)
			long MUR = 0;
			try {
				MUR = ThemeBridgeUtil.generateRandom(16);
			} catch (Exception e) {
			}
			reSwiftMessage = reSwiftMessage + String.valueOf(MUR) + "";
			// Possible Duplicate flag
			reSwiftMessage = reSwiftMessage + "2";
			// Service Identifier
			String ServiceIdentifier = "";
			if (mT != null && !mT.isEmpty()) {
				if (mT.startsWith("7")) {
					if (mT.equals("760") || mT.equals("767") || mT.equals("768") || mT.equals("769")) {
						ServiceIdentifier = "BGS";
					} else {
						ServiceIdentifier = "ILC";
						logger.debug("inside the MT messages type loop" + mT + ServiceIdentifier);
					}
				}
				if (mT.startsWith("4")) {
					ServiceIdentifier = "CCL";
					logger.debug("inside the MT messages type loop " + mT + ServiceIdentifier);
				}
				if (mT.startsWith("9")) {
					ServiceIdentifier = "CPT";
					logger.debug("inside the MT messages type loop " + mT + ServiceIdentifier);
				}
			}
			reSwiftMessage = reSwiftMessage + ServiceIdentifier;
			// Originating date
			SimpleDateFormat dateCurrFormat = new SimpleDateFormat("yyyy/MM/dd");
			SimpleDateFormat dateChngeFormat = new SimpleDateFormat("yyyyMMdd");
			// logger.debug(ThemeBridgeUtil.getCurrentDate());
			try {
				// Date valueDate =
				// dateCurrFormat.parse(ThemeBridgeUtil.getCurrentDate());
				Date valueDate = dateCurrFormat.parse(DateTimeUtil.getCurrentDate());
				logger.debug("Value Date-->" + dateChngeFormat.format(valueDate));
				reSwiftMessage = reSwiftMessage + dateChngeFormat.format(valueDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Originating time
			DateFormat timeFormat = new SimpleDateFormat("HHmm");
			Calendar calendar = Calendar.getInstance();
			reSwiftMessage = reSwiftMessage + timeFormat.format(calendar.getTime()) + "";
			// Testing and training flag
			reSwiftMessage = reSwiftMessage + "2";
			// Sequence Number
			reSwiftMessage = reSwiftMessage + ThemeBridgeUtil.generateRandom(9);
			// Filler
			String Filler = "XXXXXXXXX";
			reSwiftMessage = reSwiftMessage + Filler;
			// Unique Transaction Reference.
			String TransRef = "XXXXXXXXXXXXXXXX";
			// TransRef = getReferenceNo(swiftMessage);
			reSwiftMessage = reSwiftMessage + TransRef;
			// Priority Flag
			reSwiftMessage = reSwiftMessage + "99";
			// Final SFMS Header tag
			reSwiftMessage = reSwiftMessage + "}";
			logger.debug(reSwiftMessage.length());
			// Release 1: and 2: to A:
			String newHeader = "";
			int indexOfMT = swiftMessage.lastIndexOf("{1:");
			logger.debug("==>" + indexOfMT + "==>" + swiftMessage.length());
			if (indexOfMT >= 0 && indexOfMT < swiftMessage.length()) {
				newHeader = new StringBuffer(swiftMessage).substring(indexOfMT, 29 + 21);
				newHeader = newHeader.replaceAll("/", "_");
				logger.debug("--->" + newHeader);
			}
			reSwiftMessage = swiftMessage.replace(newHeader, reSwiftMessage);
			sfmsincomingMap.put("STATUS", "0");
			sfmsincomingMap.put("HEADERPART", reSwiftMessage);
		} else {
			reSwiftMessage = "Sender and Receiver IFSCCode is not available";
			sfmsincomingMap.put("STATUS", "1");
			sfmsincomingMap.put("HEADERPART", reSwiftMessage);
		}
		logger.debug("\n-------------SFMS Header-----------------------\n");
		logger.debug("SFMS Msg-->" + reSwiftMessage);
		logger.debug("\n-------------SFMS Header Finished-----------------------\n");
		return sfmsincomingMap;
	}

	/**
	 * SFMS 1.3
	 * 
	 * @param masterRef
	 * @return
	 */
	private static Map<String, String> getSenderInIFSCCode(String masterRef) {

		Connection con = null;
		ResultSet rs = null;
		Statement stmt = null;
		Map<String, String> map = new HashMap<String, String>();
		con = DatabaseUtility.getTizoneConnection();
		try {
			String query = "select trim(SENDERIFSC) as SENDER_IDFC, trim(RECIVERIFSC) as RECIVER_IFSC from SFMSINIFSCSTORE where SFMSINREF = '"
					+ masterRef + "' order by id desc";

			logger.debug("Get Sender IFSC Code Query : " + query);
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			int i = 0;
			if (rs.next()) {
				String sendserIFSC = rs.getString("SENDER_IDFC");
				String recIsfc = rs.getString("RECIVER_IFSC");
				map.put("sendserIFSC", sendserIFSC);
				map.put("recIsfc", recIsfc);
				i = 1;
			}

			if (i != 0) {
				map.put("status", "1");
			} else
				map.put("status", "0");

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("SQLException :>", e);
			map.put("status", "0");

		} finally {
			logger.debug("map : " + map);
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}
		return map;
	}

	/**
	 * SFMS 1.4
	 * 
	 * @param swiftMessage
	 * @param MsgType
	 * @return
	 */
	public static String updateSFMSBodyDateFormat(String swiftMessage, String MsgType) {

		logger.info("UpdateSFMSBody DateFormat process started");
		logger.debug("Update SFMSBody Date Format SFMS Message -->" + swiftMessage);

		SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
		String SwiftSFMSDateFormat = "";

		try {
			SwiftSFMSDateFormat = ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SWIFT_DATE_FORMAT);

		} catch (Exception e) {
			SwiftSFMSDateFormat = "31C|31D|44C|30|31E|32A|33A|34A";
			e.printStackTrace();
		}

		try {
			logger.debug("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);
			logger.debug("MsgType -->" + MsgType);

			String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");
			for (String tag : prodDateKeys) {
				logger.debug("Date Tag -->" + tag);
				// tag = tag + ":";
				String value = m.getBlock4().getTagValue(tag);
				logger.debug("Key-->" + tag + "\tvalue -->" + value);
				// value = "250923IN";
				if (value != null && !value.equals("")) {
					int taglen = value.length();
					logger.debug(taglen);
					String tag2nd = "";
					if (taglen > 6) {
						tag2nd = value.substring(6, value.length());
						value = value.substring(0, 6);
					}
					String valueChange = DateTimeUtil.getDateTimeChangeFormat(value, "yyMMdd", "yyyyMMdd");
					// valueChange = "20150923" + "IN";
					// String valueChange = "20" + value;
					logger.debug("Key-->" + tag + "\t Old value -->" + value + "\t change value -->" + valueChange);
					logger.debug("<:::::::::::Date Replace CHanges INput\t:::::::::::>"
							+ "\n Searching string\t::::::::::::>" + tag + ":" + value + tag2nd
							+ "\n Replace string\t:::::::::::>" + tag + ":" + valueChange + tag2nd + " \t tag2nd:>"
							+ tag2nd);
					swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tag + ":" + value + tag2nd,
							tag + ":" + valueChange + tag2nd);
				}
				tag = "";
				value = "";
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception !!!!!!!!!!!!! ", e);
		}
		logger.debug("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);
		return swiftMessage;
	}

	/**
	 * SFMS 1.6
	 * 
	 * @param swiftMessage
	 * @param MsgType
	 * @param masterRef
	 * @param eventPriFix
	 * @param serl
	 * @return
	 */
	public static String updateSFMSBodyBicIfscFormat(String swiftMessage, String MsgType, String masterRef,
			String eventPriFix, String serl) {

		logger.info("updateSFMSBody BicIfscFormat process started");
		logger.debug("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);

		SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
		String SwiftSFMSDateFormat = "";
		try {

			SwiftSFMSDateFormat = ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SFMS_DATE_FORMAT);
		} catch (Exception e) {
			// TODO: handle exception
			// SwiftSFMSDateFormat = DBPropertiesLoader.SFMSBicIfscFormat;
			e.printStackTrace();
		}
		try {
			logger.info("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);
			logger.info("MsgType -->" + MsgType);
			SFMSInMessageGenerator generator = new SFMSInMessageGenerator();
			String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");
			Map<String, String> map = generator.getSwiftBodyBicifscCode(masterRef, eventPriFix, serl);
			logger.info("3 rd Party Ifsc Code list :>" + map);
			for (String tag : prodDateKeys) {
				logger.info("Date Tag -->" + tag);
				String string2 = tag + ":";
				String value = m.getBlock4().getTagValue(tag);
				logger.info("Key-->" + string2 + "\tvalue -->" + value);
				if (value != null && !value.equals("")) {
					// String valueChange =
					// ThemeBridgeUtil.getChangeFormat(
					// value, "yyMMdd", "yyyyMMdd");
					// Map<String, String> map = new HashMap<String, String>();
					String IFSC = map.get(tag);
					logger.info("IFSC :>" + IFSC);
					String valueChange = "XXXXXXXXXXX";
					if (ValidationsUtil.isValidString(IFSC)) {
						valueChange = IFSC;
					} else {
						valueChange = "XXXXXXXXXXX";
					}
					logger.info("Key-->" + string2 + "\t Old value -->" + value + "\t change value -->" + valueChange);
					logger.info("<:::::::::::Date Replace CHanges INput\t:::::::::::>"
							+ "\n Searching string\t::::::::::::>" + string2 + value + "\n Replace string\t:::::::::::>"
							+ string2 + valueChange);
					if (tag.equals("41A")) {
						valueChange = valueChange + value.substring(11, value.length());
					}
					swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, string2 + value,
							string2 + valueChange);
				}
				string2 = "";
				value = "";
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Exception !!!!!!!!!!!!! ", e);
		}
		logger.info("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);
		return swiftMessage;
	}

}

// package com.bs.themebridge.adapter.adaptee;
//
// import java.sql.Connection;
// import java.sql.ResultSet;
// import java.sql.SQLException;
// import java.sql.Statement;
// import java.text.DateFormat;
// import java.text.SimpleDateFormat;
// import java.util.Calendar;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;
// import java.text.ParseException;
//
// import org.apache.log4j.Logger;
//
// import com.bs.themebridge.adapter.util.SwiftIncomingMessagerGenerator;
// import com.bs.themebridge.util.DatabaseUtility;
// import com.bs.themebridge.token.util.ConfigurationUtil;
// import com.bs.themebridge.util.ThemeBridgeStatusEnum;
// import com.bs.themebridge.util.ThemeBridgeUtil;
// import com.bs.themebridge.util.ThemeConstant;
// import com.prowidesoftware.swift.io.ConversionService;
// import com.prowidesoftware.swift.model.SwiftMessage;
//
// public class SFMSAdaptee {
//
// private final static Logger logger =
// Logger.getLogger(SFMSAdaptee.class.getName());
//
// public static void main(String[] args) {
// // TODO Auto-generated method stub
//
// }
//
// /**
// * Swift 1.0
// *
// * @param prdType
// * @param eventcode
// * @param subType
// * @return
// */
// public static String checkinSFMSFlag(String prdType, String eventcode, String
// subType) {
//
// String flag = "";
// Connection con = null;
// ResultSet rs = null;
// Statement stmt = null;
// String query = "Select Count(*) as SFMSFLAG from EXTSFMSPRMA where PROTY = '"
// + prdType + "' and PRSUB = '"
// + subType + "' and EVENT = '" + eventcode + "' ";
//
// int count = 0;
// try {
// logger.info("Checkin SFMSFlag Query :>" + query);
// con = DatabaseUtility.getTizoneConnection();
// stmt = con.createStatement();
// rs = stmt.executeQuery(query);
// if (rs.next()) {
// count = rs.getInt("SFMSFLAG");
// }
// if (count > 0) {
// flag = "YES";
// } else {
// flag = "NO";
// }
//
// } catch (SQLException e) {
// e.printStackTrace();
// logger.error(e.getMessage(), e);
//
// } finally {
// logger.info("SFMS Flag :>" + flag);
// DatabaseUtility.surrenderConnection(con, null, rs);
// }
// return flag;
// }
//
// /**
// *
// * @param masterRef
// * @param sworno
// * @return
// */
// public static Map<String, String> getswiftMSGType(String masterRef, String
// sworno) {
//
// logger.debug("To getting Sender and recevier IFSC code process started....");
// Statement stmt = null;
// Connection connection = null;
// ResultSet resultSet = null;
// Map<String, String> ifscResp = new HashMap<String, String>();
// String subType = "";
// // 1. prd.CODE79 = product
// // 2. pad.role =
// // 3. bev.refno_pfix = event ref no
// // 4. mas.master_ref= master ref
// // 5. sw.SWORNO = corrlation id
//
// String query = "select distinct p.code subType, p.descrip subTYPEDesc ,
// prd.code79 as ProdType, MAS.MASTER_REF, BEV.REFNO_PFIX AS eventcode,"
// + " trim(BEV.REFNO_PFIX || LPAD(BEV.REFNO_SERL,3,0)) AS EventREFNO_PFIXSERL "
// + " from master mas, tidataitem tid, exempl30 prd, prodtype p , "
// + " baseevent bev , swopf sw, docrelitem dri, relitem rel where mas.exemplar
// = prd.key97 "
// + " and mas.key97 = tid.master_key " + " and mas.key97 = bev.master_key "
// // + " and mas.master_ref='" + masterRef + "' "
// + " and sw.owner = dri.key97 and dri.key97 = rel.key97 and rel.event_key =
// bev.key97 "
// + " and bev.master_key = mas.key97 " + " and mas.prodtype = p.key97 " + " and
// sw.sworno = '"
// + sworno + "' ";
// try {
//
// // connection = DBUtility.getTIConnection();
// logger.info("query-->" + query);
//
// connection = DatabaseUtility.getTizoneConnection();
// stmt = connection.createStatement();
//
// resultSet = stmt.executeQuery(query);
// String CusRole = "";
// int i = 0;
// while (resultSet.next()) {
//
// logger.info("Count--" + i);
// subType = resultSet.getString("subType");
// ifscResp.put("Status", "SUCCESS");
//
// ifscResp.put("subType", subType.trim());
// String ProdType = resultSet.getString("ProdType");
// ifscResp.put("ProdType", ProdType.trim());
//
// logger.info("IFSC Code-->" + CusRole);
//
// String subTYPEDesc = resultSet.getString("subTYPEDesc");
// ifscResp.put("subTYPEDesc", subTYPEDesc.trim());
//
// String eventcode = resultSet.getString("eventcode");
// ifscResp.put("eventcode", eventcode.trim());
//
// String MASTER_REF = resultSet.getString("MASTER_REF");
// ifscResp.put("MASTER_REF", MASTER_REF.trim());
//
// String eventRef = resultSet.getString("EventREFNO_PFIXSERL");
// ifscResp.put("eventRef", eventRef.trim());
//
// i = i + 1;
// }
//
// if (i == 0) {
// logger.info("No Process in master refno");
// ifscResp.put("subType", "");
// ifscResp.put("subTYPEDesc", "");
// ifscResp.put("ProdType", "");
// }
//
// } catch (SQLException e) {
// e.printStackTrace();
// ifscResp.put("subTYPEDesc", "");
// ifscResp.put("Status", "FILED");
// ifscResp.put("subType", "");
// ifscResp.put("ProdType", "");
// logger.info("Exception!!!!!!", e);
// } finally {
// logger.info("IFSC Code for sender and reciver-->" + ifscResp);
// DatabaseUtility.surrenderConnection(connection, stmt, resultSet);
// }
// logger.info("To getting Sender and recevier IFSC code process ended....");
// return ifscResp;
// }
//
// /**
// * SFMS 1.1
// *
// * @param bankReqSfmsMsg
// * @param MT
// * @param corr
// * @param masterRef
// * @param eventPriFix
// * @param serl
// * @return
// */
// public String updateSFMSMsg(String bankReqSfmsMsg, String MT, String corr,
// String masterRef, String eventPriFix,
// String serl) {
//
// logger.debug("updateSFMSMsg Process Start Here");
//
// String status = "";
// Map<String, String> responseMap = new HashMap<String, String>();
//
// try {
// HashMap<String, String> sfmsincomingMap =
// SFMSAdaptee.updateSFMSHeader(bankReqSfmsMsg, corr, masterRef,
// eventPriFix, serl);
//
// status = sfmsincomingMap.get("STATUS");
//
// if (status.equals("0")) {
// logger.debug("status " + status);
//
// bankReqSfmsMsg = sfmsincomingMap.get("HEADERPART");
// logger.debug("bankReqSFMSMge : " + bankReqSfmsMsg);
//
// bankReqSfmsMsg = SFMSAdaptee.updateSFMSBodyDateFormat(bankReqSfmsMsg, MT);
// logger.debug("DateFormat : " + bankReqSfmsMsg);
//
// bankReqSfmsMsg = SFMSAdaptee.updateSFMSBodyBicIfscFormat(bankReqSfmsMsg, MT,
// masterRef, eventPriFix,
// serl);
//
// } else {
// logger.debug("status " + status);
// logger.debug("Update SFMS Message failed ");
// }
//
// responseMap.put("bankReqSFMSMge", bankReqSfmsMsg);
// logger.debug("Return bankReqSFMSMge >>> " + bankReqSfmsMsg);
//
// } catch (Exception e) {
// logger.error("Exception! " + e.getMessage(), e);
// e.printStackTrace();
// status = ThemeBridgeStatusEnum.FAILED.toString();
// responseMap.put("swiftMessage", bankReqSfmsMsg);
// }
//
// StringBuilder sb = new StringBuilder();
// for (Map.Entry<String, String> entry : responseMap.entrySet()) {
// sb.append(entry.getValue());
// }
// logger.info("sb.toString() >>> " + sb.toString());
// return sb.toString();
// }
//
// /**
// * SFMS 1.2
// *
// * @param swiftMessage
// * @param corrlID
// * @param masterRef
// * @param eventpfx
// * @param eventserl
// * @return
// */
// public static HashMap<String, String> updateSFMSHeader(String swiftMessage,
// String corrlID, String masterRef,
// String eventpfx, String eventserl) {
//
// logger.debug("SFMS Header update process Started Here");
// String reSwiftMessage = "{A:APP";
// HashMap<String, String> sfmsincomingMap = new HashMap<String, String>();
// swiftMessage = swiftMessage.trim();
// boolean identifierFlag = false;
// SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
//
// // Message Identifier
// String mT = "";
// String msgIdent = "F01";
// // MsgIdent = getMessageIdentifier(swiftMessage);
// reSwiftMessage = reSwiftMessage + msgIdent;
// // Input/output Identifier (either I or O)
// reSwiftMessage = reSwiftMessage + "O";
// // Message type
// mT = m.getType();
// logger.debug("Msg Type : " + mT);
// reSwiftMessage = reSwiftMessage + mT;
// // Sub Message type (For IFN 298C01, this field should be C01, for
// // IFN100.
// // Message, this field should be XXX)
// String subMT = "XXX";
// reSwiftMessage = reSwiftMessage + subMT;
// String receiverIFSC = "", senderIFSC = "";
// String transSwiftRef = m.getBlock4().getTagValue("20");
// // String transSwiftRef =
// // ServiceLookupUtil.getReferenceNo(swiftMessage);
// // logger.info("Sender and receiver ifsc Code :>" + ifscResp);
// SwiftIncomingMessagerGenerator generator = new
// SwiftIncomingMessagerGenerator();
// // Sender IFSC Code-SBIN0001001.
//
// Map<String, String> map = getSenderInIFSCCode(transSwiftRef);
//
// String inStatus = map.get("status");
// Map<String, String> gridifscmap = new HashMap<String, String>();
// if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
// gridifscmap = generator.getSwiftBodyBicifscCode(masterRef, eventpfx,
// eventserl);
// }
// if (inStatus.equals("1")) {
// // REMBNK NXAB
// if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
// receiverIFSC = gridifscmap.get("OTHERBNK");
// } else {
// receiverIFSC = map.get("recIsfc");
// identifierFlag = true;
// }
// senderIFSC = map.get("sendserIFSC");
// reSwiftMessage = reSwiftMessage + receiverIFSC;
// reSwiftMessage = reSwiftMessage + senderIFSC;
// if (senderIFSC != null && !senderIFSC.isEmpty() && receiverIFSC != null &&
// !receiverIFSC.isEmpty()) {
// identifierFlag = true;
// } else {
// identifierFlag = false;
// }
//
// } else {
//
// logger.info("outside the instatus loop**************************************"
// + inStatus);
//
// Map<String, String> senderRece = generator.getSwiftBicifscCode(masterRef,
// eventpfx, eventserl);
//
// if (mT.equals("740") || mT.equals("710") || mT.equals("747")) {
// receiverIFSC = gridifscmap.get("OTHERBNK");
// } else {
// receiverIFSC = senderRece.get("recIsfc");
// }
// senderIFSC = senderRece.get("sendserIFSC");
// logger.debug("senderRece Mappp :>" + senderRece);
// logger.debug("senderIFSC :>" + senderIFSC);
// logger.debug("receiverIFSC :>" + receiverIFSC);
// if (senderIFSC != null && !senderIFSC.isEmpty() && receiverIFSC != null &&
// !receiverIFSC.isEmpty()) {
// reSwiftMessage = reSwiftMessage + senderIFSC;
// reSwiftMessage = reSwiftMessage + receiverIFSC;
// identifierFlag = true;
// } else {
// identifierFlag = false;
// }
// }
// // End receiver IFSC Code
// logger.info("identifierFlag :>" + identifierFlag);
// // receiverIFSC = getReceiverIFSC(swiftMessage);
// // Delivery notification flag
// if (identifierFlag) {
// reSwiftMessage = reSwiftMessage + "1";
// // Open Notification flag
// reSwiftMessage = reSwiftMessage + "1";
// // Non-delivery Warning flag
// reSwiftMessage = reSwiftMessage + "2";
// // Obsolescence Period
// reSwiftMessage = reSwiftMessage + "000";
// // Message User Reference (MUR)
// long MUR = 0;
// try {
// MUR = ThemeBridgeUtil.generateRandom(16);
// } catch (Exception e) {
// }
// reSwiftMessage = reSwiftMessage + String.valueOf(MUR) + "";
// // Possible Duplicate flag
// reSwiftMessage = reSwiftMessage + "2";
// // Service Identifier
// String ServiceIdentifier = "";
// if (mT != null && !mT.isEmpty()) {
// if (mT.startsWith("7")) {
// if (mT.equals("760") || mT.equals("767") || mT.equals("768") ||
// mT.equals("769")) {
// ServiceIdentifier = "BGS";
// } else {
// ServiceIdentifier = "ILC";
// logger.debug("inside the MT messages type loop" + mT + ServiceIdentifier);
// }
// }
// if (mT.startsWith("4")) {
// ServiceIdentifier = "CCL";
// logger.debug("inside the MT messages type loop " + mT + ServiceIdentifier);
// }
// if (mT.startsWith("9")) {
// ServiceIdentifier = "CPT";
// logger.debug("inside the MT messages type loop " + mT + ServiceIdentifier);
// }
// }
// reSwiftMessage = reSwiftMessage + ServiceIdentifier;
// // Originating date
// SimpleDateFormat dateCurrFormat = new SimpleDateFormat("yyyy/MM/dd");
// SimpleDateFormat dateChngeFormat = new SimpleDateFormat("yyyyMMdd");
// logger.debug(ThemeBridgeUtil.getCurrentDate());
// try {
// Date valueDate = dateCurrFormat.parse(ThemeBridgeUtil.getCurrentDate());
// logger.debug("Value Date-->" + dateChngeFormat.format(valueDate));
// reSwiftMessage = reSwiftMessage + dateChngeFormat.format(valueDate);
// } catch (ParseException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// // Originating time
// DateFormat timeFormat = new SimpleDateFormat("HHmm");
// Calendar calendar = Calendar.getInstance();
// reSwiftMessage = reSwiftMessage + timeFormat.format(calendar.getTime()) + "";
// // Testing and training flag
// reSwiftMessage = reSwiftMessage + "2";
// // Sequence Number
// reSwiftMessage = reSwiftMessage + ThemeBridgeUtil.generateRandom(9);
// // Filler
// String Filler = "XXXXXXXXX";
// reSwiftMessage = reSwiftMessage + Filler;
// // Unique Transaction Reference.
// String TransRef = "XXXXXXXXXXXXXXXX";
// // TransRef = getReferenceNo(swiftMessage);
// reSwiftMessage = reSwiftMessage + TransRef;
// // Priority Flag
// reSwiftMessage = reSwiftMessage + "99";
// // Final SFMS Header tag
// reSwiftMessage = reSwiftMessage + "}";
// logger.debug(reSwiftMessage.length());
// // Release 1: and 2: to A:
// String newHeader = "";
// int indexOfMT = swiftMessage.lastIndexOf("{1:");
// logger.debug("==>" + indexOfMT + "==>" + swiftMessage.length());
// if (indexOfMT >= 0 && indexOfMT < swiftMessage.length()) {
// newHeader = new StringBuffer(swiftMessage).substring(indexOfMT, 29 + 21);
// newHeader = newHeader.replaceAll("/", "_");
// logger.debug("--->" + newHeader);
// }
// reSwiftMessage = swiftMessage.replace(newHeader, reSwiftMessage);
// sfmsincomingMap.put("STATUS", "0");
// sfmsincomingMap.put("HEADERPART", reSwiftMessage);
// } else {
// reSwiftMessage = "Sender and Receiver IFSCCode is not available";
// sfmsincomingMap.put("STATUS", "1");
// sfmsincomingMap.put("HEADERPART", reSwiftMessage);
// }
// logger.debug("\n-------------SFMS Header-----------------------\n");
// logger.debug("SFMS Msg-->" + reSwiftMessage);
// logger.debug("\n-------------SFMS Header Finished-----------------------\n");
// return sfmsincomingMap;
// }
//
// /**
// * SFMS 1.3
// *
// * @param masterRef
// * @return
// */
// private static Map<String, String> getSenderInIFSCCode(String masterRef) {
//
// Connection con = null;
// ResultSet rs = null;
// Statement stmt = null;
// Map<String, String> map = new HashMap<String, String>();
// con = DatabaseUtility.getTizoneConnection();
// try {
// String query = "select trim(SENDERIFSC) as SENDER_IDFC, trim(RECIVERIFSC) as
// RECIVER_IFSC from SFMSINIFSCSTORE where SFMSINREF = '"
// + masterRef + "' order by id desc";
//
// logger.debug("Get Sender IFSC Code Query : " + query);
// stmt = con.createStatement();
// rs = stmt.executeQuery(query);
// int i = 0;
// if (rs.next()) {
// String sendserIFSC = rs.getString("SENDER_IDFC");
// String recIsfc = rs.getString("RECIVER_IFSC");
// map.put("sendserIFSC", sendserIFSC);
// map.put("recIsfc", recIsfc);
// i = 1;
// }
//
// if (i != 0) {
// map.put("status", "1");
// } else
// map.put("status", "0");
//
// } catch (SQLException e) {
// e.printStackTrace();
// logger.error("SQLException :>", e);
// map.put("status", "0");
//
// } finally {
// logger.debug("map : " + map);
// DatabaseUtility.surrenderConnection(con, stmt, rs);
// }
// return map;
// }
//
// /**
// * SFMS 1.4
// *
// * @param swiftMessage
// * @param MsgType
// * @return
// */
// public static String updateSFMSBodyDateFormat(String swiftMessage, String
// MsgType) {
//
// logger.info("UpdateSFMSBody DateFormat process started");
// logger.debug("Update SFMSBody Date Format SFMS Message -->" + swiftMessage);
//
// SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
// String SwiftSFMSDateFormat = "";
//
// try {
// SwiftSFMSDateFormat =
// ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SWIFT_DATE_FORMAT);
//
// } catch (Exception e) {
// SwiftSFMSDateFormat = "31C|31D|44C|30|31E|32A|33A|34A";
// e.printStackTrace();
// }
//
// try {
// logger.debug("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);
// logger.debug("MsgType -->" + MsgType);
//
// String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");
// for (String tag : prodDateKeys) {
// logger.debug("Date Tag -->" + tag);
// // tag = tag + ":";
// String value = m.getBlock4().getTagValue(tag);
// logger.debug("Key-->" + tag + "\tvalue -->" + value);
// // value = "250923IN";
// if (value != null && !value.equals("")) {
// int taglen = value.length();
// logger.debug(taglen);
// String tag2nd = "";
// if (taglen > 6) {
// tag2nd = value.substring(6, value.length());
// value = value.substring(0, 6);
// }
// String valueChange = ThemeBridgeUtil.getChangeFormat(value, "yyMMdd",
// "yyyyMMdd");
// // valueChange = "20150923" + "IN";
// // String valueChange = "20" + value;
// logger.debug("Key-->" + tag + "\t Old value -->" + value + "\t change value
// -->" + valueChange);
// logger.debug("<:::::::::::Date Replace CHanges INput\t:::::::::::>"
// + "\n Searching string\t::::::::::::>" + tag + ":" + value + tag2nd
// + "\n Replace string\t:::::::::::>" + tag + ":" + valueChange + tag2nd + " \t
// tag2nd:>"
// + tag2nd);
// swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tag +
// ":" + value + tag2nd,
// tag + ":" + valueChange + tag2nd);
// }
// tag = "";
// value = "";
// }
// } catch (Exception e) {
// e.printStackTrace();
// logger.error("Exception !!!!!!!!!!!!! ", e);
// }
// logger.debug("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);
// return swiftMessage;
// }
//
// /**
// * SFMS 1.6
// *
// * @param swiftMessage
// * @param MsgType
// * @param masterRef
// * @param eventPriFix
// * @param serl
// * @return
// */
// public static String updateSFMSBodyBicIfscFormat(String swiftMessage, String
// MsgType, String masterRef,
// String eventPriFix, String serl) {
//
// logger.info("updateSFMSBody BicIfscFormat process started");
// logger.debug("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);
//
// SwiftMessage m = (new ConversionService()).getMessageFromFIN(swiftMessage);
// String SwiftSFMSDateFormat = "";
// try {
//
// SwiftSFMSDateFormat =
// ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SFMS_DATE_FORMAT);
// } catch (Exception e) {
// // TODO: handle exception
// // SwiftSFMSDateFormat = DBPropertiesLoader.SFMSBicIfscFormat;
// e.printStackTrace();
// }
// try {
// logger.info("SwiftSFMSDateFormat -->" + SwiftSFMSDateFormat);
// logger.info("MsgType -->" + MsgType);
// SwiftIncomingMessagerGenerator generator = new
// SwiftIncomingMessagerGenerator();
// String[] prodDateKeys = SwiftSFMSDateFormat.split("\\|");
// Map<String, String> map = generator.getSwiftBodyBicifscCode(masterRef,
// eventPriFix, serl);
// logger.info("3 rd Party Ifsc Code list :>" + map);
// for (String tag : prodDateKeys) {
// logger.info("Date Tag -->" + tag);
// String string2 = tag + ":";
// String value = m.getBlock4().getTagValue(tag);
// logger.info("Key-->" + string2 + "\tvalue -->" + value);
// if (value != null && !value.equals("")) {
// // String valueChange =
// // ThemeBridgeUtil.getChangeFormat(
// // value, "yyMMdd", "yyyyMMdd");
// // Map<String, String> map = new HashMap<String, String>();
// String IFSC = map.get(tag);
// logger.info("IFSC :>" + IFSC);
// String valueChange = "XXXXXXXXXXX";
// if (ValidationsUtil.isValidString(IFSC)) {
// valueChange = IFSC;
// } else {
// valueChange = "XXXXXXXXXXX";
// }
// logger.info("Key-->" + string2 + "\t Old value -->" + value + "\t change
// value -->" + valueChange);
// logger.info("<:::::::::::Date Replace CHanges INput\t:::::::::::>"
// + "\n Searching string\t::::::::::::>" + string2 + value + "\n Replace
// string\t:::::::::::>"
// + string2 + valueChange);
// if (tag.equals("41A")) {
// valueChange = valueChange + value.substring(11, value.length());
// }
// swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, string2
// + value,
// string2 + valueChange);
// }
// string2 = "";
// value = "";
// }
// } catch (Exception e) {
// e.printStackTrace();
// logger.info("Exception !!!!!!!!!!!!! ", e);
// }
// logger.info("updateSFMSBodyDateFormat swiftMessage -->" + swiftMessage);
// return swiftMessage;
// }
//
// }
