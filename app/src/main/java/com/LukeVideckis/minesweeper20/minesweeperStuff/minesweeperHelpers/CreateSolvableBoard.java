package com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper20.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MyBacktrackingSolver;

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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	//TODO: move to helper file
	public static boolean isLogicalFree(VisibleTile[][] board) {
		for (VisibleTile[] row : board) {
			for (VisibleTile cell : row) {
				if (cell.getIsLogicalFree()) {
					return true;
				}
			}
		}
		return false;
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
		 * 			//we resort to moving positions of mines. This will hopefully create new
		 * 			//deducible free squares.
		 *
		 * 			at most 5 times in a row do:
		 * 				randomly move the positions of non-deducible "interesting" mines;
		 * 				continue;
		 *
		 * 			//if we've reached here, we've tried the previous step 5 times without
		 * 			//successfully creating a new deducible free square. So we resort to a more
		 * 			//extreme step:	randomly move the positions of non-deducible "interesting" mines
		 * 			//with 1 difference: change 1 mine to a non-"interesting" square (a square not
		 * 			//next to any clue).
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
		 * loop. So we can restart completely on a fresh random board. I found this happens very
		 * roughly about 1/2 of the time.
		 */

		while (totalIterationsSoFar < maxIterationsToFindBoard) {

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			if (hasAn8) {
				minesweeperGame.setHavingAn8();
			}
			minesweeperGame.clickCell(firstClickI, firstClickJ, false);

			int numberOfTriesShufflingInterestingMines = 0;
			while (totalIterationsSoFar < maxIterationsToFindBoard && !minesweeperGame.getIsGameWon()) {
				if (minesweeperGame.getIsGameLost()) {
					throw new Exception("game is lost, but board generator should never lose");
				}

				/*try to deduce free squares with gauss solver first. Gaussian Elimination has the
				 * possibility of not finding deducible free squares, even if they exist.
				 */
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				long startTime = System.currentTimeMillis();
				gaussSolver.solvePosition(board, mines);
				totalTimeGauss += System.currentTimeMillis() - startTime;

				/* if there are any deducible free squares, click them, and continue on
				 */
				if (isLogicalFree(board)) {
					//System.out.println("gauss solver found free cell(s)");
					numberOfTriesShufflingInterestingMines = 0;
					for (int i = 0; i < rows; ++i) {
						for (int j = 0; j < cols; ++j) {
							if (board[i][j].getIsLogicalFree()) {
								minesweeperGame.clickCell(i, j, false);
							}
						}
					}
					continue;
				}

				/* then try to deduce free squares with backtracking solver (if there are any free
				 * squares, this will find them)
				 */
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				try {
					startTime = System.currentTimeMillis();
					myBacktrackingSolver.solvePosition(board, mines);
					totalTimeBacktracking += System.currentTimeMillis() - startTime;
				} catch (HitIterationLimitException ignored) {
					//TODO: think about starting over, instead of just quitting
					break;
				}
				totalIterationsSoFar += myBacktrackingSolver.getNumberOfIterations();

				/* if there are any deducible free squares, click them, and continue on
				 */
				if (isLogicalFree(board)) {
					numberOfTriesShufflingInterestingMines = 0;
					for (int i = 0; i < rows; ++i) {
						for (int j = 0; j < cols; ++j) {
							if (board[i][j].getIsLogicalFree()) {
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

				if (numberOfTriesShufflingInterestingMines < 5) {
					++numberOfTriesShufflingInterestingMines;
					minesweeperGame.shuffleInterestingMines(board);
					continue;
				}

				/* Shuffling interesting mines failed 5 times in a row, we need to do something more
				 * extreme. Now we'll try removing 1 interesting mine, and making it an away mine
				 * (not next to any clue).
				 */

				numberOfTriesShufflingInterestingMines = 0;
				try {
					minesweeperGame.shuffleInterestingMinesAndMakeOneAway(board);
				} catch (NoInterestingMinesException | NoAwayCellsToMoveAMineToException ignored) {
					break;
				}

			}
			if (minesweeperGame.getIsGameWon()) {
				System.out.println("here, gauss, backtracking total time: " + totalTimeGauss + " " + totalTimeBacktracking);
				return new MinesweeperGame(minesweeperGame, firstClickI, firstClickJ);
			}
		}
		throw new HitIterationLimitException("took too many iterations to find a solvable board");
	}
}
