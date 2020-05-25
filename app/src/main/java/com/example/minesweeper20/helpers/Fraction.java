package com.example.minesweeper20.helpers;

public class Fraction {
	private long numerator, denominator;
	public Fraction(long value) throws Exception {
		setValues(value, 1L);
	}

	public Fraction(long _numerator, long _denominator) throws Exception {
		setValues(_numerator, _denominator);
	}

	public void setValues(long _numerator, long _denominator) throws Exception {
		numerator = _numerator;
		denominator = _denominator;
		if(denominator == 0) {
			throw new Exception("fraction with 0 as denominator");
		}
	}

	public void addWith(Fraction delta) throws Exception {
		final long prevNumerator = numerator;
		final long prevDenominator = denominator;
		numerator = Math.multiplyExact(prevNumerator, delta.getDenominator());
		numerator = Math.addExact(numerator, Math.multiplyExact(prevDenominator, delta.getNumerator()));
		denominator = Math.multiplyExact(prevDenominator, delta.getDenominator());
	}

	public void multiplyWith(Fraction quotient) throws Exception {
		numerator = Math.multiplyExact(numerator, quotient.getNumerator());
		denominator = Math.multiplyExact(denominator, quotient.getDenominator());
	}

	public long getNumerator() throws Exception {
		reduce();
		return numerator;
	}

	public long getDenominator() throws Exception {
		reduce();
		return denominator;
	}

	private void reduce() throws Exception {
		final long gcd = MyMath.gcd(numerator, denominator);
		numerator /= gcd;
		denominator /= gcd;
		if(denominator == 0) {
			throw new Exception("fraction with 0 as denominator");
		}
	}
}
