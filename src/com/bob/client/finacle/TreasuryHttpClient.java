package com.bob.client.finacle;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.apache.log4j.Logger;

import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.xpath.BusinessSupportFXRateServiceXpath;
import com.bs.themebridge.xpath.XPathParsing;

/**
 * 
 * @since 2016-06-14
 * @version 1.0
 * @author Prasath Ravichandran
 */
public class TreasuryHttpClient {

	private final static Logger logger = Logger.getLogger(TreasuryHttpClient.class.getName());

	/**
	 * HTTP POST request
	 * 
	 * @param clientHttpUrl
	 * @return
	 * @throws Exception
	 */

	public static String sendPost(String clientHttpUrl, List<NameValuePair> urlParameters) throws Exception {

		logger.debug("ClientHttpUrl >> " + clientHttpUrl);

		String result = "";
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(clientHttpUrl);

		// add header
		post.setHeader("User-Agent", "");
		logger.debug("Milestone 00");

		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		// Calling end system
		logger.debug("Milestone 01 " + post);
		HttpResponse response = client.execute(post);
		logger.info(" response : " + response);

		logger.debug("Get Http Response values : " + httpURLResponseParsing(response.toString()));

		// BufferedReader rd = new BufferedReader(new
		// InputStreamReader(response.getEntity().getContent()));
		// logger.debug("Milestone 02");
		// StringBuffer httpResponseString = new StringBuffer();
		// String line = "";
		// logger.debug("Milestone 03");
		// while ((line = rd.readLine()) != null) {
		// logger.debug("Milestone 04 while loop");
		// httpResponseString.append(line);
		// }
		// result = httpResponseString.toString();
		// logger.debug("TEST : " + result);

		return result;
	}

	public static Map<String, String> httpURLResponseParsing(String httpResponse) {

		String responseValue = httpResponse.substring(httpResponse.indexOf("[") + 1, httpResponse.length() - 1);
		String[] arrayValues = responseValue.split(",");

		Map<String, String> mapResponse = new HashMap<String, String>();
		Map<String, String> mapResult = new HashMap<String, String>();

		for (int itr = 0; itr < arrayValues.length; itr++) {
			String key = arrayValues[itr].substring(0, arrayValues[itr].indexOf(":"));
			String value = arrayValues[itr].substring(arrayValues[itr].indexOf(":") + 1, arrayValues[itr].length());
			mapResponse.put(key.trim(), value.trim());
		}

		if (mapResponse.size() > 0 && mapResponse != null && mapResponse.get("Location") != null) {
			String urlValue = mapResponse.get("Location").substring(mapResponse.get("Location").indexOf("?") + 1,
					mapResponse.get("Location").length());
			String[] pairKeyValue = urlValue.split("&");
			for (int itr = 0; itr < pairKeyValue.length; itr++) {
				String[] keyvalue = pairKeyValue[itr].split("=");
				if (keyvalue.length == 2)
					mapResult.put(keyvalue[0], keyvalue[1]);
			}
		}
		return mapResult;
	}

	public static void main(String[] args) {

		TreasuryHttpClient gh = new TreasuryHttpClient();
		try {

			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			// urlParameters.add(new BasicNameValuePair("firstCurr", null));
			// urlParameters.add(new BasicNameValuePair("secondCurr", null));
			// urlParameters.add(new BasicNameValuePair("valueDate", null));
			// urlParameters.add(new BasicNameValuePair("direction", null));
			// urlParameters.add(new BasicNameValuePair("nostroFrom", null));
			// urlParameters.add(new BasicNameValuePair("nostroRef", null));
			//
			// urlParameters.add(new BasicNameValuePair("branchCode", null));
			// urlParameters.add(new BasicNameValuePair("rate", null));
			// urlParameters.add(new BasicNameValuePair("inputter", "TIPLUS"));
			// urlParameters.add(new BasicNameValuePair("pwd", "tiplus"));
			// urlParameters.add(new BasicNameValuePair("CRN", null));
			// urlParameters.add(new BasicNameValuePair("gid", null));
			// urlParameters.add(new BasicNameValuePair("msg", null));
			// urlParameters.add(new BasicNameValuePair("ls_sessionid", null));
			// urlParameters.add(new BasicNameValuePair("CONNECT", "CONNECT"));

			// urlParameters.add(new BasicNameValuePair("ls_sessionid",
			// "B8A47E882A06DEFE390484BB47792834"));
			// urlParameters.add(new BasicNameValuePair("DISCONNECT",
			// "DISCONNECT"));

			urlParameters.add(new BasicNameValuePair("firstCurr", "USD"));
			urlParameters.add(new BasicNameValuePair("secondCurr", "INR"));
			urlParameters.add(new BasicNameValuePair("Amount", "100"));
			urlParameters.add(new BasicNameValuePair("valueDate", "TODAY"));// Date
			urlParameters.add(new BasicNameValuePair("direction", "BUY"));
			urlParameters.add(new BasicNameValuePair("nostroFrom", "sometext"));// TODO
			urlParameters.add(new BasicNameValuePair("nostroRef", "sometext"));// TODO
			urlParameters.add(new BasicNameValuePair("branchCode", "0172"));
			urlParameters.add(new BasicNameValuePair("rate", "null"));
			urlParameters
					.add(new BasicNameValuePair("inputter", ConfigurationUtil.getValueFromKey("TreasuryUserName")));
			urlParameters.add(new BasicNameValuePair("pwd", "tiplus")); // Hard codede
			urlParameters.add(new BasicNameValuePair("CRN", "CRN6500073"));
			urlParameters.add(new BasicNameValuePair("gid", null));
			urlParameters.add(new BasicNameValuePair("msg", null));
			urlParameters.add(new BasicNameValuePair("ls_sessionid", "E38B426C9B7934F76887EAB10B32FC42"));
			urlParameters.add(new BasicNameValuePair("viewRates", "viewRates"));

			// urlParameters.add(new BasicNameValuePair("RFQ", "RFQ"));

			gh.sendPost("http://10.10.19.22:8086/Trapii/retservlet_trade_comb", urlParameters);

		} catch (Exception e) {
			e.printStackTrace();

		}

	}
}
