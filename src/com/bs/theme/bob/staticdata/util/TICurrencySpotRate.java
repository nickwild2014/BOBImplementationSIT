package com.bs.theme.bob.staticdata.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.XMLFileReader;

public class TICurrencySpotRate {

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String BankingEntity = "";
	private String Currency = "";
	private String SpotRate = "";
	private String Reciprocal = "";
	private String InvalidTradingCurrency = "";
	private String QuotationUnit = "";

	Map<String, String> tokens = new HashMap<String, String>();

	public void generateTokenMap() {

		tokens.put("MaintType", getMaintType());
		tokens.put("MaintainedInBackOffice", getMaintainedInBackOffice());
		tokens.put("BankingEntity", getBankingEntity());
		tokens.put("Currency", getCurrency());
		tokens.put("SpotRate", getSpotRate());
		tokens.put("Reciprocal", getReciprocal());
		tokens.put("InvalidTradingCurrency", getInvalidTradingCurrency());
		tokens.put("QuotationUnit", getQuotationUnit());

	}

	public void generateSetProperty(String key, String value) {

		if (key.equals("MaintType")) {
			setMaintType(value);
		} else if (key.equals("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equals("BankingEntity")) {
			setBankingEntity(value);
		} else if (key.equals("Currency")) {
			setCurrency(value);
		} else if (key.equals("SpotRate")) {
			setSpotRate(value);
		} else if (key.equals("Reciprocal")) {
			setReciprocal(value);
		} else if (key.equals("InvalidTradingCurrency")) {
			setInvalidTradingCurrency(value);
		} else if (key.equals("QuotationUnit")) {
			setQuotationUnit(value);
		}
	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(
				XMLFileReader.getTICurrencySpotRateSource());
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

	public String getSpotRate() {
		return SpotRate;
	}

	public void setSpotRate(String spotRate) {
		SpotRate = spotRate;
	}

	public String getReciprocal() {
		return Reciprocal;
	}

	public void setReciprocal(String reciprocal) {
		Reciprocal = reciprocal;
	}

	public String getInvalidTradingCurrency() {
		return InvalidTradingCurrency;
	}

	public void setInvalidTradingCurrency(String invalidTradingCurrency) {
		InvalidTradingCurrency = invalidTradingCurrency;
	}

	public String getQuotationUnit() {
		return QuotationUnit;
	}

	public void setQuotationUnit(String quotationUnit) {
		QuotationUnit = quotationUnit;
	}

}
