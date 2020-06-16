package com.LukeVideckis.minesweeper20.miscHelpers;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.customExceptions.NoSolutionFoundException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.BacktrackingSolverWithBigint;
import com.LukeVideckis.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.HolyGrailSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper20.minesweeperStuff.MyBacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.SlowBacktrackingSolver;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ExistsLogicalFree;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.MyMath;

import java.math.BigInteger;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class Test {
	@SuppressWarnings("SpellCheckingInspection")

	private final static String[][] previousFailedBoards = {
			//board where gauss solver determines away cells as mines
			{
					"UUUUU",
					"235UU",
					"..3UU",
					"..2UU",

					"11"
			},

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

			//failed test after basically redoing HolyGrailSolver to be more efficient with BigIntegr
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

	public static void testPreviouslyFailedBoards() throws Exception {
		int testID = 1;
		for (String[] stringBoard : previousFailedBoards) {
			System.out.println("test number: " + (testID++));
			final int rows = stringBoard.length - 1;
			final int cols = stringBoard[0].length();
			final int mines = Integer.parseInt(stringBoard[stringBoard.length - 1]);
			VisibleTile[][] boardFast;
			VisibleTile[][] boardSlow;
			boardFast = convertFormat(stringBoard);
			boardSlow = convertFormat(stringBoard);
			Pair<Integer, Integer> dimensions;
			dimensions = ArrayBounds.getArrayBounds(boardFast);
			if (rows != dimensions.first || cols != dimensions.second) {
				System.out.print("bounds don't match");
				continue;
			}

			HolyGrailSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			holyGrailSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			try {
				holyGrailSolver.solvePosition(boardFast, mines);
			} catch (NoSolutionFoundException ignored) {
				System.out.println("no solution found, void test");
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

	public static void performTestsWithBigIntSolverForLargerGrids(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(10, 30);
			final int cols = MyMath.getRand(10, 30);
			int mines = MyMath.getRand(2, 50);
			mines = Math.min(mines, rows * cols - 9);

			HolyGrailSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			holyGrailSolver.doPerformCheckPositionValidity();
			BacktrackingSolverWithBigint backtrackingSolverWithBigint = new BacktrackingSolverWithBigint(rows, cols);

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			int numberOfClicks = MyMath.getRand(3, 7);
			while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
				minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("game over, void test");
				continue;
			}
			VisibleTile[][] boardFraction = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardBigInt = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				holyGrailSolver.solvePosition(boardFraction, minesweeperGame.getNumberOfMines());
				backtrackingSolverWithBigint.solvePosition(boardBigInt, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("hit iteration limit, void test");
				continue;
			}

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
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void performTestsForMineProbability(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(3, 8);
			final int cols = MyMath.getRand(3, 40 / rows);
			int mines = MyMath.getRand(2, 9);
			mines = Math.min(mines, rows * cols - 9);

			HolyGrailSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			holyGrailSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			int numberOfClicks = MyMath.getRand(0, 4);
			while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
				minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("game over, void test");
				continue;
			}
			VisibleTile[][] boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			holyGrailSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
			try {
				slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("slow solver hit iteration limit, void test");
				continue;
			}
			if (areBoardsDifferent(boardFast, boardSlow, mines)) {
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

	public static void performTestsForGaussSolver(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(3, 15);
			final int cols = MyMath.getRand(3, 15);
			int mines = MyMath.getRand(2, 50);
			mines = Math.min(mines, rows * cols - 9);

			MyBacktrackingSolver myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
			myBacktrackingSolver.doPerformCheckPositionValidity();
			GaussianEliminationSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

			MinesweeperGame minesweeperGame;
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
			VisibleTile[][] boardBacktracking = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardGauss = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				myBacktrackingSolver.solvePosition(boardBacktracking, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("backtracking solver hit iteration limit, void test");
				continue;
			}
			gaussianEliminationSolver.solvePosition(boardGauss, minesweeperGame.getNumberOfMines());
			if (isFailed_compareGaussBoardToBacktrackingBoard(rows, cols, mines, boardBacktracking, boardGauss)) {
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	//returns true if test failed
	private static boolean isFailed_compareGaussBoardToBacktrackingBoard(int rows, int cols, int mines, VisibleTile[][] boardBacktracking, VisibleTile[][] boardGauss) {
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
			return true;
		}
		return false;
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

	public static void printBoardDebugWithLogicalStuff(VisibleTile[][] board, int mines) {
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
				} else if (visibleTile.getIsLogicalMine()) {
					System.out.print('B');
				} else if (visibleTile.getIsLogicalFree()) {
					System.out.print('F');
				} else {
					System.out.print('U');
				}
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void performTestsMultipleRunsOfSameBoard(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(3, 8);
			final int cols = MyMath.getRand(3, 40 / rows);
			int mines = MyMath.getRand(2, 9);
			mines = Math.min(mines, rows * cols - 9);

			HolyGrailSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			holyGrailSolver.doPerformCheckPositionValidity();
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);
			GaussianEliminationSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			int numberOfClicks = MyMath.getRand(0, 4);
			while (numberOfClicks-- > 0 && !minesweeperGame.getIsGameLost()) {
				minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("game over, void test");
				continue;
			}
			VisibleTile[][] boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("slow solver hit iteration limit, void test");
				continue;
			}
			for (int i = 0; i < 3; ++i) {
				VisibleTile[][] boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
				holyGrailSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
				if (areBoardsDifferent(boardFast, boardSlow, mines)) {
					return;
				}

				VisibleTile[][] boardGauss = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
				gaussianEliminationSolver.solvePosition(boardGauss, minesweeperGame.getNumberOfMines());
				if (isFailed_compareGaussBoardToBacktrackingBoard(rows, cols, mines, boardFast, boardGauss)) {
					return;
				}
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void TestThatSolvableBoardsAreSolvable(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.print("test number: " + testID);

			final int rows = MyMath.getRand(5, 30);
			final int cols = MyMath.getRand(5, 30);
			int mines = MyMath.getRand(2, 300);

			//final int rows = MyMath.getRand(5, 15);
			//final int cols = MyMath.getRand(5, 15);
			//final int mines = MyMath.getRand(2, 75);
			mines = Math.min(mines, rows * cols - 9);
			mines = Math.min(mines, (int) (rows * cols * 0.4f));

			System.out.print(" rows, cols, mines: " + rows + " " + cols + " " + mines);
			System.out.print(" percentage: " + String.format("%.2f", mines / (float) (rows * cols)));

			HolyGrailSolver solver = new HolyGrailSolver(rows, cols);

			CreateSolvableBoard createSolvableBoard = new CreateSolvableBoard(rows, cols, mines);
			final int firstClickI = MyMath.getRand(0, rows - 1);
			final int firstClickJ = MyMath.getRand(0, cols - 1);
			MinesweeperGame game;
			long startTime = System.currentTimeMillis();
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, false);
			System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");
			VisibleTile[][] visibleBoard = new VisibleTile[rows][cols];
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					visibleBoard[i][j] = new VisibleTile();
				}
			}
			while (!game.getIsGameLost() && !game.getIsGameWon()) {
				ConvertGameBoardFormat.convertToExistingBoard(game, visibleBoard, false);
				solver.solvePosition(visibleBoard, mines);
				game.updateLogicalStuff(visibleBoard);

				if (!ExistsLogicalFree.isLogicalFree(visibleBoard)) {
					System.out.println("no logical frees, failed test");
					return;
				}

				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (visibleBoard[i][j].getIsLogicalFree()) {
							game.clickCell(i, j, false);
						}
					}
				}
			}
			if (!game.getIsGameWon()) {
				System.out.println("game is not won, failed test");
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void TestThatSolvableBoardsWith8AreSolvable(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.print("test number: " + testID);

			//final int rows = MyMath.getRand(5, 30);
			//final int cols = MyMath.getRand(5, 30);
			//int mines = MyMath.getRand(2, 300);

			final int rows = MyMath.getRand(8, 15);
			final int cols = MyMath.getRand(8, 15);
			int mines = MyMath.getRand(8, 50);
			mines = Math.min(mines, rows * cols - 9);
			mines = Math.min(mines, (int) (rows * cols * 0.3f));
			mines = Math.max(mines, 8);

			System.out.print(" rows, cols, mines: " + rows + " " + cols + " " + mines);
			System.out.print(" percentage: " + String.format("%.2f", mines / (float) (rows * cols)));

			HolyGrailSolver solver = new HolyGrailSolver(rows, cols);

			CreateSolvableBoard createSolvableBoard = new CreateSolvableBoard(rows, cols, mines);
			final int firstClickI = MyMath.getRand(0, rows - 1);
			final int firstClickJ = MyMath.getRand(0, cols - 1);
			MinesweeperGame game;
			long startTime = System.currentTimeMillis();
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, true);
			System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");
			VisibleTile[][] visibleBoard = new VisibleTile[rows][cols];
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					visibleBoard[i][j] = new VisibleTile();
				}
			}
			while (!game.getIsGameLost() && !game.getIsGameWon()) {
				ConvertGameBoardFormat.convertToExistingBoard(game, visibleBoard, false);
				solver.solvePosition(visibleBoard, mines);
				game.updateLogicalStuff(visibleBoard);

				if (!ExistsLogicalFree.isLogicalFree(visibleBoard)) {
					System.out.println("no logical frees, failed test");
					return;
				}

				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (visibleBoard[i][j].getIsLogicalFree()) {
							game.clickCell(i, j, false);
						}
					}
				}
			}
			if (!game.getIsGameWon()) {
				System.out.println("game is not won, failed test");
				return;
			}
			boolean foundAn8 = false;
			for (int i = 0; i < rows && !foundAn8; ++i) {
				for (int j = 0; j < cols; ++j) {
					if (game.getCell(i, j).getNumberSurroundingMines() == 8) {
						foundAn8 = true;
						break;
					}
				}
			}
			if (!foundAn8) {
				System.out.println("no 8 found, failed test");
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}


	public static void BestSolverOnly(int numberOfTests) throws Exception {

		long[] times = new long[numberOfTests];
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.print("test number: " + testID);

			final int rows = 16;
			final int cols = 30;
			final int mines = 170;//about 35% mines

			CreateSolvableBoard boardGen = new CreateSolvableBoard(rows, cols, mines);
			long startTime = System.currentTimeMillis();
			try {
				boardGen.getSolvableBoardAlwaysMove1MineAwayNoBacktracking(10, 10, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			times[testID - 1] = System.currentTimeMillis() - startTime;
		}
		long totalFirst = 0;
		for (long time : times) {
			System.out.println("time " + time);
			totalFirst += time;
		}
		System.out.println("average " + totalFirst / numberOfTests);
	}

	public static void TestNaiveStuff() throws Exception {
		String[] board = {
				"UUUU1U12U3UU4UUUU2U113U311111.",
				"UUUU2223U33U4U5U43112UU3U22U32",
				"UUU3U22U42234344U2213U322U44UU",
				"UU2UU43UU21UU2UU4U2U333123UU43",
				"13U5U4U5U333435U63423UU22U433U",
				"U4U535U53U2U2U3UUU2U2222U23U53",
				"U43UU4UU4332322232222112323UUU",
				"23U335U6UU22U2233211U11U3U34UU",
				"U4332UUU54U212UUUU211113U5U34U",
				"3UU3U45UU21113444U312223UU44U3",
				"U44U4U3331..1UU1112U2UU45UU5U3",
				"U22U313U2..12322223233UU6UUU4U",
				"33211.2U4211U222UU3U224UUU55U3",
				"UU311122UU123U3U6U42U24U53U5U3",
				"3UU34U322211U24U7U424U4U224UU3",
				"123UUUU1...1112UUU3U3U3111UUUU",

				"170"
		};
		final int rows = 16;
		final int cols = 30;
		final int mines = 170;//about 35% mines

		GaussianEliminationSolver gauss = new GaussianEliminationSolver(rows, cols);
		VisibleTile[][] visibleBoard = convertFormat(board);

		printBoardDebug(visibleBoard, mines);
		gauss.solvePosition(visibleBoard, mines);
		printBoardDebugWithLogicalStuff(visibleBoard, mines);
	}
}

/*





    UUUU1B12B3BB4BBBB2B113B311111.
    UUUU2223B33B4B5B43112BB3B22B32
    UUU3B22B42234344B2213B322B44BB
    UU2BB43BB21BB2BB4B2B333123BB43
    13B5B4B5B333435B63423BB22B433B
    B4B535B53B2B2B3BBB2B2222B23B53
    B43BB4BB4332322232222112323BBB
    23B335B6BB22B2233211B11B3B34BB
    B4332BBB54B212BBBB211113B5B34B
    3BB3B45BB21113444B312223BB44B3
    B44B4B3331..1BB1112B2BB45BB5B3
    B22B313B2..12322223233BB6BBB4B
	33211.2B4211B222BB3B224BBB55B3
    BB311122BB123B3B6B42B24B53B5B3
    3BB34B322211B24B7B424B4B224BB3
    123BBBB1...1112BBB3B3B3111BBUU




















			.1BB32UUUUB312B22B4B3B2B1.1B3B
			.13B4BUU34BB23B22B4B313231325B
			12235B42B4432B223342324B3B2B4B
			B2B4BB322BB11112BB3B3BBB42424B
			122BB6B22321...2B33B44B43B2B32
			1123BB44B2.1111232323B3B2134B2
			2B325BB3B2.1B22B4B3B4342212BB2
			2B3B3B4311.23B34BB33BB3B33B321
			223243B321.1B3B4B412B6B4BB3211
			B32B3B3BB1.12434B2123BB65B43B2
			BB32B22221.12BB3221B35BBB5BB4B
			UB3322...123B322B234B5BBUUBUUU
			U4B2B211.1BB42135B3BBB5B4UUUUU
			B31212B2234BB21BBB325B424UUUUU
			U3122445BB45B43353213B21BBUUUU
			U2B2BBBBBBUUBB2B2B11B2113BUUUU

			U******4******U**U2**4*21*U***
			14*5455**5*6*4UU*U*43**443UUUU
			.3*43**3222*4*3UUU*4335***2111
			.2**5*421234*4**4***3*4*55*22*
			1235**21*2**44***4424*522**23*
			*11**432234*4*4*4*334**333334*
			11123*2*22*3*22134***43**22**3
			....11213*5421123**7*32333*6*3
			12321..13***1.1**4**3*12*43**3
			1***2111*3321.1334333222*4*45*
			23322*3321.11213*3*11*112*3*4*
			*11123**2111*4*5*32233311133U*
			111*13*64*233***212*3**1..2***
			111113***5*3*332213*3232113*4U
			*2.124*44**422..1*22212*32*2UU
			*2.1**212*32*1..1111*12*3*21U*

 */
