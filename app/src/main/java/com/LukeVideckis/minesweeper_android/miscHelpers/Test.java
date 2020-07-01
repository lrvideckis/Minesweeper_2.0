package com.LukeVideckis.minesweeper_android.miscHelpers;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.customExceptions.NoSolutionFoundException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.BacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.GaussianEliminationSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MyBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.OldBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.SlowBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.LukeVideckis.minesweeper_android.minesweeperStuff.BacktrackingSolver.VisibleTileWithProbability;
import static com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class Test {
	@SuppressWarnings("SpellCheckingInspection")

	private final static String[][] previousFailedBoards = {

			//first failing test: new recursive solver thingy compared to old fast backtracking solver
			{
					"U1.1UU",
					"U1.1UU",
					"11.13U",
					"U1.1UU",
					"U112UU",
					"233U4U",
					"UUUUUU",
					"UUUUUU",

					"18"
			},

			{
					"B1112B112B2BB21..12B22UUUU",
					"221B2111B2234B1..1B22BUUUU",
					"B1222..11212B21..111122UUU",
					"111B21...1B2221111.112UUUU",
					".124B2...1111B12B2.1B2UUUU",
					"12B4B3.111..2333B312122UUU",
					"B213B423B2..1BB212B212UUUU",
					"12233BB3B21233221224B3UUUU",
					"13BB2222122BB1.2B32BB31UUU",
					"UUB4111212B432.2B3B322UUUU",
					"UUB2.1B2B212B1.11211.1UUUU",

					"60",
			},

			//big board where null pointer exception happens
			{
					"...12BB11111UUUUUUUUUUUUUUUUUU",
					"11.1B4432B22UUUUUUUUUUUUUUUUUU",
					"B21212BB322B33UUUUUUUUUUUUUUUU",
					"12B1.123B2213BUUUUUUUUUUUUUUUU",
					"1211..123B1.3BU3UUBUUUUUUUUUUU",
					"B1..113B311.2BBB223UUUUUUUUUUU",
					"221.1B4B3...12321.2UUUUUUUUUUU",
					"1B1.113B2..11211113BUUUUUUUUUU",
					"111112332112B2B11B3UUUUUUUB311",
					"...1B2BB22B33321234UU3B3B4B2..",
					"..133324B33B3B112BB22B332322..",
					"..1BB1.2B23B4111B432113B21B21.",
					"..1221.1112B212322B3212B223B1.",
					"12321...112112BB123BB11111B21.",
					"2BBB1...1B2223B422B3332212221.",
					"B3321...112BB212B2111BB2B11B1.",

					"100"
			},

			//grid which causes cut nodes dfs to add multiple cut nodes to the list of cut nodes
			{
					"UU1..1U",
					"2U1112U",
					"1111U21",
					"1U123UU",
					"U11UUUU",

					"8",
			},

			//cut node not next to any sub component
			{
					"..1UU",
					"..1U1",
					"..111",
					"..1U2",
					"..1UU",
					"..13U",
					"111UU",
					"UU1UU",

					"6"
			},

			//cut node next to >1 sub components
			{
					"UU1..",
					"UU1..",
					"UU321",
					"UUBUU",
					"UUUUU",

					"6"
			},

			//initial failing case for new recursive alg with cut nodes
			{
					"U1......",
					"U211111.",
					"UUUUUU21",
					"UUUUUUUU",

					"7"
			},

			//testing new backtracking algorithm
			{
					"UUUUUU",
					"UU3221",
					"UU21..",
					"UUU1..",

					"7",
			},

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
			VisibleTileWithProbability[][] boardFast = convertFormat(stringBoard);
			VisibleTileWithProbability[][] boardOld = convertFormat(stringBoard);
			VisibleTileWithProbability[][] boardSlow = convertFormat(stringBoard);
			Pair<Integer, Integer> dimensions;
			dimensions = ArrayBounds.getArrayBounds(boardFast);
			if (rows != dimensions.first || cols != dimensions.second) {
				System.out.print("bounds don't match");
				continue;
			}

			BacktrackingSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			OldBacktrackingSolver oldBacktrackingSolver = new OldBacktrackingSolver(rows, cols);
			BacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			try {
				holyGrailSolver.solvePosition(boardFast, mines);
			} catch (NoSolutionFoundException ignored) {
				System.out.println("no solution found, void test");
				continue;
			}

			try {
				oldBacktrackingSolver.solvePosition(boardOld, mines);
				if (areBoardsDifferent(boardFast, boardOld, mines)) {
					return;
				}
			} catch (HitIterationLimitException ignored) {
				System.out.println("OLD backtracking solver hit iteration limit, void test");
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

			//sanity check
			if (areBoardsDifferent(boardSlow, boardOld, mines)) {
				throw new Exception("boardFast == boardOld && boardFast == boardSlow && boardSlow != boardOld");
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static VisibleTileWithProbability[][] convertFormat(String[] stringBoard) throws Exception {
		VisibleTileWithProbability[][] board = new VisibleTileWithProbability[stringBoard.length - 1][stringBoard[0].length()];
		for (int i = 0; i + 1 < stringBoard.length; ++i) {
			for (int j = 0; j < stringBoard[i].length(); ++j) {
				if (stringBoard[i].length() != stringBoard[0].length()) {
					throw new Exception("jagged array - not all rows are the same length");
				}
				board[i][j] = new VisibleTileWithProbability(stringBoard[i].charAt(j));
			}
		}
		return board;
	}

	public static void performTestsForMineProbability(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(3, 8);
			final int cols = MyMath.getRand(3, 40 / rows);
			int mines = MyMath.getRand(2, 9);
			mines = Math.min(mines, rows * cols - 9);

			BacktrackingSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			BacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			int numberOfClicks = MyMath.getRand(0, 4);
			while ((numberOfClicks--) > 0 && !minesweeperGame.getIsGameLost()) {
				minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("game over, void test");
				continue;
			}

			while (!minesweeperGame.getIsGameWon()) {
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("here 1: game is lost, but this shouldn't happen, failed test");
					return;
				}
				VisibleTileWithProbability[][] boardFast = convertToNewBoard(minesweeperGame);
				VisibleTileWithProbability[][] boardSlow = convertToNewBoard(minesweeperGame);

				CreateSolvableBoard.printBoardDebug(boardFast, mines);

				try {
					holyGrailSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
				} catch (HitIterationLimitException ignored) {
					System.out.println("fast solver hit iteration limit, void test");
					break;
				}
				try {
					slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
				} catch (HitIterationLimitException ignored) {
					System.out.println("slow solver hit iteration limit, void test");
					break;
				}
				if (areBoardsDifferent(boardFast, boardSlow, mines)) {
					return;
				}
				boolean clickedFree = false;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (boardFast[i][j].getIsLogicalFree()) {
							clickedFree = true;
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				if (!clickedFree) {
					break;
				}
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("here 2: game is lost, but this shouldn't happen, failed test");
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void performTestsForMineProbabilityLargeBoards(int numberOfTests) throws Exception {
		for (int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			final int rows = MyMath.getRand(10, 40);
			final int cols = MyMath.getRand(10, 40);
			int mines = Math.min((int) (rows * cols * 0.30), rows * cols - 9);

			BacktrackingSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			OldBacktrackingSolver oldBacktrackingSolver = new OldBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame;
			minesweeperGame = new MinesweeperGame(rows, cols, mines);
			int numberOfClicks = MyMath.getRand(0, 4);
			while ((numberOfClicks--) > 0 && !minesweeperGame.getIsGameLost()) {
				minesweeperGame.clickCell(MyMath.getRand(0, rows - 1), MyMath.getRand(0, cols - 1), false);
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("game over, void test");
				continue;
			}

			while (!minesweeperGame.getIsGameWon()) {
				if (minesweeperGame.getIsGameLost()) {
					System.out.println("here 1: game is lost, but this shouldn't happen, failed test");
					return;
				}
				VisibleTileWithProbability[][] boardFast = convertToNewBoard(minesweeperGame);
				VisibleTileWithProbability[][] boardOld = convertToNewBoard(minesweeperGame);

				CreateSolvableBoard.printBoardDebug(boardFast, mines);

				try {
					holyGrailSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
				} catch (HitIterationLimitException ignored) {
					System.out.println("fast solver hit iteration limit, void test");
					break;
				}
				try {
					oldBacktrackingSolver.solvePosition(boardOld, minesweeperGame.getNumberOfMines());
				} catch (HitIterationLimitException ignored) {
					System.out.println("old solver hit iteration limit, void test");
					break;
				}
				if (areBoardsDifferent(boardFast, boardOld, mines)) {
					return;
				}
				boolean clickedFree = false;
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (boardFast[i][j].getIsLogicalFree()) {
							clickedFree = true;
							minesweeperGame.clickCell(i, j, false);
						}
					}
				}
				if (!clickedFree) {
					break;
				}
			}
			if (minesweeperGame.getIsGameLost()) {
				System.out.println("here 2: game is lost, but this shouldn't happen, failed test");
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static boolean areBoardsDifferent(
			VisibleTileWithProbability[][] boardFast,
			VisibleTileWithProbability[][] boardSlow,
			int mines
	) throws Exception {
		int rows = boardFast.length, cols = boardFast[0].length;
		boolean passedTest = true;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (boardFast[i][j].getIsVisible()) {
					continue;
				}

				VisibleTileWithProbability fastTile = boardFast[i][j];
				VisibleTileWithProbability slowTile = boardSlow[i][j];

				if (!fastTile.getMineProbability().equals(slowTile.getMineProbability()) ||
						fastTile.getIsLogicalFree() != slowTile.getIsLogicalFree() ||
						fastTile.getIsLogicalMine() != slowTile.getIsLogicalMine()
				) {
					passedTest = false;
					System.out.println("here, solver outputs don't match");
					System.out.println("i,j: " + i + " " + j);
					System.out.println("fast solver " + fastTile.getMineProbability().getNumerator() + '/' + fastTile.getMineProbability().getDenominator());
					System.out.println("slow solver " + slowTile.getMineProbability().getNumerator() + '/' + slowTile.getMineProbability().getDenominator());
					System.out.println("number of away cells: " + AwayCell.getNumberOfAwayCells(boardFast));
				}
			}
		}
		if (!passedTest) {
			CreateSolvableBoard.printBoardDebug(boardFast, mines);
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

			BacktrackingSolver myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
			MinesweeperSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

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
			VisibleTile[][] boardBacktracking = convertToNewBoard(minesweeperGame);
			VisibleTile[][] boardGauss = convertToNewBoard(minesweeperGame);

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
			CreateSolvableBoard.printBoardDebug(boardBacktracking, mines);
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

			BacktrackingSolver holyGrailSolver = new HolyGrailSolver(rows, cols);
			BacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);
			MinesweeperSolver gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);

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
			VisibleTileWithProbability[][] boardSlow = convertToNewBoard(minesweeperGame);

			try {
				slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfMines());
			} catch (HitIterationLimitException ignored) {
				System.out.println("slow solver hit iteration limit, void test");
				continue;
			}
			for (int i = 0; i < 3; ++i) {
				VisibleTileWithProbability[][] boardFast = convertToNewBoard(minesweeperGame);
				holyGrailSolver.solvePosition(boardFast, minesweeperGame.getNumberOfMines());
				if (areBoardsDifferent(boardFast, boardSlow, mines)) {
					return;
				}

				VisibleTile[][] boardGauss = convertToNewBoard(minesweeperGame);
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

			final int rows = MyMath.getRand(8, 30);
			final int cols = MyMath.getRand(8, 30);
			int mines = MyMath.getRand(2, 100);
			mines = Math.min(mines, rows * cols - 9);
			mines = Math.min(mines, (int) (rows * cols * 0.23f));


			System.out.print(" rows, cols, mines: " + rows + " " + cols + " " + mines);
			System.out.print(" percentage: " + mines / (float) (rows * cols));

			BacktrackingSolver solver = new HolyGrailSolver(rows, cols);

			CreateSolvableBoard createSolvableBoard = new CreateSolvableBoard(rows, cols, mines);
			final int firstClickI = MyMath.getRand(0, rows - 1);
			final int firstClickJ = MyMath.getRand(0, cols - 1);
			MinesweeperGame game;
			long startTime = System.currentTimeMillis();
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, false, new AtomicBoolean(false));
			System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");
			sumTimes += System.currentTimeMillis() - startTime;
			VisibleTileWithProbability[][] visibleBoard = new VisibleTileWithProbability[rows][cols];
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					visibleBoard[i][j] = new VisibleTileWithProbability();
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

				if (noLogicalFrees(visibleBoard)) {
					System.out.println("no logical frees, failed test");
					CreateSolvableBoard.printBoardDebug(visibleBoard, mines);
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

			final int rows = MyMath.getRand(8, 30);
			final int cols = MyMath.getRand(8, 30);
			int mines = MyMath.getRand(8, 100);
			mines = Math.min(mines, rows * cols - 9);
			mines = Math.min(mines, (int) (rows * cols * 0.23f));

			System.out.print(" rows, cols, mines: " + rows + " " + cols + " " + mines);
			System.out.print(" percentage: " + mines / (float) (rows * cols));

			BacktrackingSolver solver = new HolyGrailSolver(rows, cols);

			CreateSolvableBoard createSolvableBoard = new CreateSolvableBoard(rows, cols, mines);
			final int firstClickI = MyMath.getRand(0, rows - 1);
			final int firstClickJ = MyMath.getRand(0, cols - 1);
			MinesweeperGame game;
			long startTime = System.currentTimeMillis();
			game = createSolvableBoard.getSolvableBoard(firstClickI, firstClickJ, true, new AtomicBoolean(false));
			System.out.println(" time to create solvable board: " + (System.currentTimeMillis() - startTime) + " ms");
			VisibleTileWithProbability[][] visibleBoard = new VisibleTileWithProbability[rows][cols];
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					visibleBoard[i][j] = new VisibleTileWithProbability();
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

				if (noLogicalFrees(visibleBoard)) {
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
			if (hitIterationLimit) {
				continue;
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


	private static VisibleTileWithProbability[][] convertToNewBoard(MinesweeperGame minesweeperGame) throws Exception {
		final int rows = minesweeperGame.getRows();
		final int cols = minesweeperGame.getCols();
		VisibleTileWithProbability[][] board = new VisibleTileWithProbability[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j] = new VisibleTileWithProbability();
				board[i][j].updateVisibilityAndSurroundingMines(minesweeperGame.getCell(i, j));
			}
		}
		return board;
	}


	private static boolean noLogicalFrees(VisibleTile[][] board) {
		for (VisibleTile[] row : board) {
			for (VisibleTile cell : row) {
				if (cell.getIsLogicalFree()) {
					return false;
				}
			}
		}
		return true;
	}
}
