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
import com.bs.themebridge.util.ValidationsUtil;
import com.bs.themebridge.util.XMLFileReader;

public class TIBaseRate {

	private final static Logger logger = Logger.getLogger(TIBaseRate.class.getName());

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String Branch = "";
	private String RateCode = "";
	private String RateDate = "";
	private String DateFlag = "";
	private String Rate = "";
	private String Historical = "";

	/*
	 * //these concadValues,tenor variable using in ANB BaseRate requirement
	 */
	private String concadValues = "";
	private String tenor = "";

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

		// //Header
		// tokens.put("Name", "Rangi");
		// tokens.put("Password", "");
		// tokens.put("Certificate", "");
		// tokens.put("Digest", "");
		// tokens.put("CorrelationId", UUID.randomUUID().toString());
		// Body

		// tokens.put("MaintType",getMaintType());
		// tokens.put("MaintainedInBackOffice",getMaintainedInBackOffice());
		tokens.put("Branch", getBranch());
		tokens.put("RateCode", getRateCode());
		tokens.put("RateDate", getRateDate());
		tokens.put("DateFlag", getDateFlag());
		tokens.put("Rate", getRate());
		tokens.put("Historical", getHistorical());

	}

	public void generateSetProperty(String key, String value) {
		String rateCode = key;
		String rate = value;
		logger.debug("rateCode, rate,  " + rateCode + "\t" + rate);
		if (key.equalsIgnoreCase("MaintType")) {
			setMaintType(value);
		} else if (key.equalsIgnoreCase("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equalsIgnoreCase("Branch")) {
			setBranch(value);
		} else if (ValidationsUtil.isValidString(rateCode)) {
			logger.debug("rateCode>>> " + rateCode);
			setRateCode(rateCode);
			setRate(rate);
		} // else if (ThemeBridgeUtil.isValidString(rate)) {
			// logger.debug("rate>>> " + rate);
			// setRate(rate);
			// }
		else if (key.equalsIgnoreCase("RateDate")) {
			setRateDate(value);
		} else if (key.equalsIgnoreCase("DateFlag")) {
			setDateFlag(value);
		} else if (key.equalsIgnoreCase("Historical")) {
			setHistorical(value);
		}

		/*
		 * This block of code only for ANB
		 */
		else if (key.equalsIgnoreCase("Tenor") || key.equalsIgnoreCase("RateCode")) {

			if (key.equalsIgnoreCase("Tenor")) {
				tenor = value;
			}
			if (key.equalsIgnoreCase("RateCode")) {
				concadValues = value + tenor;
				setRateCode(concadValues);
			}

		}

		/*
		 * This block of code only for ANB
		 */
		else if (key.equalsIgnoreCase("RateDate")) {
			setRateDate(value);
		} else if (key.equalsIgnoreCase("DateFlag")) {
			setDateFlag(value);
		} else if (key.equalsIgnoreCase(rateCode)) {
			setRate(value);
		} else if (key.equalsIgnoreCase("Historical")) {
			setHistorical(value);
		}
	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(XMLFileReader.getTIBaseRateSource());
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

	public String getBranch() {
		return Branch;
	}

	public void setBranch(String branch) {
		Branch = branch;
	}

	public String getRateCode() {
		return RateCode;
	}

	public void setRateCode(String rateCode) {
		RateCode = rateCode;
	}

	public String getRateDate() {
		return RateDate;
	}

	public void setRateDate(String rateDate) {
		RateDate = rateDate;
	}

	public String getDateFlag() {
		return DateFlag;
	}

	public void setDateFlag(String dateFlag) {
		DateFlag = dateFlag;
	}

	public String getRate() {
		return Rate;
	}

	public void setRate(String rate) {
		Rate = rate;
	}

	public String getHistorical() {
		return Historical;
	}

	public void setHistorical(String historical) {
		Historical = historical;
	}

}
