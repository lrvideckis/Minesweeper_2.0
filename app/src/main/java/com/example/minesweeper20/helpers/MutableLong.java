package com.example.minesweeper20.helpers;

public class MutableLong {
	private long value;
	public MutableLong(long startVal) {
		value = startVal;
	}
	public void addWith(long delta) {
		value = Math.addExact(value, delta);
	}
	public long get() {
		return value;
	}
}
