package com.bs.theme.bob.staticdata.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bs.themebridge.incoming.util.StaticDataConstant;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.XMLFileReader;

public class TICustomer {

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String SourceBankingBusiness = "";
	private String Mnemonic = "";
	private String CustomerNumber = "";
	private String CustomerType = "";
	private String FullName = "";
	private String ShortName = "";
	private String Reference = "";
	private String MailToBranch = "";
	private String Group = "";
	private String GroupDescription = "";
	private String AccountOfficer = "";
	private String ResidenceCountry = "";
	private String ParentCountry = "";
	private String RiskCountry = "";
	private String AnalysisCode = "";
	private String Language = "";
	private String Closed = "";
	private String Blocked = "";
	private String Deceased = "";
	private String Inactive = "";
	private String MidasFacilityAllow = "";
	private String BankCode1 = "";
	private String BankCode2 = "";
	private String BankCode3 = "";
	private String BankCode4 = "";
	private String ClearingId = "";

	private String AddressDetailsAddressType = "";
	private String AddressDetailsAddressId = "";
	private String AddressDetailsSalutation = "";
	private String AddressDetailsNameAndAddress = "";
	private String AddressDetailsZipCode = "";
	private String AddressDetailsLanguage = "";
	private String AddressDetailsPhone = "";
	private String AddressDetailsFax = "";
	private String AddressDetailsTelex = "";
	private String AddressDetailsTelexAnswerBack = "";
	private String AddressDetailsEmail = "";
	private String AddressDetailsSwiftBIC = "";
	private String AddressDetailsTransferMethod = "";
	private String AddressDetailsAddresseeCustomerSBB = "";
	private String AddressDetailsAddresseeCustomer = "";
	private String AddressDetailsNumberOfCopies = "";
	private String AddressDetailsNumberOfOriginals = "";

	private String SpecialInstructionDetailsSeverity = "";
	private String SpecialInstructionDetailsCode = "";
	private String SpecialInstructionDetailsDetails = "";
	private String SpecialInstructionDetailsStyle = "";
	private String SpecialInstructionDetailsEmphasis = "";

	private String OtherDetailsAllowMT103C = "";
	private String OtherDetailsCutoffAmountAmount = "";
	private String OtherDetailsCutoffAmountCurrency = "";
	private String OtherDetailsSWIFTAckRequired = "";
	private String OtherDetailsTransliterateSWIFT = "";
	private String OtherDetailsTeam = "";
	private String OtherDetailsCorporateAccess = "";
	private String OtherDetailsPrincipalFxRateCode = "";
	private String OtherDetailsChargeFxRateCode = "";
	private String OtherDetailsAllowTaxExemptions = "";
	private String OtherDetailsSuspended = "";

	private String SwiftDetailsMainBankingEntity = "";
	private String SwiftDetailsSwiftAddress = "";
	private String SwiftDetailsAuthenticated = "";
	private String SwiftDetailsBlocked = "";
	private String SwiftDetailsClosed = "";
	private String SwiftDetailsTransliterationRequired = "";

	private String CustomerExtraData = "";
	private String TICustomerExtraData = "";

	Map<String, String> tokens = new HashMap<String, String>();

	public void generateStaticDataConstants() {

		tokens.put("MaintType", StaticDataConstant.MaintType);
		tokens.put("MaintainedInBackOffice", StaticDataConstant.MaintainedInBackOffice);
		// tokens.put("SourceBankingBusiness",
		// StaticDataConstant.SourceBankingBusiness);
	}

	public void generateHeader() {
		// Header
		tokens.put("Name", StaticDataConstant.CredentialName);
		tokens.put("Password", "");
		tokens.put("Certificate", "");
		tokens.put("Digest", "");
		tokens.put("CorrelationId", UUID.randomUUID().toString());
	}

