package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class ConvertGameBoardFormat {
	public static VisibleTile[][] convertToNewBoard(MinesweeperGame minesweeperGame) {
		final int rows = minesweeperGame.getRows();
		final int cols = minesweeperGame.getCols();
		VisibleTile[][] board = new VisibleTile[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j] = new VisibleTile();
				board[i][j].updateVisibilityAndSurroundingMines(minesweeperGame.getCell(i, j));
			}
		}
		return board;
	}

	//TODO: this is deprecated
	public static void convertToExistingBoard(MinesweeperGame minesweeperGame, VisibleTile[][] board) throws Exception {
		final int rows = minesweeperGame.getRows();
		final int cols = minesweeperGame.getCols();
		if (board.length != rows) {
			throw new Exception("minesweeper game rows doesn't match board rows");
		}
		for (int i = 0; i < rows; ++i) {
			if (board[i].length != cols) {
				throw new Exception("minesweeper game cols doesn't match board cols");
			}
			for (int j = 0; j < cols; ++j) {
				if (board[i][j] == null) {
					throw new Exception("cell is null");
				}
				board[i][j].updateVisibilityAndSurroundingMines(minesweeperGame.getCell(i, j));
			}
		}
	}

	public static void convertToExistingBoardAndKeepLogicalStuff(MinesweeperGame minesweeperGame, VisibleTile[][] board) throws Exception {
		final int rows = minesweeperGame.getRows();
		final int cols = minesweeperGame.getCols();
		if (board.length != rows) {
			throw new Exception("minesweeper game rows doesn't match board rows");
		}
		for (int i = 0; i < rows; ++i) {
			if (board[i].length != cols) {
				throw new Exception("minesweeper game cols doesn't match board cols");
			}
			for (int j = 0; j < cols; ++j) {
				if (board[i][j] == null) {
					throw new Exception("cell is null");
				}
				board[i][j].updateVisibilitySurroundingMinesAndLogicalStuff(minesweeperGame.getCell(i, j));
			}
		}
	}
}
