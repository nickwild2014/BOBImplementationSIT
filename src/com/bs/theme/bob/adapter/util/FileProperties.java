package com.bs.theme.bob.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.ValidationsUtil;

public class FileProperties {

	private final static Logger logger = Logger.getLogger(FileProperties.class);

	public static Properties property = null;
	public static Properties fileProperty = null;

	/**
	 * To get a key value from bridgefile.properties
	 * 
	 * @param keyName
	 *            {@code allows } {@link String}
	 * @return keyValue {@link String}
	 */
	public static String getFileProperties(String keyName) {

		// logger.debug("Property KeyName : " + keyName);
		if (!ValidationsUtil.isValidObject(fileProperty)) {
			fileProperty = new Properties();
			InputStream inputStream = FileProperties.class.getClassLoader()
					.getResourceAsStream("bridgefile.properties");
			try {
				fileProperty.load(inputStream);
				// inputStream.close();
			} catch (Exception e) {
				logger.error("Load property exception! Check the logs for details", e);
			} finally {
				try {
					if (ValidationsUtil.isValidObject(inputStream))
						inputStream.close();
				} catch (Exception e) {
					logger.error("Close property exception! Check the logs for details", e);
				}
			}
		}
		// logger.debug("Property Value : " +
		// fileProperty.getProperty(keyName));
		return fileProperty.getProperty(keyName);
	}

	/**
	 * To get a key value from bridgefile.properties
	 * 
	 * @param keyName
	 *            {@code allows } {@link String}
	 * @return keyValue {@link String}
	 */
	public static String getFileProperties(String keyName, String propertyFileName) {

		// logger.debug("Property KeyName : " + keyName);
		if (!ValidationsUtil.isValidObject(fileProperty)) {
			fileProperty = new Properties();
			InputStream inputStream = FileProperties.class.getClassLoader().getResourceAsStream(propertyFileName);
			try {
				fileProperty.load(inputStream);
				// inputStream.close();
			} catch (Exception e) {
				logger.error("Load property exception! Check the logs for details", e);
			} finally {
				try {
					if (ValidationsUtil.isValidObject(inputStream))
						inputStream.close();
				} catch (Exception e) {
					logger.error("Close property exception! Check the logs for details", e);
				}
			}
		}
		// logger.debug("Property Value : " +
		// fileProperty.getProperty(keyName));
		return fileProperty.getProperty(keyName);
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, String> loadProperties(String propertyFileName) {

		HashMap<String, String> result = new HashMap<String, String>();
		Properties prop = new Properties();
		InputStream inputStream = getClass().getResourceAsStream("/TINotification.properties");
		try {
			prop.load(inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		result.put("dbdriver", prop.getProperty("dbdriver"));
		result.put("dburl", prop.getProperty("dburl"));
		result.put("dbusername", prop.getProperty("dbusername"));
		result.put("dbpassword", prop.getProperty("dbpassword"));

		result.put("tidbdriver", prop.getProperty("tidbdriver"));
		result.put("tidburl", prop.getProperty("tidburl"));
		result.put("tidbusername", prop.getProperty("tidbusername"));
		result.put("tidbpassword", prop.getProperty("tidbpassword"));
		logger.debug("tidbpassword : " + prop.getProperty("tidbpassword"));

		return result;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		logger.info("FileProperties.class");
		getFileProperties("ThemeBridgeAccess");
	}

}
