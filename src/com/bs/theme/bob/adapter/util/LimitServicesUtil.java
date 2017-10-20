package com.bs.theme.bob.adapter.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.bs.themebridge.util.DatabaseUtility;
import com.test.NumberFormatting;
import com.test.NumbersConversion;

public class LimitServicesUtil {

	private final static Logger logger = Logger.getLogger(LimitServicesUtil.class.getName());

	public static void main(String args[]) {
		// LimitServicesUtil bu = new LimitServicesUtil();

		// String postingAmount = "575000";
		// String postingCurrency = "USD";

		getFCYRate("USD");
		getFCYSpotRate("USD");

		// logger.debug(getExposureAmount(postingAmount,
		// postingCurrency));

		// getDecimalforCurrency("USD");

		// exposureLongAmount("575000", "USD");

		// floating point calculation
		// final double amount1 = 2.0;
		// final double amount2 = 1.1;
		// logger.debug("difference between 2.0 and 1.1 using double is: "
		// + (amount1 - amount2));

		// Use BigDecimal for financial calculation
		// final BigDecimal amount3 = new BigDecimal("2.00");
		// final BigDecimal amount4 = new BigDecimal("1.10");
		// logger.debug("difference between 2.0 and 1.1 using BigDecimal
		// is: " + (amount3.multiply(amount4)));
		// logger.debug("difference between 2.0 and 1.1 using BigDecimal
		// is: " + (amount3.subtract(amount4)));

		// Use BigDecimal for financial calculation
		// final BigDecimal amount5 = new BigDecimal("5750.00");
		// final BigDecimal amount6 = new BigDecimal("66.74");
		//
		// BigDecimal bd = amount5.multiply(amount6);
		// logger.debug("BigDecimal is: " + bd);
		// double amount = bd.doubleValue();
		//
		// String roundOffINRValue =
		// NumberFormatting.currencyRoundOffValue(amount);
		// logger.debug("roundOffINRValue : " + roundOffINRValue);

	}

	/**
	 * 2 Limit LCBD Reversal
	 * 
	 * @param tranAmount
	 * @param fcyRate
	 * @return
	 */
	public static String getINRAmount(String titrxnamount, String currency, double fcySpotRateDouble) {

		String inrEquivalentamount = "";

		try {
			double titrxnamountDouble = Double.parseDouble(titrxnamount);
			// logger.debug("TransactionAmount : " + titrxnamountDouble);

			double mulpliedamountDouble = titrxnamountDouble * fcySpotRateDouble;
			// logger.debug("MultipliedAmount : " + mulpliedamountDouble);

			DecimalFormat twoPlaces = null;
			if (currency.equals("BHD") || currency.equals("OMR") || currency.equals("KWD") || currency.equals("JOD"))
				twoPlaces = new DecimalFormat("0.000");
			else if (currency.equals("JPY"))
				twoPlaces = new DecimalFormat("0");
			else
				twoPlaces = new DecimalFormat("0.00");

			inrEquivalentamount = twoPlaces.format(mulpliedamountDouble);
			// logger.debug("inrEquivalentamount : " + inrEquivalentamount);

		} catch (Exception e) {

		}
		return inrEquivalentamount;
	}

	public static String getSpotEquivalentUSDAmount(String tranAmount, double fcySpotRate) {

		String inrEquivalentamount = "";

		try {
			double tranAmountDouble = Double.parseDouble(tranAmount);
			// logger.debug("TransactionAmount(string) : " + tranAmountDouble);

			double convertedAmount = tranAmountDouble / fcySpotRate;
			// logger.debug("Divided Amount(double) : " + convertedAmount);

			inrEquivalentamount = String.valueOf(convertedAmount);
			// logger.debug("USD Divided Amount(string) : " +
			// inrEquivalentamount);

		} catch (Exception e) {

		}
		return inrEquivalentamount;
	}

