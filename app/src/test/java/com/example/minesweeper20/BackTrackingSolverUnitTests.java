/*
package com.example.minesweeper20;

import com.example.minesweeper20.minesweeperStuff.solvers.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.solvers.BacktrackingSolver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


//TODO: get this to pass
public class BackTrackingSolverUnitTests {

	@Test
	public void firstSolverTest() throws Exception {
		ArrayList<String> initBoard = new ArrayList<>(Arrays.asList(
				"...",
				"121"
		));
		ArrayList<String> expectedBoard = new ArrayList<>(Arrays.asList(
				"BFB",
				"121"
		));
		doTest(initBoard, expectedBoard);
		assert(false);
	}





	private void doTest(ArrayList<String> initBoard, ArrayList<String> expectedBoard) throws Exception {
		int rows = initBoard.size();
		int cols = initBoard.get(0).length();
		System.out.println("rows, cols: " + rows + " " + cols);
		ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board = new ArrayList<>();
		initializeBoard(board, initBoard, rows, cols);

		//when(mock)


		BacktrackingSolver backtrackingSolver = new BacktrackingSolver(rows, cols);
		backtrackingSolver.solvePosition(board, 0);
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.VisibleTile currTile = board.get(i).get(j);
				char c = expectedBoard.get(i).charAt(j);
				if(c == 'B') {
					assert(currTile.isLogicalBomb);
				} else if(c == 'F') {
					assert(currTile.isLogicalFree);
				} else if(c != '.') {
					assert(!currTile.isLogicalBomb);
					assert(!currTile.isLogicalFree);
				}
			}
		}
	}

	private void initializeBoard(
			ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board,
			ArrayList<String> initBoard,
			int rows,
			int cols) {

		for(int i = 0; i < rows; ++i) {
			ArrayList<MinesweeperSolver.VisibleTile> currRow = new ArrayList<>(cols);
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.VisibleTile currTile = new MinesweeperSolver.VisibleTile();
				char c = initBoard.get(i).charAt(j);
				if('0' <= c && c <= '8') {
					currTile.isVisible = true;
					currTile.numberSurroundingBombs = c-'0';
				} else {
					currTile.isVisible = false;
				}
				currRow.add(currTile);
			}
			board.add(currRow);
		}
		assertEquals(rows, board.size());
		for(int i = 0; i < rows; ++i) {
			assertEquals(cols, board.get(i).size());
		}
	}
}
 */
