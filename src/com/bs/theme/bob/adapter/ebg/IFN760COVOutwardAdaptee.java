package com.bs.theme.bob.adapter.ebg;

import static com.bs.theme.bob.template.util.KotakConstant.OPERATION_SFMSOUT_760COV;
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
import com.bs.themebridge.xpath.IFN760COVGatewayXpath;
import com.bs.themebridge.xpath.XPathParsing;
import com.test.BreakString;
import com.test.StringLineCount;
import com.test.XmlSpecialCharacterEncoding;

public class IFN760COVOutwardAdaptee {

	private final static Logger logger = Logger.getLogger(IFN760COVOutwardAdaptee.class.getName());

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

		IFN760COVOutwardAdaptee ifAn = new IFN760COVOutwardAdaptee();
		// String tiGwRequestXML = ThemeBridgeUtil.readFile(
		// "D:\\_Prasath\\Filezilla\\task\\task
		// 760COV\\TIRequest.IFN760COV-0958IGF160100168-corrections3.xml");

		// System.out.println(geteStampDetails("0958IGF160100507", "ISS001"));

		String tiGwRequestXML = ThemeBridgeUtil
				.readFile("C:\\Users\\KXT51472.KBANK\\Desktop\\SFMSOutwardNew\\760COV.xml");
		String get = ifAn.processIFN760COV(tiGwRequestXML, "", "");

