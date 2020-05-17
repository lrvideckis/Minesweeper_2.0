package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.minesweeperStuff.helpers.ArrayBounds;

import java.util.ArrayList;
import java.util.Collections;

public class MinesweeperGame {
	public static class Tile {
		private Boolean isRevealed, isFlagged;
		public Boolean isBomb;
		private Integer numberSurroundingBombs;
		Tile() {
			isRevealed = false;
			isFlagged = false;
			isBomb = false;
			numberSurroundingBombs = 0;
		}
		public boolean isRevealed() {
			return isRevealed;
		}
		public boolean isFlagged() throws Exception {
			if(isFlagged && isRevealed) {
				throw new Exception("invalid state: flagged && revealed");
			}
			return isFlagged;
		}
		void revealTile() {
			isRevealed = true;
			isFlagged = false;
		}
		void toggleFlag() {
			if(isRevealed) {
				isFlagged = false;
				return;
			}
			isFlagged = !isFlagged;
		}
		public Integer getNumberSurroundingBombs() {
			return numberSurroundingBombs;
		}
	}
	private final Integer numberOfRows, numberOfCols, numberOfBombs;
	private Integer numberOfFlags;
	private boolean firstClick, isGameOver;
	private final Tile[][] grid;

	public MinesweeperGame(int _numberOfRows, int _numberOfCols, int _numberOfBombs) {
		numberOfRows = _numberOfRows;
		numberOfCols = _numberOfCols;
		numberOfBombs = _numberOfBombs;
		numberOfFlags = 0;
		firstClick = true;
		isGameOver = false;
		grid = new Tile[numberOfRows][numberOfCols];
		for(int i = 0; i < numberOfRows; ++i) {
			for(int j = 0; j < numberOfCols; ++j) {
				grid[i][j] = new Tile();
			}
		}
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
		if(ArrayBounds.outOfBounds(row,col,numberOfRows,numberOfCols)) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return grid[row][col];
	}

	public void clickCell(int row, int col, boolean toggleBombs) throws Exception {
		if(firstClick && !toggleBombs) {
			firstClick = false;
			firstClickedCell(row, col);
			return;
		}
		final Tile curr = getCell(row, col);
		if(curr.isRevealed()) {
			checkToRevealAdjacentBombs(row, col);
		}
		if(toggleBombs && !curr.isRevealed()) {
			if(curr.isFlagged()) {
				--numberOfFlags;
			} else {
				++numberOfFlags;
			}
			curr.toggleFlag();
			return;
		}
		if(curr.isBomb && !curr.isFlagged()) {
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
					if (adj.isRevealed()) {
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
		if(!revealSurroundingCells) {
			return;
		}
		for (int dRow = -1; dRow <= 1; ++dRow) {
			for (int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					Tile adj = getCell(row+dRow, col+dCol);
					if(adj.isBomb) {
						continue;
					}
					revealCell(row+dRow, col+dCol);
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	private void firstClickedCell(int row, int col) throws Exception {
		ArrayList<Integer> spotsI = new ArrayList<>();
		ArrayList<Integer> spotsJ = new ArrayList<>();
		ArrayList<Integer> permutation = new ArrayList<>();
		for(int i = 0; i < numberOfRows; ++i) {
			for(int j = 0; j < numberOfCols; ++j) {
				if(Math.abs(row-i) <= 1 && Math.abs(col-j) <= 1) {
					continue;
				}
				permutation.add(spotsI.size());
				spotsI.add(i);
				spotsJ.add(j);
			}
		}

		if(spotsI.size() != spotsJ.size() || permutation.size() != spotsJ.size()) {
			throw new Exception("array list not working as expected");
		}

		if(spotsI.size() < numberOfBombs) {
			throw new Exception("too many bombs to have a zero start");
		}

		Collections.shuffle(permutation);

		for(int i = 0; i < numberOfBombs; ++i) {
			int bombRow = spotsI.get(permutation.get(i));
			int bombCol = spotsJ.get(permutation.get(i));
			getCell(bombRow, bombCol).isBomb = true;
			for(int dRow = -1; dRow <= 1; ++dRow) {
				for(int dCol = -1; dCol <= 1; ++dCol) {
					if(dRow == 0 && dCol == 0) {
						continue;
					}
					try {
						getCell(bombRow+dRow, bombCol+dCol).numberSurroundingBombs++;
					} catch(ArrayIndexOutOfBoundsException ignored) {
					}
				}
			}
		}
		if(getCell(row, col).isBomb) {
			throw new Exception("starting click shouldn't be a bomb");
		}
		revealCell(row, col);
	}

	private void revealCell(int row, int col) throws Exception {
		Tile curr = getCell(row, col);
		if(curr.isBomb) {
			throw new Exception("can't reveal a bomb");
		}
		if(curr.isFlagged()) {
			--numberOfFlags;
		}
		curr.revealTile();
		if(curr.numberSurroundingBombs > 0) {
			return;
		}
		for(int dRow = -1; dRow <= 1; ++dRow) {
			for(int dCol = -1; dCol <= 1; ++dCol) {
				if (dRow == 0 && dCol == 0) {
					continue;
				}
				try {
					final int adjRow = row + dRow;
					final int adjCol = col + dCol;
					Tile adjacent = getCell(adjRow, adjCol);
					if (!adjacent.isRevealed()) {
						revealCell(adjRow, adjCol);
					}
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	public boolean getIsGameOver() {
		return isGameOver;
	}
}
