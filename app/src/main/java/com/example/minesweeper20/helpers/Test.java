package com.example.minesweeper20.helpers;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.SlowBacktrackingSolver;

import java.util.ArrayList;
import java.util.Random;

public class Test {
	private final static int numberOfTests = 50;
	private static int rows, cols;

	public static void perform100SolverTestsForProbability() {
		Random r = new Random();
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
				int numberOfClicks = r.nextInt(5);
				while(numberOfClicks-- > 0 && !minesweeperGame.getIsGameOver()) {
					minesweeperGame.clickCell(r.nextInt(rows), r.nextInt(cols), false);
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
				if(!checkBoardEquality(boardFast, boardSlow)) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		System.out.println("passed all tests!!!!!!!!!!!!!!!!!!!");
	}

	//returns true if test passed
	private static boolean checkBoardEquality(
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
				Fraction fast = new Fraction(fastTile.getNumberOfBombConfigs(), fastTile.getNumberOfTotalConfigs());

				MinesweeperSolver.VisibleTile slowTile = boardSlow.get(i).get(j);
				Fraction slow = new Fraction(slowTile.getNumberOfBombConfigs(), slowTile.getNumberOfTotalConfigs());

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
		return passedTest;
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
