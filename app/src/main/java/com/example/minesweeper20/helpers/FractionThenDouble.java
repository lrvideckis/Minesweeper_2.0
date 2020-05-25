package com.example.minesweeper20.helpers;

//TODO: code this with bigInts to test double logic
public class FractionThenDouble {

	private int numerator, denominator;
	private double value;
	private boolean hasOverflowed = false;

	public FractionThenDouble(int value) {
		numerator = value;
		denominator = 1;
	}

	FractionThenDouble(FractionThenDouble other) throws Exception {
		setValue(other);
	}

	public void setValue(FractionThenDouble other) throws Exception {
		hasOverflowed = other.hasOverflowed;
		if(other.hasOverflowed) {
			value = other.value;
		} else {
			numerator = other.numerator;
			denominator = other.denominator;
			if(MyMath.gcd(numerator,denominator) != 1) {
				throw new Exception("given fraction isn't in reduced form, but I reduced after each operation");
			}
			if(denominator == 0) {
				throw new Exception("given fraction has a 0 denominator, but this shouldn't happen");
			}
		}
	}

	//this should only throw if _denominator is 0
	public void setValues(int _numerator, int _denominator) throws Exception {
		hasOverflowed = false;
		reduceAndSet(_numerator, _denominator);
	}

	public void addWith(int delta) {
		if(hasOverflowed) {
			value += delta;
			return;
		}
		long currNumerator;
		try {
			currNumerator = Math.addExact(numerator, Math.multiplyExact(delta, (long)denominator));
		} catch (ArithmeticException ignored) {
			hasOverflowed = true;
			value = numerator / (double) denominator;
			value += delta;
			return;
		}
		try {
			reduceAndSet(currNumerator, denominator);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void addWith(FractionThenDouble delta) {
		if(!hasOverflowed && !delta.hasOverflowed) {
			long newNumerator, newDenominator;
			try {
				newNumerator = Math.addExact(
						Math.multiplyExact((long) numerator, delta.denominator),
						Math.multiplyExact((long) denominator, delta.numerator)
				);
				newDenominator = Math.multiplyExact((long) denominator, delta.denominator);
			} catch(ArithmeticException ignored) {
				hasOverflowed = true;
				value = numerator / (double) denominator;
				value += delta.numerator / (double) delta.denominator;
				return;
			}
			try {
				reduceAndSet(newNumerator, newDenominator);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		//we have overflowed
		if(!hasOverflowed) {
			hasOverflowed = true;
			value = numerator / (double) denominator;
		}
		if(delta.hasOverflowed) {
			value += delta.value;
		} else {
			value += delta.numerator / (double) delta.denominator;
		}
	}

	//this will only throw if _denominator == 0
	public void multiplyWith(int _numerator, int _denominator) throws Exception {
		if(hasOverflowed) {
			value *= _numerator;
			value /= _denominator;
			return;
		}
		reduceAndSet(numerator * (long) _numerator, denominator * (long) _denominator);
	}

	public void multiplyWith(FractionThenDouble quotient) {
		if(!quotient.hasOverflowed) {
			try {
				multiplyWith(quotient.getNumerator(), quotient.getDenominator());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		if(!hasOverflowed) {
			hasOverflowed = true;
			value = numerator / (double) denominator;
		}
		value *= quotient.value;
	}

	//only will throw if quotient == 0 (divide by zero error)
	public void divideWith(FractionThenDouble quotient) throws ArithmeticException {
		if(!quotient.hasOverflowed) {
			try {
				multiplyWith(quotient.getDenominator(), quotient.getNumerator());
			} catch (Exception ignored) {
				throw new ArithmeticException("divide by zero");
			}
			return;
		}
		if(!hasOverflowed) {
			hasOverflowed = true;
			value = numerator / (double) denominator;
		}
		value /= quotient.value;
	}

	public int getNumerator() throws Exception {
		if(hasOverflowed) {
			throw new ArithmeticException("fraction has overflowed");
		}
		if(MyMath.gcd(numerator, denominator) != 1) {
			throw new Exception("fraction isn't in reduced form, but I reduced after every operation");
		}
		return numerator;
	}

	public int getDenominator() throws Exception {
		if(hasOverflowed) {
			throw new ArithmeticException("fraction has overflowed");
		}
		if(MyMath.gcd(numerator, denominator) != 1) {
			throw new Exception("fraction isn't in reduced form, but I reduced after every operation");
		}
		return denominator;
	}

	public boolean getHasOverflowed() {
		return hasOverflowed;
	}

	public double getValue() {
		return value;
	}

	private void reduceAndSet(long _numerator, long _denominator) throws Exception {
		if(hasOverflowed) {
			throw new Exception("this function should never be called when the fraction has overflowed");
		}
		final long gcd = MyMath.gcd(_numerator, _denominator);
		_numerator /= gcd;
		_denominator /= gcd;
		if(_numerator != (int)_numerator || _denominator != (int)_denominator) {
			hasOverflowed = true;
			value = _numerator / (double) _denominator;
			return;
		}
		numerator = (int)_numerator;
		denominator = (int)_denominator;
		if(denominator == 0) {
			throw new Exception("fraction with 0 as denominator");
		}
	}

	public boolean equals(FractionThenDouble other) {
		if(hasOverflowed != other.hasOverflowed) {
			return false;
		}
		if(hasOverflowed) {
			final double epsilon = 0.000000001;
			return Math.abs(value - other.value) < epsilon;
		}
		try {
			return numerator * (long) other.getDenominator() == denominator * (long) other.getNumerator();
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean equals(int other) {
		if(hasOverflowed) {
			return false;
		}
		return numerator == other * (long) denominator;
	}
}
