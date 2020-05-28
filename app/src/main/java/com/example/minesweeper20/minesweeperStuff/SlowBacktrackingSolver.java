package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.helpers.AllCellsAreHidden;
import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetAdjacentCells;
import com.example.minesweeper20.helpers.MutableInt;

import java.util.ArrayList;

public class SlowBacktrackingSolver implements MinesweeperSolver {

	private final static int iterationLimit = 500000;
	private final int[][][] lastUnvisitedSpot;
	private final boolean[][] isBomb;
	private final int[][] cntSurroundingBombs;
	private int rows, cols;
	private VisibleTile[][] board;
	private int numberOfBombs;

	public SlowBacktrackingSolver(int rows, int cols) {
		isBomb = new boolean[rows][cols];
		cntSurroundingBombs = new int[rows][cols];
		lastUnvisitedSpot = new int[rows][cols][2];
	}

	@Override
	public void solvePosition(VisibleTile[][] _board, int _numberOfBombs) throws Exception {
		initialize(_board, _numberOfBombs);

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					board[i][j].numberOfBombConfigs.setValues(numberOfBombs, 1);
					board[i][j].numberOfTotalConfigs.setValues(rows * cols, 1);
				}
			}
			return;
		}

		ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!board[i][j].getIsVisible()) {
					component.add(new Pair<>(i, j));
				}
			}
		}
		initializeLastUnvisitedSpot(component);

		MutableInt currIterations = new MutableInt(0);
		MutableInt currNumberOfBombs = new MutableInt(0);
		solveComponent(0, component, currIterations, currNumberOfBombs);

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile curr = board[i][j];
				if (curr.getIsVisible()) {
					continue;
				}
				if (curr.numberOfTotalConfigs.equals(0)) {
					throw new Exception("There should be at least one bomb configuration for non-visible cells");
				}
				if (curr.numberOfBombConfigs.equals(0)) {
					curr.isLogicalFree = true;
				} else if (curr.numberOfBombConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalBomb = true;
				}
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<Pair<Integer, Integer>> component) {
		for (Pair<Integer, Integer> spot : component) {
			for (int[] adj : GetAdjacentCells.getAdjacentCells(spot.first, spot.second, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				if (board[adjI][adjJ].isVisible) {
					lastUnvisitedSpot[adjI][adjJ][0] = spot.first;
					lastUnvisitedSpot[adjI][adjJ][1] = spot.second;
				}
			}
		}
	}

	public boolean[][] getBombConfiguration(VisibleTile[][] _board, int _numberOfBombs, int _spotI, int _spotJ, boolean _wantBomb) throws Exception {
		throw new Exception("to make warning go away");
	}

	private void initialize(VisibleTile[][] _board, int _numberOfBombs) throws Exception {
		board = _board;
		numberOfBombs = _numberOfBombs;
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				isBomb[i][j] = false;
				cntSurroundingBombs[i][j] = 0;
			}
		}
	}

	private void solveComponent(int pos, ArrayList<Pair<Integer, Integer>> component, MutableInt currIterations, MutableInt currNumberOfBombs) throws Exception {
		if (pos == component.size()) {
			checkSolution(currNumberOfBombs.get());
			return;
		}
		currIterations.addWith(1);
		if (currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException();
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		//isBomb.get(i).set(j, true);
		isBomb[i][j] = true;
		if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
			currNumberOfBombs.addWith(1);
			updateSurroundingBombCnt(i, j, 1);
			solveComponent(pos + 1, component, currIterations, currNumberOfBombs);
			updateSurroundingBombCnt(i, j, -1);
			currNumberOfBombs.addWith(-1);
		}

		//try free
		isBomb[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, component, currIterations, currNumberOfBombs);
		}
	}

	private void updateSurroundingBombCnt(int i, int j, int delta) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (board[adjI][adjJ].isVisible) {
				final int cnt = cntSurroundingBombs[adjI][adjJ];
				cntSurroundingBombs[adjI][adjJ] = cnt + delta;
			}
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingABomb) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			VisibleTile adjTile = board[adjI][adjJ];
			if (!adjTile.isVisible) {
				continue;
			}
			final int currBacktrackingCount = cntSurroundingBombs[adjI][adjJ];
			if (currBacktrackingCount + arePlacingABomb > adjTile.numberSurroundingBombs) {
				return false;
			}
			if (
					lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
							lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
							currBacktrackingCount + arePlacingABomb != adjTile.numberSurroundingBombs) {
				return false;
			}
		}
		return true;
	}

	private void checkSolution(int currNumberOfBombs) throws Exception {
		if (!checkPositionValidity(currNumberOfBombs)) {
			return;
		}

		if (currNumberOfBombs != numberOfBombs) {
			return;
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible()) {
					continue;
				}
				if (isBomb[i][j]) {
					board[i][j].numberOfBombConfigs.addWith(1);
				}
				board[i][j].numberOfTotalConfigs.addWith(1);
			}
		}
	}

	//returns true if valid
	private boolean checkPositionValidity(int currNumberOfBombs) throws Exception {
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					VisibleTile adjTile = board[adjI][adjJ];
					if (!adjTile.isVisible) {
						continue;
					}
					if (cntSurroundingBombs[adjI][adjJ] != adjTile.numberSurroundingBombs) {
						return false;
					}
				}
			}
		}
		int prevNumberOfBombs = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (isBomb[i][j]) {
					++prevNumberOfBombs;
				}
			}
		}
		if (prevNumberOfBombs != currNumberOfBombs) {
			throw new Exception("number of bombs doesn't match");
		}

		return true;
	}
}