	public void generateTokenMap() {

		tokens.put("SourceBankingBusiness", getSourceBankingBusiness());
		tokens.put("Mnemonic", getMnemonic());
		tokens.put("CustomerNumber", getCustomerNumber());
		tokens.put("CustomerType", "");
		tokens.put("FullName", getFullName());
		tokens.put("ShortName", getShortName());
		tokens.put("Reference", "");
		tokens.put("MailToBranch", "");
		// BehalfOfBranch
		tokens.put("Group", "");
		tokens.put("GroupDescription", "");
		tokens.put("AccountOfficer", getAccountOfficer());
		tokens.put("ResidenceCountry", getResidenceCountry());
		tokens.put("ParentCountry", getParentCountry());
		tokens.put("RiskCountry", "");
		tokens.put("AnalysisCode", getAnalysisCode());
		tokens.put("Language", getLanguage());
		tokens.put("Closed", getClosed());
		tokens.put("Blocked", getBlocked());
		tokens.put("Deceased", getDeceased());
		tokens.put("Inactive", getInactive());
		tokens.put("MidasFacilityAllow", "");
		tokens.put("BehalfOfBranch", getBankCode1());
		tokens.put("BankCode2", getBankCode2());
		tokens.put("BankCode3", getBankCode3());
		tokens.put("BankCode4", getBankCode4());
		tokens.put("ClearingId", "");
		tokens.put("AddressDetailsAddressType", getAddressDetailsAddressType());
		tokens.put("AddressDetailsAddressId", "");
		tokens.put("AddressDetailsSalutation", "");
		tokens.put("AddressDetailsNameAndAddress", getAddressDetailsNameAndAddress());
		tokens.put("AddressDetailsZipCode", getAddressDetailsZipCode());
		tokens.put("AddressDetailsLanguage", getAddressDetailsLanguage());
		tokens.put("AddressDetailsPhone", getAddressDetailsPhone());
		tokens.put("AddressDetailsFax", getAddressDetailsFax());
		tokens.put("AddressDetailsTelex", getAddressDetailsTelex());
		tokens.put("AddressDetailsTelexAnswerBack", getAddressDetailsTelexAnswerBack());
		tokens.put("AddressDetailsEmail", getAddressDetailsEmail());
		tokens.put("AddressDetailsSwiftBIC", getAddressDetailsSwiftBIC());
		tokens.put("AddressDetailsTransferMethod", getAddressDetailsTransferMethod());
		tokens.put("AddressDetailsAddresseeCustomerSBB", "");
		tokens.put("AddressDetailsAddresseeCustomer", "");
		tokens.put("AddressDetailsNumberOfCopies", "");
		tokens.put("AddressDetailsNumberOfOriginals", "");

		tokens.put("SpecialInstructionDetailsSeverity", "");
		tokens.put("SpecialInstructionDetailsCode", "");
		tokens.put("SpecialInstructionDetailsDetails", "");
		tokens.put("SpecialInstructionDetailsStyle", "");
		tokens.put("SpecialInstructionDetailsEmphasis", "");

		tokens.put("OtherDetailsAllowMT103C", getOtherDetailsAllowMT103C());
		tokens.put("OtherDetailsCutoffAmountAmount", "");
		tokens.put("OtherDetailsCutoffAmountCurrency", "");
		tokens.put("OtherDetailsSWIFTAckRequired", getOtherDetailsSWIFTAckRequired());
		tokens.put("OtherDetailsTransliterateSWIFT", getOtherDetailsTransliterateSWIFT());
		tokens.put("OtherDetailsTeam", "");
		tokens.put("OtherDetailsCorporateAccess", "");
		tokens.put("OtherDetailsPrincipalFxRateCode", "");
		tokens.put("OtherDetailsChargeFxRateCode", "");
		tokens.put("OtherDetailsAllowTaxExemptions", "");
		tokens.put("OtherDetailsSuspended", "");

		tokens.put("SwiftDetailsMainBankingEntity", getSwiftDetailsMainBankingEntity());
		tokens.put("SwiftDetailsSwiftAddress", getSwiftDetailsSwiftAddress());
		tokens.put("SwiftDetailsAuthenticated", getSwiftDetailsAuthenticated());
		tokens.put("SwiftDetailsBlocked", getSwiftDetailsBlocked());
		tokens.put("SwiftDetailsClosed", getSwiftDetailsClosed());
		tokens.put("SwiftDetailsTransliterationRequired", getSwiftDetailsTransliterationRequired());

		tokens.put("CustomerExtraData", "");
		tokens.put("TICustomerExtraData", "");
	}

