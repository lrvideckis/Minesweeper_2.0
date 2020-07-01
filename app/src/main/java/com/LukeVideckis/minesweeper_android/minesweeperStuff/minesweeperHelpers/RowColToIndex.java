package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import android.util.Pair;

public class RowColToIndex {
	public static int rowColToIndex(int i, int j, int rows, int cols) {
		if (ArrayBounds.outOfBounds(i, j, rows, cols)) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i * cols + j;
	}

	public static Pair<Integer, Integer> indexToRowCol(int index, int rows, int cols) {
		if (index < 0 || index >= rows * cols) {
			throw new ArrayIndexOutOfBoundsException("index is out of bounds");
		}
		return new Pair<>(index / cols, index % cols);
	}
}
