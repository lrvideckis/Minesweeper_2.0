package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper20.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MyBacktrackingSolver;

import java.util.ArrayList;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

//TODO: investigate probability of board generation failing
public class CreateSolvableBoard {
	//TODO: change this back to something smaller
	//TODO: revert this back to single combined backtracking solver (no holy grail solver) as ***best (fastest) alg only uses gauss***
	private final BacktrackingSolver myBacktrackingSolver;
	private final MinesweeperSolver gaussSolver;
	private final VisibleTile[][] board;
	private final int rows;
	private final int cols;
	private final int mines;

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

	//TODO: make this as fast as: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/mines.html
	//TODO: use previously found logical stuff (this can also be used to improve the gauss solver)
	//TODO: look at average number of iterations
	public MinesweeperGame getSolvableBoard(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}
		long totalTimeGauss = 0, totalTimeBacktracking = 0;
		int totalIterationsSoFar = 0;

		MinesweeperGame minesweeperGame;
		minesweeperGame = new MinesweeperGame(rows, cols, mines);
		if (hasAn8) {
			minesweeperGame.setHavingAn8();
		}
		minesweeperGame.clickCell(firstClickI, firstClickJ, false);

		int numberOfTriesShufflingInterestingMines = 0;
		while (!minesweeperGame.getIsGameWon()) {
			if (minesweeperGame.getIsGameLost()) {
				throw new Exception("game is lost, but board generator should never lose");
			}

			System.out.println("trying gauss solver");
			/*try to deduce free squares with gauss solver first. Gaussian Elimination has the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, true);
			long startTime = System.currentTimeMillis();
			gaussSolver.solvePosition(board, mines);
			minesweeperGame.updateLogicalStuff(board);

			totalTimeGauss += System.currentTimeMillis() - startTime;

			/* if there are any deducible free squares, click them, and continue on
			 */
			if (ExistsLogicalFree.isLogicalFree(board)) {
				System.out.println("gauss solver found logical frees, clicking them");
				numberOfTriesShufflingInterestingMines = 0;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalMine() && !minesweeperGame.getCell(i, j).isMine()) {
							throw new Exception("found a logical mine which is free");
						}
						if (board[i][j].getIsLogicalFree()) {
							if (minesweeperGame.getCell(i, j).isMine()) {
								throw new Exception("found a logical free which is mine");
							}
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				continue;
			}

			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, true);
			try {
				startTime = System.currentTimeMillis();
				myBacktrackingSolver.solvePosition(board, mines);
				totalTimeBacktracking += System.currentTimeMillis() - startTime;

				minesweeperGame.updateLogicalStuff(board);
			} catch (HitIterationLimitException ignored) {
				totalIterationsSoFar += MyBacktrackingSolver.iterationLimit;
			}


			/* if there are any deducible free squares, click them, and continue on
			 */
			if (ExistsLogicalFree.isLogicalFree(board)) {
				numberOfTriesShufflingInterestingMines = 0;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalMine() && !minesweeperGame.getCell(i, j).isMine()) {
							throw new Exception("found a logical mine which is free");
						}
						if (board[i][j].getIsLogicalFree()) {
							if (minesweeperGame.getCell(i, j).isMine()) {
								throw new Exception("clicking on a logical free which is a mine");
							}
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				continue;
			}

			/* At this point, there are no deducible free squares to click on. Some options are:
			 *
			 * 1) start over, and generate a new random board
			 * 		After trying this approach, I found that for larger grids, and for high mine
			 * 		percentages, this will rarely find a solvable grid. It's simply too unlikely
			 * 		that a randomly generated grid will be completely solvable.
			 *
			 * 2) keep the existing grid, and rearrange the mines (randomly) in the hope of creating
			 * new deducible free squares
			 * 		The question is what mines do you rearrange?
			 * 		- Mines adjacent to a clue (or visible number) which I'll call "interesting"
			 * 		- Mines not adjacent to any clue
			 *  	- Previously deduced mines (this would be moving backwards)
			 */

			/* First try rearranging mines adjacent to visible clues
			 */

			if (numberOfTriesShufflingInterestingMines < 3) {
				System.out.println("shuffling interesting mines");
				++numberOfTriesShufflingInterestingMines;
				minesweeperGame.shuffleInterestingMines();
				continue;
			}

			/* Shuffling interesting mines failed 3 times in a row, we need to do something more
			 * extreme. Now we'll try removing 1 interesting mine, and making it an away mine
			 * (not next to any clue).
			 */

