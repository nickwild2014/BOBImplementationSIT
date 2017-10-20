package com.bs.theme.bob.unused;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.log4j.Logger;

/**
 * @author BSIT-THEMEBRIDGE(RaviPrasath)
 */
public class SwiftMailerUtil {

	private static final Logger logger = Logger.getLogger(SwiftMailerUtil.class);

	public static void main(String args[]) throws Exception {
		// String path =
		// "D:\\tiplus\\_prasath\\Task\\task 36
		// swiftmailer\\SWIFT_FILES\\103\\prasathOGT0005150035102.prt";
		// SwiftMailerUtil s = new SwiftMailerUtil();
		// int rowNumber = s.GetRowNumberFromFile_Old(path);
		// logger.debug("rowNumber>>>----->>>>>>" + rowNumber);
		// String ReferenceNumber = s.GetReferenceNumber(path, rowNumber);
		// logger.debug("ReferenceNumber>>>----->>>>>>" +
		// ReferenceNumber);
		//
		// int rowNumberEve = s.GetRowNumberFromFile(path,"23:");
		// logger.debug("rowNumber>>>----->>>>>>" + rowNumberEve);
		// String ReferenceNumberEVE = s.GetReferenceNumber(path, rowNumberEve);
		// logger.debug("ReferenceNumber>>>----->>>>>>" +
		// ReferenceNumberEVE);

		String str = "Roja" + "\n" + ":25:PRASAfooTH 2" + "\n" + "TEST END";
		// getSwiftTagValue(str, ":25:");
		getSwiftTagValue(str, "PRA");
		// InputStream is = new ByteArrayInputStream(str.getBytes());
		// // read it with BufferedReader
		// BufferedReader br = new BufferedReader(new InputStreamReader(is));
		//
		// int num = GetRowNumberFromFile("", br, ":25:");
		// logger.debug(num);
	}

	/**
	 * 2017-FEB-21 Prasath Ravichandran
	 * 
	 * @param swiftmessage
	 *            {@code allows }{@link String}
	 * @param tagname
	 *            {@code allows }{@link String}
	 * @throws FileNotFoundException
	 *             {@code allows }{@link String}
	 */
	public static String getSwiftTagValue(String swiftmessage, String tagname) throws FileNotFoundException {

		// String str = "Roja" + "\n" + ":25:PRASAfooTH";
		InputStream is = new ByteArrayInputStream(swiftmessage.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String result = "";
		try {
			// File file = new File("file1.txt");
			Scanner in = null;
			in = new Scanner(br);
			// in = new Scanner(file);
			while (in.hasNext()) {
				String line = in.nextLine();
				if (line.contains(tagname)) {
					result = line.substring(4, line.length());
					// logger.debug(line);
				}
				logger.debug("Result : " + result);
			}

		} catch (Exception e) {
			logger.error("Exception e" + e.getMessage());
		}
		return result;
	}

	/**
	 * Get row number from file using keyword search
	 * 
	 * @param filepath
	 * @param swiftNum
	 * @return
	 * @throws IOException
	 */
	public static int GetRowNumberFromFile(String filepath, BufferedReader br, String swiftNum) throws IOException {
		// String words[] = new String[500];
		// FileReader fr = new FileReader(
		// "D:\\tiplus\\_prasath\\Task\\task 36
		// swiftmailer\\SWIFT_FILES\\110\\01647814.prt");

		// FileReader fr = new FileReader(filepath);
		// BufferedReader br = new BufferedReader(fr);
		String s;
		int linecount = 0;
		int rowNumber = 0;
		while ((s = br.readLine()) != null) {
			linecount++;
			int indexfound = s.indexOf(swiftNum);
			if (indexfound > -1) {
				rowNumber = linecount;
				rowNumber++;
			}
		}
		// fr.close();
		logger.info("Reference number position : " + rowNumber);
		return rowNumber;
	}

	/**
	 * Get specific line from file
	 * 
	 * @throws FileNotFoundException
	 */
	public String GetReferenceNumber(String filepath, int rowNumber) throws FileNotFoundException {
		File f = new File(filepath);
		Scanner fileScanner = new Scanner(f);
		int lineNumber = 1;
		String d = "";
		while (fileScanner.hasNextLine()) {
			fileScanner.nextLine();
			lineNumber++;
			if (lineNumber == rowNumber) {
				logger.info("\n\n");
				d = fileScanner.nextLine().trim();
				logger.info("\n\n");
			}
		}
		fileScanner.close();
		return d;
	}

	/**
	 * NOT IN USE (INDUSIND)
	 * 
	 * @return
	 * @throws IOException
	 */
	public int GetRowNumberFromFile_Old(String filepath) throws IOException {
		// String words[] = new String[500];
		// FileReader fr = new FileReader(
		// "D:\\tiplus\\_prasath\\Task\\task 36
		// swiftmailer\\SWIFT_FILES\\110\\01647814.prt");
		FileReader fr = new FileReader(filepath);
		BufferedReader br = new BufferedReader(fr);
		String s;
		int linecount = 0;
		int rowNumber = 0;
		while ((s = br.readLine()) != null) {
			linecount++;
			int indexfound = s.indexOf("20:");
			if (indexfound > -1) {
				rowNumber = linecount;
				rowNumber++;
			}
		}
		fr.close();
		logger.info("Reference number position : " + rowNumber);
		return rowNumber;
	}
}