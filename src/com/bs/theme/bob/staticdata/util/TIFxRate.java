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

public class TIFxRate implements Cloneable {

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String FxRateCode = "";
	private String BankingEntity = "";
	private String Currency = "";
	private String BuyExchangeRate = "";
	private String BuyPercentSpread = "";
	private String BuySpreadRate = "";
	private String SellExchangeRate = "";
	private String SellPercentSpread = "";
	private String SellSpreadRate = "";
	private String BuyRateSpecific = "";
	private String SellRateSpecific = "";
	private String BaseCurrency = "";
	private String Reciprocal = "";

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

		tokens.put("FxRateCode", getFxRateCode());
		tokens.put("BankingEntity", StaticDataConstant.SourceBankingBusiness);
		tokens.put("Currency", getCurrency());
		tokens.put("BuyExchangeRate", getBuyExchangeRate());
		tokens.put("BuyPercentSpread", getBuyPercentSpread());
		tokens.put("BuySpreadRate", getBuySpreadRate());
		tokens.put("SellExchangeRate", getSellExchangeRate());
		tokens.put("SellPercentSpread", getSellPercentSpread());
		tokens.put("SellSpreadRate", getSellSpreadRate());
		tokens.put("BuyRateSpecific", getBuyRateSpecific());
		tokens.put("SellRateSpecific", getSellRateSpecific());
		tokens.put("BaseCurrency", getBaseCurrency());
		tokens.put("Reciprocal", getReciprocal());
	}

	public void generateSetProperty(String key, String value) {

		if (key.equalsIgnoreCase("MaintType")) {
			setMaintType(value);
		} else if (key.equalsIgnoreCase("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equalsIgnoreCase("FxRateCode")) {
			setFxRateCode(value);
		} else if (key.equalsIgnoreCase("BankingEntity")) {
			setBankingEntity(value);
		} else if (key.equalsIgnoreCase("Currency")) {
			setCurrency(value);
		} else if (key.equalsIgnoreCase("BuyExchangeRate")) {
			setBuyExchangeRate(value);
		} else if (key.equalsIgnoreCase("SellExchangeRate")) {
			setSellExchangeRate(value);
		} else if (key.equalsIgnoreCase("BuyPercentSpread")) {
			setBuyPercentSpread(value);
		} else if (key.equalsIgnoreCase("BuySpreadRate")) {
			setBuySpreadRate(value);
		}
		// else if (key.equalsIgnoreCase("BuyExchangeRate")) {
		// setSellExchangeRate(value);
		// }
		else if (key.equalsIgnoreCase("SellPercentSpread")) {
			setSellPercentSpread(value);
		} else if (key.equalsIgnoreCase("SellSpreadRate")) {
			setSellSpreadRate(value);
		} else if (key.equalsIgnoreCase("BuyRateSpecific")) {
			setBuyRateSpecific(value);
		} else if (key.equalsIgnoreCase("SellRateSpecific")) {
			setSellRateSpecific(value);
		} else if (key.equalsIgnoreCase("QWBaseCurrency")) {
			setBaseCurrency(value);
		} else if (key.equalsIgnoreCase("EWReciprocal")) {
			setReciprocal(value);
		}

	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(XMLFileReader.getTIFxRateSource());
		Reader reader = new TokenReplacingReader(fileValue, resolver);
		// logger.debug("reader:"+reader.toString());
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

	public String getFxRateCode() {
		return FxRateCode;
	}

	public void setFxRateCode(String fxRateCode) {
		FxRateCode = fxRateCode;
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

	public String getBuyExchangeRate() {
		return BuyExchangeRate;
	}

	public void setBuyExchangeRate(String buyExchangeRate) {
		BuyExchangeRate = buyExchangeRate;
	}

	public String getBuyPercentSpread() {
		return BuyPercentSpread;
	}

	public void setBuyPercentSpread(String buyPercentSpread) {
		BuyPercentSpread = buyPercentSpread;
	}

	public String getBuySpreadRate() {
		return BuySpreadRate;
	}

	public void setBuySpreadRate(String buySpreadRate) {
		BuySpreadRate = buySpreadRate;
	}

	public String getSellExchangeRate() {
		return SellExchangeRate;
	}

	public void setSellExchangeRate(String sellExchangeRate) {
		SellExchangeRate = sellExchangeRate;
	}

	public String getSellPercentSpread() {
		return SellPercentSpread;
	}

	public void setSellPercentSpread(String sellPercentSpread) {
		SellPercentSpread = sellPercentSpread;
	}

	public String getSellSpreadRate() {
		return SellSpreadRate;
	}

	public void setSellSpreadRate(String sellSpreadRate) {
		SellSpreadRate = sellSpreadRate;
	}

	public String getBuyRateSpecific() {
		return BuyRateSpecific;
	}

	public void setBuyRateSpecific(String buyRateSpecific) {
		BuyRateSpecific = buyRateSpecific;
	}

	public String getSellRateSpecific() {
		return SellRateSpecific;
	}

	public void setSellRateSpecific(String sellRateSpecific) {
		SellRateSpecific = sellRateSpecific;
	}

	public String getBaseCurrency() {
		return BaseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		BaseCurrency = baseCurrency;
	}

	public String getReciprocal() {
		return Reciprocal;
	}

	public void setReciprocal(String reciprocal) {
		Reciprocal = reciprocal;
	}

}
