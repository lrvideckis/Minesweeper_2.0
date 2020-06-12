package com.LukeVideckis.minesweeper20.minesweeperStuff;

public interface BacktrackingSolver extends MinesweeperSolver {
	boolean[][] getMineConfiguration(
			VisibleTile[][] board,
			int numberOfMines,
			int spotI,
			int spotJ,
			boolean wantMine
	) throws Exception;

	int getNumberOfIterations();

}
