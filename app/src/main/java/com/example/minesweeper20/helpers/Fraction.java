package com.example.minesweeper20.helpers;

import java.math.BigInteger;

public class Fraction {
	private int numerator, denominator;
	public Fraction(int value) throws Exception {
		setValues(value, 1);
	}

	public Fraction(int _numerator, int _denominator) throws Exception {
		setValues(_numerator, _denominator);
	}

	public void setValues(int _numerator, int _denominator) throws Exception {
		numerator = _numerator;
		denominator = _denominator;
		if(denominator == 0) {
			throw new Exception("fraction with 0 as denominator");
		}
	}

	public void addWith(Fraction delta) throws Exception {
		final int prevNumerator = numerator;
		final int prevDenominator = denominator;
		numerator = prevNumerator * delta.getDenominator() + prevDenominator * delta.getNumerator();
		denominator = prevDenominator * delta.getDenominator();
	}

	public void multiplyWith(Fraction quotient) throws Exception {
		numerator *= quotient.getNumerator();
		denominator *= quotient.getDenominator();
	}

	public int getNumerator() throws Exception {
		reduce();
		return numerator;
	}

	public int getDenominator() throws Exception {
		reduce();
		return denominator;
	}

	private void reduce() throws Exception {
		final int gcd = BigInteger.valueOf(numerator).gcd(BigInteger.valueOf(denominator)).intValue();
		numerator /= gcd;
		denominator /= gcd;
		if(denominator == 0) {
			throw new Exception("fraction with 0 as denominator");
		}
	}
}
