package com.bob.client.finacle;

import java.io.IOException;
import java.sql.Timestamp;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.util.DateTimeUtil;

/**
 * 
 * @since 2016-06-14
 * @version 1.0
 * @author Supreme
 */
public class FinacleHttpClient {

	/**
	 * 
	 * @param inputXML
	 *            {@code allows object is }{@link String}
	 * @return responseXML {@link String}
	 * @throws HttpException
	 * @throws IOException
	 * @throws FinacleServiceException
	 */
	public static String postXML(String inputXML) throws HttpException, IOException, FinacleServiceException {

		// below url has to come from configuration file
		String strURL = "";
		strURL = ConfigurationUtil.getValueFromKey("FinacleHttpClientUrl");
		System.out.println("strURL "+strURL);

		PostMethod post = new PostMethod(strURL);

		String responseXML = null;
		StringRequestEntity requestEntity = new StringRequestEntity(inputXML, "text/xml", "UTF-8");
		post.setRequestEntity(requestEntity);
		// Get HTTP client
		HttpClient httpclient = new HttpClient();
		// Execute request
		try {
			int result = httpclient.executeMethod(post);
			// Display status code
			// if result code is not 200 OK then throw an exception here.
			if (result != 200) {
				throw new FinacleServiceException("Server returned errror code " + result);
			}
			responseXML = post.getResponseBodyAsString();
			if (responseXML == null || responseXML.trim().equals("")) {
				throw new FinacleServiceException("Server did not return any result");
			}
		} finally {
			// Release current connection to the connection pool once you are
			// done
			post.releaseConnection();
		}
		return responseXML;
	}
	
	public static String postXML(String inputXML,String availBalFinacleUrl) throws HttpException, IOException, FinacleServiceException {

		// below url has to come from configuration file
		String strURL = "";
		strURL = ConfigurationUtil.getValueFromKey("FinacleHttpClientUrl");
		System.out.println("strURL "+strURL);

		PostMethod post = new PostMethod(strURL);

		String responseXML = null;
		StringRequestEntity requestEntity = new StringRequestEntity(inputXML, "text/xml", "UTF-8");
		post.setRequestEntity(requestEntity);
		// Get HTTP client
		HttpClient httpclient = new HttpClient();
		// Execute request
		try {
			int result = httpclient.executeMethod(post);
			// Display status code
			// if result code is not 200 OK then throw an exception here.
			if (result != 200) {
				throw new FinacleServiceException("Server returned errror code " + result);
			}
			responseXML = post.getResponseBodyAsString();
			if (responseXML == null || responseXML.trim().equals("")) {
				throw new FinacleServiceException("Server did not return any result");
			}
		} finally {
			// Release current connection to the connection pool once you are
			// done
			post.releaseConnection();
		}
		return responseXML;
	}

	/**
	 * Only for Gatewat testing
	 * 
	 * @param inputXML
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 * @throws FinacleServiceException
	 */
	public static String processFinacle(String finacleRequest, String gateway)
			throws HttpException, IOException, FinacleServiceException {

		String finacleResponse = null;
		Timestamp bankReqTime = DateTimeUtil.getSqlLocalDateTime();

		finacleResponse = postXML(finacleRequest);
		Timestamp bankResTime = DateTimeUtil.getSqlLocalDateTime();

		// ServiceLogging.pushLogData("Finacle", "Finacle", "ZONE1", "",
		// "ZONE1", "FINACLE", "", "", finacleRequest,
		// finacleResponse, "AVAILABLE", "", "", false, "0", "Finacle gateway
		// test", null, bankReqTime,
		// bankResTime, null);

		// New Logging
		ServiceLogging.pushLogData("SERVICE", "OPERATION", "ZONE", "BRANCH", "ZONE1", "FINACLE", "", "", "SUCCEEDED",
				"", "", finacleRequest, finacleResponse, bankReqTime, bankResTime, bankReqTime, bankResTime, "", "", "",
				"", false, "0", "ERROR DESC");

		return finacleResponse;
	}

	public static void main(String[] args) {
System.out.println(ConfigurationUtil.getValueFromKey("FinacleHttpClientUrl"));
	}
}