	/**
	 * 2 Limit LCBD Reversal
	 * 
	 * @param tranAmount
	 * @param fcyRate
	 * @return
	 */
	public static String getSpotEquivalentINRAmount(String tranAmount, double fcySpotRate) {

		String inrEquivalentamount = "";

		try {
			double tranAmountDouble = Double.parseDouble(tranAmount);
			// logger.debug("TransactionAmount(string) : " + tranAmountDouble);

			double convertedAmount = tranAmountDouble * fcySpotRate;
			// logger.debug("MultipliedAmount(double) : " + convertedAmount);

			inrEquivalentamount = String.valueOf(convertedAmount);
			// logger.debug("INR MultipliedAmount(string) : " +
			// inrEquivalentamount);

		} catch (Exception e) {

		}
		return inrEquivalentamount;
	}

	/**
	 * TEST 2
	 * 
	 * @param tranAmount
	 * @param fcyRate
	 * @return
	 */
	public static String getSpotEquivalentINRAmountF(String tranAmount, float fcySpotRate) {

		String inrEquivalentamount = "";

		try {
			float tranAmountDouble = Float.parseFloat(tranAmount);
			// logger.debug("TransactionAmount(string) : " + tranAmountDouble);

			float convertedAmount = tranAmountDouble * fcySpotRate;
			// logger.debug("MultipliedAmount(double) : " + convertedAmount);

			inrEquivalentamount = String.valueOf(convertedAmount);
			// logger.debug("INR MultipliedAmount(string) : " +
			// inrEquivalentamount);

		} catch (Exception e) {

		}
		return inrEquivalentamount;
	}

	/**
	 * 1
	 * 
	 * @param tranAmount
	 * @param fcyRate
	 * @return
	 */
	public String getEquivalentINRAmount(String tranAmount, double fcyRate) {

		logger.debug("Get Equivalent amount");

		logger.debug("fcyRate : " + fcyRate);
		logger.debug("tranAmount : " + fcyRate);
		String inrEquivalentamount = "";

		double tranAmounts = Double.parseDouble(tranAmount);
		// logger.debug("fcyAmountRate " + tranAmounts);

		double convertedAmount = tranAmounts * fcyRate;
		// logger.debug("convertedAmount " + convertedAmount);

		long l = (long) convertedAmount;
		// logger.debug("l >> " + l);

		inrEquivalentamount = String.valueOf(l);
		// logger.debug("inrEquivalentamount1 " + inrEquivalentamount);

		// inrEquivalentamount = String.valueOf(convertedAmount);
		// logger.debug("inrEquivalentamount " + inrEquivalentamount);
		// float f = Float.valueOf(inrEquivalentamount);
		// long value = (long) f;
		// logger.debug("f " + value);

		return inrEquivalentamount;
	}

	/**
	 * 
	 * @param tranAmount
	 * @param ccy
	 * @param fcyRate
	 * @return
	 */
	public static long getEquivalentINRAmount(String tranAmount, String ccy, double fcyRate) {

		logger.debug(tranAmount + ccy + fcyRate);

		String tran = getTransactionAmount(tranAmount, ccy);
		// logger.debug("tran : " + tran);

		double tranAmounts = Double.parseDouble(tran);
		// logger.debug("Transaction amount : " + tranAmounts);

		double convertedAmount = tranAmounts * fcyRate;
		// logger.debug("PostingAmount * FCY : " + convertedAmount);

		String convertedStr = String.valueOf(convertedAmount);
		String roundOffINRValue = NumberFormatting.CurrencyRoundOffValue(convertedStr);
		// logger.debug(roundOffINRValue);

		long inrlongvalue = NumbersConversion.StringLong(roundOffINRValue);
		// logger.debug("inrlongvalue >>" + inrlongvalue);

		// long inrlongvalue = Math.round(convertedAmount);
		// logger.debug("Long " + inrlongvalue);

		return inrlongvalue;
	}

