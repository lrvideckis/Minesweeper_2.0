package com.example.minesweeper20.minesweeperStuff.minesweeperHelpers;

public class MyMath {

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
