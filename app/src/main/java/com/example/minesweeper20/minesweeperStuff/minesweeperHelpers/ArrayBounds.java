package com.example.minesweeper20.minesweeperStuff.minesweeperHelpers;

import android.util.Pair;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class ArrayBounds {
	public static Pair<Integer, Integer> getArrayBounds(VisibleTile[][] board) throws Exception {
		int rows = board.length;
		if (rows == 0) {
			throw new Exception("board has 0 rows");
		}
		int cols = board[0].length;
		if (cols == 0) {
			throw new Exception("board has 0 columns");
		}
		for (VisibleTile[] visibleTiles : board) {
			if (visibleTiles.length != cols) {
				throw new Exception("jagged board, not all rows are the same length");
			}
		}
		return new Pair<>(rows, cols);
	}

	public static boolean outOfBounds(int i, int j, int rows, int cols) {
		return (i < 0 || j < 0 || i >= rows || j >= cols);
	}
}
