package com.example.minesweeper20.helpers;

import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class ConvertGameBoardFormat {
	public static VisibleTile[][] convertToNewBoard(MinesweeperGame minesweeperGame) {
		final int rows = minesweeperGame.getNumberOfRows();
		final int cols = minesweeperGame.getNumberOfCols();
		VisibleTile[][] board = new VisibleTile[rows][cols];
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				board[i][j] = new VisibleTile();
				board[i][j].updateVisibilityAndSurroundingBombs(minesweeperGame.getCell(i,j));
			}
		}
		return board;
	}

	public static void convertToExistingBoard(MinesweeperGame minesweeperGame, VisibleTile[][] board) throws Exception {
		final int rows = minesweeperGame.getNumberOfRows();
		final int cols = minesweeperGame.getNumberOfCols();
		if(board.length != rows) {
			throw new Exception("minesweeper game rows doesn't match board rows");
		}
		for(int i = 0; i < rows; ++i) {
			if(board[i].length != cols) {
				throw new Exception("minesweeper game cols doesn't match board cols");
			}
			for(int j = 0; j < cols; ++j) {
				if(board[i][j] == null) {
					throw new Exception("cell is null");
				}
				board[i][j].updateVisibilityAndSurroundingBombs(minesweeperGame.getCell(i,j));
			}
		}
	}
}