	/**
	 * LIMIT FACILITIES SUPERB!!!
	 * 
	 * @param postingLongAmount
	 * @param postingCurrency
	 * @return
	 */
	public static long exposureLongAmount(String postingLongAmount, String postingCurrency) {

		long exposureAmount = 0;
		if (!postingCurrency.equals("INR")) {
			String fcyRate = getFCYRate(postingCurrency);
			logger.debug("Rate for (" + postingCurrency + ">>-->>INR) : " + fcyRate);
			// New 2017-08-01
			fcyRate = LimitServicesUtil.getFCYSpotRate(postingCurrency);
			// String fcyRate =
			// LimitServicesUtil.getINRAmount(postingLongAmount,
			// postingCurrency, fcySpotRate);
			logger.debug("Rate for (" + postingCurrency + ">>-->>INR) : " + fcyRate);

			String trxnPostingAmount = getTransactionAmount(postingLongAmount, postingCurrency);
			// logger.debug("TrxnPostingAmount : " + trxnPostingAmount);

			// Use BigDecimal for financial calculation
			final BigDecimal amount5 = new BigDecimal(trxnPostingAmount);
			final BigDecimal amount6 = new BigDecimal(fcyRate);

			BigDecimal multipliedAmonunt = amount5.multiply(amount6);
			// logger.debug("MultipliedAmonunt : " + multipliedAmonunt);
			// double amount = multipliedAmonunt.doubleValue();

			String roundOffINRValue = NumberFormatting.currencyRoundOffValue(multipliedAmonunt, postingCurrency);
			// logger.debug("RoundOffINRValue : " + roundOffINRValue);
			exposureAmount = NumbersConversion.StringLong(roundOffINRValue);

		} else {
			logger.debug("INR currency");
			exposureAmount = Long.parseLong(postingLongAmount);

		}
		logger.debug("ExposureAmount : " + exposureAmount);
		return exposureAmount;
	}

	public static double getDecimalforCurrency(String ccy) {

		double decimal = 0.0;
		Connection aConnection = null;
		PreparedStatement aPreparedStatement = null;
		ResultSet aResultset = null;
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			String query = "select power(10, C8CED) from C8PF where trim(c8ccy) ='" + ccy + "'";
			// logger.debug("Qurey is " + query);
			aPreparedStatement = aConnection.prepareStatement(query);
			aResultset = aPreparedStatement.executeQuery();
			if (aResultset.next()) {
				decimal = aResultset.getDouble(1);
			}

		} catch (Exception e) {
			logger.error("Exception is " + e.getMessage());
			e.printStackTrace();
		}

