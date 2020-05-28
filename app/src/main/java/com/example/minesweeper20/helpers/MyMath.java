package com.example.minesweeper20.helpers;

public class MyMath {
	public static FractionThenDouble BinomialCoefficient(int n, int k) throws Exception {
		if (k < 0 || k > n) {
			throw new Exception("invalid input");
		}
		if ((n == k) || (k == 0)) {
			return new FractionThenDouble(1);
		}
		if ((k == 1) || (k == n - 1)) {
			return new FractionThenDouble(n);
		}
		if (k > n / 2) {
			return BinomialCoefficient(n, n - k);
		}

		FractionThenDouble result = new FractionThenDouble(1);
		int i = n - k + 1;
		for (int j = 1; j <= k; j++) {
			final int gcd = gcd(i, j);
			result.multiplyWith(1, j / gcd);
			result.multiplyWith(i / gcd, 1);
			i++;
		}
		return result;
	}

	public static int MyLog10MinWith4(long n) throws Exception {
		if (n < 0) {
			throw new Exception("log of negative number is undefined");
		}
		if (n < 10) return 1;
		if (n < 100) return 2;
		if (n < 1000) return 3;
		return 4;
	}

	static long gcd(long a, long b) {
		while (b > 0) {
			a %= b;

			long temp = b;
			b = a;
			a = temp;
		}
		return a;
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

	static int getRand(int min, int max) throws Exception {
		if (min > max) {
			throw new Exception("invalid parameters: min > max");
		}
		return (int) (Math.random() * ((max - min) + 1)) + min;
	}
}
