package com.example.minesweeper20.minesweeperStuff.minesweeperHelpers;

import java.math.BigInteger;

public class MyMath {
	public static BigFraction BinomialCoefficient(int n, int k) throws Exception {
		if (k < 0 || k > n) {
			throw new Exception("invalid input");
		}
		if ((n == k) || (k == 0)) {
			return new BigFraction(1);
		}
		if ((k == 1) || (k == n - 1)) {
			return new BigFraction(n);
		}
		if (k > n / 2) {
			return BinomialCoefficient(n, n - k);
		}

		BigFraction result = new BigFraction(1);
		int i = n - k + 1;
		for (int j = 1; j <= k; j++) {
			final int gcd = gcd(i, j);
			result.multiplyWith(1, j / gcd);
			result.multiplyWith(i / gcd, 1);
			i++;
		}
		return result;
	}

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
