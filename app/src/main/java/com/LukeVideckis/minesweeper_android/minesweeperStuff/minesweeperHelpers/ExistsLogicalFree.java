package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver;

public class ExistsLogicalFree {
	public static boolean isLogicalFree(MinesweeperSolver.VisibleTile[][] board) {
		for (MinesweeperSolver.VisibleTile[] row : board) {
			for (MinesweeperSolver.VisibleTile cell : row) {
				if (cell.getIsLogicalFree()) {
					return true;
				}
			}
		}
		return false;
	}
}