			System.out.println("shuffling interesting mines and changing one to away");
			numberOfTriesShufflingInterestingMines = 0;
			try {
				minesweeperGame.shuffleInterestingMinesAndMakeOneAway();
			} catch (NoAwayCellsToMoveAMineToException e) {
				System.out.println("no away cells");
				break;
			} catch (NoInterestingMinesException e) {
				System.out.println("no interesting mines (a mine next to a clue which isn't a logical mine)");
				break;
			}
		}
		if (minesweeperGame.getIsGameWon()) {
			System.out.println(" here, gauss, backtracking total time: " + totalTimeGauss + " " + totalTimeBacktracking);
			System.out.println("total iterations: " + totalIterationsSoFar);
			return new MinesweeperGame(minesweeperGame, firstClickI, firstClickJ);
		}
		printBoardDebug(board);
		System.out.println("asdf");
		printBoardDebugMines(minesweeperGame);

		throw new Exception("no solution found");
	}


	public MinesweeperGame getSolvableBoardAlwaysMove1MineAway(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
		if (ArrayBounds.outOfBounds(firstClickI, firstClickJ, rows, cols)) {
			throw new Exception("first click is out of bounds");
		}
		long totalTimeGauss = 0, totalTimeBacktracking = 0;
		int totalIterationsSoFar = 0;


		MinesweeperGame minesweeperGame;
		minesweeperGame = new MinesweeperGame(rows, cols, mines);
		if (hasAn8) {
			minesweeperGame.setHavingAn8();
		}
		minesweeperGame.clickCell(firstClickI, firstClickJ, false);

		while (!minesweeperGame.getIsGameWon()) {
			if (minesweeperGame.getIsGameLost()) {
				throw new Exception("game is lost, but board generator should never lose");
			}

			System.out.println("trying gauss solver");
			/*try to deduce free squares with gauss solver first. Gaussian Elimination has the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, true);
			long startTime = System.currentTimeMillis();
			gaussSolver.solvePosition(board, mines);
			minesweeperGame.updateLogicalStuff(board);

			totalTimeGauss += System.currentTimeMillis() - startTime;

			/* if there are any deducible free squares, click them, and continue on
			 */
			if (ExistsLogicalFree.isLogicalFree(board)) {
				System.out.println("gauss solver found logical frees, clicking them");
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalMine() && !minesweeperGame.getCell(i, j).isMine()) {
							throw new Exception("found a logical mine which is free");
						}
						if (board[i][j].getIsLogicalFree()) {
							if (minesweeperGame.getCell(i, j).isMine()) {
								throw new Exception("found a logical free which is mine");
							}
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				continue;
			}

			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, true);
			try {
				startTime = System.currentTimeMillis();
				myBacktrackingSolver.solvePosition(board, mines);
				totalTimeBacktracking += System.currentTimeMillis() - startTime;

				minesweeperGame.updateLogicalStuff(board);
			} catch (HitIterationLimitException ignored) {
				totalIterationsSoFar += MyBacktrackingSolver.iterationLimit;
			}


			/* if there are any deducible free squares, click them, and continue on
			 */
			if (ExistsLogicalFree.isLogicalFree(board)) {
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalMine() && !minesweeperGame.getCell(i, j).isMine()) {
							throw new Exception("found a logical mine which is free");
						}
						if (board[i][j].getIsLogicalFree()) {
							if (minesweeperGame.getCell(i, j).isMine()) {
								throw new Exception("clicking on a logical free which is a mine");
							}
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				continue;
			}

			/* At this point, there are no deducible free squares to click on. Some options are:
			 *
			 * 1) start over, and generate a new random board
			 * 		After trying this approach, I found that for larger grids, and for high mine
			 * 		percentages, this will rarely find a solvable grid. It's simply too unlikely
			 * 		that a randomly generated grid will be completely solvable.
			 *
			 * 2) keep the existing grid, and rearrange the mines (randomly) in the hope of creating
			 * new deducible free squares
			 * 		The question is what mines do you rearrange?
			 * 		- Mines adjacent to a clue (or visible number) which I'll call "interesting"
			 * 		- Mines not adjacent to any clue
			 *  	- Previously deduced mines (this would be moving backwards)
			 */

			/* First try rearranging mines adjacent to visible clues
			 */

			/* Shuffling interesting mines failed 3 times in a row, we need to do something more
			 * extreme. Now we'll try removing 1 interesting mine, and making it an away mine
			 * (not next to any clue).
			 */

			System.out.println("shuffling interesting mines and changing one to away");
			try {
				minesweeperGame.shuffleInterestingMinesAndMakeOneAway();
			} catch (NoAwayCellsToMoveAMineToException e) {
				System.out.println("no away cells");
				break;
			} catch (NoInterestingMinesException e) {
				System.out.println("no interesting mines (a mine next to a clue which isn't a logical mine)");
				break;
			}
		}
		if (minesweeperGame.getIsGameWon()) {
			System.out.println(" here, gauss, backtracking total time: " + totalTimeGauss + " " + totalTimeBacktracking);
			System.out.println("total iterations: " + totalIterationsSoFar);
			return new MinesweeperGame(minesweeperGame, firstClickI, firstClickJ);
		}
		printBoardDebug(board);
		System.out.println("asdf");
		printBoardDebugMines(minesweeperGame);

		throw new Exception("no solution found");
	}

	public MinesweeperGame getSolvableBoardAlwaysMove1MineAwayNoBacktracking(int firstClickI, int firstClickJ, boolean hasAn8) throws Exception {
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

		while (!game.getIsGameWon()) {
			if (game.getIsGameLost()) {
				throw new Exception("game is lost, but board generator should never lose");
			}

			System.out.println("trying gauss solver");
			/*try to deduce free squares with gauss solver first. Gaussian Elimination has the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			ConvertGameBoardFormat.convertToExistingBoard(game, board, true);
			gaussSolver.solvePosition(board, mines);
			//Test.printBoardDebugWithLogicalStuff(board, mines);
			game.updateLogicalStuff(board);

			states.add(game);


			/* if there are any deducible free squares, click them, and continue on
			 */
			if (ExistsLogicalFree.isLogicalFree(board)) {
				System.out.println("gauss solver found logical frees, clicking them");
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (board[i][j].getIsLogicalMine() && !game.getCell(i, j).isMine()) {
							throw new Exception("found a logical mine which is free");
						}
						if (board[i][j].getIsLogicalFree()) {
							if (game.getCell(i, j).isMine()) {
								throw new Exception("found a logical free which is mine");
							}
							game.clickCell(i, j, false);
						}
					}
				}
				continue;
			}

			System.out.println("before1");
			System.out.println("before2");
			System.out.println("before3");
			printBoardDebug(board);

			if (game.removeGuessMines()) {
				System.out.println("after1");
				System.out.println("after2");
				System.out.println("after3");
				printBoardDebug(board);
				continue;
			}


			System.out.println("shuffling interesting mines and changing one to away");
			try {
				game.shuffleInterestingMinesAndMakeOneAway();
			} catch (NoAwayCellsToMoveAMineToException e) {
				System.out.println("no away cells");
				break;
			} catch (NoInterestingMinesException e) {
				System.out.println("no interesting mines (a mine next to a clue which isn't a logical mine)");
				break;
			}
		}
		ConvertGameBoardFormat.convertToExistingBoard(game, board, true);
		printBoardDebug(board);
		System.out.println("asdf");
		printBoardDebugMines(game);

		if (game.getIsGameWon()) {
			return new MinesweeperGame(game, firstClickI, firstClickJ);
		}

		throw new Exception("no solution found");
	}
}
/*

			BB2.1B3BB1..12222323B322222BBB
			3B31213B43333BB2BBB4BB3BB4B5B4
			235B2.113BBBB422233B33B5BB45B3
			2BBB311.3B55B5211121113B43BB3B
			4B533B113B44BBB11B33224B313331
			BB22B311B4BB442123BBB3BB422B21
			2335B3.13B43B2233B3434B4BB44B2
			23BBB31.2B4322BBB321B3223B3BB4
			BB6B5B3333BB22333B234B311134BB
			UUUB43BBB3222B1.12B4BBB1..1B43
			UUU55B44B2..222..23BB532.124B2
			UUUBBB3233322B1..1B322B212B5B4
			UUUUU43B4BBB211..111123B12B5BB
			UUUUUB33BB642..111..1B43213B42
			UUUUU5B324BB2113B32344BB113B42
			UUUUUUB2.2B4B11BB3BBBB3211B3BB

			**2.1*3**1..12222323*322222***
			3*31213*43333**2***4**3**4*5*4
			235*2.113****422233*33*5**45*3
			2***311.3*55*5211121113*43**3*
			4*533*113*44***11*33224*313331
			**22*311*4**442123***3**422*21
			2335*3.13*43*2233*3434*4**44*2
			23***31.2*4322***321*3223*3**4
			**6*5*3333**22333*234*311134**
			*U**43***3222*1.12*4***1..1*43
			***55*44*2..222..23**532.124*2
			**U***3233322*1..1*322*212*5*4
			****U43*4***211..111123*12*5**
			******33**642..111..1*43213*42
			****U5*324**2113*32344**113*42
			*****U*2.2*4*11**3****3211*3**
 */
