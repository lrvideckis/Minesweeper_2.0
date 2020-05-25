package com.example.minesweeper20.helpers;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolverWithBigint;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.SlowBacktrackingSolver;

import java.math.BigInteger;
import java.util.ArrayList;

public class Test {
	private final static int numberOfTests = 20;
	private static int rows, cols;

	public static void performTestsForFractionOverflow() {
		for(int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int bombs = 5;
			rows = cols = 5;
			try {
				rows = MyMath.getRand(10, 15);
				cols = MyMath.getRand(10, 15);
				bombs = MyMath.getRand(2, 50);
			} catch(Exception ignored) {}
			bombs = Math.min(bombs, rows*cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			BacktrackingSolverWithBigint backtrackingSolverWithBigint = new BacktrackingSolverWithBigint(rows, cols);

			MinesweeperGame minesweeperGame = new MinesweeperGame(rows, cols, bombs);
			try {
				int numberOfClicks = MyMath.getRand(3, 7);
				while(numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows-1), MyMath.getRand(0,cols-1), false);
				}
				if(minesweeperGame.getIsGameOver()) {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardFraction = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardBigInt = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFraction, minesweeperGame.getNumberOfBombs());
				backtrackingSolverWithBigint.solvePosition(boardBigInt, minesweeperGame.getNumberOfBombs());
				boolean testPassed = true;
				for(int i = 0; i < rows; ++i) {
					for(int j = 0; j < cols; ++j) {
						if(boardBigInt.get(i).get(j).getIsVisible()) {
							continue;
						}
						FractionThenDouble curr = boardFraction.get(i).get(j).getNumberOfBombConfigs();
						curr.divideWith(boardFraction.get(i).get(j).getNumberOfTotalConfigs());

						BigInteger top = backtrackingSolverWithBigint.getNumberOfBombConfigs(i,j);
						BigInteger bottom = backtrackingSolverWithBigint.getNumberOfTotalConfigs(i,j);
						BigInteger gcd = top.gcd(bottom);
						top = top.divide(gcd);
						bottom = bottom.divide(gcd);

						if(curr.getHasOverflowed()) {
							if(Math.abs(top.doubleValue() / bottom.doubleValue() - curr.getValue()) > 0.000000001) {
								testPassed = false;
								System.out.println("here, solver outputs don't match");
								System.out.println("i,j: " + i + " " + j);
								System.out.println("fraction solver " + curr.getValue());
								System.out.println("big int solver " + top.toString() + '/' + bottom.toString());
							}
						} else {
							if(
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
				if(!testPassed) {
					printBoardDebug(boardBigInt);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				printBoardDebug(boardBigInt);
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	public static void performTestsForBombProbability() {
		for(int testID = 1; testID <= numberOfTests; ++testID) {
			System.out.println("test number: " + testID);
			int bombs = 5;
			rows = cols = 5;
			try {
				rows = MyMath.getRand(3, 8);
				cols = MyMath.getRand(3, 40 / rows);
				bombs = MyMath.getRand(2, 9);
			} catch(Exception ignored) {}
			bombs = Math.min(bombs, rows*cols - 9);

			BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
			SlowBacktrackingSolver slowBacktrackingSolver = new SlowBacktrackingSolver(rows, cols);

			MinesweeperGame minesweeperGame = new MinesweeperGame(rows, cols, bombs);
			try {
				int numberOfClicks = MyMath.getRand(0, 4);
				while(numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					minesweeperGame.clickCell(MyMath.getRand(0, rows-1), MyMath.getRand(0, cols-1), false);
				}
				if(minesweeperGame.getIsGameOver()) {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardFast = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardSlow = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

			try {
				backtrackingSolver.solvePosition(boardFast, minesweeperGame.getNumberOfBombs());
				try {
					slowBacktrackingSolver.solvePosition(boardSlow, minesweeperGame.getNumberOfBombs());
				} catch(HitIterationLimitException e) {
					System.out.println("slow solver hit iteration limit, void test");
					continue;
				}
				if(areBoardsDifferent(boardFast, boardSlow)) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	private static boolean areBoardsDifferent(
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardFast,
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> boardSlow
	) throws Exception {
		boolean passedTest = true;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(boardFast.get(i).get(j).getIsVisible()) {
					continue;
				}

				MinesweeperSolver.VisibleTile fastTile = boardFast.get(i).get(j);
				FractionThenDouble fast = new FractionThenDouble(fastTile.getNumberOfBombConfigs());
				fast.divideWith(fastTile.getNumberOfTotalConfigs());

				MinesweeperSolver.VisibleTile slowTile = boardSlow.get(i).get(j);
				FractionThenDouble slow = new FractionThenDouble(slowTile.getNumberOfBombConfigs());
				slow.divideWith(slowTile.getNumberOfTotalConfigs());

				if(fast.getNumerator() != slow.getNumerator() || fast.getDenominator() != slow.getDenominator()) {
					passedTest = false;
					System.out.println("here, solver outputs don't match");
					System.out.println("i,j: " + i + " " + j);
					System.out.println("fast solver " + fast.getNumerator() + '/' + fast.getDenominator());
					System.out.println("slow solver " + slow.getNumerator() + '/' + slow.getDenominator());
					System.out.println("number of away cells: " + GetConnectedComponents.getNumberOfAwayCells(boardFast));
				}
			}
		}
		if(!passedTest) {
			printBoardDebug(boardFast);
		}
		return !passedTest;
	}

	private static void printBoardDebug(ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board) {
		System.out.println("\nboard is:");
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(board.get(i).get(j).getIsVisible()) {
					if(board.get(i).get(j).getNumberSurroundingBombs() == 0) {
						System.out.print('.');
					} else {
						System.out.print(board.get(i).get(j).getNumberSurroundingBombs());
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
