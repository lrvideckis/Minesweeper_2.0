package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.CheckForLocalStuff;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MyBacktrackingSolver;

import java.util.ArrayList;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

//TODO: investigate probability of board generation failing
public class CreateSolvableBoard {
	//TODO: change this back to something smaller
	//TODO: revert this back to single combined backtracking solver (no holy grail solver) as ***best (fastest) alg only uses gauss***
	private final GaussianEliminationSolver gaussSolver;
	private final VisibleTile[][] board;
	private final int rows;
	private final int cols;
	private final int mines;
	private MyBacktrackingSolver myBacktrackingSolver;

	public CreateSolvableBoard(int rows, int cols, int mines) {
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		//TODO: find a way to change iteration limit to something smaller
		//MyBacktrackingSolver.iterationLimit = 500;
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

	@SuppressWarnings("unused")
	public static void printBoardDebug(VisibleTile[][] board) {
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

	@SuppressWarnings("unused")
	public static void printBoardDebugMines(MinesweeperGame game) {
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

	private boolean clickedLogicalFrees(MinesweeperGame game) throws Exception {
		boolean clickedFree = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible()) {
					if (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree()) {
						throw new Exception("visible tiles can't be logical");
					}
				}
				if (board[i][j].getIsLogicalFree() && board[i][j].getIsLogicalMine()) {
					throw new Exception("can't be both logical free and logical mine");
				}
				if (board[i][j].getIsLogicalMine() && !game.getCell(i, j).isMine()) {
					throw new Exception("found a logical mine which is free");
				}
				if (board[i][j].getIsLogicalFree()) {
					if (game.getCell(i, j).isMine()) {
						throw new Exception("found a logical free which is mine");
					}
					game.clickCell(i, j, false);
					clickedFree = true;
				}
			}
		}
		return clickedFree;
	}

	//TODO: make this as fast as: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/mines.html
	//TODO: now just guarentee that this will always create a new board (instead of sometimes failing
	public MinesweeperGame getSolvableBoard(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}

		ArrayList<MinesweeperGame> states = new ArrayList<>();

		MinesweeperGame game;
		game = new MinesweeperGame(rows, cols, mines);
		if (hasAn8) {
			game.setHavingAn8();
		}
		game.clickCell(firstClickI, firstClickJ, false);

		states.add(game);

		/* Main board generation loop.
		 * I'm calling an "interesting" mine a mine which is next to at least 1 clue
		 *
		 * In this loop, we try to create a solvable board by this
		 * algorithm:
		 *
		 * 		while(board isn't won yet) {
		 *	 		while (there exists a deducible non-mine square) {
		 * 				click all those deducible non-mine squares;
		 * 				continue;
		 *  		}
		 *
		 * 			//now there aren't any deducible mine-free squares and the game isn't won, so
		 * 			//we resort to moving positions of mines. Also, we'll change 1 mine to a
		 * 			//non-"interesting" square (a square not next to any clue).
		 *
		 * 			//Doing this has pros:
		 * 			//		- the entire board generation algorithm runs faster (fast enough to
		 * 			//		  execute in real time for the user).
		 * 			//		- this step will always eventually produce a deducible free square as
		 * 			//		  eventually one border clue will become 0, leading to more clues.
		 *
		 * 			//And cons:
		 * 			//		- many mines will be eventually moved to the outside of the board
		 * 			//		  effectively making the board smaller
		 * 			//		- the mine density of the inside of the board will be smaller, which
		 * 			//		  generally creates easier boards
		 *
		 * 			randomly move the positions of non-deducible "interesting" mines, and move 1
		 * 			"interesting" mine to a square not next to any mines;
		 *  	}
		 *
		 *
		 * The above algorithm can fail to generate a solvable board when there's a 50/50 at the
		 * very end (there may be other cases when the alg. fails). This is why there is an outer
		 * loop. So we can restart completely on a fresh random board.
		 */
		while (!game.getIsGameWon()) {
			if (game.getIsGameLost()) {
				throw new Exception("game is lost, but board generator should never lose");
			}

			states.add(game);

			ConvertGameBoardFormat.convertToExistingBoard(game, board, true);

			/*try to deduce free squares with local rules. There is the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			if (CheckForLocalStuff.checkAndUpdateBoardForTrivialStuff(board) && clickedLogicalFrees(game)) {
				continue;
			}

			/*try to deduce free squares with gauss solver. Gaussian Elimination has the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			boolean clickedLogicalFrees = false;
			while (gaussSolver.runGaussSolverOnce(board, mines)) {
				game.updateLogicalStuff(board);
				if (clickedLogicalFrees(game)) {
					clickedLogicalFrees = true;
					break;
				}
			}
			if (clickedLogicalFrees) {
				continue;
			}

			/* if there are any mines which aren't deducible, we'll move them to un-"interesting"
			 * locations to remove the forced - guess
			 */
			try {
				if (game.removeGuessMines()) {
					continue;
				}
			} catch (Exception ignored) {
				break;
			}

			try {
				game.shuffleInterestingMinesAndMakeOneAway();
			} catch (NoAwayCellsToMoveAMineToException | NoInterestingMinesException e) {
				break;
			}
		}

		if (game.getIsGameWon()) {
			System.out.println("found board!!!!!");
			return new MinesweeperGame(game, firstClickI, firstClickJ);
		}

		throw new Exception("no solution found");
	}


	//TODO: remove after testing
	public MinesweeperGame getSolvableBoardOld(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}
		long totalTimeGauss = 0, totalTimeBacktracking = 0;
		int totalIterationsSoFar = 0;
		final int maxIterationsToFindBoard = 20000;
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
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, false);
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
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, false);
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
		throw new Exception("took too many tries (>= 1000) to find a solvable board");
	}
}
