package com.bob.client.finacle;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

/**
 * 
 * @since 2016-06-14
 * @version 1.0
 * @author Supreme
 */
public class ExternalHttpClient {

	private final static Logger logger = Logger.getLogger(ExternalHttpClient.class.getName());

	/**
	 * 
	 * @param inputXML
	 *            {@code allows object is }{@link String}
	 * @return responseXML {@link String}
	 * @throws HttpException
	 * @throws IOException
	 * @throws FinacleServiceException
	 */
	public static String postXML(String inputXML, String clientHttpUrl)
			throws HttpException, IOException, TreasuryServiceException {

		logger.debug("HttpClientUrl : " + clientHttpUrl);

		PostMethod post = new PostMethod(clientHttpUrl);

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
				throw new TreasuryServiceException("Server returned errror code " + result);
			}
			responseXML = post.getResponseBodyAsString();
			if (responseXML == null || responseXML.trim().equals("")) {
				throw new TreasuryServiceException("Server did not return any result");
			}
		} finally {
			// Release current connection to the connection pool once you are
			// done
			post.releaseConnection();
		}
		return responseXML;
	}

	public static void main(String[] args) {

	}
}
