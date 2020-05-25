package com.example.minesweeper20.helpers;

import java.math.BigInteger;

public class Combinatorics {
	static int getRand(int min, int max) throws Exception {
		if(min > max) {
			throw new Exception("invalid parameters: min > max");
		}
		return (int) (Math.random() * ((max - min) + 1)) + min;
	}
	public static long BinomialCoefficient(int n, int k) throws Exception {
		if(k < 0 || k > n) {
			throw new Exception("invalid input");
		}
		if((n == k) || (k == 0)) {
			return 1;
		}
		if((k == 1) || (k == n - 1)) {
			return n;
		}
		if(k > n / 2) {
			return BinomialCoefficient(n, n - k);
		}

		long result = 1;
		int i = n - k + 1;
		for (int j = 1; j <= k; j++) {
			final long gcd = BigInteger.valueOf(i).gcd(BigInteger.valueOf(j)).longValue();
			result = (result / (j / gcd)) * (i / gcd);
			i++;
		}
		return result;
	}
}
