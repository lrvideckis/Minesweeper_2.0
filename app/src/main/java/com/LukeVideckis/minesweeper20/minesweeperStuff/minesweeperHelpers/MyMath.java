package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import java.math.BigInteger;

public class MyMath {
	//returns (n choose x) / (n choose y)
	public static BigFraction BinomialCoefficientFraction(int n, int x, int y) throws Exception {
		if (x < 0 || y < 0 || x > n || y > n) {
			throw new Exception("invalid parameters (n,x,y): " + n + " " + x + " " + y);
		}
		if (x == y) {
			return new BigFraction(1);
		}
		if (x > y) {
			return new BigFraction(productRange(n - x + 1, n - y), productRange(y + 1, x));
		}
		return new BigFraction(productRange(x + 1, y), productRange(n - y + 1, n - x));
	}

	private static BigInteger productRange(int min, int max) throws Exception {
		if (min > max) {
			throw new Exception("min > max");
		}
		BigInteger result = BigInteger.valueOf(min);
		for (int i = min + 1; i <= max; ++i) {
			result = result.multiply(BigInteger.valueOf(i));
		}
		return result;
	}

	public static int gcd(int a, int b) {
		while (b > 0) {
			a %= b;

			int temp = b;
			b = a;
			a = temp;
		}
		return a;
	}

	public static int getRand(int min, int max) throws Exception {
		if (min > max) {
			throw new Exception("invalid parameters: min > max");
		}
		return (int) (Math.random() * ((max - min) + 1)) + min;
	}
}
