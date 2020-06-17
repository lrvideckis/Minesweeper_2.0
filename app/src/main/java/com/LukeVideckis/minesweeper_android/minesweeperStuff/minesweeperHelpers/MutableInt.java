package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

public class MutableInt {
	private int value;

	public MutableInt(int startVal) {
		value = startVal;
	}

	public void addWith(int delta) {
		value = Math.addExact(value, delta);
	}

	public int get() {
		return value;
	}
}
