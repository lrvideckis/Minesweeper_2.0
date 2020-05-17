package com.example.minesweeper20.minesweeperStuff.helpers;

import android.util.Pair;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import java.util.ArrayList;

public class ArrayBounds {
	public static Pair<Integer,Integer> getArrayBounds(ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board) throws Exception {
		int rows = board.size();
		if(rows == 0) {
			throw new Exception("board has 0 rows");
		}
		int cols = board.get(0).size();
		if(cols == 0) {
			throw new Exception("board has 0 columns");
		}
		for(int i = 0; i < rows; ++i) {
			if(board.get(i).size() != cols) {
				throw new Exception("jagged board, not all rows are the same length");
			}
		}
		return new Pair<>(rows, cols);
	}

	public static boolean outOfBounds(int i, int j, int rows, int cols) {
		return (i < 0 || j < 0 || i >= rows || j >= cols);
	}
}
