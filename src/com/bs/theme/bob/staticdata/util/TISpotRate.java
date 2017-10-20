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

public class TISpotRate implements Cloneable {

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String BankingEntity = "";
	private String Currency = "";
	private String SpotRate = "";
	private String Reciprocal = "";
	private String InvalidTradingCurrency = "";
	private String QuotationUnit = "";

	Map<String, String> tokens = new HashMap<String, String>();

	public void generateStaticDataConstants() {

		tokens.put("MaintType", StaticDataConstant.MaintType);
		tokens.put("MaintainedInBackOffice", StaticDataConstant.MaintainedInBackOffice);
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

		tokens.put("BankingEntity", StaticDataConstant.SourceBankingBusiness);
		tokens.put("Currency", getCurrency());
		tokens.put("SpotRate", getSpotRate());
		tokens.put("Reciprocal", getReciprocal());
		tokens.put("InvalidTradingCurrency", getInvalidTradingCurrency());
		tokens.put("QuotationUnit", getQuotationUnit());
	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(XMLFileReader.getTICurrencySpotRateSource());
		Reader reader = new TokenReplacingReader(fileValue, resolver);
		// logger.debug("reader:"+reader.toString());
		return reader.toString();
	}

	public void generateSetProperty(String key, String value) {

		if (key.equalsIgnoreCase("MaintType")) {
			setMaintType(value);
		} else if (key.equalsIgnoreCase("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equalsIgnoreCase("BankingEntity")) {
			setBankingEntity(value);
		} else if (key.equalsIgnoreCase("Currency")) {
			setCurrency(value);
		} else if (key.equalsIgnoreCase("SpotRate")) {
			setSpotRate(value);
		} else if (key.equalsIgnoreCase("Reciprocal")) {
			setReciprocal(value);
		} else if (key.equalsIgnoreCase("InvalidTradingCurrency")) {
			setInvalidTradingCurrency(value);
		} else if (key.equalsIgnoreCase("QuotationUnit")) {
			setQuotationUnit(value);
		}

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

	public String getBankingEntity() {
		return BankingEntity;
	}

	public void setBankingEntity(String bankingEntity) {
		BankingEntity = bankingEntity;
	}

	public String getCurrency() {
		return Currency;
	}

	public void setCurrency(String currency) {
		Currency = currency;
	}

	public String getReciprocal() {
		return Reciprocal;
	}

	public void setReciprocal(String reciprocal) {
		Reciprocal = reciprocal;
	}

	private String getSpotRate() {
		return SpotRate;
	}

	private void setSpotRate(String spotRate) {
		SpotRate = spotRate;
	}

	private String getInvalidTradingCurrency() {
		return InvalidTradingCurrency;
	}

	private void setInvalidTradingCurrency(String invalidTradingCurrency) {
		InvalidTradingCurrency = invalidTradingCurrency;
	}

	private String getQuotationUnit() {
		return QuotationUnit;
	}

	private void setQuotationUnit(String quotationUnit) {
		QuotationUnit = quotationUnit;
	}

	private Map<String, String> getTokens() {
		return tokens;
	}

	private void setTokens(Map<String, String> tokens) {
		this.tokens = tokens;
	}

}
