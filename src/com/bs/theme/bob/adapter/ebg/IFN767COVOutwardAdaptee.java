package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSOUT_767COV;
import static com.bs.theme.bob.template.util.KotakConstant.SOURCE_SYSTEM;
import static com.bs.theme.bob.template.util.KotakConstant.TARGET_SYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bs.theme.bob.adapter.adaptee.GatewayRtgsNeftAdapteeStaging;
import com.bs.theme.bob.adapter.sfms.SFMSInMessageGenerator;
import com.bs.theme.bob.adapter.util.SWIFTMessageUtil;
import com.bs.theme.bob.template.util.RequestResponseTemplate;
import com.bs.themebridge.listener.mq.MQMessageManager;
import com.bs.themebridge.logging.ServiceLogging;
import com.bs.themebridge.token.util.ConfigurationUtil;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.DatabaseUtility;
import com.bs.themebridge.util.DateTimeUtil;
import com.bs.themebridge.util.ThemeBridgeStatusEnum;
import com.bs.themebridge.util.ThemeBridgeUtil;
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.xpath.IFN767COVGatewayXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.test.StringLineCount;
import com.test.XmlSpecialCharacterEncoding;

public class IFN767COVOutwardAdaptee {

	private final static Logger logger = Logger.getLogger(IFN767COVOutwardAdaptee.class.getName());

	private Timestamp tiReqTime = null;
	private Timestamp tiResTime = null;
	private Timestamp bankReqTime = null;
	private Timestamp bankResTime = null;
	private static String branch = "";
	// private static String service = "";
	// private static String operation = "";
	private static String tiRequest = "";
	private static String tiResponse = "";
	private static String bankRequest = "";
	private static String bankResponse = "";
	private static String eventReference = "";
	private static String correlationId = "";
	private static String masterReference = "";

	public static void main(String[] args) throws Exception {

		IFN767COVOutwardAdaptee ifAn = new IFN767COVOutwardAdaptee();
		// String tiGwRequestXML = ThemeBridgeUtil
		// .readFile("D:\\_Prasath\\Filezilla\\task\\task
		// 767cov\\MT767COV2017-01-19.xml");

		String tiGwRequestXML = ThemeBridgeUtil.readFile("D:\\_Prasath\\00_TASK\\task 767cov\\767SplitValues.xml");

		String get = ifAn.processIFN767COV(tiGwRequestXML, "", "");
		logger.debug("IFNMESSAGE >>> \n" + get);
	}

