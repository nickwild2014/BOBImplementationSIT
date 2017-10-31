package com.bs.theme.bob.adapter.adaptee;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.bs.theme.bob.template.util.KotakConstant;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.serverinterface.AdapteeInterface;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.CSVToMapping;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ServiceProcessorUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.XPathParsing;

public class BusinessSupportFXRateServiceInternal extends ServiceProcessorUtil implements AdapteeInterface {
	
	private final static Logger logger = Logger.getLogger(BusinessSupportFXRateServiceInternal.class.getName());

	private String sourceSystem = "";
	private String targetSystem = "";

	private static String branch = "";
	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private Timestamp processtime = null;
	private String tiRequest = "";
	private String tiResponse = "";
	private String bankRequest = "";
	private String bankResponse = "";
	private String correlationId = "";
	private String eventReference = "";
	private String masterReference = "";
	private String dealTicketId = "";
	
	String Date1 = "";
	String TxnAmount1 = "";
	String FromCrncy1 = "";
	String ToCrncy = "";
	String RateCode = "";
	String tresuryQuery ="";
	
	public HashMap<String,String> getResultFromTreasury(String dealTicketId)
	{
		Connection con = null;
		ResultSet res = null;
		PreparedStatement ps = null;
		
		HashMap<String,String> rateMap = new HashMap<String,String>();
		
		 tresuryQuery = "SELECT substr(Deal.PROXY_GROUP_FULL_NAME,0,(InStr(Deal.PROXY_GROUP_FULL_NAME,'/')-1)) "
				+ "AS Branch_Alpha,REQUIREMENT.MAKER_REFERENCE AS Kplus_Number, c.CCY_PAIR# AS CCY_Pair,decode"
				+ "(DEAL.FX_CROSS_DEAL#CCY1,LEG.DEALT_CCY,LEG.DEALT_AMOUNT,LEG.CONTRA_AMOUNT) AS Amount1 ,"
				+ "decode(DEAL.FX_CROSS_DEAL#CCY2,LEG.DEALT_CCY,LEG.DEALT_AMOUNT,LEG.CONTRA_AMOUNT) AS Amount2,  "
				+ "leg.QUOTE#ALL_IN AS ALL_IN_Rate FROM retprod.DEAL, retprod.LEG, retprod.REQUIREMENT, "
				+ "retprod.CROSS_COMPONENT c WHERE (LEG.OWNER=DEAL.ID AND REQUIREMENT.OWNER=LEG.ID AND (deal.VERSION#SUPERSEDED# = 'N' "
				+ "OR deal.VERSION#SUPERSEDED# IS NULL) AND c.NUM#=0 AND c.OWNER=LEG.ID AND REQUIREMENT.MAKER_REFERENCE = "+dealTicketId+" ) "
				+ "ORDER BY DEAL.GID_ID";
		try {
			con = DatabaseUtility.getTresuryConnection();
			ps = con.prepareStatement(tresuryQuery);
			res = ps.executeQuery();
			while (res.next()) {
				String BRANCH_ALPHA = res.getString("Branch_Alpha");
				String Kplus_Number = res.getString("Kplus_Number");
				String Currency_Pair = res.getString("CCY_Pair");
				String Amount1 = res.getString("Amount1");
				String Amount2 = res.getString("Amount2");
				String Client_Spot = res.getString("ALL_IN_Rate");
				
				rateMap.put("BRANCH_ALPHA", BRANCH_ALPHA);
				rateMap.put("Kplus_Number", Kplus_Number);
				rateMap.put("Currency_Pair", Currency_Pair);
				rateMap.put("Amount1", Amount1);
				rateMap.put("Amount2", Amount2);
				rateMap.put("Client_Spot", Client_Spot);
				
			}
		} catch (SQLException e) {
			System.out.println("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}
		
		return rateMap;
	}

	@Override
	public String process(String tirequestXML) throws Exception {
		dealTicketId = XPathParsing.getValue(tirequestXML, "/ServiceRequest/FXRateServiceRequest/DealTicketId");
		System.out.println(" ************ FXRateService adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			System.out.println("FXRateService TI Request : \n" + tiRequest);
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			bankRequest = tresuryQuery;
			System.out.println("FXRateService BankRequest : \n" + bankRequest);
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			HashMap<String,String> rateMap = getResultFromTreasury(dealTicketId);
			bankResponse = rateMap.toString();
			System.out.println("FXRateService TI Response : \n" + bankResponse);
			if (rateMap != null && rateMap.size()>0) {
				tiResponse = getTIResponseFromBankResponse(rateMap);
				System.out.println(tiResponse);
			} else {
				tiResponse = getDefaultErrorResponse("FXRateService: No record found [IM]");
			}
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
		} catch (Exception e) {
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			tiResponse = getDefaultErrorResponse("FXRateService: No record found [IM]");

		} finally {
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			processtime = DateTimeUtil.getSqlLocalDateTime();
			ServiceLogging.pushLogData("BusinessSupport", "FXRateServiceIn", sourceSystem, branch, sourceSystem, targetSystem,
					masterReference, eventReference, status, tiRequest, tiResponse, bankRequest, bankResponse,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);
			System.out.println("finally block completed..!!");
		}
		return tiResponse;
	}
	
	public String getDefaultErrorResponse(String errorMsg) {

		System.out.println("***** Account Search error response initiated *****");

		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			tokens.put("Status", "FAILED");
			tokens.put("Info", "");
			tokens.put("Error", errorMsg);
			tokens.put("Warning", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("dealTicketId", dealTicketId);
			tokens.put("RateCode", RateCode);
			
			tokens.put("TxnAmount1", "");
			tokens.put("FromCrncy1", "");
			tokens.put("ToCrncy", "");
			
			tokens.put("Reciprocal", "");
			tokens.put("ExchangeRate", "");
			
			tokens.put("DealDirection", "");
			tokens.put("FXRateType", "");
			tokens.put("BuyCurrency", "");
			tokens.put("BuyCurrencyQuotationUnit", "");
			tokens.put("BuyRateToBase", "");
			tokens.put("SellCurrency", "");
			tokens.put("SellCurrencyQuotationUnit", "");
			tokens.put("SellRateToBase", "");
			tokens.put("FxRateDate", "");
			tokens.put("FxRateTime", "");
			tokens.put("BaseCurrency", "");
			tokens.put("OrderId", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			String responseTemplate;
			InputStream anInputStream = AccountAccountSearchAdapteeInternal.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.FX_RATE_TI_RESPONSE_TEMPLATE);
			responseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Reader fileValue = new StringReader(responseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			String responseXML = reader.toString();
			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			// System.out.println("Removed empty <tag> responseXML : " +
			// responseXML);
			response = responseXML;
			reader.close();

		} catch (Exception e) {
			System.out.println("Default exception! " + e.getMessage());
			e.printStackTrace();
		}
		return response;
	}


	
		
		
	private String getTIResponseFromBankResponse(HashMap<String, String> rateMap)
			throws XPathExpressionException, SAXException, IOException {
		String tiResponseXML = null;
		try {
			//rateMap.get("BRANCH_ALPHA");
			//rateMap.get("Kplus_Number");
			try {
				InputStream anInputStream = BackOfficeBatchAdaptee.class.getClassLoader()
						.getResourceAsStream(RequestResponseTemplate.FX_RATE_TI_RESPONSE_TEMPLATE);
				String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);
				String dateTime = DateTimeUtil.getDateAsEndSystemFormat();
				String requestId = "Req_" + correlationId;
				Map<String, String> tokens = new HashMap<String, String>();
				tokens.put("dateTime", dateTime);
				tokens.put("requestId", requestId);
				tokens.put("ChannelId", KotakConstant.CHANNELID);
				tokens.put("BankId", KotakConstant.BANKID);
				tokens.put("ServiceReqVersion", KotakConstant.SERVICEREQUESTVERSION);
				tokens.put("CorrelationId", correlationId);
				tokens.put("dealTicketId", dealTicketId);
				tokens.put("RateCode", RateCode);
				
				tokens.put("TxnAmount1", rateMap.get("Amount1"));
				tokens.put("FromCrncy1", rateMap.get("Currency_Pair"));
				tokens.put("ToCrncy", rateMap.get("Currency_Pair"));
				
				tokens.put("Reciprocal", rateMap.get("Amount1"));
				tokens.put("ExchangeRate", rateMap.get("Client_Spot"));
				
				tokens.put("DealDirection", "");
				tokens.put("FXRateType", "");
				tokens.put("BuyCurrency", "");
				tokens.put("BuyCurrencyQuotationUnit", "");
				tokens.put("BuyRateToBase", "");
				tokens.put("SellCurrency", "");
				tokens.put("SellCurrencyQuotationUnit", "");
				tokens.put("SellRateToBase", "");
				tokens.put("FxRateDate", Date1);
				tokens.put("FxRateTime", "");
				tokens.put("BaseCurrency", "INR");
				tokens.put("OrderId", "");

				// if(STATUS.equalsIgnoreCase("SUCCESS"))
				tokens.put("Status", "SUCCEEDED");
				// else tokens.put("Status", "FAILED");
				tokens.put("Error", "");
				tokens.put("Warning", "");
				tokens.put("Info", "");

				MapTokenResolver resolver = new MapTokenResolver(tokens);
				Reader fileValue = new StringReader(requestTemplate);
				Reader reader = new TokenReplacingReader(fileValue, resolver);
				tiResponseXML = reader.toString();
				reader.close();
			} catch (Exception e) {
				logger.error("Bank Request XML generate exceptions! " + e.getMessage());
				e.printStackTrace();
				return e.getMessage();
			}
			logger.info("Final tiResponseXML message-->" + tiResponseXML);

		} catch (Exception e) {
			System.out.println("tiResponseXML Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		return tiResponseXML;
	}



}
