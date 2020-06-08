package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;

import java.util.ArrayList;
import java.util.Collections;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class MinesweeperGame {
	private final int numberOfRows, numberOfCols, numberOfMines;
	private final Tile[][] grid;
	private int numberOfFlags;
	private boolean firstClick, isGameOver;

	public MinesweeperGame(int numberOfRows, int numberOfCols, int numberOfMines) throws Exception {

		//TODO: look into removing this, it is kinda pointless
		if (tooManyMinesForZeroStart(numberOfRows, numberOfCols, numberOfMines)) {
			throw new Exception("too many mines for zero start, UI doesn't allow for this to happen");
		}

		this.numberOfRows = numberOfRows;
		this.numberOfCols = numberOfCols;
		this.numberOfMines = numberOfMines;
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

	public static boolean tooManyMinesForZeroStart(int numberOfRows, int numberOfCols, int numberOfMines) {
		return (numberOfMines > numberOfRows * numberOfCols - 9);
	}

	public int getNumberOfRows() {
		return numberOfRows;
	}

	public int getNumberOfCols() {
		return numberOfCols;
	}

	public int getNumberOfMines() {
		return numberOfMines;
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

	public void clickCell(int row, int col, boolean toggleMines) throws Exception {
		if (firstClick && !toggleMines) {
			firstClick = false;
			firstClickedCell(row, col);
			return;
		}
		if (isGameOver) {
			return;
		}
		final Tile curr = getCell(row, col);
		if (curr.getIsVisible()) {
			checkToRevealAdjacentMines(row, col);
		}
		if (toggleMines) {
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
		if (curr.isMine && !curr.isFlagged()) {
			isGameOver = true;
			return;
		}
		if (curr.isFlagged()) {
			return;
		}
		revealCell(row, col);
	}

	private void checkToRevealAdjacentMines(int row, int col) throws Exception {
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
					if (adj.isFlagged() && !adj.isMine) {
						isGameOver = true;
						return;
					}
					if (adj.isFlagged() != adj.isMine) {
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
					if (adj.isMine) {
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

		if (spotsI.size() < numberOfMines) {
			throw new Exception("too many mines to have a zero start");
		}

		Collections.shuffle(permutation);

		for (int i = 0; i < numberOfMines; ++i) {
			int mineRow = spotsI.get(permutation.get(i));
			int mineCol = spotsJ.get(permutation.get(i));
			getCell(mineRow, mineCol).isMine = true;
			incrementSurroundingMineCounts(mineRow, mineCol);
		}
		if (getCell(row, col).isMine) {
			throw new Exception("starting click shouldn't be a mine");
		}
		revealCell(row, col);
	}

	private void incrementSurroundingMineCounts(int mineRow, int mineCol) {
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					getCell(mineRow + dRow, mineCol + dCol).numberSurroundingMines++;
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	private void revealCell(int row, int col) throws Exception {
		Tile curr = getCell(row, col);
		if (curr.isMine) {
			throw new Exception("can't reveal a mine");
		}
		if (curr.isFlagged()) {
			--numberOfFlags;
		}
		curr.revealTile();
		if (curr.numberSurroundingMines > 0) {
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

	public void changeMineLocations(boolean[][] newMineLocations) throws Exception {
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				Tile curr = getCell(i, j);
				if (!curr.getIsVisible()) {
					continue;
				}
				int cntSurroundingMines = 0;
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, numberOfRows, numberOfCols)) {
					if (newMineLocations[adj[0]][adj[1]]) {
						++cntSurroundingMines;
					}
				}
				if (cntSurroundingMines != getCell(i, j).numberSurroundingMines) {
					throw new Exception("bad mine configuration: surrounding mine count doesn't match");
				}
			}
		}
		int numberOfNewMines = 0;
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				getCell(i, j).numberSurroundingMines = 0;
				if (newMineLocations[i][j]) {
					++numberOfNewMines;
				}
			}
		}
		if (numberOfNewMines != numberOfMines) {
			throw new Exception("bad mine configuration: wrong # of mines");
		}
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				if (newMineLocations[i][j] && getCell(i, j).getIsVisible()) {
					throw new Exception("bad mine configuration: mine is in revealed cell");
				}
				getCell(i, j).isMine = newMineLocations[i][j];
				if (getCell(i, j).isMine) {
					incrementSurroundingMineCounts(i, j);
				}
			}
		}
	}

	public boolean getIsGameLost() {
		return isGameOver;
	}

	//game is won if all free cells are visible
	public boolean getIsGameWon() {
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				Tile currCell = getCell(i, j);
				if (!currCell.isMine() && !currCell.isVisible) {
					return false;
				}
			}
		}
		return true;
	}

	public static class Tile extends VisibleTile {
		private boolean isFlagged, isMine;

		private Tile() {
			isFlagged = isMine = false;
			numberSurroundingMines = 0;
		}

		public boolean isMine() {
			return isMine;
		}

		public boolean isFlagged() {
			if (isVisible) {
				isFlagged = false;
			}
			return isFlagged;
		}

		private void revealTile() throws Exception {
			isVisible = true;
			isFlagged = false;
			if (isMine) {
				throw new Exception("can't reveal a mine");
			}
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