	/**
	 * 
	 * @param tiRequestXML
	 * @return
	 */
	public String processIFN767COV(String tiGwRequestXML, String service, String operation) {

		String eventRef = "";
		String masterRef = "";
		// bankResponse = "FAILED";
		String errorMessage = "";
		String ifnMsgStaus = "FAILED";
		String ifn767COVRequestMQMessage = "";
		try {
			tiRequest = tiGwRequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("GATEWAY.IFN767CV TI Request : \n" + tiRequest);

			tiGwRequestXML = XmlSpecialCharacterEncoding.xmlEscapeText(tiGwRequestXML);
			logger.debug("\n\nReplacedSpecialChar : \n" + tiGwRequestXML);

			// service = XPathParsing.getValue(tiGwRequestXML,
			// IFN767COVGatewayXpath.SERVICE);
			// operation = XPathParsing.getValue(tiGwRequestXML,
			// IFN767COVGatewayXpath.OPERATION);
			branch = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.BEHALFOFBRANCH);
			correlationId = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.CORRELATIONID);
			masterRef = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.MASTERREFERENCE);
			eventRef = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.EVENTREFERENCE);
			String eventRefnoPfix = "";
			String eventRefnoSerl = "";
			if (!eventRef.isEmpty() && eventRef.length() > 5) {
				eventRefnoPfix = eventRef.substring(0, 3);
				eventRefnoSerl = eventRef.substring(3, 6);
			}
			eventReference = eventRef;
			masterReference = masterRef;
			logger.debug("IFN767CV Request Reference : " + masterRef + "-" + eventRef + "\t" + eventRefnoPfix
					+ eventRefnoSerl);

			// Get IFN Request message
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			ifn767COVRequestMQMessage = getIFN767COVSFMSMessage(masterRef, eventRef, tiGwRequestXML);
			bankRequest = ifn767COVRequestMQMessage;
			// logger.debug("GATEWAY.IFN767CV Bank Request : \n" + bankRequest);

			// Push to MQ
			String ifnOutMQName = ConfigurationUtil.getValueFromKey("IFN760COVOUTMQName");// SfmsOutMQName
			String ifnOutMQJNDIName = ConfigurationUtil.getValueFromKey("IFN760COVOUTMQJndiName");// SfmsOutMQJndiName
			// String filePath =
			// ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SFMS_IFN_COV_PATH);
			// boolean isValid = SWIFTSwiftInAdaptee.writeFile(filePath +
			// "01767" + masterRef + eventRef,
			// ifn767COVRequestMQMessage);
			ifn767COVRequestMQMessage = MQMessageManager.formatSwiftMsg(ifn767COVRequestMQMessage);
			boolean ifn767COVOutQueuePostingStatus = MQMessageManager.pushMqMessage(ifnOutMQJNDIName, ifnOutMQName,
					ifn767COVRequestMQMessage);
			// boolean isValid2 = SWIFTSwiftInAdaptee.writeFile(filePath +
			// "02767" + masterRef + eventRef,
			// ifn767COVRequestMQMessage);
			if (ifn767COVOutQueuePostingStatus) {
				ifnMsgStaus = ThemeBridgeStatusEnum.TRANSMITTED.toString();
				// ifnMsgStaus = StatusEnum.SUCCEEDED.toString();
			} else {
				ifnMsgStaus = ThemeBridgeStatusEnum.FAILED.toString();
			}
			bankResponse = ifnMsgStaus;
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.IFN767CV Bank Response : " + bankResponse);

			tiResponse = getTIResponse(operation, ifnMsgStaus);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.IFN767CV TI Response : " + tiResponse);

		} catch (Exception e) {
			ifnMsgStaus = "FAILED";
			bankResponse = "FAILED";
			errorMessage = e.getMessage();
			logger.error("IFN767CV Processing Exceptions..! " + e.getMessage());
			tiResponse = getTIResponse(operation, ifnMsgStaus);

		} finally {
			// service logging, "SFMSOut_" + operation, "SWIFT", operation,
			ServiceLogging.pushLogData(service, operation, SOURCE_SYSTEM, branch, SOURCE_SYSTEM, TARGET_SYSTEM,
					masterRef, eventRef, ifnMsgStaus, tiRequest, tiResponse, bankRequest, "MQ status : " + ifnMsgStaus,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "COVER", "767", "1/1", false, "0",
					errorMessage);
		}

		return tiResponse;
	}

	/**
	 * I . FIRST
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @param masterReferenceNum
	 *            {@code allows }{@link String}
	 * @param messageType
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public String getIFN767COVSFMSMessage(String masterRef, String eventRef, String tiGwRequestXML) {

		String IFN767COVSFMSMessage = "";
		try {
			Map<String, String> senderRece = SFMSInMessageGenerator.getSenderReceiverIfscCode(masterRef, eventRef);
			String senderIFSC = senderRece.get("senderIFSC");
			String receiverIFSC = senderRece.get("receiverIFSC");

			// II
			String sfmsIfn767COVHeader = getSFMSIFN767COVRequestHeader(masterRef, eventRef, senderIFSC, receiverIFSC);
			logger.debug("IFN767CV Header " + sfmsIfn767COVHeader);

			// III
			String sfmsIfn767COVMessage = getSFMSIFN767COVRequestBody(sfmsIfn767COVHeader, masterRef, eventRef,
					senderIFSC, tiGwRequestXML);
			logger.debug("IFN767CV Message \n" + sfmsIfn767COVMessage);

			// IV
			try {
				// SFMSMessage =
				// DigitalSignature.signSFMSMessage(sfmsIfn767COVMessage);

			} catch (Exception e) {
				e.printStackTrace();
				logger.error("DigitalSignature signSFMSMessage exceptions..!");
			}

			// V
			IFN767COVSFMSMessage = sfmsIfn767COVMessage;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("IFN767CV SFMS Message generation exceptions..! " + e.getMessage());
		}

		// logger.debug("IFN767 SDP OUT : " + IFN767SFMSMessage);
		return IFN767COVSFMSMessage;
	}

	/**
	 * II . SECOND
	 * 
	 * @param masterRef
	 *            {@code allows }{@link String}
	 * @param eventpfx
	 *            {@code allows }{@link String}
	 * @param eventserl
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSFMSIFN767COVRequestHeader(String masterRef, String eventRef, String senderIFSC,
			String receiverIFSC) {

		// Sender Bank application identifier -- (APP) //It's not constant, may
		// be it will change.
		StringBuffer stringBuf = new StringBuffer("{A:");

		try {
			// get receiver IFSC Code from message
			// String receiverIFSC = m.getReceiver();

			// Prodcut confirmation
			stringBuf.append("BGS"); // always

			// - Doc OK
			String msgIdent = "F01";
			stringBuf.append(msgIdent);// always

			// Input/output Identifier (either I or O) - Doc OK
			stringBuf.append("O"); // always

			// Message type - Doc OK
			stringBuf.append("767"); // always

			// Sub Message Type ( For IFN 298C01, this field should be C01, for
			// IFN100 message, this field should be XXX) - Doc OK
			String subMT = "COV";
			stringBuf.append(subMT);

			// below code written by subhash

			/*******************************************************/
			// logger.debug("stringBuf " + stringBuf.toString());
			if (senderIFSC != null && !senderIFSC.isEmpty()) {
				stringBuf.append(senderIFSC);
			} else {
				stringBuf.append("XXXXXXXXXXX");
			}
			// logger.debug("stringBuf " + stringBuf.toString());

			if (receiverIFSC != null && !receiverIFSC.isEmpty()) {
				stringBuf.append(receiverIFSC);
			} else {
				stringBuf.append("XXXXXXXXXXX");
			}
			// logger.debug("stringBuf " + stringBuf.toString());
			/*******************************************************/

			// Delivery Monitoring flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Open Notification flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Non-delivery Warning flag YES-1, NO-2 - Doc OK
			stringBuf.append("2");

			// Obsolescence Period - Doc OK
			// If Non-delivery warning flag is 2, then this value should be set
			// to ‘000’.
			// If Non-delivery warning flag is 1, then this value should be set
			// to ‘002’.
			stringBuf.append("000");

			// Message User Reference (MUR) - Doc OK
			String murPrefix = "TI";
			long murSuffix = ThemeBridgeUtil.generateRandom(14);
			stringBuf.append(murPrefix + String.valueOf(murSuffix));
			// logger.debug("MUR : " + murPrefix + String.valueOf(murSuffix));

			// Possible Duplicate flag - Doc OK
			stringBuf.append("2");

			// Service Identifier 3 digit - Doc OK
			stringBuf.append("BGS"); // always

			// Originating date YYYYMMDD - Doc OK
			String Originatingdate = SWIFTMessageUtil.getSFMSDate();
			stringBuf.append(Originatingdate);// alway current date YYYYMMDD

			// Originating time HHMM - Doc OK
			String hourMins = SWIFTMessageUtil.getHourMins();
			stringBuf.append(hourMins); // always curreny Time HHMM

			// Authorization flag - Doc OK
			stringBuf.append("2"); // 1

			// Testing and training flag - Doc OK
			stringBuf.append("2"); // 1

			// Sequence Number - Doc OK
			stringBuf.append(ThemeBridgeUtil.generateRandom(8));

			// Filler for future use and default
			String Filler = "XXXXXXXXX";
			stringBuf.append(Filler); // always

			// Unique Transaction Reference.masterRef
			// String TransRef = "XXXXXXXXXXXXXXXX";
			stringBuf.append(masterRef);

			// Priority Flag - Urgent, High, Normal, Low
			// 00, 99
			stringBuf.append("00"); // always

			// Final SFMS Header tag close
			stringBuf.append("}");

			// Body
			stringBuf.append("{4:");

		} catch (Exception e) {
			logger.error("Get IFN767CV SFMS Header Exception!! ");
			e.printStackTrace();
		}

		return stringBuf.toString();
	}

	/**
	 * III . THIRD
	 * 
	 * @param swiftMessage
	 *            {@code allows }{@link String}
	 * @return {@code allows }{@link String}
	 */
	public static String getSFMSIFN767COVRequestBody(String sfmsIfn767COVHeader, String masterRef, String eventRef,
			String senderIFSC, String tiGwRequestXML) {

		// logger.error("Header " + sfmsIfn767COVHeader);
		String fin767COVBody = "";

		try {
			StringBuilder ifnmsgBody = new StringBuilder();

			String TransactionRefNumber = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.TRANSACTIONREFNUMBER);
			String RelatedReference = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.RELATEDREFERENCE);
			String FurtherIdentification = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.FURTHERIDENTIFICATION);
			String AmendmentDate = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.AMENDMENTDATE);
			String currency = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.CURRENCY);
			String NumberOfAmendment = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.NUMBEROFAMENDMENT);
			String DateOfIssue = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.DATEOFISSUE);
			String AmendmentDetails = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.AMENDMENTDETAILS);

			String SenderToReceiverInformation = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.SENDERTORECEIVERINFORMATION);

			String IssuingBranchIFSC = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.ISSUINGBRANCHIFSC);
			String IssuingBranchNameAddress = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.ISSUINGBRANCHNAMEADDRESS);
			String NameOfApplicant = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.NAMEOFAPPLICANT);
			String NameOfBeneficiary = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.NAMEOFBENEFICIARY);
			String BeneficiaryIFSC = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.BENEFICIARYIFSC);
			String BeneficiaryBranchNameAddress = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.BENEFICIARYBRANCHNAMEADDRESS);
			String ElectronicallyPaid = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.ELECTRONICALLYPAID);
			// get from Query
			String EStampCertificateNumber = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.ESTAMPCERTIFICATENUMBER);
			String EStampDate = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.ESTAMPDATE);
			String EStampTime = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.ESTAMPTIME);

			String AmountPaid = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.AMOUNTPAID);
			String StateCode = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.STATECODE);
			String ArticleNumber = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.ARTICLENUMBER);
			String DateOfPayment = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.DATEOFPAYMENT);
			String PlaceOfPayment = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.PLACEOFPAYMENT);
			String EBGHeldInDematForm = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.EBGHELDINDEMATFORM);
			String DematAccountNumber = XPathParsing.getValue(tiGwRequestXML, IFN767COVGatewayXpath.DEMATACCOUNTNUMBER);
			String CustodianServiceProvider = XPathParsing.getValue(tiGwRequestXML,
					IFN767COVGatewayXpath.CUSTODIANSERVICEPROVIDER);

			Map<String, String> eStampDetailsMapList = geteStampDetails(masterRef, eventRef);
			String eStampCertifiNoQ = eStampDetailsMapList.get("eCertificateno");
			String eStampDateQ = eStampDetailsMapList.get("eStampDate");
			String eStampTimeQ = eStampDetailsMapList.get("eStampTime");
			logger.debug("eStampDetailsMapList : " + eStampCertifiNoQ + "\t" + eStampDateQ + "\t" + eStampTimeQ);

			// HEADER
			ifnmsgBody.append(sfmsIfn767COVHeader);
			// BODY
			if (ValidationsUtil.isValidString(TransactionRefNumber)) {
				ifnmsgBody.append("\r\n" + ":7020:" + TransactionRefNumber.trim());// 16xMandatory
			}
			if (ValidationsUtil.isValidString(RelatedReference)) {
				if (RelatedReference.length() > 16)
					ifnmsgBody.append("\r\n" + ":7021:" + RelatedReference.substring(0, 16));
				else
					ifnmsgBody.append("\r\n" + ":7021:" + RelatedReference);
			}
			if (ValidationsUtil.isValidString(FurtherIdentification)) {
				if (FurtherIdentification.length() > 16)
					ifnmsgBody.append("\r\n" + ":7055:" + FurtherIdentification.substring(0, 16));
				else
					ifnmsgBody.append("\r\n" + ":7055:" + FurtherIdentification);// 16xMandatory
			}
			// TODO ? INR1000,00
			if (ValidationsUtil.isValidString(AmendmentDate)) {
				ifnmsgBody.append("\r\n" + ":7056:" + AmendmentDate);// Date
			}
			// TODO ? 2017011520170120
			if (ValidationsUtil.isValidString(NumberOfAmendment)) {
				ifnmsgBody.append("\r\n" + ":7057:" + NumberOfAmendment); // 2n
			}
			if (ValidationsUtil.isValidString(DateOfIssue)) {
				ifnmsgBody.append("\r\n" + ":7058:" + DateOfIssue);// date
			}

			if (ValidationsUtil.isValidString(AmendmentDetails)) {
				// ifnmsgBody.append("\r\n" + ":7059:" + AmendmentDetails);
				String value = StringLineCount.splitStringRowsCharacters(AmendmentDetails, 150, 65);
				ifnmsgBody.append("\r\n" + ":7059:" + value);// 150*65xMandatory
			}

			if (ValidationsUtil.isValidString(SenderToReceiverInformation)) {
				// ifnmsgBody.append("\r\n" + ":7037:" +
				// SenderToReceiverInformation);
				String value = StringLineCount.splitStringRowsCharacters(SenderToReceiverInformation, 10, 35);
				ifnmsgBody.append("\r\n" + ":7037:" + value);// 10*35xMandatory
			}

			if (ValidationsUtil.isValidString(IssuingBranchIFSC)) {
				ifnmsgBody.append("\r\n" + ":7031:" + IssuingBranchIFSC);
			}
			if (ValidationsUtil.isValidString(IssuingBranchNameAddress)) {
				// ifnmsgBody.append("\r\n" + ":7032:" +
				// IssuingBranchNameAddress);
				String value = StringLineCount.splitStringRowsCharacters(IssuingBranchNameAddress, 6, 35);
				ifnmsgBody.append("\r\n" + ":7032:" + value);// 6*35xMandatory
			}
			if (ValidationsUtil.isValidString(NameOfApplicant)) {
				// ifnmsgBody.append("\r\n" + ":7033:" + NameOfApplicant);//
				// 6*35xMandatory
				String value = StringLineCount.splitStringRowsCharacters(NameOfApplicant, 6, 35);
				ifnmsgBody.append("\r\n" + ":7033:" + value);// 6*35xMandatory
			}
			if (ValidationsUtil.isValidString(NameOfBeneficiary)) {
				String value = StringLineCount.splitStringRowsCharacters(NameOfBeneficiary, 6, 35);
				ifnmsgBody.append("\r\n" + ":7034:" + value);// 6*35xMandatory
			}
			if (ValidationsUtil.isValidString(BeneficiaryIFSC)) {
				ifnmsgBody.append("\r\n" + ":7035:" + BeneficiaryIFSC);
			}
			if (ValidationsUtil.isValidString(BeneficiaryBranchNameAddress)) {
				String value = StringLineCount.splitStringRowsCharacters(BeneficiaryBranchNameAddress, 6, 35);
				ifnmsgBody.append("\r\n" + ":7036:" + value);// 6*35xMandatory
			}
			if (ValidationsUtil.isValidString(ElectronicallyPaid)) {
				ifnmsgBody.append("\r\n" + ":7040:" + ElectronicallyPaid);
			}

			/** Get from GatewayMesaage **/
			// ifnmsgBody.append("\r\n" + ":7041:" + EStampCertificateNumber);
			// ifnmsgBody.append("\r\n" + ":7042:" + EStampDate + EStampTime);
			/** Get from Query **/

			if (ValidationsUtil.isValidString(eStampCertifiNoQ)) {
				ifnmsgBody.append("\r\n" + ":7041:" + eStampCertifiNoQ); // 20x
			}
			if (ValidationsUtil.isValidString(eStampDateQ) && ValidationsUtil.isValidString(eStampTimeQ)) {
				ifnmsgBody.append("\r\n" + ":7042:" + eStampDateQ + eStampTimeQ);
			}

			if (ElectronicallyPaid.equalsIgnoreCase("Y")) {
				if (ValidationsUtil.isValidString(AmountPaid)) {
					// TODO ? INR1000,00
					ifnmsgBody.append("\r\n" + ":7043:" + AmountPaid);// 7040
				}
				if (ValidationsUtil.isValidString(StateCode)) {
					if (StateCode.length() > 50)
						ifnmsgBody.append("\r\n" + ":7044:" + StateCode.substring(0, 50));// 50x7040
					else
						ifnmsgBody.append("\r\n" + ":7044:" + StateCode);// 50x7040
				}
				if (ValidationsUtil.isValidString(ArticleNumber)) {
					if (ArticleNumber.length() > 35)
						ifnmsgBody.append("\r\n" + ":7045:" + ArticleNumber.substring(0, 35));// 35x7040
					else
						ifnmsgBody.append("\r\n" + ":7045:" + ArticleNumber);
				}
				if (ValidationsUtil.isValidString(DateOfPayment)) {
					ifnmsgBody.append("\r\n" + ":7046:" + DateOfPayment);// 7040
				}
				if (ValidationsUtil.isValidString(PlaceOfPayment)) {
					if (StateCode.length() > 35)
						ifnmsgBody.append("\r\n" + ":7047:" + PlaceOfPayment.substring(0, 35));// 7040
					else
						ifnmsgBody.append("\r\n" + ":7047:" + PlaceOfPayment);
				}
			}

			if (ValidationsUtil.isValidString(EBGHeldInDematForm)) {
				ifnmsgBody.append("\r\n" + ":7048:" + EBGHeldInDematForm); // 1x
			}

			if (EBGHeldInDematForm.equalsIgnoreCase("Y")) {
				if (ValidationsUtil.isValidString(DematAccountNumber)) {
					if (DematAccountNumber.length() > 16)
						ifnmsgBody.append("\r\n" + ":7049:" + DematAccountNumber.substring(0, 16));// 16x7048
					else
						ifnmsgBody.append("\r\n" + ":7049:" + DematAccountNumber);
				}
				if (ValidationsUtil.isValidString(CustodianServiceProvider)) {
					if (CustodianServiceProvider.length() > 16)
						ifnmsgBody.append("\r\n" + ":7050:" + CustodianServiceProvider.substring(0, 16));// 16x
					else
						ifnmsgBody.append("\r\n" + ":7050:" + CustodianServiceProvider);// 16x
				}
			}
			ifnmsgBody.append("\r\n" + "-}");

			fin767COVBody = ifnmsgBody.toString();
			// logger.debug("ifnmsgBody : " + fin767COVBody);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("GetSFMSBody Exceptions! " + e.getMessage());
		}

		// logger.error("GetSFMSBody return " + fin767COVBody);
		return fin767COVBody;
	}

	/**
	 * @param masterRef
	 * @param BEVREFNO
	 * @param BEVREFNO_SERL
	 * @return
	 */
	public static Map<String, String> geteStampDetails(String masterRef, String eventRef) {

		ResultSet rs = null;
		Connection con = null;
		PreparedStatement ps = null;

		Date eStampDate = null;
		String eStampTime = "";
		String eCertificateno = "";
		Map<String, String> eBgData = new HashMap<String, String>();

		try {
			String query = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) AS EVENT_REF, EXT.ESTMPCNO AS ESTAMPCERTIFINO, "
					+ " EXT.ESTMPDT AS ESTAMPDATE, EXT.ESTPTIME AS ESTAMPTIME FROM MASTER MAS, BASEEVENT BEV,EXTEVENT EXT WHERE MAS.KEY97 = BEV.MASTER_KEY "
					+ " AND BEV.KEY97 = EXT.EVENT AND TRIM(MAS.MASTER_REF) = ? AND TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) = ? ";
			logger.debug("Get e-BankGuarantee detailsQuery : " + query);

			con = DatabaseUtility.getTizoneConnection();
			ps = con.prepareStatement(query);
			ps.setString(1, masterRef);
			ps.setString(2, eventRef);
			rs = ps.executeQuery();
			while (rs.next()) {
				eStampDate = rs.getDate("ESTAMPDATE");
				eStampTime = rs.getString("ESTAMPTIME");
				eCertificateno = rs.getString("ESTAMPCERTIFINO");
				String eStampDateStr = DateTimeUtil.getStringLocalDate(eStampDate, "yyyyMMdd");

				eBgData.put("eCertificateno", eCertificateno);
				eBgData.put("eStampTime", eStampTime);
				eBgData.put("eStampDate", eStampDateStr);
			}
			// logger.debug("List : " + eBgData);

		} catch (SQLException e) {
			logger.error("SQLExceptions..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderPrepdConnection(con, ps, rs);
		}

		return eBgData;
	}

	/**
	 * 
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public String getTIResponse(String operation, String status) {

		String result = "";
		InputStream anInputStream = null;
		try {
			anInputStream = GatewayRtgsNeftAdapteeStaging.class.getClassLoader()
					.getResourceAsStream(RequestResponseTemplate.GATEWAY_DOCUMENTS_TI_RESPONSE_TEMPLATE);

			String swiftTiResponseTemplate = ThemeBridgeUtil.readFile(anInputStream);
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("operation", operation);
			tokens.put("status", status);
			tokens.put("correlationId", correlationId);
			MapTokenResolver resolver = new MapTokenResolver(tokens);
			Reader fileValue = new StringReader(swiftTiResponseTemplate);
			Reader reader = new TokenReplacingReader(fileValue, resolver);
			result = reader.toString();

			reader.close();

		} catch (IOException e) {
			logger.error("IOException..! " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				anInputStream.close();
			} catch (IOException e) {
				logger.error("InputStream close " + e.getMessage());
			}
		}

		return result;
	}

}
