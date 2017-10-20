package com.bs.theme.bob.staticdata.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.bs.themebridge.token.util.MapTokenResolver;
import com.bs.themebridge.token.util.TokenReplacingReader;
import com.bs.themebridge.util.XMLFileReader;

/**
 * Not implemented in Kotak Mahindra Bank
 * 
 * @author Prasath Ravichandran
 *
 */
public class TIAccountOfficer {

	private String MaintType = "";
	private String MaintainedInBackOffice = "";
	private String Code = "";
	private String Description = "";
	private String DepartmentCode = "";
	private String ManagerType = "";
	private String TransferMethod = "";
	private String Language = "";
	private String NameAndAddress = "";
	private String ZIP = "";
	private String Phone = "";
	private String Extension = "";
	private String Fax = "";
	private String Telex = "";
	private String TelexAnswerBack = "";
	private String EmailAddress = "";

	Map<String, String> tokens = new HashMap<String, String>();

	public void generateTokenMap() {
		tokens.put("MaintType", getMaintType());
		tokens.put("MaintainedInBackOffice", getMaintainedInBackOffice());
		tokens.put("Code", getCode());
		tokens.put("Description", getDescription());
		tokens.put("DepartmentCode", getDepartmentCode());
		tokens.put("ManagerType", getManagerType());
		tokens.put("TransferMethod", getTransferMethod());
		tokens.put("Language", getLanguage());
		tokens.put("NameAndAddress", getNameAndAddress());
		tokens.put("ZIP", getZIP());
		tokens.put("Phone", getPhone());
		tokens.put("Extension", getExtension());
		tokens.put("Fax", getFax());
		tokens.put("Telex", getTelex());
		tokens.put("TelexAnswerBack", getTelexAnswerBack());
		tokens.put("EmailAddress", getEmailAddress());
	}

	public void generateSetProperty(String key, String value) {

		if (key.equals("MaintType")) {
			setMaintType(value);
		} else if (key.equals("MaintainedInBackOffice")) {
			setMaintainedInBackOffice(value);
		} else if (key.equals("Code")) {
			setCode(value);
		} else if (key.equals("Description")) {
			setDescription(value);
		} else if (key.equals("DepartmentCode")) {
			setDepartmentCode(value);
		} else if (key.equals("ManagerType")) {
			setManagerType(value);
		} else if (key.equals("TransferMethod")) {
			setTransferMethod(value);
		} else if (key.equals("Language")) {
			setLanguage(value);
		} else if (key.equals("NameAndAddress")) {
			setNameAndAddress(value);
		} else if (key.equals("ZIP")) {
			setZIP(value);
		} else if (key.equals("Phone")) {
			setPhone(value);
		} else if (key.equals("Extension")) {
			setExtension(value);
		} else if (key.equals("Fax")) {
			setFax(value);
		} else if (key.equals("Telex")) {
			setTelex(value);
		} else if (key.equals("TelexAnswerBack")) {
			setTelexAnswerBack(value);
		} else if (key.equals("EmailAddress")) {
			setEmailAddress(value);
		}
	}

	public String getXMLString() throws Exception {

		MapTokenResolver resolver = new MapTokenResolver(tokens);
		Reader fileValue = new StringReader(
				XMLFileReader.getTIAccountOfficerSource());
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

	public String getCode() {
		return Code;
	}

	public void setCode(String code) {
		Code = code;
	}

	public String getDescription() {
		return Description;
	}

	public void setDescription(String description) {
		Description = description;
	}

	public String getDepartmentCode() {
		return DepartmentCode;
	}

	public void setDepartmentCode(String departmentCode) {
		DepartmentCode = departmentCode;
	}

	public String getManagerType() {
		return ManagerType;
	}

	public void setManagerType(String managerType) {
		ManagerType = managerType;
	}

	public String getTransferMethod() {
		return TransferMethod;
	}

	public void setTransferMethod(String transferMethod) {
		TransferMethod = transferMethod;
	}

	public String getLanguage() {
		return Language;
	}

	public void setLanguage(String language) {
		Language = language;
	}

	public String getNameAndAddress() {
		return NameAndAddress;
	}

	public void setNameAndAddress(String nameAndAddress) {
		NameAndAddress = nameAndAddress;
	}

	public String getZIP() {
		return ZIP;
	}

	public void setZIP(String zIP) {
		ZIP = zIP;
	}

	public String getPhone() {
		return Phone;
	}

	public void setPhone(String phone) {
		Phone = phone;
	}

	public String getExtension() {
		return Extension;
	}

	public void setExtension(String extension) {
		Extension = extension;
	}

	public String getFax() {
		return Fax;
	}

	public void setFax(String fax) {
		Fax = fax;
	}

	public String getTelex() {
		return Telex;
	}

	public void setTelex(String telex) {
		Telex = telex;
	}

	public String getTelexAnswerBack() {
		return TelexAnswerBack;
	}

	public void setTelexAnswerBack(String telexAnswerBack) {
		TelexAnswerBack = telexAnswerBack;
	}

	public String getEmailAddress() {
		return EmailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		EmailAddress = emailAddress;
	}

}
