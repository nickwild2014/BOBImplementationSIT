package com.bs.theme.bob.staticdata.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.bs.themebridge.incoming.util.StaticDataConstant;
import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.XMLFileReader;

public class TIAccount {

	private final static Logger logger = Logger.getLogger(TIAccount.class.getName());

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String BackOfficeAccount = "";
	private String Branch = "";
	private String CustomerSourceBankingBusiness = "";
	private String CustomerMnemonic = "";
	private String CategoryCode = "";
	private String AccountType = "";
	private String Currency = "";
	private String OtherCurrency = "";
	private String ExternalAccount = "";
	private String IBAN = "";
	private String ShortName = "";
	private String Country = "";
	private String ContingentAccount = "";
	private String InternalAccount = "";
	private String DateOpened = "";
	private String DateMaintained = "";
	private String DateClosed = "";
	private String Description1 = "";
	private String Description2 = "";

	Map<String, String> tokens = new HashMap<String, String>();

	public void generateStaticDataConstants() {

		tokens.put("MaintType", StaticDataConstant.Account_MaintType);
		tokens.put("MaintainedInBackOffice", StaticDataConstant.MaintainedInBackOffice);
		tokens.put("SourceBankingBusiness", StaticDataConstant.SourceBankingBusiness);
	}

	public void generateHeader() {

		tokens.put("Name", StaticDataConstant.CredentialName);
		tokens.put("Password", "");
		tokens.put("Certificate", "");
		tokens.put("Digest", "");
		tokens.put("CorrelationId", UUID.randomUUID().toString());
	}

	public void generateTokenMap() {

		tokens.put("BackOfficeAccount", getBackOfficeAccount());
		tokens.put("Branch", getBranch());
		// tokens.put("SourceBankingBusiness",
		// getCustomerSourceBankingBusiness());
		tokens.put("Mnemonic", getCustomerMnemonic());
		tokens.put("CategoryCode", "");
		tokens.put("AccountType", getAccountType());
		tokens.put("Currency", getCurrency());
		tokens.put("OtherCurrency", "");
		tokens.put("ExternalAccount", "");
		tokens.put("IBAN", "");
		tokens.put("ShortName", "");
		tokens.put("Country", "");
		tokens.put("ContingentAccount", "");
		tokens.put("InternalAccount", getInternalAccount());// TRUE FALSE
		// logger.debug(getDateOpened().substring(0, 10));
		tokens.put("DateOpened", getDateOpened().substring(0, 10));
		// logger.debug(getDateClosed().substring(0, 10));
		tokens.put("DateMaintained", "");
		tokens.put("DateClosed", getDateClosed().substring(0, 10));
		tokens.put("Description1", getDescription1());
		tokens.put("Description2", getDescription2());
	}

