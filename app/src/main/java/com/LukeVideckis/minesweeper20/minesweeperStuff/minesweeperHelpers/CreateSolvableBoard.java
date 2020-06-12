package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MyBacktrackingSolver;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class CreateSolvableBoard {
	private static final int maxIterationsToFindBoard = 5000;
	private final BacktrackingSolver myBacktrackingSolver;
	private final MinesweeperSolver gaussSolver;
	private final VisibleTile[][] board;
	private final int rows;
	private final int cols;
	private final int mines;

	public CreateSolvableBoard(int rows, int cols, int mines) {
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		gaussSolver = new GaussianEliminationSolver(rows, cols);
		board = new VisibleTile[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j] = new VisibleTile();
			}
		}
		this.rows = rows;
		this.cols = cols;
		this.mines = mines;
	}

	//TODO: make this as fast as: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/mines.html
	//TODO: use previously found logical stuff (this can also be used to improve the gauss solver)
	public MinesweeperGame getSolvableBoard(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}
		long totalTimeGauss = 0, totalTimeBacktracking = 0;
		int totalIterationsSoFar = 0;
		//TODO: look into making this parallel
		while (totalIterationsSoFar < maxIterationsToFindBoard) {
			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			if (hasAn8) {
				minesweeperGame.setHavingAn8();
			}
			minesweeperGame.clickCell(firstClickI, firstClickJ, false);
			MinesweeperGame saveGame = new MinesweeperGame(minesweeperGame);
			while (!minesweeperGame.getIsGameLost() && !minesweeperGame.getIsGameWon()) {
				//try to solve with gauss solver first
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				long startTime = System.currentTimeMillis();
				gaussSolver.solvePosition(board, mines);
				totalTimeGauss += System.currentTimeMillis() - startTime;
				boolean foundLogicalFree = false;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalFree()) {
							foundLogicalFree = true;
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				if (foundLogicalFree) {
					continue;
				}

				//then try to solve with backtracking solver
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				try {
					startTime = System.currentTimeMillis();
					myBacktrackingSolver.solvePosition(board, mines);
					totalTimeBacktracking += System.currentTimeMillis() - startTime;
				} catch (HitIterationLimitException ignored) {
					totalIterationsSoFar += MyBacktrackingSolver.iterationLimit;
					break;
				}
				totalIterationsSoFar += myBacktrackingSolver.getNumberOfIterations();
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalFree()) {
							foundLogicalFree = true;
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				if (!foundLogicalFree) {
					break;
				}
			}
			System.out.println("here, gauss, backtracking total time: " + totalTimeGauss + " " + totalTimeBacktracking);
			if (minesweeperGame.getIsGameWon()) {
				return saveGame;
			}
		}
		throw new HitIterationLimitException("took too many tries (>= 1000) to find a solvable board");
	}
}
