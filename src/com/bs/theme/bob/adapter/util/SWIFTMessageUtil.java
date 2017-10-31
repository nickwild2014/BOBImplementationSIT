package com.bs.theme.bob.adapter.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.prowidesoftware.swift.io.ConversionService;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftMessage;

public class SWIFTMessageUtil {

	private final static Logger logger = Logger.getLogger(SWIFTMessageUtil.class.getName());

	/**
	 * @param value
	 * @param tag
	 * @param swiftMessage
	 * @return
	 */
	public static String getSFMSDate(String value, String tag, String swiftMessage) {

		if (value != null && !value.equals("")) {
			int taglen = value.length();
			String tag2nd = "";
			if (taglen > 6) {
				tag2nd = value.substring(6, value.length());
				value = value.substring(0, 6);
			}
			/*
			 * Prasath remove below method we just need to add number "20" the
			 * only difference between this "yyMMdd" and this is "yyyyMMdd" "20"
			 * only.
			 */
			// String sfmsDate = ThemeBridgeUtil.getChangeFormat(value,
			// "yyMMdd", "yyyyMMdd");
			/**
			 * below line added by subhash
			 */
			String sfmsDate = 20 + value;
			swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tag + ":" + value + tag2nd,
					tag + ":" + sfmsDate + tag2nd);
		}
		return swiftMessage;
	}

	/**
	 * To get Master reference number
	 * 
	 * @param swiftOutmsg
	 * @return
	 */
	public static String getMasterReference(String swiftOutmsg) {

		String masterRef = "";
		try {
			// use SWIFT Parser to get master reference.
			SwiftMessage message = (new ConversionService()).getMessageFromFIN(swiftOutmsg);
			masterRef = message.getBlock4().getTagValue("20");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return masterRef;
	}

	/**
	 * To get swift message type
	 * 
	 * @param swiftMessage
	 * @return
	 */
	public static String getMessagetype(String swiftMessage) {

		String messagetype = "";
		try {
			// use SWIFT Parser to get getMessagetype.
			SwiftMessage message = (new ConversionService()).getMessageFromFIN(swiftMessage);
			messagetype = message.getType();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return messagetype;
	}

	/**
	 * @param swiftinmsg
	 * @return
	 */
	public static String getSwiftInMsgType(String swiftinmsg) {

		String messageType = "";
		int indexCode = swiftinmsg.indexOf("A:");
		logger.info(indexCode);
		if (indexCode >= 0 && indexCode < swiftinmsg.length()) {
			messageType = new StringBuffer(swiftinmsg).substring(indexCode + 9, indexCode + 12);

			logger.debug("Msg Type -> " + messageType);
		}
		return messageType;

	}

	/**
	 * @param correlationId
	 * @return eventRef
	 */
	public static String getEventReference(String correlationId) {

		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		String eventRef = "";
		String eventRefQuery = "SELECT A.REFNO_PFIX AS EVENTCODE, A.REFNO_SERL AS EVENTSEQ, (TRIM(A.REFNO_PFIX)||LPAD(TRIM(A.REFNO_SERL),3,'0')) AS EVENTREF FROM BASEEVENT A, RELITEM B, DOCRELITEM C, SWOPF D WHERE A.KEY97 = B.EVENT_KEY AND B.KEY97 = C.KEY97 AND C.KEY97 = D.OWNER AND D.SWORNO = "
				+ correlationId + "";
		try {
			con = DatabaseUtility.getTizoneConnection();
			stmt = con.createStatement();
			res = stmt.executeQuery(eventRefQuery);
			while (res.next()) {
				eventRef = res.getString("EVENTREF");
			}

		} catch (Exception e) {

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return eventRef;
	}

	/**
	 * 
	 * @param masterRef
	 * @param correlationID
	 *            / corID
	 * @return
	 */
	public static Map<String, String> getswiftMSGType(String masterRef, String correlationID) {

		String subType = "";
		Statement stmt = null;
		ResultSet resultSet = null;
		Connection connection = null;
		Map<String, String> ifscResp = new HashMap<String, String>();
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
				+ correlationID + "' ";
		try {

			int i = 0;
			String cusRole = "";
			connection = DatabaseUtility.getTizoneConnection();
			stmt = connection.createStatement();
			resultSet = stmt.executeQuery(query);
			while (resultSet.next()) {

				subType = resultSet.getString("subType");
				ifscResp.put("Status", "SUCCESS");

				ifscResp.put("subType", subType.trim());
				String ProdType = resultSet.getString("ProdType");
				ifscResp.put("ProdType", ProdType.trim());

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

		} finally {
			DatabaseUtility.surrenderConnection(connection, stmt, resultSet);
		}
		return ifscResp;
	}

	/**
	 * @param sourceSys
	 * @return
	 */
	public static String getCurrentDateofTISystem() {

		Date result;
		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		result = null;
		String tidate = "";
		try {
			String query = "SELECT PROCDATE AS TI_CURRENTDATE FROM DLYPRCCYCL";
			logger.info("Query fro TI Sysdate process:>" + query);
			con = DatabaseUtility.getTizoneConnection();

			stmt = con.createStatement();
			res = stmt.executeQuery(query);
			if (res.next()) {
				result = res.getDate("TI_CURRENTDATE");
			}

			DateFormat df = new SimpleDateFormat("yyMMdd");
			tidate = df.format(result);

			logger.info("The date is: " + tidate);

		} catch (Exception ex) {
			logger.error((Object) "Exception! Check the logs for detail", (Throwable) ex);

		} finally {
			DatabaseUtility.surrenderConnection(con, stmt, res);
		}
		return tidate;
	}

	/**
	 * @return current date
	 */
	public static String getCurrentDate() {

		DateFormat dateFormat = new SimpleDateFormat("yyMMdd");
		java.util.Date date = new java.util.Date();

		return dateFormat.format(date);
	}

	/**
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
	 * @param length
	 * @return random number
	 */
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
	 * @param swiftMessage
	 * @return
	 */
	public static Boolean validateSwiftMessage(String swiftMessage) {

		Boolean flag = null;
		SwiftParser parser;
		parser = new SwiftParser(swiftMessage);
		try {
			SwiftMessage message = parser.message();
			flag = message.isCOV();
		} catch (IOException ex) {
			logger.info("Exception!!!!!!!!!", ex);
		}
		return flag;
	}

	/**
	 * get serviceIdentifier value and sender IFSC code from SFMS.properties
	 * file
	 * 
	 * @param messageType
	 * @return propertiesMap
	 */
	public static Map<String, String> getSFMSPropertiesValue(String messageType) {

		String propertyFilePath = "SFMS.properties";
		Map<String, String> propertiesMap = new HashMap<String, String>();

		try {
			// FileReader reader = new FileReader("SFMS.properties");
			InputStream reader = SWIFTMessageUtil.class.getClassLoader().getResourceAsStream(propertyFilePath);
			// logger.debug("SFMS.properties : " + reader);
			Properties p = new Properties();
			p.load(reader);

			if (messageType != null && !messageType.isEmpty()) {
				propertiesMap.put("serviceIdentifier", p.getProperty(messageType));
			}
			propertiesMap.put("SenderIFSCCode", p.getProperty("SenderIFSCCode"));
			propertiesMap.put("DateTag", p.getProperty("DateTag"));

		} catch (FileNotFoundException e) {
			logger.debug("FileNotFoundException!");
			e.printStackTrace();

		} catch (IOException e) {
			logger.debug("IOException!");
			e.printStackTrace();
		}

		return propertiesMap;
	}

	/**
	 * @param prdType
	 * @param eventcode
	 * @param subType
	 * @return
	 */
	public static String checkinSFMSFlag(String prdType, String subType, String eventcode) {

		// event code is not required for validations
		int count = 0;
		String flag = "";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		String query = "SELECT COUNT(*) AS SFMSFLAG FROM EXTSFMSPRMA WHERE trim(PROTY) = '" + prdType
				+ "' AND trim(PRSUB) = '" + subType + "' ";
		// and EVENT = '" + eventcode + "' ";
		logger.debug("SFMSFlag Query : " + query);

		try {
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
			logger.error("CheckinSFMSFlag exceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			logger.info("SFMS Flag :>" + flag);
			DatabaseUtility.surrenderConnection(con, stmt, rs);
		}
		return flag;
	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static String getsenderIFSC(String swiftMessage) {
		try {
			swiftMessage = swiftMessage.substring(3 + 13, 3 + 13 + 11);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return swiftMessage;
	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static String getreceiverIFSC(String swiftMessage) {
		try {
			swiftMessage = swiftMessage.substring(3 + 13 + 11, 3 + 13 + 11 + 11);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return swiftMessage;
	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static String getapplicationID(String swiftMessage) {
		String applicationID = "";
		try {
			applicationID = swiftMessage.substring(6, 7);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return applicationID;
	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static String getserviceID(String swiftMessage) {
		String serviceID = "";
		try {
			serviceID = swiftMessage.substring(7, 9);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return serviceID;
	}

	/**
	 * @return
	 */
	public static String getSFMSDate() {

		String SFMSDate = "";
		Date valueDate = null;
		SimpleDateFormat dateCurrFormat = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat dateChngeFormat = new SimpleDateFormat("yyyyMMdd");
		// logger.debug(ThemeBridgeUtil.getCurrentDate());

		try {
			// valueDate =
			// dateCurrFormat.parse(ThemeBridgeUtil.getCurrentDate());
			valueDate = dateCurrFormat.parse(DateTimeUtil.getCurrentDate());
			SFMSDate = dateChngeFormat.format(valueDate);
			// logger.debug("Value Date-->" + SFMSDate);

		} catch (ParseException e) {
			logger.error("ParseException..! " + e.getMessage());
			e.printStackTrace();
		}

		return SFMSDate;
	}

	/**
	 * @return
	 */
	public static String getHourMins() {

		String hourMins = "";
		try {
			DateFormat timeFormat = new SimpleDateFormat("HHmm");
			Calendar calendar = Calendar.getInstance();
			hourMins = timeFormat.format(calendar.getTime());

		} catch (Exception e) {
			logger.error("Exception..! " + e.getMessage());
			e.printStackTrace();
		}

		return hourMins;
	}

	/**
	 * @param tagValue
	 * @param tagName
	 * @param swiftMessage
	 * @return
	 */
	public static String getSWIFTDate(String tagValue, String tagName, String swiftMessage) {

		if (tagValue != null && !tagValue.equals("")) {

			int taglen = tagValue.length();

			String tag2nd = "";
			if (taglen > 8) {
				tag2nd = tagValue.substring(8, tagValue.length());
				tagValue = tagValue.substring(0, 8);

			}
			String valueChange = DateTimeUtil.getDateTimeChangeFormat(tagValue, "yyyyMMdd", "yyMMdd");

			swiftMessage = ThemeBridgeUtil.stringReplaceCommonUtil(swiftMessage, tagName + ":" + tagValue + tag2nd,
					tagName + ":" + valueChange + tag2nd);

		}
		return swiftMessage;
	}

	/**
	 * 
	 * @param SFMSDateTag
	 * @param swiftMessage
	 * @return
	 */
	public static String changeSWIFTDate(String[] SFMSDateTag, String swiftMessage) {

		SwiftMessage swiftMsg = (new ConversionService()).getMessageFromFIN(swiftMessage);
		try {
			for (String tagName : SFMSDateTag) {
				String tagValue = swiftMsg.getBlock4().getTagValue(tagName);
				swiftMessage = SWIFTMessageUtil.getSFMSDate(tagValue, tagName, swiftMessage);
				tagName = "";
				tagValue = "";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return swiftMessage;

	}

	/**
	 * @param prodDateKeys
	 * @param swiftMessage
	 * @return swiftMessage
	 */
	public static String chageSFMSDate(String[] prodDateKeys, String swiftMessage) {
		SwiftMessage swiftMsg = (new ConversionService()).getMessageFromFIN(swiftMessage);
		try {

			for (String tagName : prodDateKeys) {

				String tagValue = swiftMsg.getBlock4().getTagValue(tagName);
				swiftMessage = SWIFTMessageUtil.getSWIFTDate(tagValue, tagName, swiftMessage);
				tagName = "";
				tagValue = "";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return swiftMessage;

	}

	/**
	 * @param swiftMessage
	 * @return
	 */
	public static boolean isUMACContains(String swiftMessage) {
		try {
			if (swiftMessage.contains("{UMAC:"))
				return true;
			else
				return false;
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * @param sfmsMessage
	 * @return sfmsMsg
	 */
	public static String removeUMAC(String sfmsMessage) {
		String sfmsMsg = "";
		try {
			int indexCode = sfmsMessage.indexOf("{UMAC:");
			if (indexCode > 0) {
				String UMACvale = sfmsMessage.substring(indexCode);
				UMACvale = sfmsMessage.substring(0, indexCode);
				sfmsMsg = UMACvale;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sfmsMsg;
	}

	public static void main(String[] args) {
		System.out.println(SWIFTMessageUtil.getSFMSPropertiesValue("768"));
	}
}
