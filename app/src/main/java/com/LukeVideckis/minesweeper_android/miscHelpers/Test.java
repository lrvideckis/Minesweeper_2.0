package com.LukeVideckis.minesweeper_android.miscHelpers;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoSolutionFoundException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.BacktrackingSolverWithBigint;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MyBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.SlowBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ExistsLogicalFree;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class Test {
	@SuppressWarnings("SpellCheckingInspection")

	private final static String[][] previousFailedBoards = {

			{
					".112B1...1111UU",
					".1B2121213B33UU",
					"2321.1B2B4B4BBB",
					"BB1..1233B3UUUU",
					"B31...1B224UUUB",
					"11.1122212BB5UU",
					"..12B2B212B4BB2",
					"..1B222B2212232",
					"..111.13B2...2B",
					".111...2B31..2B",
					"23B1...12B1..11",
					"BB431..133311..",
					"U5BB4333BB2B21.",
					"UUUBBBBB4222B21",
					"UUUUUUUB2..112B",

					"55"
			},

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
				} else if (stringBoard[i].charAt(j) == 'B') {

					board[i][j].updateVisibilityAndSurroundingMines(false, 0);
					board[i][j].setIsLogicalMine();

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
				CreateSolvableBoard.printBoardDebug(boardBigInt);
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
			CreateSolvableBoard.printBoardDebug(boardFast);
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
			CreateSolvableBoard.printBoardDebug(boardBacktracking);
			return true;
		}
		return false;
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

	@SuppressLint("DefaultLocale")
	public static void TestThatSolvableBoardsAreSolvable(int numberOfTests) throws Exception {
		long sumTimes = 0;
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);

			/*
			final int rows = MyMath.getRand(8, 30);
			final int cols = MyMath.getRand(8, 30);
			int mines = MyMath.getRand(2, 100);
			mines = Math.min(mines, rows * cols - 9);
			mines = Math.min(mines, (int) (rows * cols * 0.23f));
			 */

			final int rows = 16;
			final int cols = 30;
			int mines = 100;

			System.out.print(" rows, cols, mines: " + rows + " " + cols + " " + mines);
			System.out.println(" percentage: " + String.format("%.2f", mines / (float) (rows * cols)));

			HolyGrailSolver solver = new HolyGrailSolver(rows, cols);

			CreateSolvableBoard createSolvableBoard = new CreateSolvableBoard(rows, cols, mines);
			final int firstClickI = MyMath.getRand(0, rows - 1);
			final int firstClickJ = MyMath.getRand(0, cols - 1);
			MinesweeperGame game;
			long startTime = System.currentTimeMillis();
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, false, new AtomicBoolean(false));
			System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");
			sumTimes += System.currentTimeMillis() - startTime;
			VisibleTile[][] visibleBoard = new VisibleTile[rows][cols];
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					visibleBoard[i][j] = new VisibleTile();
				}
			}
			boolean hitIterationLimit = false;
			while (!game.getIsGameLost() && !game.getIsGameWon()) {
				ConvertGameBoardFormat.convertToExistingBoard(game, visibleBoard, false);
				try {
					solver.solvePosition(visibleBoard, mines);
				} catch (HitIterationLimitException ignored) {
					System.out.println("hit iteration limit, void test");
					hitIterationLimit = true;
					break;
				}
				game.updateLogicalStuff(visibleBoard);

				if (!ExistsLogicalFree.isLogicalFree(visibleBoard)) {
					System.out.println("no logical frees, failed test");
					CreateSolvableBoard.printBoardDebug(visibleBoard);
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
			if (hitIterationLimit) {
				continue;
			}
			if (!game.getIsGameWon()) {
				System.out.println("game is not won, failed test");
				return;
			}
		}
		System.out.println("average total time (ms): " + sumTimes / numberOfTests);
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
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, true, new AtomicBoolean(false));
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

	public static void BestSolverOnly(int numberOfTests) {
		final int numberOfSolvers = 5;

		long[][] times = new long[numberOfTests][numberOfSolvers];
		int[] numberOfSuccessfulSolves = new int[numberOfSolvers];

		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);

			final int rows = 16;
			final int cols = 30;
			final int mines = 100;


			CreateSolvableBoard boardGen = new CreateSolvableBoard(rows, cols, mines);
			long startTime = System.currentTimeMillis();
			boolean solved = true;
			try {
				boardGen.getSolvableBoard(5, 5, false, new AtomicBoolean(false));
			} catch (Exception e) {
				e.printStackTrace();
				solved = false;
			}
			times[testID - 1][0] = System.currentTimeMillis() - startTime;
			if (solved) numberOfSuccessfulSolves[0]++;


			/*
			CreateSolvableBoard boardGen = new CreateSolvableBoard(rows, cols, mines);
			long startTime = System.currentTimeMillis();
			boolean solved = true;
			try {
				boardGen.getSolvableBoard2(5, 5, false, new AtomicBoolean(false));
			} catch (Exception e) {
				e.printStackTrace();
				solved = false;
			}
			times[testID - 1][1] = System.currentTimeMillis() - startTime;
			if (solved) numberOfSuccessfulSolves[1]++;


			boardGen = new CreateSolvableBoard(rows, cols, mines);
			startTime = System.currentTimeMillis();
			solved = true;
			try {
				boardGen.getSolvableBoard3(5, 5, false, new AtomicBoolean(false));
			} catch (Exception e) {
				solved = false;
				e.printStackTrace();
			}
			times[testID - 1][2] = System.currentTimeMillis() - startTime;
			if (solved) numberOfSuccessfulSolves[2]++;
			 */


			/*
			boardGen = new CreateSolvableBoard(rows, cols, mines);
			startTime = System.currentTimeMillis();
			solved = true;
			try {
				boardGen.getSolvableBoard4(5, 5, false, new AtomicBoolean(false));
			} catch (Exception e) {
				solved = false;
				e.printStackTrace();
			}
			times[testID - 1][3] = System.currentTimeMillis() - startTime;
			if (solved) numberOfSuccessfulSolves[3]++;
			 */


			boardGen = new CreateSolvableBoard(rows, cols, mines);
			startTime = System.currentTimeMillis();
			solved = true;
			try {
				boardGen.getSolvableBoardOld(5, 5, false);
			} catch (Exception e) {
				solved = false;
				e.printStackTrace();
			}
			times[testID - 1][4] = System.currentTimeMillis() - startTime;
			if (solved) numberOfSuccessfulSolves[4]++;
		}
		long[] total = new long[numberOfSolvers];
		for (long[] time : times) {
			System.out.print("time ");
			for (int i = 0; i < numberOfSolvers; ++i) {
				System.out.print(time[i] + " ");
				total[i] += time[i];
			}
			System.out.println();
		}
		System.out.print("averages: ");
		for (int i = 0; i < numberOfSolvers; ++i) {
			System.out.print(total[i] / numberOfTests + " ");
		}
		System.out.println();

		System.out.print("solves:");
		for (int i = 0; i < numberOfSolvers; ++i) {
			System.out.println(numberOfSuccessfulSolves[i] + " out of " + numberOfTests);
		}
	}
}
