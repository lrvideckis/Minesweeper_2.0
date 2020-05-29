package com.example.minesweeper20.helpers;

import android.util.Pair;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.NoSolutionFoundException;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolverWithBigint;
import com.example.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.SlowBacktrackingSolver;

import java.math.BigInteger;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class Test {
	private final static double EPSILON = 0.000000001;

	@SuppressWarnings("SpellCheckingInspection")
	private final static String[][] previousFailedBoards = {
			//bug with calling BinomialCoefficient with invalid parameters
			{
					"UUUU",
					"U2UU",
					"U3UU",
					"UUUU",
					"UUUU",
					"U211",
					"11..",
					"...."
			},
			//bug with dfs connect components - upper component and lower component should be the same, but DFS splits them into separate components
			{
					"UUU",
					"UU2",
					"UUU",
					"U21",
					"22.",
					"U21",
					"UUU"
			},
			//bug with away cells - (incorrectly) returns true when cell is visible, and all adjacent cells are not visible
			{
					"...",
					"232",
					"UUU",
					"4UU",
					"UUU",
					"U3U",
					"UUU"
			}
	};

	public static void testPreviouslyFailedBoards() {
		for (String[] stringBoard : previousFailedBoards) {
			final int rows = stringBoard.length;
			final int cols = stringBoard[0].length();
			for (int bombs = 0; bombs <= rows * cols; ++bombs) {
				VisibleTile[][] boardFast;
				VisibleTile[][] boardSlow;
				try {
					boardFast = convertFormat(stringBoard);
					boardSlow = convertFormat(stringBoard);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				if (bombs == 0) {
					printBoardDebug(boardFast);
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
				SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);


				System.out.println("number of bombs: " + bombs);
				try {
					try {
						backtrackingSolver.solvePosition(boardFast, bombs);
					} catch (NoSolutionFoundException ignored) {
						continue;
					}
					try {
						slowBacktrackingSolver.solvePosition(boardSlow, bombs);
					} catch (NoSolutionFoundException ignored) {
						System.out.println("SLOW solver didn't find a solution, void test");
						continue;
					} catch (HitIterationLimitException ignored) {
						System.out.println("SLOW solver hit iteration limit, void test");
						continue;
					}
					if (areBoardsDifferent(boardFast, boardSlow)) {
						return;
					}
				} catch (Exception e) {
					System.out.println("one of the solvers threw exception, failed test");
					e.printStackTrace();
					return;
				}
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static VisibleTile[][] convertFormat(String[] stringBoard) throws Exception {
		VisibleTile[][] board = new VisibleTile[stringBoard.length][stringBoard[0].length()];
		for (int i = 0; i < stringBoard.length; ++i) {
			for (int j = 0; j < stringBoard[i].length(); ++j) {
				if (stringBoard[i].length() != stringBoard[0].length()) {
					throw new Exception("jagged array - not all rows are the same length");
				}
				board[i][j] = new VisibleTile();
				if (stringBoard[i].charAt(j) == '.') {
					board[i][j].updateVisibilityAndSurroundingBombs(true, 0);
				} else if (stringBoard[i].charAt(j) == 'U') {
					board[i][j].updateVisibilityAndSurroundingBombs(false, 0);
				} else {
					board[i][j].updateVisibilityAndSurroundingBombs(true, stringBoard[i].charAt(j) - '0');
				}
			}
		}
		return board;
	}

	public static void performTestsForFractionOverflow(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int bombs = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(10, 15);
				cols = MyMath.getRand(10, 15);
				bombs = MyMath.getRand(2, 50);
			} catch (Exception ignored) {
			}
			bombs = Math.min(bombs, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			BacktrackingSolverWithBigint backtrackingSolverWithBigint = new BacktrackingSolverWithBigint(rows, cols);

			MinesweeperGame minesweeperGame = new MinesweeperGame(rows, cols, bombs);
			try {
				int numberOfClicks = MyMath.getRand(3, 7);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
				}
				if (minesweeperGame.getIsGameOver()) {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardFraction = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardBigInt = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFraction, minesweeperGame.getNumberOfBombs());
				backtrackingSolverWithBigint.solvePosition(boardBigInt, minesweeperGame.getNumberOfBombs());
				boolean testPassed = true;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (boardBigInt[i][j].getIsVisible()) {
							continue;
						}
						FractionThenDouble curr = boardFraction[i][j].getNumberOfBombConfigs();
						curr.divideWith(boardFraction[i][j].getNumberOfTotalConfigs());

						BigInteger top = backtrackingSolverWithBigint.getNumberOfBombConfigs(i, j);
						BigInteger bottom = backtrackingSolverWithBigint.getNumberOfTotalConfigs(i, j);
						BigInteger gcd = top.gcd(bottom);
						top = top.divide(gcd);
						bottom = bottom.divide(gcd);

						if (curr.getHasOverflowed()) {
							if (Math.abs(top.doubleValue() / bottom.doubleValue() - curr.getValue()) > EPSILON) {
								testPassed = false;
								System.out.println("here, solver outputs don't match");
								System.out.println("i,j: " + i + " " + j);
								System.out.println("fraction solver " + curr.getValue());
								System.out.println("big int solver " + top.toString() + '/' + bottom.toString());
							}
						} else {
							//noinspection SuspiciousNameCombination
							if (
									!BigInteger.valueOf(curr.getNumerator()).equals(top) ||
											!BigInteger.valueOf(curr.getDenominator()).equals(bottom)
							) {
								testPassed = false;
								System.out.println("here, solver outputs don't match");
								System.out.println("i,j: " + i + " " + j);
								System.out.println("fraction solver " + curr.getNumerator() + '/' + curr.getDenominator());
								System.out.println("big int solver " + top + '/' + bottom);
							}
						}
					}
				}
				if (!testPassed) {
					printBoardDebug(boardBigInt);
					return;
				}
			} catch (HitIterationLimitException ignored) {
				System.out.println("hit iteration limit, void test");
			} catch (Exception e) {
				e.printStackTrace();
				printBoardDebug(boardBigInt);
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void performTestsForBombProbability(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int bombs = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(3, 8);
				cols = MyMath.getRand(3, 40 / rows);
				bombs = MyMath.getRand(2, 9);
			} catch (Exception ignored) {
			}
			bombs = Math.min(bombs, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame = new MinesweeperGame(rows, cols, bombs);
			try {
				int numberOfClicks = MyMath.getRand(0, 4);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
				}
				if (minesweeperGame.getIsGameOver()) {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFast, minesweeperGame.getNumberOfBombs());
				try {
					slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfBombs());
				} catch (HitIterationLimitException ignored) {
					System.out.println("slow solver hit iteration limit, void test");
					continue;
				}
				if (areBoardsDifferent(boardFast, boardSlow)) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				printBoardDebug(boardFast);
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static boolean areBoardsDifferent(
			VisibleTile[][] boardFast,
			VisibleTile[][] boardSlow
	) throws Exception {
		int rows = boardFast.length, cols = boardFast[0].length;
		boolean passedTest = true;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (boardFast[i][j].getIsVisible()) {
					continue;
				}

				VisibleTile fastTile = boardFast[i][j];
				FractionThenDouble fast = new FractionThenDouble(fastTile.getNumberOfBombConfigs());
				fast.divideWith(fastTile.getNumberOfTotalConfigs());

				VisibleTile slowTile = boardSlow[i][j];
				FractionThenDouble slow = new FractionThenDouble(slowTile.getNumberOfBombConfigs());
				slow.divideWith(slowTile.getNumberOfTotalConfigs());
				if (fast.getHasOverflowed()) {
					double slowVal;
					if (slow.getHasOverflowed()) {
						slowVal = slow.getValue();
					} else {
						slowVal = slow.getNumerator() / (double) slow.getDenominator();
					}
					if (Math.abs(fast.getValue() - slowVal) > EPSILON) {
						passedTest = false;
						System.out.println("here, solver outputs don't match");
						System.out.println("i,j: " + i + " " + j);
						System.out.println("fast solver " + fast.getValue());
						if (slow.getHasOverflowed()) {
							System.out.println("slow solver " + slow.getValue());
						} else {
							System.out.println("slow solver " + slow.getNumerator() + '/' + slow.getDenominator());
						}
					}
				} else {
					if (fast.getNumerator() != slow.getNumerator() || fast.getDenominator() != slow.getDenominator()) {
						passedTest = false;
						System.out.println("here, solver outputs don't match");
						System.out.println("i,j: " + i + " " + j);
						System.out.println("fast solver " + fast.getNumerator() + '/' + fast.getDenominator());
						System.out.println("slow solver " + slow.getNumerator() + '/' + slow.getDenominator());
						System.out.println("number of away cells: " + AwayCell.getNumberOfAwayCells(boardFast));
					}
				}
			}
		}
		if (!passedTest) {
			printBoardDebug(boardFast);
		}
		return !passedTest;
	}

	public static void performTestsForGaussSolver(int numberOfTests) {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int bombs = 5;
			int rows = 5, cols = 5;
			try {
				rows = MyMath.getRand(3, 15);
				cols = MyMath.getRand(3, 15);
				bombs = MyMath.getRand(2, 50);
			} catch (Exception ignored) {
			}
			bombs = Math.min(bombs, rows * cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			GaussianEliminationSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

			MinesweeperGame minesweeperGame = new MinesweeperGame(rows, cols, bombs);
			try {
				int numberOfClicks = MyMath.getRand(0, 4);
				while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					int x = MyMath.getRand(0, rows - 1);
					int y = MyMath.getRand(0, cols - 1);
					minesweeperGame.clickCell(x, y, false);
				}
				if (minesweeperGame.getIsGameOver()) {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			VisibleTile[][] boardBacktracking = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardGauss = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardBacktracking, minesweeperGame.getNumberOfBombs());
				gaussianEliminationSolver.solvePosition(boardGauss, minesweeperGame.getNumberOfBombs());
				boolean passedTest = true;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (!boardBacktracking[i][j].getIsLogicalBomb() && boardGauss[i][j].getIsLogicalBomb()) {
							passedTest = false;
							System.out.println("it isn't a logical bomb, but Gauss solver says it's a logical bomb " + i + " " + j);
						}
						if (!boardBacktracking[i][j].getIsLogicalFree() && boardGauss[i][j].getIsLogicalFree()) {
							passedTest = false;
							System.out.println("it isn't a logical free, but Gauss solver says it's a logical free " + i + " " + j);
						}
					}
				}
				if (!passedTest) {
					printBoardDebug(boardBacktracking);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static void printBoardDebug(VisibleTile[][] board) {
		System.out.println("\nboard is:");
		for (VisibleTile[] visibleTiles : board) {
			for (VisibleTile visibleTile : visibleTiles) {
				if (visibleTile.getIsVisible()) {
					if (visibleTile.getNumberSurroundingBombs() == 0) {
						System.out.print('.');
					} else {
						System.out.print(visibleTile.getNumberSurroundingBombs());
					}
				} else {
					System.out.print('U');
				}
			}
			System.out.println();
		}
		System.out.println();
	}
}
