package com.example.minesweeper20.miscHelpers;

import android.text.InputFilter;
import android.text.Spanned;

public class NumberInputFilterMinMax implements InputFilter {
	private final int min, max;

	public NumberInputFilterMinMax(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destStart, int destEnd) {
		try {
			int input = Integer.parseInt(dest.toString() + source.toString());
			if (isInRange(min, max, input))
				return null;
		} catch (NumberFormatException ignored) {
		}
		return "";
	}

	private boolean isInRange(int lowerBound, int upperBound, int value) {
		return upperBound > lowerBound ? value >= lowerBound && value <= upperBound : value >= upperBound && value <= lowerBound;
	}
}
