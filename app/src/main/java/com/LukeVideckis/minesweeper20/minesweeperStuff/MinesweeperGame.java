package com.LukeVideckis.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;

import java.util.ArrayList;
import java.util.Collections;

import static com.LukeVideckis.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class MinesweeperGame {
	private final int rows, cols, numberOfMines;
	private final Tile[][] grid;
	private int numberOfFlags, rowWith8 = -1, colWith8 = -1;
	private boolean firstClick, isGameLost, hasAn8 = false;

	public MinesweeperGame(int rows, int cols, int numberOfMines) throws Exception {
		if (tooManyMinesForZeroStart(rows, cols, numberOfMines)) {
			throw new Exception("too many mines for zero start, UI doesn't allow for this to happen");
		}

		this.rows = rows;
		this.cols = cols;
		this.numberOfMines = numberOfMines;
		numberOfFlags = 0;
		firstClick = true;
		isGameLost = false;
		grid = new Tile[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				grid[i][j] = new Tile();
			}
		}
	}

	//copy constructor
	public MinesweeperGame(MinesweeperGame minesweeperGame) {
		rows = minesweeperGame.getRows();
		cols = minesweeperGame.getCols();
		numberOfMines = minesweeperGame.getNumberOfMines();
		hasAn8 = minesweeperGame.hasAn8;
		rowWith8 = minesweeperGame.rowWith8;
		colWith8 = minesweeperGame.colWith8;
		numberOfFlags = minesweeperGame.numberOfFlags;
		firstClick = minesweeperGame.firstClick;
		isGameLost = minesweeperGame.isGameLost;
		grid = new Tile[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				grid[i][j] = new Tile(minesweeperGame.getCell(i, j));
			}
		}
	}

	public static boolean tooManyMinesForZeroStart(int rows, int cols, int numberOfMines) {
		return (numberOfMines > rows * cols - 9);
	}

	public void setHavingAn8() {
		hasAn8 = true;
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public int getNumberOfMines() {
		return numberOfMines;
	}

	public int getNumberOfFlags() {
		return numberOfFlags;
	}

	public boolean isBeforeFirstClick() {
		return firstClick;
	}

	public Tile getCell(int row, int col) {
		if (ArrayBounds.outOfBounds(row, col, rows, cols)) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return grid[row][col];
	}

	public void clickCell(int row, int col, boolean toggleMines) throws Exception {
		if (firstClick && !toggleMines) {
			firstClick = false;
			if (hasAn8) {
				firstClickedCellWith8(row, col);
			} else {
				firstClickedCell(row, col);
			}
			return;
		}
		if (isGameLost || getIsGameWon()) {
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
			isGameLost = true;
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
						isGameLost = true;
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
		ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
					continue;
				}
				spots.add(new Pair<>(i, j));
			}
		}

		if (spots.size() < numberOfMines) {
			throw new Exception("too many mines to have a zero start");
		}

		Collections.shuffle(spots);

		for (int pos = 0; pos < numberOfMines; ++pos) {
			final int mineRow = spots.get(pos).first;
			final int mineCol = spots.get(pos).second;
			getCell(mineRow, mineCol).isMine = true;
			incrementSurroundingMineCounts(mineRow, mineCol);
		}
		if (getCell(row, col).isMine) {
			throw new Exception("starting click shouldn't be a mine");
		}
		revealCell(row, col);
	}

	private void firstClickedCellWith8(int row, int col) throws Exception {
		ArrayList<Pair<Integer, Integer>> spots = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
					continue;
				}
				spots.add(new Pair<>(i, j));
			}
		}

		if (spots.size() < numberOfMines) {
			throw new Exception("too many mines to have a zero start");
		}

		if (numberOfMines < 8) {
			throw new Exception("too few mines for an 8");
		}

		Collections.shuffle(spots);

		for (int pos = 0; pos < spots.size(); ++pos) {
			final int i = spots.get(pos).first;
			final int j = spots.get(pos).second;
			if (i == 0 || j == 0 || i == rows - 1 || j == cols - 1) {
				continue;
			}
			rowWith8 = i;
			colWith8 = j;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				getCell(adjI, adjJ).isMine = true;
				incrementSurroundingMineCounts(adjI, adjJ);
			}
			break;
		}
		if (rowWith8 == -1 || colWith8 == -1) {
			throw new Exception("didn't find a spot for an 8, but there should be one");
		}

		spots.clear();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (Math.abs(row - i) <= 1 && Math.abs(col - j) <= 1) {
					continue;
				}
				if (Math.abs(rowWith8 - i) <= 1 && Math.abs(colWith8 - j) <= 1) {
					continue;
				}
				spots.add(new Pair<>(i, j));
			}
		}

		Collections.shuffle(spots);

		if (spots.size() < numberOfMines - 8) {
			throw new Exception("too many mines to have a zero start with an 8");
		}

		for (int pos = 0; pos < numberOfMines - 8; ++pos) {
			final int i = spots.get(pos).first;
			final int j = spots.get(pos).second;
			getCell(i, j).isMine = true;
			incrementSurroundingMineCounts(i, j);
		}
		if (getCell(row, col).isMine) {
			throw new Exception("starting click shouldn't be a mine");
		}
		revealCell(row, col);
	}

	//TODO: make this use adjacent cell helper
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

	public void shuffleAwayMines() {
		int numberOfAwayMines = 0;
		ArrayList<Pair<Integer, Integer>> allAwayCells = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!AwayCell.isAwayCell(this, i, j)) {
					continue;
				}
				if (hasAn8 && Math.abs(i - rowWith8) <= 1 && Math.abs(j - colWith8) <= 1) {
					continue;
				}
				if (getCell(i, j).isMine()) {
					++numberOfAwayMines;
					grid[i][j].isMine = false;
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						final int adjI = adj[0], adjJ = adj[1];
						getCell(adjI, adjJ).numberSurroundingMines--;
					}

				}
				allAwayCells.add(new Pair<>(i, j));
			}
		}
		Collections.shuffle(allAwayCells);
		for (int pos = 0; pos < numberOfAwayMines; ++pos) {
			final int i = allAwayCells.get(pos).first;
			final int j = allAwayCells.get(pos).second;
			grid[i][j].isMine = true;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				getCell(adjI, adjJ).numberSurroundingMines++;
			}
		}
	}

	public void changeMineLocations(boolean[][] newMineLocations) throws Exception {
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (newMineLocations[i][j]) {
					System.out.print('1');
				} else {
					System.out.print('0');
				}
			}
			System.out.println();
		}
		System.out.println();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				Tile curr = getCell(i, j);
				if (!curr.getIsVisible()) {
					continue;
				}
				int cntSurroundingMines = 0;
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					if (newMineLocations[adj[0]][adj[1]]) {
						++cntSurroundingMines;
					}
				}
				if (cntSurroundingMines != getCell(i, j).numberSurroundingMines) {
					System.out.println("i,j: " + i + " " + j);
					throw new Exception("bad mine configuration: surrounding mine count doesn't match");
				}
			}
		}
		int numberOfNewMines = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				getCell(i, j).numberSurroundingMines = 0;
				if (newMineLocations[i][j]) {
					++numberOfNewMines;
				}
			}
		}
		if (numberOfNewMines != numberOfMines) {
			System.out.println("number of mines should be: " + numberOfMines + " but it is: " + numberOfNewMines);
			throw new Exception("bad mine configuration: wrong # of mines");
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
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
		return isGameLost;
	}

	//game is won if all free cells are visible
	public boolean getIsGameWon() {
		if (isGameLost) {
			return false;
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
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

		//copy constructor
		private Tile(Tile other) {
			isMine = other.isMine;
			isFlagged = other.isFlagged;

			isVisible = other.isVisible;
			isLogicalMine = other.isLogicalMine;
			isLogicalFree = other.isLogicalFree;
			numberSurroundingMines = other.numberSurroundingMines;
			numberOfMineConfigs = new BigFraction(other.numberOfMineConfigs);
			numberOfTotalConfigs = new BigFraction(other.numberOfTotalConfigs);
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
