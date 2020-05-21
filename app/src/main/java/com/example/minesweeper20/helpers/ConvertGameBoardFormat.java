package com.example.minesweeper20.helpers;

import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;

public class ConvertGameBoardFormat {
	public static ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> convertToNewBoard(MinesweeperGame minesweeperGame) {
		int rows = minesweeperGame.getNumberOfRows();
		int cols = minesweeperGame.getNumberOfCols();
		ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<MinesweeperSolver.VisibleTile> currRow = new ArrayList<>(cols);
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.VisibleTile currTile = new MinesweeperSolver.VisibleTile();
				updateCell(currTile, minesweeperGame.getCell(i,j));
				currRow.add(currTile);
			}
			board.add(currRow);
		}
		return board;
	}

	public static void convertToExistingBoard(MinesweeperGame minesweeperGame, ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board) throws Exception {
		int rows = minesweeperGame.getNumberOfRows();
		int cols = minesweeperGame.getNumberOfCols();
		if(board.size() != rows) {
			throw new Exception("minesweeper game rows doesn't match board rows");
		}
		for(int i = 0; i < rows; ++i) {
			if(board.get(i).size() != cols) {
				throw new Exception("minesweeper game cols doesn't match board cols");
			}
			for(int j = 0; j < cols; ++j) {
				if(board.get(i).get(j) == null) {
					throw new Exception("cell is null");
				}
				MinesweeperSolver.VisibleTile solverTile = board.get(i).get(j);
				solverTile.reset();
				updateCell(solverTile, minesweeperGame.getCell(i,j));
			}
		}
	}

	private static void updateCell(MinesweeperSolver.VisibleTile solverTile, MinesweeperGame.Tile gameTile) {
		solverTile.isVisible = gameTile.isRevealed();
		if(gameTile.isRevealed()) {
			solverTile.numberSurroundingBombs = gameTile.getNumberSurroundingBombs();
		}
	}
}