		finally {
			DatabaseUtility.surrenderPrepdConnection(aConnection, aPreparedStatement, aResultset);
		}
		logger.debug(decimal);
		return decimal;
	}

	/**
	 * 
	 * @param postingAmount
	 * @param postingCurrency
	 * @return
	 */
	public static long getExposureAmount(String postingAmount, String postingCurrency) {

		long postingAmountlong = 0;
		if (!postingCurrency.equals("INR")) {
			logger.debug("FCY Currency");
			// LimitServicesUtil limitUtil = new LimitServicesUtil();
			double fcyRate = LimitServicesUtil.getRateForCcy(postingCurrency);
			// fcyRate = LimitServicesUtil.getSpotRateFCY(postingCurrency);
			// logger.debug("1 double : " + fcyRate);

			long equalentINRAmount = LimitServicesUtil.getEquivalentINRAmount(postingAmount, postingCurrency, fcyRate);
			// logger.debug(">> " + equalentINRAmount);
			// postingAmountlong = equalentINRAmount * 100;
			postingAmountlong = equalentINRAmount;

		} else {
			logger.debug("INR currency");
			postingAmountlong = Long.parseLong(postingAmount);

		}
		// logger.debug("PostingAmountlong : " + postingAmountlong);
		return postingAmountlong;
	}

	/**
	 * 
	 * @param amount
	 * @param currency
	 * @return
	 */
	public static String getTransactionAmount2(String amount, String currency) {

		String result = "";
		BigDecimal transAmount = new BigDecimal(amount);

		if (currency.equals("OMR") || currency.equals("BHD") || currency.equals("KWD") || currency.equals("JOD")) {
			result = transAmount.divide(new BigDecimal(1000), 3, RoundingMode.CEILING).toString();
		} else if (currency.equals("JPY")) {
			result = amount;
		} else {
			result = transAmount.divide(new BigDecimal(100), 2, RoundingMode.CEILING).toString();
		}

		return result.trim();
	}

	/**
	 * 
	 * @param amount
	 * @param currency
	 * @return
	 */
	public static String getTransactionAmount(String amount, String currency) {

		String result = "";
		BigDecimal transAmount = new BigDecimal(amount);

		if (currency.equals("OMR") || currency.equals("BHD") || currency.equals("KWD") || currency.equals("JOD")) {
			result = transAmount.divide(new BigDecimal(1000), 3, RoundingMode.CEILING).toString();
		} else if (currency.equals("JPY")) {
			result = amount;
		} else {
			result = transAmount.divide(new BigDecimal(100), 2, RoundingMode.CEILING).toString();
		}

		return result.trim();
	}

	/**
	 * 
	 * @param currency
	 * @return
	 */
	public static String getFCYRate(String currency) {

		String buyRate = "";
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;

		String accountTypeQuery = "SELECT CODE53, CURREN49, BUYEXC03, SELLEX99 FROM FXRATE86 WHERE CURREN49 = '"
				+ currency + "' AND CODE53 = 'RVRTCD' ";
		// logger.debug("AccountTypeQuery : " + accountTypeQuery);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(accountTypeQuery);
			while (aResultset.next()) {
				// aResultset.getString("CODE53");
				// aResultset.getString("CURREN49");
				buyRate = aResultset.getString("BUYEXC03");
				// sellRate = aResultset.getInt("SELLEX99");
			}
		} catch (Exception e) {
			logger.error("Exception while" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		// logger.debug("Rate For Ccy : " + buyRate);
		return buyRate;
	}

	/**
	 * 
	 * @param currency
	 * @return
	 */
	public static double getRateForCcy(String currency) {

		double buyRate = 0;
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;

		// AND CODE53 = 'RVRTCD'
		String accountTypeQuery = "SELECT CODE53, CURREN49, BUYEXC03, SELLEX99 FROM FXRATE86 WHERE CURREN49 = '"
				+ currency + "' AND CODE53 = 'RVRTCD' ";
		logger.debug("AccountTypeQuery : " + accountTypeQuery);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(accountTypeQuery);
			while (aResultset.next()) {
				// aResultset.getString("CODE53");
				// aResultset.getString("CURREN49");
				buyRate = aResultset.getDouble("BUYEXC03");
				// sellRate = aResultset.getInt("SELLEX99");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		// logger.debug("Rate For Ccy : " + buyRate);
		return buyRate;
	}

	/**
	 * 
	 * @param currency
	 * @return
	 */
	public static double getSpotRateFCY(String currency) {

		double buyRate = 0;
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;

		String SpotRateQuery = "SELECT SPOTRATE FROM SPOTRATE WHERE CURRENCY = '" + currency + "'";
		// logger.debug("SpotRateQuery : " + SpotRateQuery);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(SpotRateQuery);
			while (aResultset.next()) {
				buyRate = aResultset.getDouble("SPOTRATE");
			}
		} catch (Exception e) {
			logger.error("Exceptions! while getting spotrate..! " + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		// logger.debug("SPOTRATE For Ccy : " + buyRate);
		return buyRate;
	}

	/**
	 * 
	 * @param currency
	 * @return
	 */
	public static String getFCYSpotRate(String currency) {

		String spotRate = "";
		ResultSet aResultset = null;
		Statement aStatement = null;
		Connection aConnection = null;

		String accountTypeQuery = "SELECT SPOTRATE FROM SPOTRATE WHERE CURRENCY = '" + currency + "'";
		// logger.debug("AccountTypeQuery : " + accountTypeQuery);
		try {
			aConnection = DatabaseUtility.getTizoneConnection();
			aStatement = aConnection.createStatement();
			aResultset = aStatement.executeQuery(accountTypeQuery);
			while (aResultset.next()) {
				spotRate = aResultset.getString("SPOTRATE");
			}
		} catch (Exception e) {
			logger.error("Exception while" + e.getMessage());
			e.printStackTrace();

		} finally {
			DatabaseUtility.surrenderConnection(aConnection, aStatement, aResultset);
		}

		// logger.debug("Rate For Ccy : " + spotRate);
		return spotRate;
	}
}