	public void generateSetProperty(String key, String value) {

		if (key.equalsIgnoreCase("MaintType")) {
			setMaintType(value);
		} else if (key.equalsIgnoreCase("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equalsIgnoreCase("SourceBankingBusiness")) {
			setSourceBankingBusiness(value);
		} else if (key.equalsIgnoreCase("Mnemonic")) {
			setMnemonic(value);
		} else if (key.equalsIgnoreCase("CustomerNumber")) {
			setCustomerNumber(value);
		} else if (key.equalsIgnoreCase("CustomerType")) {
			setCustomerType(value);
		} else if (key.equalsIgnoreCase("FullName")) {
			setFullName(value);
		} else if (key.equalsIgnoreCase("ShortName")) {
			setShortName(value);
		} else if (key.equalsIgnoreCase("Reference")) {
			setReference(value);
		} else if (key.equalsIgnoreCase("MailToBranch")) {
			setMailToBranch(value);
		} else if (key.equalsIgnoreCase("Group")) {
			setGroup(value);
		} else if (key.equalsIgnoreCase("GroupDescription")) {
			setGroupDescription(value);
		} else if (key.equalsIgnoreCase("AccountOfficer")) {
			setAccountOfficer(value);
		} else if (key.equalsIgnoreCase("ResidenceCountry")) {
			setResidenceCountry(value);
		} else if (key.equalsIgnoreCase("ParentCountry")) {
			setParentCountry(value);
		} else if (key.equalsIgnoreCase("RiskCountry")) {
			setRiskCountry(value);
		} else if (key.equalsIgnoreCase("AnalysisCode")) {
			setAnalysisCode(value);
		} else if (key.equalsIgnoreCase("Language")) {
			setLanguage(value);
		} else if (key.equalsIgnoreCase("Closed")) {
			setClosed(value);
		} else if (key.equalsIgnoreCase("Blocked")) {
			setBlocked(value);
		} else if (key.equalsIgnoreCase("Deceased")) {
			setDeceased(value);
		} else if (key.equalsIgnoreCase("Inactive")) {
			setInactive(value);
		} else if (key.equalsIgnoreCase("MidasFacilityAllow")) {
			setMidasFacilityAllow(value);
		} else if (key.equalsIgnoreCase("BankCode1")) {
			setBankCode1(value);
		} else if (key.equalsIgnoreCase("BankCode2")) {
			setBankCode2(value);
		} else if (key.equalsIgnoreCase("BankCode3")) {
			setBankCode3(value);
		} else if (key.equalsIgnoreCase("BankCode4")) {
			setBankCode4(value);
		} else if (key.equalsIgnoreCase("ClearingId")) {
			setClearingId(value);
		} else if (key.equalsIgnoreCase("AddressType")) {
			setAddressDetailsAddressType(value);
		} else if (key.equalsIgnoreCase("AddressDetailsAddressId")) {
			setAddressDetailsAddressId(value);
		} else if (key.equalsIgnoreCase("AddressDetailsSalutation")) {
			setAddressDetailsSalutation(value);
		} else if (key.equalsIgnoreCase("AddressDetailsNameAndAddress")) {
			setAddressDetailsNameAndAddress(value);
		} else if (key.equalsIgnoreCase("AddressDetailsZipCode")) {
			setAddressDetailsZipCode(value);
		} else if (key.equalsIgnoreCase("AddressDetailsLanguage")) {
			setAddressDetailsLanguage(value);
		} else if (key.equalsIgnoreCase("AddressDetailsPhone")) {
			setAddressDetailsPhone(value);
		} else if (key.equalsIgnoreCase("AddressDetailsFax")) {
			setAddressDetailsFax(value);
		} else if (key.equalsIgnoreCase("AddressDetailsTelex")) {
			setAddressDetailsTelex(value);
		} else if (key.equalsIgnoreCase("AddressDetailsTelexAnswerBack")) {
			setAddressDetailsTelexAnswerBack(value);
		} else if (key.equalsIgnoreCase("AddressDetailsEmail")) {
			setAddressDetailsEmail(value);
		} else if (key.equalsIgnoreCase("AddressDetailsSwelse iftBIC")) {
			setAddressDetailsSwiftBIC(value);
		} else if (key.equalsIgnoreCase("AddressDetailsTransferMethod")) {
			setAddressDetailsTransferMethod(value);
		} else if (key.equalsIgnoreCase("AddressDetailsAddresseeCustomerSBB")) {
			setAddressDetailsAddresseeCustomerSBB(value);
		} else if (key.equalsIgnoreCase("AddressDetailsAddresseeCustomer")) {
			setAddressDetailsAddresseeCustomer(value);
		} else if (key.equalsIgnoreCase("AddressDetailsNumberOfCopies")) {
			setAddressDetailsNumberOfCopies(value);
		} else if (key.equalsIgnoreCase("AddressDetailsNumberOfOriginals")) {
			setAddressDetailsNumberOfOriginals(value);
		}

		else if (key.equalsIgnoreCase("SpecialInstructionDetailsSeverity")) {
			setSpecialInstructionDetailsSeverity(value);
		} else if (key.equalsIgnoreCase("SpecialInstructionDetailsCode")) {
			setSpecialInstructionDetailsCode(value);
		} else if (key.equalsIgnoreCase("SpecialInstructionDetailsDetails")) {
			setSpecialInstructionDetailsDetails(value);
		} else if (key.equalsIgnoreCase("SpecialInstructionDetailsStyle")) {
			setSpecialInstructionDetailsStyle(value);
		} else if (key.equalsIgnoreCase("SpecialInstructionDetailsEmphasis")) {
			setSpecialInstructionDetailsEmphasis(value);
		}

		else if (key.equalsIgnoreCase("AllowMT103C")) {
			setOtherDetailsAllowMT103C(value);
		} else if (key.equalsIgnoreCase("OtherDetailsCutoffAmountAmount")) {
			setOtherDetailsCutoffAmountAmount(value);
		} else if (key.equalsIgnoreCase("OtherDetailsCutoffAmountCurrency")) {
			setOtherDetailsCutoffAmountCurrency(value);
		} else if (key.equalsIgnoreCase("SWIFTAckRequired")) {
			setOtherDetailsSWIFTAckRequired(value);
		} else if (key.equalsIgnoreCase("OtherDetailsTransliterateSWelse ifT")) {
			setOtherDetailsTransliterateSWIFT(value);
		} else if (key.equalsIgnoreCase("OtherDetailsTeam")) {
			setOtherDetailsTeam(value);
		} else if (key.equalsIgnoreCase("OtherDetailsCorporateAccess")) {
			setOtherDetailsCorporateAccess(value);
		} else if (key.equalsIgnoreCase("OtherDetailsPrincipalFxRateCode")) {
			setOtherDetailsPrincipalFxRateCode(value);
		} else if (key.equalsIgnoreCase("OtherDetailsChargeFxRateCode")) {
			setOtherDetailsChargeFxRateCode(value);
		} else if (key.equalsIgnoreCase("OtherDetailsAllowTaxExemptions")) {
			setOtherDetailsAllowTaxExemptions(value);
		} else if (key.equalsIgnoreCase("OtherDetailsSuspended")) {
			setOtherDetailsSuspended(value);
		}

		else if (key.equalsIgnoreCase("MainBankingEntity")) {
			setSwiftDetailsMainBankingEntity(value);
		} else if (key.equalsIgnoreCase("SwiftAddress")) {
			setSwiftDetailsSwiftAddress(value);
		} else if (key.equalsIgnoreCase("Authenticated")) {
			setSwiftDetailsAuthenticated(value);
		} else if (key.equalsIgnoreCase("Swift_Blocked")) {
			setSwiftDetailsBlocked(value);
		} else if (key.equalsIgnoreCase("Swift_Closed")) {
			setSwiftDetailsClosed(value);
		} else if (key.equalsIgnoreCase("wewTransliterationRequired")) {
			setSwiftDetailsTransliterationRequired(value);
		}

		else if (key.equalsIgnoreCase("CustomerExtraData")) {
			setCustomerExtraData(value);
		} else if (key.equalsIgnoreCase("TICustomerExtraData")) {
			setTICustomerExtraData(value);
		}
	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(XMLFileReader.getTICustomerSource());
		Reader reader = new TokenReplacingReader(fileValue, resolver);
		// logger.debug(reader.toString());
		return reader.toString();
	}

	public String getMaintType() {
		return MaintType;
	}

	public void setMaintType(String maintType) {
		MaintType = maintType;
	}

	public String getMaintainedInBackOffice() {
		return MaintainedInBackOffice;
	}

	public void setMaintainedInBackOffice(String maintainedInBackOffice) {
		MaintainedInBackOffice = maintainedInBackOffice;
	}

	public String getSourceBankingBusiness() {
		return SourceBankingBusiness;
	}

	public void setSourceBankingBusiness(String sourceBankingBusiness) {
		SourceBankingBusiness = sourceBankingBusiness;
	}

	public String getMnemonic() {
		return Mnemonic;
	}

	public void setMnemonic(String mnemonic) {
		Mnemonic = mnemonic;
	}

	public String getCustomerNumber() {
		return CustomerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		CustomerNumber = customerNumber;
	}

	public String getCustomerType() {
		return CustomerType;
	}

	public void setCustomerType(String customerType) {
		CustomerType = customerType;
	}

	public String getFullName() {
		return FullName;
	}

	public void setFullName(String fullName) {
		FullName = fullName;
	}

	public String getShortName() {
		return ShortName;
	}

	public void setShortName(String shortName) {
		ShortName = shortName;
	}

	public String getReference() {
		return Reference;
	}

	public void setReference(String reference) {
		Reference = reference;
	}

	public String getMailToBranch() {
		return MailToBranch;
	}

	public void setMailToBranch(String mailToBranch) {
		MailToBranch = mailToBranch;
	}

	public String getGroup() {
		return Group;
	}

	public void setGroup(String group) {
		Group = group;
	}

	public String getGroupDescription() {
		return GroupDescription;
	}

	public void setGroupDescription(String groupDescription) {
		GroupDescription = groupDescription;
	}

	public String getAccountOfficer() {
		return AccountOfficer;
	}

	public void setAccountOfficer(String accountOfficer) {
		AccountOfficer = accountOfficer;
	}

	public String getResidenceCountry() {
		return ResidenceCountry;
	}

	public void setResidenceCountry(String residenceCountry) {
		ResidenceCountry = residenceCountry;
	}

	public String getParentCountry() {
		return ParentCountry;
	}

	public void setParentCountry(String parentCountry) {
		ParentCountry = parentCountry;
	}

	public String getRiskCountry() {
		return RiskCountry;
	}

	public void setRiskCountry(String riskCountry) {
		RiskCountry = riskCountry;
	}

	public String getAnalysisCode() {
		return AnalysisCode;
	}

	public void setAnalysisCode(String analysisCode) {
		AnalysisCode = analysisCode;
	}

	public String getLanguage() {
		return Language;
	}

	public void setLanguage(String language) {
		Language = language;
	}

	public String getClosed() {
		return Closed;
	}

	public void setClosed(String closed) {
		Closed = closed;
	}

	public String getBlocked() {
		return Blocked;
	}

	public void setBlocked(String blocked) {
		Blocked = blocked;
	}

	public String getDeceased() {
		return Deceased;
	}

	public void setDeceased(String deceased) {
		Deceased = deceased;
	}

	public String getInactive() {
		return Inactive;
	}

	public void setInactive(String inactive) {
		Inactive = inactive;
	}

	public String getMidasFacilityAllow() {
		return MidasFacilityAllow;
	}

	public void setMidasFacilityAllow(String midasFacilityAllow) {
		MidasFacilityAllow = midasFacilityAllow;
	}

	public String getBankCode1() {
		return BankCode1;
	}

	public void setBankCode1(String bankCode1) {
		BankCode1 = bankCode1;
	}

	public String getBankCode2() {
		return BankCode2;
	}

	public void setBankCode2(String bankCode2) {
		BankCode2 = bankCode2;
	}

	public String getBankCode3() {
		return BankCode3;
	}

	public void setBankCode3(String bankCode3) {
		BankCode3 = bankCode3;
	}

	public String getBankCode4() {
		return BankCode4;
	}

	public void setBankCode4(String bankCode4) {
		BankCode4 = bankCode4;
	}

	public String getClearingId() {
		return ClearingId;
	}

	public void setClearingId(String clearingId) {
		ClearingId = clearingId;
	}

	public String getAddressDetailsAddressType() {
		return AddressDetailsAddressType;
	}

	public void setAddressDetailsAddressType(String addressDetailsAddressType) {
		AddressDetailsAddressType = addressDetailsAddressType;
	}

	public String getAddressDetailsAddressId() {
		return AddressDetailsAddressId;
	}

	public void setAddressDetailsAddressId(String addressDetailsAddressId) {
		AddressDetailsAddressId = addressDetailsAddressId;
	}

	public String getAddressDetailsSalutation() {
		return AddressDetailsSalutation;
	}

	public void setAddressDetailsSalutation(String addressDetailsSalutation) {
		AddressDetailsSalutation = addressDetailsSalutation;
	}

	public String getAddressDetailsNameAndAddress() {
		return AddressDetailsNameAndAddress;
	}

	public void setAddressDetailsNameAndAddress(String addressDetailsNameAndAddress) {
		AddressDetailsNameAndAddress = addressDetailsNameAndAddress;
	}

	public String getAddressDetailsZipCode() {
		return AddressDetailsZipCode;
	}

	public void setAddressDetailsZipCode(String addressDetailsZipCode) {
		AddressDetailsZipCode = addressDetailsZipCode;
	}

	public String getAddressDetailsLanguage() {
		return AddressDetailsLanguage;
	}

	public void setAddressDetailsLanguage(String addressDetailsLanguage) {
		AddressDetailsLanguage = addressDetailsLanguage;
	}

	public String getAddressDetailsPhone() {
		return AddressDetailsPhone;
	}

	public void setAddressDetailsPhone(String addressDetailsPhone) {
		AddressDetailsPhone = addressDetailsPhone;
	}

	public String getAddressDetailsFax() {
		return AddressDetailsFax;
	}

	public void setAddressDetailsFax(String addressDetailsFax) {
		AddressDetailsFax = addressDetailsFax;
	}

	public String getAddressDetailsTelex() {
		return AddressDetailsTelex;
	}

	public void setAddressDetailsTelex(String addressDetailsTelex) {
		AddressDetailsTelex = addressDetailsTelex;
	}

	public String getAddressDetailsTelexAnswerBack() {
		return AddressDetailsTelexAnswerBack;
	}

	public void setAddressDetailsTelexAnswerBack(String addressDetailsTelexAnswerBack) {
		AddressDetailsTelexAnswerBack = addressDetailsTelexAnswerBack;
	}

	public String getAddressDetailsEmail() {
		return AddressDetailsEmail;
	}

	public void setAddressDetailsEmail(String addressDetailsEmail) {
		AddressDetailsEmail = addressDetailsEmail;
	}

	public String getAddressDetailsSwiftBIC() {
		return AddressDetailsSwiftBIC;
	}

	public void setAddressDetailsSwiftBIC(String addressDetailsSwiftBIC) {
		AddressDetailsSwiftBIC = addressDetailsSwiftBIC;
	}

	public String getAddressDetailsTransferMethod() {
		return AddressDetailsTransferMethod;
	}

	public void setAddressDetailsTransferMethod(String addressDetailsTransferMethod) {
		AddressDetailsTransferMethod = addressDetailsTransferMethod;
	}

	public String getAddressDetailsAddresseeCustomerSBB() {
		return AddressDetailsAddresseeCustomerSBB;
	}

	public void setAddressDetailsAddresseeCustomerSBB(String addressDetailsAddresseeCustomerSBB) {
		AddressDetailsAddresseeCustomerSBB = addressDetailsAddresseeCustomerSBB;
	}

	public String getAddressDetailsAddresseeCustomer() {
		return AddressDetailsAddresseeCustomer;
	}

	public void setAddressDetailsAddresseeCustomer(String addressDetailsAddresseeCustomer) {
		AddressDetailsAddresseeCustomer = addressDetailsAddresseeCustomer;
	}

	public String getAddressDetailsNumberOfCopies() {
		return AddressDetailsNumberOfCopies;
	}

	public void setAddressDetailsNumberOfCopies(String addressDetailsNumberOfCopies) {
		AddressDetailsNumberOfCopies = addressDetailsNumberOfCopies;
	}

	public String getAddressDetailsNumberOfOriginals() {
		return AddressDetailsNumberOfOriginals;
	}

	public void setAddressDetailsNumberOfOriginals(String addressDetailsNumberOfOriginals) {
		AddressDetailsNumberOfOriginals = addressDetailsNumberOfOriginals;
	}

	public String getSpecialInstructionDetailsSeverity() {
		return SpecialInstructionDetailsSeverity;
	}

	public void setSpecialInstructionDetailsSeverity(String specialInstructionDetailsSeverity) {
		SpecialInstructionDetailsSeverity = specialInstructionDetailsSeverity;
	}

	public String getSpecialInstructionDetailsCode() {
		return SpecialInstructionDetailsCode;
	}

	public void setSpecialInstructionDetailsCode(String specialInstructionDetailsCode) {
		SpecialInstructionDetailsCode = specialInstructionDetailsCode;
	}

	public String getSpecialInstructionDetailsDetails() {
		return SpecialInstructionDetailsDetails;
	}

	public void setSpecialInstructionDetailsDetails(String specialInstructionDetailsDetails) {
		SpecialInstructionDetailsDetails = specialInstructionDetailsDetails;
	}

	public String getSpecialInstructionDetailsStyle() {
		return SpecialInstructionDetailsStyle;
	}

	public void setSpecialInstructionDetailsStyle(String specialInstructionDetailsStyle) {
		SpecialInstructionDetailsStyle = specialInstructionDetailsStyle;
	}

	public String getSpecialInstructionDetailsEmphasis() {
		return SpecialInstructionDetailsEmphasis;
	}

	public void setSpecialInstructionDetailsEmphasis(String specialInstructionDetailsEmphasis) {
		SpecialInstructionDetailsEmphasis = specialInstructionDetailsEmphasis;
	}

	public String getOtherDetailsAllowMT103C() {
		return OtherDetailsAllowMT103C;
	}

	public void setOtherDetailsAllowMT103C(String otherDetailsAllowMT103C) {
		OtherDetailsAllowMT103C = otherDetailsAllowMT103C;
	}

	public String getOtherDetailsCutoffAmountAmount() {
		return OtherDetailsCutoffAmountAmount;
	}

	public void setOtherDetailsCutoffAmountAmount(String otherDetailsCutoffAmount) {
		OtherDetailsCutoffAmountAmount = otherDetailsCutoffAmount;
	}

	public String getOtherDetailsCutoffAmountCurrency() {
		return OtherDetailsCutoffAmountCurrency;
	}

	public void setOtherDetailsCutoffAmountCurrency(String otherDetailsCutoffAmountCurrency) {
		OtherDetailsCutoffAmountCurrency = otherDetailsCutoffAmountCurrency;
	}

	public String getOtherDetailsSWIFTAckRequired() {
		return OtherDetailsSWIFTAckRequired;
	}

	public void setOtherDetailsSWIFTAckRequired(String otherDetailsSWIFTAckRequired) {
		OtherDetailsSWIFTAckRequired = otherDetailsSWIFTAckRequired;
	}

	public String getOtherDetailsTransliterateSWIFT() {
		return OtherDetailsTransliterateSWIFT;
	}

	public void setOtherDetailsTransliterateSWIFT(String otherDetailsTransliterateSWIFT) {
		OtherDetailsTransliterateSWIFT = otherDetailsTransliterateSWIFT;
	}

	public String getOtherDetailsTeam() {
		return OtherDetailsTeam;
	}

	public void setOtherDetailsTeam(String otherDetailsTeam) {
		OtherDetailsTeam = otherDetailsTeam;
	}

	public String getOtherDetailsCorporateAccess() {
		return OtherDetailsCorporateAccess;
	}

	public void setOtherDetailsCorporateAccess(String otherDetailsCorporateAccess) {
		OtherDetailsCorporateAccess = otherDetailsCorporateAccess;
	}

	public String getOtherDetailsPrincipalFxRateCode() {
		return OtherDetailsPrincipalFxRateCode;
	}

	public void setOtherDetailsPrincipalFxRateCode(String otherDetailsPrincipalFxRateCode) {
		OtherDetailsPrincipalFxRateCode = otherDetailsPrincipalFxRateCode;
	}

	public String getOtherDetailsChargeFxRateCode() {
		return OtherDetailsChargeFxRateCode;
	}

	public void setOtherDetailsChargeFxRateCode(String otherDetailsChargeFxRateCode) {
		OtherDetailsChargeFxRateCode = otherDetailsChargeFxRateCode;
	}

	public String getOtherDetailsAllowTaxExemptions() {
		return OtherDetailsAllowTaxExemptions;
	}

	public void setOtherDetailsAllowTaxExemptions(String otherDetailsAllowTaxExemptions) {
		OtherDetailsAllowTaxExemptions = otherDetailsAllowTaxExemptions;
	}

	public String getOtherDetailsSuspended() {
		return OtherDetailsSuspended;
	}

	public void setOtherDetailsSuspended(String otherDetailsSuspended) {
		OtherDetailsSuspended = otherDetailsSuspended;
	}

	public String getSwiftDetailsMainBankingEntity() {
		return SwiftDetailsMainBankingEntity;
	}

	public void setSwiftDetailsMainBankingEntity(String swiftDetailsMainBankingEntity) {
		SwiftDetailsMainBankingEntity = swiftDetailsMainBankingEntity;
	}

	public String getSwiftDetailsSwiftAddress() {
		return SwiftDetailsSwiftAddress;
	}

	public void setSwiftDetailsSwiftAddress(String swiftDetailsSwiftAddress) {
		SwiftDetailsSwiftAddress = swiftDetailsSwiftAddress;
	}

	public String getSwiftDetailsAuthenticated() {
		return SwiftDetailsAuthenticated;
	}

	public void setSwiftDetailsAuthenticated(String swiftDetailsAuthenticated) {
		SwiftDetailsAuthenticated = swiftDetailsAuthenticated;
	}

	public String getSwiftDetailsBlocked() {
		return SwiftDetailsBlocked;
	}

	public void setSwiftDetailsBlocked(String swiftDetailsBlocked) {
		SwiftDetailsBlocked = swiftDetailsBlocked;
	}

	public String getSwiftDetailsClosed() {
		return SwiftDetailsClosed;
	}

	public void setSwiftDetailsClosed(String swiftDetailsClosed) {
		SwiftDetailsClosed = swiftDetailsClosed;
	}

	public String getSwiftDetailsTransliterationRequired() {
		return SwiftDetailsTransliterationRequired;
	}

	public void setSwiftDetailsTransliterationRequired(String swiftDetailsTransliterationRequired) {
		SwiftDetailsTransliterationRequired = swiftDetailsTransliterationRequired;
	}

	public String getCustomerExtraData() {
		return CustomerExtraData;
	}

	public void setCustomerExtraData(String customerExtraData) {
		CustomerExtraData = customerExtraData;
	}

	public String getTICustomerExtraData() {
		return TICustomerExtraData;
	}

	public void setTICustomerExtraData(String tICustomerExtraData) {
		TICustomerExtraData = tICustomerExtraData;
	}

}
