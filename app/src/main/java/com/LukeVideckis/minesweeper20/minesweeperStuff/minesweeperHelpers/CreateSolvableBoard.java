package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MyBacktrackingSolver;

import java.util.ArrayList;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class CreateSolvableBoard {
	//TODO: change this back to something smaller
	private static final int maxIterationsToFindBoard = 500000000;
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

	private static void printBoardDebug(VisibleTile[][] board) {
		System.out.println("visible board is:");
		for (VisibleTile[] visibleTiles : board) {
			for (VisibleTile visibleTile : visibleTiles) {
				if (visibleTile.getIsVisible()) {
					if (visibleTile.getNumberSurroundingMines() == 0) {
						System.out.print('.');
					} else {
						System.out.print(visibleTile.getNumberSurroundingMines());
					}
				} else if (visibleTile.getIsLogicalFree()) {
					System.out.print('F');
				} else if (visibleTile.getIsLogicalMine()) {
					System.out.print('B');
				} else {
					System.out.print('U');
				}
			}
			System.out.println();
		}
		System.out.println();
	}

	private static void printBoardDebugMines(MinesweeperGame game) {
		System.out.println("\nmines: " + game.getNumberOfMines());
		System.out.println("board and mines are:");
		for (int i = 0; i < game.getRows(); ++i) {
			for (int j = 0; j < game.getCols(); ++j) {
				if (game.getCell(i, j).getIsVisible()) {
					if (game.getCell(i, j).getNumberSurroundingMines() == 0) {
						System.out.print('.');
					} else {
						System.out.print(game.getCell(i, j).getNumberSurroundingMines());
					}
				} else if (game.getCell(i, j).isMine()) {
					System.out.print("*");
				} else {
					System.out.print("U");
				}
			}
			System.out.println();
		}

		System.out.println();
	}

	//TODO: make this as fast as: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/mines.html
	//TODO: use previously found logical stuff (this can also be used to improve the gauss solver)
	public MinesweeperGame getSolvableBoard(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}
		long totalTimeGauss = 0, totalTimeBacktracking = 0;
		int totalIterationsSoFar = 0;
		while (totalIterationsSoFar < maxIterationsToFindBoard) {
			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			if (hasAn8) {
				minesweeperGame.setHavingAn8();
			}
			System.out.println("here, starting new game");
			minesweeperGame.clickCell(firstClickI, firstClickJ, false);

			ArrayList<MinesweeperGame> gameStack = new ArrayList<>();

			while (totalIterationsSoFar < maxIterationsToFindBoard && !minesweeperGame.getIsGameLost() && !minesweeperGame.getIsGameWon()) {
				//try to solve with gauss solver first
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				long startTime = System.currentTimeMillis();
				gaussSolver.solvePosition(board, mines);
				totalTimeGauss += System.currentTimeMillis() - startTime;


				if (isLogicalFree(board)) {
					gameStack.add(new MinesweeperGame(minesweeperGame));
					for (int i = 0; i < rows; ++i) {
						for (int j = 0; j < cols; ++j) {
							if (board[i][j].getIsLogicalFree()) {
								minesweeperGame.clickCell(i, j, false);
							}
						}
					}
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

				if (isLogicalFree(board)) {
					gameStack.add(new MinesweeperGame(minesweeperGame));
					for (int i = 0; i < rows; ++i) {
						for (int j = 0; j < cols; ++j) {
							if (board[i][j].getIsLogicalFree()) {
								minesweeperGame.clickCell(i, j, false);
							}
						}
					}
					continue;
				}


				if (minesweeperGame.getIsGameWon()) {
					System.out.println("here, FOUND!!!!!!!!!!!!!!");
					return null;
				}

				if (gameStack.size() >= 5) {


					System.out.println("here, stack size: " + gameStack.size());
					System.out.println("total iterations: " + totalIterationsSoFar);
					int cnt = 0;
					for (int i = 0; i < rows; ++i) {
						for (int j = 0; j < cols; ++j) {
							if (AwayCell.isAwayCell(minesweeperGame, i, j)) ++cnt;
						}
					}
					System.out.println("number of away cells: " + cnt);


					gameStack.remove(gameStack.size() - 1);
					gameStack.remove(gameStack.size() - 1);
					gameStack.remove(gameStack.size() - 1);
					gameStack.remove(gameStack.size() - 1);

					MinesweeperGame pointer = gameStack.get(gameStack.size() - 1);
					pointer.shuffleAwayMines();
					minesweeperGame = new MinesweeperGame(pointer);


					ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
					MyBacktrackingSolver asdf = new MyBacktrackingSolver(rows, cols);
					asdf.solvePosition(board, mines);
					printBoardDebug(board);

				} else {
					break;
				}
			}
			System.out.println("here, gauss, backtracking total time: " + totalTimeGauss + " " + totalTimeBacktracking);
			if (minesweeperGame.getIsGameWon()) {
				System.out.println("here, FOUND!!!!!!!!!!!!!!");
				return null;
				//return saveGame;
			}
		}
		throw new HitIterationLimitException("took too many tries (>= 1000) to find a solvable board");
	}

	private boolean isLogicalFree(VisibleTile[][] board) {
		for (VisibleTile[] row : board) {
			for (VisibleTile cell : row) {
				if (cell.getIsLogicalFree()) {
					return true;
				}
			}
		}
		return false;
	}
}
