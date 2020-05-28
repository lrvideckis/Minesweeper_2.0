package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetAdjacentCells;

import java.util.ArrayList;
import java.util.Collections;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class MinesweeperGame {
	private final int numberOfRows, numberOfCols, numberOfBombs;
	private final Tile[][] grid;
	private int numberOfFlags;
	private boolean firstClick, isGameOver;
	//TODO: constructor with bomb placements as param for testing - you can test the same board over multiple runs
	public MinesweeperGame(int _numberOfRows, int _numberOfCols, int _numberOfBombs) {
		numberOfRows = _numberOfRows;
		numberOfCols = _numberOfCols;
		numberOfBombs = _numberOfBombs;
		numberOfFlags = 0;
		firstClick = true;
		isGameOver = false;
		grid = new Tile[numberOfRows][numberOfCols];
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				grid[i][j] = new Tile();
			}
		}
	}

	public static boolean tooManyBombsForZeroStart(int numberOfRows, int numberOfCols, int numberOfBombs) {
		return (numberOfBombs > numberOfRows * numberOfCols - 9);
	}

	public int getNumberOfRows() {
		return numberOfRows;
	}

	public int getNumberOfCols() {
		return numberOfCols;
	}

	public int getNumberOfBombs() {
		return numberOfBombs;
	}

	public int getNumberOfFlags() {
		return numberOfFlags;
	}

	public Tile getCell(int row, int col) {
		if (ArrayBounds.outOfBounds(row, col, numberOfRows, numberOfCols)) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return grid[row][col];
	}

	public void clickCell(int row, int col, boolean toggleBombs) throws Exception {
		if (firstClick && !toggleBombs) {
			firstClick = false;
			firstClickedCell(row, col);
			return;
		}
		final Tile curr = getCell(row, col);
		if (curr.getIsVisible()) {
			checkToRevealAdjacentBombs(row, col);
		}
		if (toggleBombs) {
			if (!curr.getIsVisible()) {
				if (curr.isFlagged()) {
					--numberOfFlags;
				} else {
					++numberOfFlags;
				}
				curr.toggleFlag();
			}
			return;
		}
		if (curr.isBomb && !curr.isFlagged()) {
			isGameOver = true;
			return;
		}
		revealCell(row, col);
	}

	private void checkToRevealAdjacentBombs(int row, int col) throws Exception {
		boolean revealSurroundingCells = true;
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					Tile adj = getCell(row + dRow, col + dCol);
					if (adj.getIsVisible()) {
						continue;
					}
					if (adj.isFlagged() && !adj.isBomb) {
						isGameOver = true;
						return;
					}
					if (adj.isFlagged() != adj.isBomb) {
						revealSurroundingCells = false;
					}
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
		if (!revealSurroundingCells) {
			return;
		}
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					Tile adj = getCell(row + dRow, col + dCol);
					if (adj.isBomb) {
						continue;
					}
					revealCell(row + dRow, col + dCol);
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	private void firstClickedCell(int row, int col) throws Exception {
		ArrayList<Integer> spotsI = new ArrayList<>();
		ArrayList<Integer> spotsJ = new ArrayList<>();
		ArrayList<Integer> permutation = new ArrayList<>();
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
					continue;
				}
				permutation.add(spotsI.size());
				spotsI.add(i);
				spotsJ.add(j);
			}
		}

		if (spotsI.size() != spotsJ.size() || permutation.size() != spotsJ.size()) {
			throw new Exception("array list not working as expected");
		}

		if (spotsI.size() < numberOfBombs) {
			throw new Exception("too many bombs to have a zero start");
		}

		Collections.shuffle(permutation);

		for (int i = 0; i < numberOfBombs; ++i) {
			int bombRow = spotsI.get(permutation.get(i));
			int bombCol = spotsJ.get(permutation.get(i));
			getCell(bombRow, bombCol).isBomb = true;
			incrementSurroundingBombCounts(bombRow, bombCol);
		}
		if (getCell(row, col).isBomb) {
			throw new Exception("starting click shouldn't be a bomb");
		}
		revealCell(row, col);
	}

	private void incrementSurroundingBombCounts(int bombRow, int bombCol) {
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					getCell(bombRow + dRow, bombCol + dCol).numberSurroundingBombs++;
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	private void revealCell(int row, int col) throws Exception {
		Tile curr = getCell(row, col);
		if (curr.isBomb) {
			throw new Exception("can't reveal a bomb");
		}
		if (curr.isFlagged()) {
			--numberOfFlags;
		}
		curr.revealTile();
		if (curr.numberSurroundingBombs > 0) {
			return;
		}
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					final int adjRow = row + dRow;
					final int adjCol = col + dCol;
					Tile adjacent = getCell(adjRow, adjCol);
					if (!adjacent.getIsVisible()) {
						revealCell(adjRow, adjCol);
					}
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	public void changeBombLocations(boolean[][] newBombLocations) throws Exception {
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				Tile curr = getCell(i, j);
				if (!curr.getIsVisible()) {
					continue;
				}
				int cntSurroundingBombs = 0;
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, numberOfRows, numberOfCols)) {
					if (newBombLocations[adj[0]][adj[1]]) {
						++cntSurroundingBombs;
					}
				}
				if (cntSurroundingBombs != getCell(i, j).numberSurroundingBombs) {
					throw new Exception("bad bomb configuration: surrounding bomb count doesn't match");
				}
			}
		}
		int numberOfNewBombs = 0;
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				getCell(i, j).numberSurroundingBombs = 0;
				if (newBombLocations[i][j]) {
					++numberOfNewBombs;
				}
			}
		}
		if (numberOfNewBombs != numberOfBombs) {
			throw new Exception("bad bomb configuration: wrong # of bombs");
		}
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				if (newBombLocations[i][j] && getCell(i, j).getIsVisible()) {
					throw new Exception("bad bomb configuration: bomb is in revealed cell");
				}
				getCell(i, j).isBomb = newBombLocations[i][j];
				if (getCell(i, j).isBomb) {
					incrementSurroundingBombCounts(i, j);
				}
			}
		}
	}

	public boolean getIsGameOver() {
		return isGameOver;
	}

	public static class Tile extends VisibleTile {
		private boolean isFlagged, isBomb;

		private Tile() {
			isFlagged = isBomb = false;
			numberSurroundingBombs = 0;
		}

		public boolean isBomb() {
			return isBomb;
		}

		public boolean isFlagged() {
			if (isVisible) {
				isFlagged = false;
			}
			return isFlagged;
		}

		private void revealTile() {
			isVisible = true;
			isFlagged = false;
		}

		private void toggleFlag() {
			if (isVisible) {
				isFlagged = false;
				return;
			}
			isFlagged = !isFlagged;
		}
	}
}