		// logger.debug("IFNMESSAGE >>> \n" + get);
	}

	/**
	 * 
	 * @param tiRequestXML
	 * @return
	 */
	public String processIFN760COV(String tiGwRequestXML, String service, String operation) {

		String eventRef = "";
		String masterRef = "";
		String errorMessage = "";
		String ifnMsgStaus = "FAILED";
		String ifn760COVRequestMQMessage = "";
		try {
			tiRequest = tiGwRequestXML;
			tiReqTime = DateTimeUtil.getSqlLocalDateTime();
			// logger.debug("GATEWAY.IFN760CV TI Request : \n" + tiRequest);

			tiGwRequestXML = XmlSpecialCharacterEncoding.xmlEscapeText(tiGwRequestXML);
			logger.debug("\n\nReplacedSpecialChar : \n" + tiGwRequestXML);

			branch = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.BEHALFOFBRANCH);
			correlationId = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.CORRELATIONID);
			masterRef = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.MASTERREFERENCE);
			eventRef = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.EVENTREFERENCE);
			String eventRefnoPfix = "";
			String eventRefnoSerl = "";
			if (!eventRef.isEmpty() && eventRef.length() > 5) {
				eventRefnoPfix = eventRef.substring(0, 3);
				eventRefnoSerl = eventRef.substring(3, 6);
			}
			eventReference = eventRef;
			masterReference = masterRef;
			logger.debug("IFN760CV Request Reference : " + masterRef + "-" + eventRef + "\t" + eventRefnoPfix
					+ eventRefnoSerl);

			// Get IFN Request message
			bankReqTime = DateTimeUtil.getSqlLocalDateTime();
			ifn760COVRequestMQMessage = getIFN760COVSFMSMessage(masterRef, eventRef, tiGwRequestXML);
			bankRequest = ifn760COVRequestMQMessage;
			// logger.debug("GATEWAY.IFN760CV Bank Request : \n" + bankRequest);

			// Push to MQ
			String ifnOutMQName = ConfigurationUtil.getValueFromKey("IFN760COVOUTMQName");// SfmsOutMQName
			String ifnOutMQJNDIName = ConfigurationUtil.getValueFromKey("IFN760COVOUTMQJndiName");// SfmsOutMQJndiName

			// String filePath =
			// ConfigurationUtil.getValueFromKey(ThemeConstant.PROPERTY_SFMS_IFN_COV_PATH);
			// boolean isValid = SWIFTSwiftInAdaptee.writeFile(filePath +
			// "01760" + masterRef + eventRef,
			// ifn760COVRequestMQMessage);
			// logger.debug("isValid " + isValid);
			ifn760COVRequestMQMessage = MQMessageManager.formatSwiftMsg(ifn760COVRequestMQMessage);
			// logger.debug("Format finished ");
			// boolean isValid2 = SWIFTSwiftInAdaptee.writeFile(filePath +
			// "02760" + masterRef + eventRef,
			// ifn760COVRequestMQMessage);
			// logger.debug("isValid2 : " + isValid2);

			boolean ifn760COVOutQueuePostingStatus = MQMessageManager.pushMqMessage(ifnOutMQJNDIName, ifnOutMQName,
					ifn760COVRequestMQMessage);
			if (ifn760COVOutQueuePostingStatus) {
				ifnMsgStaus = ThemeBridgeStatusEnum.TRANSMITTED.toString();
				// ifnMsgStaus = StatusEnum.SUCCEEDED.toString();
			} else {
				ifnMsgStaus = ThemeBridgeStatusEnum.FAILED.toString();
			}
			bankResponse = ifnMsgStaus;
			bankResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.IFN760CV Bank Response : " + bankResponse);

			tiResponse = getTIResponse(operation, ifnMsgStaus);
			tiResTime = DateTimeUtil.getSqlLocalDateTime();
			logger.debug("GATEWAY.IFN760CV TI Response : " + tiResponse);

		} catch (Exception e) {
			ifnMsgStaus = "FAILED";
			bankResponse = "FAILED";
			errorMessage = e.getMessage();
			logger.error("IFN760CV Processing Exceptions..! " + e.getMessage());
			tiResponse = getTIResponse(operation, ifnMsgStaus);

		} finally {
			// service logging, "SFMSOut_" + operation, "SWIFT", operation,
			ServiceLogging.pushLogData(service, operation, SOURCE_SYSTEM, branch, SOURCE_SYSTEM, TARGET_SYSTEM,
					masterRef, eventRef, ifnMsgStaus, tiRequest, tiResponse, bankRequest, "MQ status : " + ifnMsgStaus,
					tiReqTime, bankReqTime, bankResTime, tiResTime, "", "COVER", "760", "1/1", false, "0",
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
	public String getIFN760COVSFMSMessage(String masterRef, String eventRef, String tiGwRequestXML) {

		String IFN760COVSFMSMessage = "";
		try {
			Map<String, String> senderRece = SFMSInMessageGenerator.getSenderReceiverIfscCode(masterRef, eventRef);
			String senderIFSC = senderRece.get("senderIFSC");
			String receiverIFSC = senderRece.get("receiverIFSC");

			// II
			String sfmsIfn760COVHeader = getSFMSIFN760COVRequestHeader(masterRef, eventRef, senderIFSC, receiverIFSC);
			logger.debug("SFMSIFN760 Header : " + sfmsIfn760COVHeader);

			// III
			String sfmsIfn760COVMessage = getSFMSIFN760COVRequestBody(sfmsIfn760COVHeader, masterRef, eventRef,
					senderIFSC, tiGwRequestXML);
			logger.debug("SFMSIFN760 Message : " + sfmsIfn760COVMessage);

			// IV
			try {
				// SFMSMessage =
				// DigitalSignature.signSFMSMessage(sfmsIfn760COVMessage);

			} catch (Exception e) {
				e.printStackTrace();
				logger.error("DigitalSignature signSFMSMessage exceptions..!");
			}

			// V
			IFN760COVSFMSMessage = sfmsIfn760COVMessage;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("IFN760COV SFMS Message generation exceptions..! " + e.getMessage());
		}

		// logger.debug("IFN760 SDP OUT : " + IFN760SFMSMessage);
		return IFN760COVSFMSMessage;
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
	public static String getSFMSIFN760COVRequestHeader(String masterRef, String eventRef, String senderIFSC,
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
			stringBuf.append("760"); // always

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
			logger.error("Get IFN760COV SFMS Header Exception!! ");
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
	public static String getSFMSIFN760COVRequestBody(String sfmsIfn760COVHeader, String masterRef, String eventRef,
			String senderIFSC, String tiGwRequestXML) {

		// logger.error("Header " + sfmsIfn760COVHeader);
		String fin760COVBody = "";

		try {
			StringBuilder ifnmsgBody = new StringBuilder();

			String TransactionRefNumber = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.TRANSACTIONREFNUMBER);
			String GuaranteeFormNumber = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.GUARANTEEFORMNUMBER);
			String TypeOfBankGuarantee = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.TYPEOFBANKGUARANTEE);
			String GuaranteeCurrencyAmount = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.GUARANTEECURRENCYAMOUNT);
			// GuaranteeCurrencyAmount =
			// GuaranteeCurrencyAmount.replaceAll("[^0-9.]", "");
			String currency = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.CURRENCY);
			String GuaranteeFromDate = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.GUARANTEEFROMDATE);
			String GuaranteeToDate = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.GUARANTEETODATE);
			String GuaranteeEffectiveDate = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.GUARANTEEEFFECTIVEDATE);
			String EndDateForLodgmentOfClaim = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.ENDDATEFORLODGMENTOFCLAIM);
			// amountPaid = amountPaid.replaceAll("[^0-9.]", "");
			String PlaceOfLodgmentOfClaim = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.PLACEOFLODGMENTOFCLAIM);
			String IssuingBranchIFSC = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.ISSUINGBRANCHIFSC);

			String IssuingBranchNameAddress = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.ISSUINGBRANCHNAMEADDRESS);
			String NameOfApplicantandDetails = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.NAMEOFAPPLICANT);
			String NameOfBeneficiary = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.NAMEOFBENEFICIARY);
			String BeneficiaryIFSC = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.BENEFICIARYIFSC);
			String BeneficiaryBranchNameAddress = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.BENEFICIARYBRANCHNAMEADDRESS);

			String SenderInformation = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.SENDERINFORMATION);
			String ReceiverInformation = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.RECEIVERINFORMATION);
			String SenderToReceiverInformation = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.SENDERTORECEIVERINFORMATION);

			String PurposeOfGuarantee = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.PURPOSEOFGUARANTEE);
			String ReferenceOfUnderlinedContract = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.REFERENCEOFUNDERLINEDCONTRACT);
			String ElectronicallyPaid = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.ELECTRONICALLYPAID);
			// get from Query
			String EStampCertificateNumber = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.ESTAMPCERTIFICATENUMBER);
			String EStampDate = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.ESTAMPDATE);
			String EStampTime = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.ESTAMPTIME);

			String AmountPaid = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.AMOUNTPAID);
			String StateCode = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.STATECODE);
			String ArticleNumber = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.ARTICLENUMBER);
			String DateOfPayment = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.DATEOFPAYMENT);
			String PlaceOfPayment = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.PLACEOFPAYMENT);
			String EBGHeldInDematForm = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.EBGHELDINDEMATFORM);
			String DematAccountNumber = XPathParsing.getValue(tiGwRequestXML, IFN760COVGatewayXpath.DEMATACCOUNTNUMBER);
			String CustodianServiceProvider = XPathParsing.getValue(tiGwRequestXML,
					IFN760COVGatewayXpath.CUSTODIANSERVICEPROVIDER);

			Map<String, String> eStampDetailsMapList = geteStampDetails(masterRef, eventRef);
			String eStampDateQ = eStampDetailsMapList.get("eStampDate");
			String eStampTimeQ = eStampDetailsMapList.get("eStampTime");
			String eStampCertifiNoQ = eStampDetailsMapList.get("eCertificateno");
			String lodgementClaimExpiryDate = eStampDetailsMapList.get("lodgementClaimExpiryDate");
			logger.debug("eStampDetailsMapList : " + eStampCertifiNoQ + "\t" + eStampDateQ + "\t" + eStampTimeQ + "\t"
					+ lodgementClaimExpiryDate);

			// HEADER
			ifnmsgBody.append(sfmsIfn760COVHeader);
			// BODY
			if (ValidationsUtil.isValidString(TransactionRefNumber)) {
				ifnmsgBody.append("\r\n" + ":7020:" + TransactionRefNumber.trim());// 16xMandatory
			}

			if (ValidationsUtil.isValidString(GuaranteeFormNumber)) {
				if (GuaranteeFormNumber.length() > 16)
					ifnmsgBody.append("\r\n" + ":7021:" + GuaranteeFormNumber.substring(0, 16));
				else
					ifnmsgBody.append("\r\n" + ":7022:" + GuaranteeFormNumber);
			}

			// 2017-06-17
			if (ValidationsUtil.isValidString(TypeOfBankGuarantee)) {
				// TypeOfBankGuarantee = TypeOfBankGuarantee.toUpperCase();

				if (TypeOfBankGuarantee.contains("Performance (IGF)") || TypeOfBankGuarantee.contains("Performance")) {
					TypeOfBankGuarantee = "PERFORMANCE";

				} else if (TypeOfBankGuarantee.contains("Financial (IGF)")
						|| TypeOfBankGuarantee.contains("Financial")) {
					TypeOfBankGuarantee = "FINANCIAL";

				} else if (TypeOfBankGuarantee.contains("Buyers Credit (IGF)")
						|| TypeOfBankGuarantee.contains("Buyers Credit")) {
					TypeOfBankGuarantee = "OTHERS";
				}
				logger.debug("TypeOfBankGuarantee : " + TypeOfBankGuarantee);

				if (TypeOfBankGuarantee.length() > 16)
					ifnmsgBody.append("\r\n" + ":7024:" + TypeOfBankGuarantee.substring(0, 16));// 16xMandatory
				else
					ifnmsgBody.append("\r\n" + ":7024:" + TypeOfBankGuarantee);// 16xMandatory
			}

			if (ValidationsUtil.isValidString(GuaranteeCurrencyAmount)) {
				// INR1000,00
				ifnmsgBody.append("\r\n" + ":7025:" + GuaranteeCurrencyAmount);// Mandatory
			}

			if (ValidationsUtil.isValidString(GuaranteeFromDate) && ValidationsUtil.isValidString(GuaranteeToDate)) {
				// 2017011520170120
				ifnmsgBody.append("\r\n" + ":7026:" + GuaranteeFromDate + GuaranteeToDate);// Mandatory
			}

			if (ValidationsUtil.isValidString(GuaranteeEffectiveDate)) {
				ifnmsgBody.append("\r\n" + ":7027:" + GuaranteeEffectiveDate);// Mandatory
			}
			// if (ValidationsUtil.isValidString(EndDateForLodgmentOfClaim)) {
			ifnmsgBody.append("\r\n" + ":7029:" + lodgementClaimExpiryDate);// Mandatory
			// }

			if (ValidationsUtil.isValidString(PlaceOfLodgmentOfClaim)) {
				if (PlaceOfLodgmentOfClaim.length() > 35)
					ifnmsgBody.append("\r\n" + ":7030:" + PlaceOfLodgmentOfClaim.substring(0, 35));
				else
					ifnmsgBody.append("\r\n" + ":7030:" + PlaceOfLodgmentOfClaim);
			}

			if (ValidationsUtil.isValidString(IssuingBranchIFSC)) {
				ifnmsgBody.append("\r\n" + ":7031:" + IssuingBranchIFSC);// Mandatory
			}

			if (ValidationsUtil.isValidString(IssuingBranchNameAddress)) {
				String value = StringLineCount.splitStringRowsCharacters(IssuingBranchNameAddress, 6, 35);
				ifnmsgBody.append("\r\n" + ":7032:" + value);// 6*35xMandatory
			}

			if (ValidationsUtil.isValidString(NameOfApplicantandDetails)) {
				String value = StringLineCount.splitStringRowsCharacters(NameOfApplicantandDetails, 6, 35);
				ifnmsgBody.append("\r\n" + ":7033:" + value);// 6*35xMandatory
			}

			if (ValidationsUtil.isValidString(NameOfBeneficiary)) {
				String value = StringLineCount.splitStringRowsCharacters(NameOfBeneficiary, 6, 35);
				ifnmsgBody.append("\r\n" + ":7034:" + value);// 6*35xMandatory
			}

			if (ValidationsUtil.isValidString(BeneficiaryIFSC)) {
				ifnmsgBody.append("\r\n" + ":7035:" + BeneficiaryIFSC);// Mandatory
			}

			if (ValidationsUtil.isValidString(BeneficiaryBranchNameAddress)) {
				String value = StringLineCount.splitStringRowsCharacters(BeneficiaryBranchNameAddress, 6, 35);
				ifnmsgBody.append("\r\n" + ":7036:" + value);// 6*35xMandatory
			}

			if (ValidationsUtil.isValidString(SenderToReceiverInformation)) {
				String value = StringLineCount.splitStringRowsCharacters(SenderToReceiverInformation, 10, 35);
				ifnmsgBody.append("\r\n" + ":7037:" + value); // 10*35x
			}

			if (ValidationsUtil.isValidString(PurposeOfGuarantee)) {
				String value = StringLineCount.splitStringRowsCharacters(PurposeOfGuarantee, 6, 35);
				ifnmsgBody.append("\r\n" + ":7038:" + value);// 6*35x
			}

			if (ValidationsUtil.isValidString(ReferenceOfUnderlinedContract)) {
				// String freeText =
				// splitFreeText7039(ReferenceOfUnderlinedContract);
				// ifnmsgBody.append("\r\n" + ":7039:" + freeText);
				String value = StringLineCount.splitStringRowsCharacters(ReferenceOfUnderlinedContract, 3, 35);
				ifnmsgBody.append("\r\n" + ":7039:" + value);// 3*35x
			}

			if (ValidationsUtil.isValidString(ElectronicallyPaid)) {
				ifnmsgBody.append("\r\n" + ":7040:" + ElectronicallyPaid);// 1x
			}
			/** Get from GatewayMesaage **/
			// ifnmsgBody.append("\r\n" + ":7041:" + EStampCertificateNumber);
			// ifnmsgBody.append("\r\n" + ":7042:" + EStampDate + EStampTime);
			/** Get from Query **/

			if (ValidationsUtil.isValidString(eStampCertifiNoQ)) {
				if (PlaceOfLodgmentOfClaim.length() > 35)
					ifnmsgBody.append("\r\n" + ":7041:" + eStampCertifiNoQ.substring(0, 20));// 20x
				else
					ifnmsgBody.append("\r\n" + ":7041:" + eStampCertifiNoQ);// 20x
			}
			if (ValidationsUtil.isValidString(eStampDateQ) && ValidationsUtil.isValidString(eStampTimeQ)) {
				ifnmsgBody.append("\r\n" + ":7042:" + eStampDateQ + eStampTimeQ);
			}

			if (ElectronicallyPaid.equalsIgnoreCase("Y")) {
				if (ValidationsUtil.isValidString(AmountPaid)) {
					// TODO ? INR1000,00
					ifnmsgBody.append("\r\n" + ":7043:" + AmountPaid);// 15d7040
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
					if (PlaceOfPayment.length() > 35)
						ifnmsgBody.append("\r\n" + ":7047:" + PlaceOfPayment.substring(0, 35));// 35x7040
					else
						ifnmsgBody.append("\r\n" + ":7047:" + PlaceOfPayment);
				}
			}
			/*** ***/
			if (ValidationsUtil.isValidString(EBGHeldInDematForm)) {
				ifnmsgBody.append("\r\n" + ":7048:" + EBGHeldInDematForm);// 1x
			}
			if (EBGHeldInDematForm.equalsIgnoreCase("Y")) {
				if (ValidationsUtil.isValidString(DematAccountNumber)) {
					if (DematAccountNumber.length() > 16)
						ifnmsgBody.append("\r\n" + ":7049:" + DematAccountNumber.substring(0, 16));// 16x7048
					else
						ifnmsgBody.append("\r\n" + ":7049:" + DematAccountNumber);
				}
			}
			if (ValidationsUtil.isValidString(CustodianServiceProvider)) {
				if (CustodianServiceProvider.length() > 16)
					ifnmsgBody.append("\r\n" + ":7050:" + CustodianServiceProvider.substring(0, 16));// 16x
				else
					ifnmsgBody.append("\r\n" + ":7050:" + CustodianServiceProvider);// 16x
			}
			ifnmsgBody.append("\r\n" + "-}");

			fin760COVBody = ifnmsgBody.toString();
			// logger.debug("ifnmsgBody : " + fin760COVBody);

		} catch (

		Exception e)

		{
			e.printStackTrace();
			logger.error("GetSFMSBody Exceptions! " + e.getMessage());
		}

		// logger.error("GetSFMSBody return " + fin760COVBody);
		return fin760COVBody;

	}

	public static String splitFreeText7039(String ReferenceOfUnderlinedContract) {

		String freeText = "";

		try {
			if (ReferenceOfUnderlinedContract.length() > 105) {
				ReferenceOfUnderlinedContract = ReferenceOfUnderlinedContract.substring(0, 105);
			}

			String[] splitStringArray = BreakString.splitStringByCharCount(ReferenceOfUnderlinedContract, 35);
			logger.debug("No. of Lines in Freetext : " + splitStringArray.length);

			for (int arrayIterator = 0; arrayIterator < splitStringArray.length; arrayIterator++) {
				if (arrayIterator == 0) {
					freeText = splitStringArray[arrayIterator];
				} else {

					freeText = freeText + "\n" + splitStringArray[arrayIterator];
				}
			}
			logger.debug(":7039:" + freeText + "<<<END>>>");

		} catch (Exception e) {
			logger.error("FreeText 7039 parsing exception..!" + e.getMessage());
		}
		return freeText;
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
		Date lodgementClaimExpiryDate = null;
		Map<String, String> eBgData = new HashMap<String, String>();

		try {
			String query = "SELECT TRIM(MAS.MASTER_REF) AS MASTER_REF, TRIM(BEV.REFNO_PFIX||LPAD(BEV.REFNO_SERL,3,000)) AS EVENT_REF, EXT.ESTMPCNO AS ESTAMPCERTIFINO, "
					+ " EXT.ESTMPDT AS ESTAMPDATE, EXT.ESTPTIME AS ESTAMPTIME, EXT.CLIMEXPD AS LODGECLAIMEXPDATE FROM MASTER MAS, BASEEVENT BEV,EXTEVENT EXT WHERE MAS.KEY97 = BEV.MASTER_KEY "
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
				lodgementClaimExpiryDate = rs.getDate("LODGECLAIMEXPDATE");
			}

			String eStampDateStr = "";
			if (eStampDate != null) {
				eStampDateStr = DateTimeUtil.getStringLocalDate(eStampDate, "yyyyMMdd");
			}
			String lodgementClaimExpiryDateStr = null;
			if (lodgementClaimExpiryDate != null) {
				lodgementClaimExpiryDateStr = DateTimeUtil.getStringLocalDate(lodgementClaimExpiryDate, "yyyyMMdd");
				// lodgementClaimExpiryDate =
				// eStampDetailsMapList.get("lodgementClaimExpiryDate").substring(0,
				// 10);
			}
			eBgData.put("eCertificateno", eCertificateno);
			eBgData.put("eStampTime", eStampTime);
			eBgData.put("eStampDate", eStampDateStr);
			eBgData.put("lodgementClaimExpiryDate", lodgementClaimExpiryDateStr);

			// logger.debug("List : " + eBgData);

		} catch (SQLException e) {
			logger.error("eStampDetails..! " + e.getMessage());
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
