package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class ExistsLogicalFree {
	public static boolean noLogicalFrees(VisibleTile[][] board) {
		for (VisibleTile[] row : board) {
			for (VisibleTile cell : row) {
				if (cell.getIsLogicalFree()) {
					return false;
				}
			}
		}
		return true;
	}
}
