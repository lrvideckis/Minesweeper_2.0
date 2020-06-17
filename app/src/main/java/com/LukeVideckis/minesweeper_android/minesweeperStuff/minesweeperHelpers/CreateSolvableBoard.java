package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import com.LukeVideckis.minesweeper_android.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoInterestingMinesException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

//TODO: investigate probability of board generation failing
public class CreateSolvableBoard {
	//TODO: change this back to something smaller
	//TODO: revert this back to single combined backtracking solver (no holy grail solver) as ***best (fastest) alg only uses gauss***
	private final MinesweeperSolver gaussSolver;
	private final VisibleTile[][] board;
	private final int rows;
	private final int cols;
	private final int mines;

	public CreateSolvableBoard(int rows, int cols, int mines) {
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

			/*try to deduce free squares with gauss solver. Gaussian Elimination has the
			 * possibility of not finding deducible free squares, even if they exist.
			 */
			ConvertGameBoardFormat.convertToExistingBoard(game, board, true);
			gaussSolver.solvePosition(board, mines);
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

			/* if there are any mines which aren't deducible, we'll move them to un-"interesting"
			 * locations to remove the forced - guess
			 */
			if (game.removeGuessMines()) {
				continue;
			}

			try {
				game.shuffleInterestingMinesAndMakeOneAway();
			} catch (NoAwayCellsToMoveAMineToException | NoInterestingMinesException e) {
				break;
			}
		}

		if (game.getIsGameWon()) {
			return new MinesweeperGame(game, firstClickI, firstClickJ);
		}

		throw new Exception("no solution found");
	}
}