	public void generateSetProperty(String key, String value) {

		if (key.equalsIgnoreCase("MaintType")) {
			setMaintType(value);
		} else if (key.equalsIgnoreCase("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equalsIgnoreCase("BackOfficeAccount")) {
			setBackOfficeAccount(value);
		} else if (key.equalsIgnoreCase("Branch")) {
			setBranch(value);
			// } else if (key.equalsIgnoreCase("SourceBankingBusiness")) {
			// setCustomerSourceBankingBusiness(value);
		} else if (key.equalsIgnoreCase("Mnemonic")) {
			setCustomerMnemonic(value);
		} else if (key.equalsIgnoreCase("CustomerMnemonic")) {
			setCustomerMnemonic(value);
		} else if (key.equalsIgnoreCase("CategoryCode")) {
			setCategoryCode(value);
		} else if (key.equalsIgnoreCase("AccountType")) {
			setAccountType(value);
		} else if (key.equalsIgnoreCase("Currency")) {
			setCurrency(value);
		} else if (key.equalsIgnoreCase("OtherCurrency")) {
			setOtherCurrency(value);
		} else if (key.equalsIgnoreCase("ExternalAccount")) {
			setExternalAccount(value);
		} else if (key.equalsIgnoreCase("IBAN")) {
			setIBAN(value);
		} else if (key.equalsIgnoreCase("ShortName")) {
			setShortName(value);
		} else if (key.equalsIgnoreCase("ContingentAccount")) {
			setContingentAccount(value);
		} else if (key.equalsIgnoreCase("Country")) {
			setCountry(value);
		} else if (key.equalsIgnoreCase("InternalAccount")) {
			setInternalAccount(value);
		} else if (key.equalsIgnoreCase("DateOpened")) {
			setDateOpened(value);
		} else if (key.equalsIgnoreCase("DateMaintained")) {
			setDateMaintained(value);
		} else if (key.equalsIgnoreCase("DateClosed")) {
			setDateClosed(value);
		} else if (key.equalsIgnoreCase("Description1")) {
			setDescription1(value);
		} else if (key.equalsIgnoreCase("Description2")) {
			setDescription2(value);
		}
	}

	// create header class
	public void constructRequestHeader() {

	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(XMLFileReader.getTIAccountSource());
		Reader reader = new TokenReplacingReader(fileValue, resolver);
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

	public String getBackOfficeAccount() {
		return BackOfficeAccount;
	}

	public void setBackOfficeAccount(String backOfficeAccount) {
		BackOfficeAccount = backOfficeAccount;
	}

	public String getBranch() {
		return Branch;
	}

	public void setBranch(String branch) {
		Branch = branch;
	}

	public String getCustomerSourceBankingBusiness() {
		return CustomerSourceBankingBusiness;
	}

	public void setCustomerSourceBankingBusiness(String customerSourceBankingBusiness) {
		CustomerSourceBankingBusiness = customerSourceBankingBusiness;
	}

	public String getCustomerMnemonic() {
		return CustomerMnemonic;
	}

	public void setCustomerMnemonic(String customerMnemonic) {
		CustomerMnemonic = customerMnemonic;
	}

	public String getCategoryCode() {
		return CategoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		CategoryCode = categoryCode;
	}

	public String getAccountType() {
		return AccountType;
	}

	public void setAccountType(String accountType) {
		AccountType = accountType;
	}

	public String getCurrency() {
		return Currency;
	}

	public void setCurrency(String currency) {
		Currency = currency;
	}

	public String getOtherCurrency() {
		return OtherCurrency;
	}

	public void setOtherCurrency(String otherCurrency) {
		OtherCurrency = otherCurrency;
	}

	public String getExternalAccount() {
		return ExternalAccount;
	}

	public void setExternalAccount(String externalAccount) {
		ExternalAccount = externalAccount;
	}

	public String getIBAN() {
		return IBAN;
	}

	public void setIBAN(String iBAN) {
		IBAN = iBAN;
	}

	public String getShortName() {
		return ShortName;
	}

	public void setShortName(String shortName) {
		ShortName = shortName;
	}

	public String getCountry() {
		return Country;
	}

	public void setCountry(String country) {
		Country = country;
	}

	public String getContingentAccount() {
		return ContingentAccount;
	}

	public void setContingentAccount(String contingentAccount) {
		ContingentAccount = contingentAccount;
	}

	public String getInternalAccount() {
		return InternalAccount;
	}

	public void setInternalAccount(String internalAccount) {
		InternalAccount = internalAccount;
	}

	public String getDateOpened() {
		return DateOpened;
	}

	public void setDateOpened(String dateOpened) {
		DateOpened = dateOpened;
	}

	public String getDateMaintained() {
		return DateMaintained;
	}

	public void setDateMaintained(String dateMaintained) {
		DateMaintained = dateMaintained;
	}

	public String getDateClosed() {
		return DateClosed;
	}

	public void setDateClosed(String dateClosed) {
		DateClosed = dateClosed;
	}

	public String getDescription1() {
		return Description1;
	}

	public void setDescription1(String description1) {
		Description1 = description1;
	}

	public String getDescription2() {
		return Description2;
	}

	public void setDescription2(String description2) {
		Description2 = description2;
	}

}
