package com.bs.theme.bob.adapter.sfms;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.AccountAvailBalAdaptee;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

public class SfmsAdvicePDFCreator {

	private final static Logger logger = Logger.getLogger(SfmsAdvicePDFCreator.class.getName());

	// private static String USER_PASSWORD = "password";
	// private static String OWNER_PASSWORD = "lokesh";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String SfmsOutMsg;
		try {
			SfmsOutMsg = ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\sfms printer friendly\\sfmsSIT.txt");

			// pdfDocumentCreator(swiftMsg);

			String sfmsIFSCAddr = SfmsAdviceHandler.getSfmsIFSCAddrDetails(SfmsOutMsg, "700");
			pdfDocumentCreatorLocalMachine("Titile", "123", SfmsOutMsg);

		} catch (Exception e) {
			logger.debug("");
			e.printStackTrace();
		}

	}

	public static byte[] pdfDocumentCreator(String msgType, String title, String ifscAddress, String printerFrndlyMsg) {

		byte[] pdfBytes = null;
		InputStream anInputStream = null;
		Document document = new Document(PageSize.A4, 30, 20, 100, 0);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			logger.debug("Initiated !!!");

			// Rectangle pageSize = new Rectangle(780, 525);
			Font catFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
			Font catHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
			PdfWriter.getInstance(document, byteArrayOutputStream);

			document.open();
			// Set attributes here
			document.addAuthor("TI Plus2-a:G.4.2.2 v. 2.7");
			document.addCreationDate();
			document.addCreator("TI Plus2-a");
			document.addTitle("SFMS advice copy");// BGSWIFT:1.24:226_4
			document.addSubject("Advice");

			logger.debug("Processing !!");
			/** Add Image **/
			// Image image1 = Image.getInstance("/kotaklogo.jpg");
			anInputStream = SfmsAdvicePDFCreator.class.getClassLoader().getResourceAsStream("kotaklogo.jpg");
			Image image1 = Image.getInstance(IOUtils.toByteArray(anInputStream));
			logger.debug("Processing a !!!");

			/** Fixed Positioning **/
			image1.setAbsolutePosition(30f, 750f);
			/** Scale to new height and new width of image **/
			image1.scaleAbsolute(100, 30);
			/** Add to document **/
			document.add(image1);
			logger.debug("Processing !!!");

			// document.add(new Paragraph("\n\n----------- Message Header
			// -----------"));
			// if (!ifscAddress.isEmpty() && ifscAddress != null)
			// document.add(new Paragraph(ifscAddress));
			// document.add(new Paragraph("\n\n----------- Message Text
			// -----------"));

			if (!title.isEmpty() && title != null)
				document.add(new Paragraph("IFN " + msgType + "\t" + title));

			// document.add(Chunk.NEWLINE);
			LineSeparator ls = new LineSeparator();
			document.add(new Chunk(ls));

			document.add(new Paragraph("\n----------------- Message Header -----------------", catHeaderFont));
			if (!ifscAddress.isEmpty() && ifscAddress != null)
				document.add(new Paragraph(ifscAddress, catFont));

			document.add(new Paragraph("\n----------------- Message Text -----------------", catHeaderFont));
			Paragraph message = new Paragraph(printerFrndlyMsg, catFont);
			// document.add(new Paragraph(printerFrndlyMsg));
			document.add(message);
			document.add(new Paragraph("\n----------------- End -----------------", catHeaderFont));

			logger.debug("Completed !!!");

			document.close();
			// writer.close();
			pdfBytes = byteArrayOutputStream.toByteArray();

		} catch (Exception e) {
			logger.error("PDF DocumentCreator Exceptions!! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}
		return pdfBytes;
	}

	/**
	 * NOT USED in KOTAK implementation
	 * 
	 * @param title
	 * @param ifscAddress
	 * @param printerFrndlyMsg
	 */
	public static void pdfDocumentCreatorLocalMachine(String title, String ifscAddress, String printerFrndlyMsg) {
		Document document = new Document(PageSize.A4, 30, 20, 100, 0);
		try {
			logger.debug("Initiated !!!");

			// Rectangle pageSize = new Rectangle(780, 525);
			Font catFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
			PdfWriter writer = PdfWriter.getInstance(document,
					new FileOutputStream("D:\\_Prasath\\Personal\\Kotak-Logo\\le3.pdf"));

			// writer.setEncryption(USER_PASSWORD.getBytes(),
			// OWNER_PASSWORD.getBytes(), PdfWriter.ALLOW_PRINTING,
			// PdfWriter.ENCRYPTION_AES_128);

			document.open();
			// Set attributes here
			document.addAuthor("TI Plus2-a:G.4.2.2 v. 2.7");
			document.addCreationDate();
			document.addCreator("TI Plus2-a");
			document.addTitle("SFMS advice copy");// BGSWIFT:1.24:226_4
			document.addSubject("Advice");

			// Add Image
			Image image1 = Image.getInstance("XImage\\kotaklogo.jpg");

			// Fixed Positioning
			// image1.setAbsolutePosition(100f, 550f);
			image1.setAbsolutePosition(30f, 750f);

			// Scale to new height and new width of image
			image1.scaleAbsolute(100, 30);

			// Add to document
			document.add(image1);
			logger.debug("Processing !!!");

			if (!title.isEmpty() && title != null)
				document.add(new Paragraph(title));

			document.add(Chunk.NEWLINE);
			LineSeparator ls = new LineSeparator();
			document.add(new Chunk(ls));

			if (!ifscAddress.isEmpty() && ifscAddress != null)
				document.add(new Paragraph(ifscAddress));

			// content
			// document.add(new Paragraph("Image Example3"));

			// document.add(new Paragraph(printerFrndlyMsg));
			Paragraph message = new Paragraph(printerFrndlyMsg, catFont);
			document.add(message);

			logger.debug("Completed !!!");

			document.close();
			writer.close();

		} catch (Exception e) {
			logger.debug("" + e.getMessage());
			e.printStackTrace();
		}
	}

}
