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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

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
import com.bs.themebridge.xpath.RequestHeaderXpath;
import com.bs.themebridge.xpath.XPathParsing;

public class AccountAccountSearchAdapteeInternal extends ServiceProcessorUtil implements AdapteeInterface {

	public AccountAccountSearchAdapteeInternal(String inputXml)
			throws ParserConfigurationException, SAXException, IOException, JAXBException {
		super(inputXml);
	}

	public AccountAccountSearchAdapteeInternal() {
	}
	
	private String sourceSystem = "";
	private  String targetSystem = "";
	
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
	private String relatedParty = "";
	private String postingAmount = "";
	private String postingProduct = "";
	private String postingSubProduct = "";
	private String postingCurrency = "";

	public static void main(String[] args) {

		AccountAccountSearchAdapteeInternal accSearchObj = new AccountAccountSearchAdapteeInternal();
		try {
			String testxml = ThemeBridgeUtil.readFile("C:\\Users\\KXT51472\\Desktop\\new293.AccSearch.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\Account.AccountSearch-REQUEST.xml");
			// .readFile("C:\\Users\\KXT51472\\Desktop\\AccountData\\SerachREQ.xml");

			String resp = accSearchObj.process(testxml);
			System.out.println("000000000" + resp);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String tirequestXML) {

		//return "bob AccountSearch implementaion called";
		
		System.out.println(" ************ Account.Search adaptee process started ************ ");
		String status = ThemeBridgeStatusEnum.SUCCEEDED.toString();
		String errorMsg = "";
		String serviceStatus = "SUCCEEDED";
		try {
			tiRequest = tirequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			System.out.println("Account.Search TI Request : \n" + tiRequest);

			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			HashMap<String, String> tiReqValues = getBankRequestFromTiRequest(tirequestXML);
			bankRequest = tiReqValues.toString();
			System.out.println("Account.Search BankRequest : \n" + bankRequest);

			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			List<HashMap<String, String>> accDataMapList = getAccountData(tiReqValues);
			System.out.println("TESTST >>> " + accDataMapList.size());

			if (accDataMapList.size() > 0) {
				tiResponse = getTIResponse(accDataMapList);
				System.out.println("Account.Search TI Response : \n" + tiResponse);
			} else {
				tiResponse = getDefaultErrorResponse("Account Search: No record found [IM]");
			}

			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			// System.out.println("Loop 5 completed");

		} catch (Exception e) {
			serviceStatus = "FAILED";
			status = ThemeBridgeStatusEnum.FAILED.toString();
			errorMsg = e.getMessage();
			tiResponse = getDefaultErrorResponse("Account Search: No record found [IM]");

		} finally {
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			processtime = DateTimeUtil.getSqlLocalDateTime();

			
			ServiceLogging.pushLogData("Account", "AccountSearch", sourceSystem, branch, sourceSystem,
					targetSystem, masterReference, eventReference, status, tiRequest, tiResponse, bankRequest,
					bankResponse, tiReqTime, bankReqTime, bankResTime, tiResTime, "", "", "", "", false, "0", errorMsg);


			System.out.println("finally block completed..!!");
		}
		System.out.println(" ************ Account.Search adaptee process ended ************ ");
		return tiResponse;

	}

	/**
	 * @ @param
	 *       errorMsg {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getDefaultErrorResponse(String errorMsg) {

		System.out.println("***** Account.Search error response initiated *****");

		String response = "";
		Map<String, String> tokens = new HashMap<String, String>();

		try {
			tokens.put("Status", "SUCCEEDED");
			tokens.put("Info", "");
			tokens.put("Error", errorMsg);
			tokens.put("Warning", "");
			tokens.put("CorrelationId", correlationId);
			tokens.put("AccountSearchResult", "");
			tokens.put("AdditionalResults", "");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			String responseTemplate;
			InputStream anInputStream = AccountAccountSearchAdapteeInternal.class.getClassLoader()
					.getResourceAsStream("Account.SearchResponseTemplate.xml");
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

	public String getTIResponse(List<HashMap<String, String>> mapList)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		try {
			InputStream anInputStream = AccountAccountSearchAdapteeInternal.class.getClassLoader()
					.getResourceAsStream("AAA_Account.AccountSearch-TI internal RESPONSE.xml");
			String requestTemplate = ThemeBridgeUtil.readFile(anInputStream);

			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("Status", "SUCCEEDED");
			tokens.put("Error", "");
			tokens.put("Warning", "");
			tokens.put("Info", "");
			tokens.put("CorrelationId", correlationId);

			String AccountSearchResult = resultset(mapList);

			tokens.put("AccountSearchResult", AccountSearchResult);
			tokens.put("AdditionalResults", "");
			System.out.println("Milestone 02");

			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(requestTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			String responseXML = reader.toString();
			responseXML = CSVToMapping.RemoveEmptyTagXML(responseXML);
			// System.out.println("Removed empty <tag> responseXML : " +
			// responseXML);
			result = responseXML;
			reader.close();

		} catch (Exception e) {
			System.out.println("Exception e " + e.getMessage());

		} finally {

		}

		return result;
	}

	public String resultset(List<HashMap<String, String>> mapList) {

		String result = "";
		StringBuilder xferTrnDetail = new StringBuilder();
		try {
			int mapSize = mapList.size();
			System.out.println("AccountSearchResult size : " + mapSize);

			for (int mapiterator = 0; mapiterator < mapSize; mapiterator++) {

				xferTrnDetail.append("<m:AccountSearchResult>");
				xferTrnDetail.append("\n<m:Branch>" + mapList.get(mapiterator).get("BRCH_MNM") + "</m:Branch>");
				// xferTrnDetail
				// .append("\n<m:BranchNumber>" +
				// mapList.get(mapiterator).get("BRCH_MNM") +
				// "</m:BranchNumber>");
				xferTrnDetail.append("\n<m:BranchNumber>" + "" + "</m:BranchNumber>");
				xferTrnDetail.append("\n<m:Customer>" + mapList.get(mapiterator).get("CUS_MNM") + "</m:Customer>");
				// xferTrnDetail.append(
				// "\n<m:CustomerNumber>" +
				// mapList.get(mapiterator).get("CUS_MNM") +
				// "</m:CustomerNumber>");
				xferTrnDetail.append("\n<m:CustomerNumber>" + "" + "</m:CustomerNumber>");
				xferTrnDetail.append("\n<m:SystemParameter>" + "" + "</m:SystemParameter>");
				xferTrnDetail.append("\n<m:ChargeCode>" + "" + "</m:ChargeCode>");
				xferTrnDetail.append("\n<m:CategoryCode>" + "" + "</m:CategoryCode>");
				xferTrnDetail
						.append("\n<m:AccountType>" + mapList.get(mapiterator).get("ACC_TYPE") + "</m:AccountType>");
				xferTrnDetail.append("\n<m:Currency>" + mapList.get(mapiterator).get("CURRENCY") + "</m:Currency>");
				xferTrnDetail.append("\n<m:OtherCurrency>" + "" + "</m:OtherCurrency>");
				xferTrnDetail.append("\n<m:BackOfficeAccount>" + mapList.get(mapiterator).get("BO_ACCTNO")
						+ "</m:BackOfficeAccount>");
				xferTrnDetail.append("\n<m:ShortName>" + mapList.get(mapiterator).get("SHORTNAME") + "</m:ShortName>");
				xferTrnDetail.append(
						"\n<m:ExternalAccount>" + mapList.get(mapiterator).get("EXT_ACCTNO") + "</m:ExternalAccount>");
				xferTrnDetail.append("\n<m:IBAN>" + "" + "</m:IBAN>");
				xferTrnDetail.append("</m:AccountSearchResult>");
			}
			result = xferTrnDetail.toString();

		} catch (Exception e) {
			System.out.println("Exceptions e " + e.getMessage());
			e.printStackTrace();

		} finally {
			// System.out.println("Chile mapping completed..!!");
		}

		return result;

	}

	public List<HashMap<String, String>> getAccountData(HashMap<String, String> mapList)
			throws XPathExpressionException, SAXException, IOException {

		Connection con = null;
		ResultSet res = null;
		PreparedStatement ps = null;
		List<HashMap<String, String>> accountDataMapList = new ArrayList<HashMap<String, String>>();

		String branch = "";
		if (mapList.get("branch") != null)
			branch = mapList.get("branch");

		String customer = "";
		if (mapList.get("Customer") != null)
			customer = mapList.get("Customer");

		String accType = "";
		if (mapList.get("AccountType") != null)
			accType = mapList.get("AccountType");

		String currency = "";
		if (mapList.get("Currency") != null)
			currency = mapList.get("Currency");

		String boAccountNumber = "";
		if (mapList.get("BackOfficeAccount") != null)
			boAccountNumber = mapList.get("BackOfficeAccount");

		String shortName = "";
		if (mapList.get("ShortName") != null)
			shortName = mapList.get("ShortName");

		String category = "";
		if (mapList.get("Branch") != null)
			category = mapList.get("Branch");

		masterReference = customer;
//		String accSearchQuery = "SELECT trim(BRCH_MNM) as BRCH_MNM, trim(CUS_MNM) CUS_MNM, trim(ACC_TYPE) ACC_TYPE, trim(CATEGORY) CATEGORY, trim(CURRENCY) CURRENCY, trim(SHORTNAME) SHORTNAME, trim(BO_ACCTNO) BO_ACCTNO, trim(EXT_ACCTNO) EXT_ACCTNO, trim(IBAN) IBAN FROM ACCOUNT WHERE BRCH_MNM  LIKE '%"
//				+ branch + "%' and CUS_MNM like '%" + customer + "%' and ACC_TYPE like '%" + accType
//				+ "%' and CURRENCY like '%" + currency + "%' and BO_ACCTNO like '%" + boAccountNumber
//				+ "%' and SHORTNAME like '%" + shortName + "%' ";
		
		String accSearchQuery = "SELECT trim(BRCH_MNM) as BRCH_MNM, trim(CUS_MNM) CUS_MNM, trim(ACC_TYPE) ACC_TYPE, trim(CATEGORY) CATEGORY, trim(CURRENCY) CURRENCY, trim(SHORTNAME) SHORTNAME, trim(BO_ACCTNO) BO_ACCTNO, trim(EXT_ACCTNO) EXT_ACCTNO, trim(IBAN) IBAN FROM ACCOUNT WHERE BRCH_MNM  LIKE '%1368%'  and CUS_MNM like '%72765118%' and ACC_TYPE like '%CA%' and CURRENCY like '%INR%' and BO_ACCTNO like '%BO-1368-72765118-CA-INR-3%' and SHORTNAME like '%AADI DIAMONDS A%'";
		// + " and CATEGORY like '%" + category + "%' ";
		System.out.println("SwiftDetails : " + accSearchQuery);

		// String whereClause = " where ";
		// if (mapList.get("Branch") != null) {
		// whereClause = whereClause + mapList.get("Branch");
		// }

		try {
			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(accSearchQuery);
			res = ps.executeQuery();

			while (res.next()) {
				HashMap<String, String> accountDataMap = new HashMap<String, String>();
				accountDataMap.put("BRCH_MNM", res.getString("BRCH_MNM"));
				accountDataMap.put("CUS_MNM", res.getString("CUS_MNM"));
				accountDataMap.put("ACC_TYPE", res.getString("ACC_TYPE"));
				accountDataMap.put("CATEGORY", res.getString("CATEGORY"));
				accountDataMap.put("CURRENCY", res.getString("CURRENCY"));
				accountDataMap.put("SHORTNAME", res.getString("SHORTNAME"));
				accountDataMap.put("BO_ACCTNO", res.getString("BO_ACCTNO"));
				accountDataMap.put("EXT_ACCTNO", res.getString("EXT_ACCTNO"));
				accountDataMap.put("IBAN", res.getString("IBAN"));
				accountDataMapList.add(accountDataMap);
			}

		} catch (SQLException e) {
			System.out.println("SQLException..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, res);
		}

		// System.out.println("accountDataMapList : " + accountDataMapList);
		return accountDataMapList;
	}

	public HashMap<String, String> getBankRequestFromTiRequest(String requestXML)
			throws XPathExpressionException, SAXException, IOException {

		String result = "";
		HashMap<String, String> tiReqValues = new HashMap<String, String>();
		try {
			sourceSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.SOURCESYSTEM);
			targetSystem = XPathParsing.getValue(requestXML, RequestHeaderXpath.TARGETSYSTEM);
			
			correlationId = XPathParsing.getValue(requestXML, "/ServiceRequest/RequestHeader/CorrelationId");
			System.out.println("Milestone 01A");
			String SearchType = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/SearchType");
			String AccountSearchFormat = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/AccountSearchFormat");
			String BranchNumber = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/BranchNumber");
			String Customer = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/Customer");
			String CustomerNumber = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/CustomerNumber");
			System.out.println("Milestone 01B");
			branch = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/Branch");
			String SystemParameter = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/SystemParameter");
			String ChargeCode = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/ChargeCode");
			System.out.println("Milestone 01C");
			String CategoryCode = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/CategoryCode");
			String AccountType = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/AccountType");
			String Currency = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/Currency");
			String CurrencyNumber = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/CurrencyNumber");
			String OtherCurrency = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/OtherCurrency");
			String OtherCurrencyNumber = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/OtherCurrencyNumber");
			String BackOfficeAccount = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/BackOfficeAccount");
			String ShortName = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/ShortName");
			String ExternalAccount = XPathParsing.getValue(requestXML,
					"/ServiceRequest/AccountSearchRequest/ExternalAccount");
			String IBAN = XPathParsing.getValue(requestXML, "/ServiceRequest/AccountSearchRequest/IBAN");

			// System.out.println("Milestone 01D");
			tiReqValues.put("SearchType", SearchType);
			tiReqValues.put("AccountSearchFormat", AccountSearchFormat);
			tiReqValues.put("BranchNumber", BranchNumber);
			tiReqValues.put("branch", branch);
			tiReqValues.put("ChargeCode", ChargeCode);
			tiReqValues.put("Customer", Customer);
			tiReqValues.put("CustomerNumber", CustomerNumber);
			tiReqValues.put("SystemParameter", SystemParameter);
			tiReqValues.put("AccountType", AccountType);
			tiReqValues.put("Currency", Currency);
			tiReqValues.put("Currency", Currency);
			tiReqValues.put("CurrencyNumber", CurrencyNumber);
			// tiReqValues.put("OtherCurrency", OtherCurrency);
			// tiReqValues.put("OtherCurrencyNumber", OtherCurrencyNumber);
			tiReqValues.put("BackOfficeAccount", BackOfficeAccount);
			tiReqValues.put("ShortName", ShortName);
			// tiReqValues.put("ExternalAccount", ExternalAccount);
			// tiReqValues.put("IBAN", IBAN);

		} catch (Exception e) {
			System.out.println("TiValues Exceptions..! " + e.getMessage());
			e.printStackTrace();
		}

		// System.out.println(">>>>>>>>" + tiReqValues);
		return tiReqValues;
	}

}
