package com.example.minesweeper20.helpers;

public class MyMath {
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
			final long gcd = gcd(i,j);
			result = Math.multiplyExact(result / (j / gcd), (i / gcd));
			i++;
		}
		return result;
	}

	public static int MyLog10MinWith4(long n) throws Exception {
		if(n < 0) {
			throw new Exception("log of negative number is undefined");
		}
		if(n < 10) return 1;
		if(n < 100) return 2;
		if(n < 1000) return 3;
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
}
