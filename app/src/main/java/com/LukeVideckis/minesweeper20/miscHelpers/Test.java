package com.LukeVideckis.minesweeper20.miscHelpers;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.customExceptions.NoSolutionFoundException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolverWithBigint;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.SlowBacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.MyMath;

import java.math.BigInteger;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class Test {
	@SuppressWarnings("SpellCheckingInspection")

	private final static String[][] previousFailedBoards = {

			//gauss solver says a cell is both a logical mine, and logical free
			{
					"UUUUUUUUUUUU",
					"U112UUUUUUUU",
					"U1.23U4U6UUU",
					"U2.2U4445UUU",
					"U314U5UUU5UU",
					"U3U4U6UU43UU",
					"2U4U4UUU22UU",
					"U5U45UUU21UU",
					"U5U3UU4U223U",
					"U5232222U23U",
					"U5U21122UUUU",
					"UUU43UUUUUUU",
					"UUUUUUUUUUUU",

					"61"
			},

			//cell (0,3) is a logical - free, but fast solver doesn't set it
			//basically, testing if away cells are set as logical free/mine
			{
					"U3UU",
					"UUUU",
					"U421",
					"U2..",

					"5"
			},

			//failed test after basically redoing BacktrackingSolver to be more efficient with BigIntegr
			{
					"U1......",
					"U1111...",
					"UUUU1...",
					"U1111...",
					"U1......",

					"3"
			},

			{
					"UUU",
					"UUU",
					"UUU",
					"12U",
					".11",
					"...",
					"122",
					"UUU",

					"9"
			},

			{
					".1U",
					"23U",
					"UUU",

					"3",
			},

			//bug with calling BinomialCoefficient with invalid parameters
			{
					"UUUU",
					"U2UU",
					"U3UU",
					"UUUU",
					"UUUU",
					"U211",
					"11..",
					"....",

					"6"
			},
			{
					"UUUU",
					"U2UU",
					"U3UU",
					"UUUU",
					"UUUU",
					"U211",
					"11..",
					"....",

					"11"
			},

			//bug with dfs connect components - upper component and lower component should be the same, but DFS splits them into separate components
			{
					"UUU",
					"UU2",
					"UUU",
					"U21",
					"22.",
					"U21",
					"UUU",

					"6"
			},

			//bug with away cells - (incorrectly) returns true when cell is visible, and all adjacent cells are not visible
			{
					"...",
					"232",
					"UUU",
					"4UU",
					"UUU",
					"U3U",
					"UUU",

					"6"
			}
	};

	public static void testPreviouslyFailedBoards() {
		int testID = 1;
		for (String[] stringBoard : previousFailedBoards) {
			System.out.println("test number: " + (testID++));
			final int rows = stringBoard.length - 1;
			final int cols = stringBoard[0].length();
			final int mines = Integer.parseInt(stringBoard[stringBoard.length - 1]);
			VisibleTile[][] boardFast;
			VisibleTile[][] boardSlow;
			try {
				boardFast = convertFormat(stringBoard);
				boardSlow = convertFormat(stringBoard);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			Pair<Integer, Integer> dimensions;
			try {
				dimensions = ArrayBounds.getArrayBounds(boardFast);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			if (rows != dimensions.first || cols != dimensions.second) {
				System.out.print("bounds don't match");
				continue;
			}

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			backtrackingSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			try {
				try {
					backtrackingSolver.solvePosition(boardFast, mines);
				} catch (NoSolutionFoundException ignored) {
					continue;
				}
				try {
					slowBacktrackingSolver.solvePosition(boardSlow, mines);
				} catch (NoSolutionFoundException ignored) {
					System.out.println("SLOW solver didn't find a solution, void test");
					continue;
				} catch (HitIterationLimitException ignored) {
					System.out.println("SLOW solver hit iteration limit, void test");
					continue;
				}
				if (areBoardsDifferent(boardFast, boardSlow, mines)) {
					return;
				}
			} catch (Exception e) {
				System.out.println("one of the solvers threw exception, failed test");
				e.printStackTrace();
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static VisibleTile[][] convertFormat(String[] stringBoard) throws Exception {
		VisibleTile[][] board = new VisibleTile[stringBoard.length - 1][stringBoard[0].length()];
		for (int i = 0; i + 1 < stringBoard.length; ++i) {
			for (int j = 0; j < stringBoard[i].length(); ++j) {
				if (stringBoard[i].length() != stringBoard[0].length()) {
					throw new Exception("jagged array - not all rows are the same length");
				}
				board[i][j] = new VisibleTile();
				if (stringBoard[i].charAt(j) == '.') {
					board[i][j].updateVisibilityAndSurroundingMines(true, 0);
				} else if (stringBoard[i].charAt(j) == 'U') {
					board[i][j].updateVisibilityAndSurroundingMines(false, 0);
				} else {
					board[i][j].updateVisibilityAndSurroundingMines(true, stringBoard[i].charAt(j) - '0');
				}
			}
		}
		return board;
	}

	public static void performTestsWithBigIntSolverForLargerGrids(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int mines = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(10, 30);
				cols = MyMath.getRand(10, 30);
				mines = MyMath.getRand(2, 50);
			} catch (Exception ignored) {
			}
			mines = Math.min(mines, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			backtrackingSolver.doPerformCheckPositionValidity();
			BacktrackingSolverWithBigint backtrackingSolverWithBigint = new BacktrackingSolverWithBigint(rows, cols);

			MinesweeperGame minesweeperGame;
			try {
				minesweeperGame = new MinesweeperGame(rows, cols, mines);
				int numberOfClicks = MyMath.getRand(3, 7);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
				}
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("game over, void test");
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardFraction = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardBigInt = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFraction, minesweeperGame.getNumberOfMines());
				backtrackingSolverWithBigint.solvePosition(boardBigInt, minesweeperGame.getNumberOfMines());
				boolean testPassed = true;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (boardBigInt[i][j].getIsVisible()) {
							continue;
						}
						BigFraction curr = boardFraction[i][j].getNumberOfMineConfigs();
						curr.divideWith(boardFraction[i][j].getNumberOfTotalConfigs());

						BigInteger top = backtrackingSolverWithBigint.getNumberOfMineConfigs(i, j);
						BigInteger bottom = backtrackingSolverWithBigint.getNumberOfTotalConfigs(i, j);
						BigInteger gcd = top.gcd(bottom);
						top = top.divide(gcd);
						bottom = bottom.divide(gcd);

						//noinspection SuspiciousNameCombination
						if (!curr.getNumerator().equals(top) ||
								!curr.getDenominator().equals(bottom) ||
								boardFraction[i][j].getIsLogicalMine() != boardBigInt[i][j].getIsLogicalMine() ||
								boardFraction[i][j].getIsLogicalFree() != boardBigInt[i][j].getIsLogicalFree()
						) {
							testPassed = false;
							System.out.println("here, solver outputs don't match");
							System.out.println("i,j: " + i + " " + j);
							System.out.println("fraction solver " + curr.getNumerator() + '/' + curr.getDenominator());
							System.out.println("big int solver " + top + '/' + bottom);
						}
					}
				}
				if (!testPassed) {
					printBoardDebug(boardBigInt, mines);
					return;
				}
			} catch (HitIterationLimitException ignored) {
				System.out.println("hit iteration limit, void test");
			} catch (Exception e) {
				e.printStackTrace();
				printBoardDebug(boardBigInt, mines);
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void performTestsForMineProbability(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int mines = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(3, 8);
				cols = MyMath.getRand(3, 40 / rows);
				mines = MyMath.getRand(2, 9);
			} catch (Exception ignored) {
			}
			mines = Math.min(mines, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			backtrackingSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			try {
				minesweeperGame = new MinesweeperGame(rows, cols, mines);
				int numberOfClicks = MyMath.getRand(0, 4);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
				}
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("game over, void test");
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
				try {
					slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
				} catch (HitIterationLimitException ignored) {
					System.out.println("slow solver hit iteration limit, void test");
					continue;
				}
				if (areBoardsDifferent(boardFast, boardSlow, mines)) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				printBoardDebug(boardFast, mines);
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static boolean areBoardsDifferent(
			VisibleTile[][] boardFast,
			VisibleTile[][] boardSlow,
			int mines
	) throws Exception {
		int rows = boardFast.length, cols = boardFast[0].length;
		boolean passedTest = true;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (boardFast[i][j].getIsVisible()) {
					continue;
				}

				VisibleTile fastTile = boardFast[i][j];
				BigFraction fast = new BigFraction(fastTile.getNumberOfMineConfigs());
				fast.divideWith(fastTile.getNumberOfTotalConfigs());

				VisibleTile slowTile = boardSlow[i][j];
				BigFraction slow = new BigFraction(slowTile.getNumberOfMineConfigs());
				slow.divideWith(slowTile.getNumberOfTotalConfigs());
				if (!fast.getNumerator().equals(slow.getNumerator()) ||
						!fast.getDenominator().equals(slow.getDenominator()) ||
						fastTile.getIsLogicalFree() != slowTile.getIsLogicalFree() ||
						fastTile.getIsLogicalMine() != slowTile.getIsLogicalMine()
				) {
					passedTest = false;
					System.out.println("here, solver outputs don't match");
					System.out.println("i,j: " + i + " " + j);
					System.out.println("fast solver " + fast.getNumerator() + '/' + fast.getDenominator());
					System.out.println("slow solver " + slow.getNumerator() + '/' + slow.getDenominator());
					System.out.println("number of away cells: " + AwayCell.getNumberOfAwayCells(boardFast));
				}
			}
		}
		if (!passedTest) {
			printBoardDebug(boardFast, mines);
		}
		return !passedTest;
	}

	public static void performTestsForGaussSolver(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int mines = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(3, 15);
				cols = MyMath.getRand(3, 15);
				mines = MyMath.getRand(2, 50);
			} catch (Exception ignored) {
			}
			mines = Math.min(mines, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			backtrackingSolver.doPerformCheckPositionValidity();
			GaussianEliminationSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			try {
				minesweeperGame = new MinesweeperGame(rows, cols, mines);
				int numberOfClicks = MyMath.getRand(0, 4);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
					int x = MyMath.getRand(0, rows - 1);
					int y = MyMath.getRand(0, cols - 1);
					minesweeperGame.clickCell(x, y, false);
				}
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("game over, void test");
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardBacktracking = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardGauss = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardBacktracking, minesweeperGame.getNumberOfMines());
				gaussianEliminationSolver.solvePosition(boardGauss, minesweeperGame.getNumberOfMines());
				boolean passedTest = true;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (!boardBacktracking[i][j].getIsLogicalMine() && boardGauss[i][j].getIsLogicalMine()) {
							passedTest = false;
							System.out.println("it isn't a logical mine, but Gauss solver says it's a logical mine " + i + " " + j);
						}
						if (!boardBacktracking[i][j].getIsLogicalFree() && boardGauss[i][j].getIsLogicalFree()) {
							passedTest = false;
							System.out.println("it isn't a logical free, but Gauss solver says it's a logical free " + i + " " + j);
						}
					}
				}
				if (!passedTest) {
					printBoardDebug(boardBacktracking, mines);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static void printBoardDebug(VisibleTile[][] board, int mines) {
		System.out.println("\nmines: " + mines);
		System.out.println("board is:");
		for (VisibleTile[] visibleTiles : board) {
			for (VisibleTile visibleTile : visibleTiles) {
				if (visibleTile.getIsVisible()) {
					if (visibleTile.getNumberSurroundingMines() == 0) {
						System.out.print('.');
					} else {
						System.out.print(visibleTile.getNumberSurroundingMines());
					}
				} else {
					System.out.print('U');
				}
			}
			System.out.println();
		}
		System.out.println();
	}

	//TODO: test gauss solver also multiple times on the same board
	public static void performTestsMultipleRunsOfSameBoard(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int mines = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(3, 8);
				cols = MyMath.getRand(3, 40 / rows);
				mines = MyMath.getRand(2, 9);
			} catch (Exception ignored) {
			}
			mines = Math.min(mines, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			backtrackingSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			try {
				minesweeperGame = new MinesweeperGame(rows, cols, mines);
				int numberOfClicks = MyMath.getRand(0, 4);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
				}
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("game over, void test");
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("slow solver hit iteration limit, void test");
				continue;
			} catch (Exception e) {
				System.out.println("slow solver crashed, void test");
				e.printStackTrace();
				continue;
			}
			for (int i = 0; i < 5; ++i) {
				VisibleTile[][] boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
				try {
					backtrackingSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
					if (areBoardsDifferent(boardFast, boardSlow, mines)) {
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
					printBoardDebug(boardFast, mines);
					return;
				}
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}
}
