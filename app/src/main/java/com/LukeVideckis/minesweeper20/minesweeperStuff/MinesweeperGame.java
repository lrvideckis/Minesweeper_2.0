package com.LukeVideckis.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.customExceptions.NoAwayCellsToMoveAMineToException;
import com.LukeVideckis.minesweeper20.customExceptions.NoInterestingMinesException;
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
	public MinesweeperGame(MinesweeperGame minesweeperGame) throws Exception {
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

		boolean foundAn8 = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (grid[i][j].isMine) {
					continue;
				}

				if (grid[i][j].numberSurroundingMines == 8) {
					foundAn8 = true;
				}
				int cntSurroundingMines = 0;
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					if (getCell(adjI, adjJ).isMine) {
						++cntSurroundingMines;
					}
				}
				if (cntSurroundingMines != grid[i][j].numberSurroundingMines) {
					throw new Exception("number of surrounding mines doesn't match");
				}
			}
		}
		if (hasAn8 && !foundAn8) {
			throw new Exception("game should have an 8, but no 8 was found");
		}
	}

	public MinesweeperGame(MinesweeperGame game, int firstClickI, int firstClickJ) throws Exception {
		this(game);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				getCell(i, j).isVisible = false;
			}
		}
		if (getCell(firstClickI, firstClickJ).isMine) {
			throw new Exception("first clicked cell shouldn't be a mine");
		}
		if (getCell(firstClickI, firstClickJ).numberSurroundingMines != 0) {
			throw new Exception("first clicked cell isn't a zero start");
		}
		revealCell(firstClickI, firstClickJ);
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
		for (int[] adjCells : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
			Tile adj = getCell(adjCells[0], adjCells[1]);
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
		}
		if (!revealSurroundingCells) {
			return;
		}
		for (int[] adjCells : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
			final int adjI = adjCells[0], adjJ = adjCells[0];
			Tile adj = getCell(adjI, adjJ);
			if (adj.isMine) {
				continue;
			}
			revealCell(adjI, adjJ);
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
			changeMineStatus(mineRow, mineCol, true);
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
				changeMineStatus(adjI, adjJ, true);
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

	//TODO: replace this with update mine status function
	private void incrementSurroundingMineCounts(int mineRow, int mineCol) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(mineRow, mineCol, rows, cols)) {
			getCell(adj[0], adj[1]).numberSurroundingMines++;
		}
	}

	private void revealCell(int row, int col) throws Exception {
		Tile curr = getCell(row, col);
		if (curr.isMine) {
			throw new Exception("can't reveal a mine");
		}
		if (curr.isLogicalMine) {
			throw new Exception("can't reveal a logical mine");
		}
		if (curr.isFlagged()) {
			--numberOfFlags;
		}
		curr.revealTile();
		if (curr.numberSurroundingMines > 0) {
			return;
		}
		for (int[] adj : GetAdjacentCells.getAdjacentCells(row, col, rows, cols)) {
			final int adjRow = adj[0];
			final int adjCol = adj[1];
			Tile adjacent = getCell(adjRow, adjCol);
			if (!adjacent.getIsVisible()) {
				revealCell(adjRow, adjCol);
			}
		}
	}

	public void shuffleAwayMines() {
		int numberOfAwayMines = 0;
		ArrayList<Pair<Integer, Integer>> allAwayCells = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!AwayCell.isAwayCell(this, i, j) ||
						(hasAn8 && Math.abs(i - rowWith8) <= 1 && Math.abs(j - colWith8) <= 1)
				) {
					continue;
				}
				if (getCell(i, j).isMine()) {
					++numberOfAwayMines;
					changeMineStatus(i, j, false);

				}
				allAwayCells.add(new Pair<>(i, j));
			}
		}
		Collections.shuffle(allAwayCells);
		for (int pos = 0; pos < numberOfAwayMines; ++pos) {
			final int i = allAwayCells.get(pos).first;
			final int j = allAwayCells.get(pos).second;
			changeMineStatus(i, j, true);
		}
	}

	private boolean isInterestingCell(int i, int j) {
		if (getCell(i, j).isVisible) {
			return false;
		}
		if (AwayCell.isAwayCell(this, i, j)) {
			return false;
		}
		return !hasAn8 || Math.abs(i - rowWith8) > 1 || Math.abs(j - colWith8) > 1;
	}

	//interesting mines are mines which are adjacent to a visible clue
	public void shuffleInterestingMines(VisibleTile[][] visibleBoard) throws Exception {
		int numberInterestingMines = 0;
		ArrayList<Pair<Integer, Integer>> interestingSpots = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (isInterestingCell(i, j) && !visibleBoard[i][j].isLogicalMine) {
					if (getCell(i, j).isMine) {
						++numberInterestingMines;
						changeMineStatus(i, j, false);
					}
					interestingSpots.add(new Pair<>(i, j));
				}
			}
		}
		Collections.shuffle(interestingSpots);
		for (int pos = 0; pos < numberInterestingMines; ++pos) {
			final int i = interestingSpots.get(pos).first;
			final int j = interestingSpots.get(pos).second;
			changeMineStatus(i, j, true);
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (getCell(i, j).isVisible) {
					revealCell(i, j);
				}
			}
		}
	}

	public void shuffleInterestingMinesAndMakeOneAway(VisibleTile[][] visibleBoard) throws Exception {
		int interestingMines = 0;
		ArrayList<Pair<Integer, Integer>> interestingSpots = new ArrayList<>();
		ArrayList<Pair<Integer, Integer>> freeAwayCells = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (isInterestingCell(i, j) && !visibleBoard[i][j].isLogicalMine) {
					if (getCell(i, j).isMine) {
						++interestingMines;
						changeMineStatus(i, j, false);
					}
					interestingSpots.add(new Pair<>(i, j));
				}
				if (AwayCell.isAwayCell(this, i, j) && !getCell(i, j).isMine) {
					freeAwayCells.add(new Pair<>(i, j));
				}
			}
		}
		if (interestingMines == 0) {
			throw new NoInterestingMinesException("no interesting mines, but there needs to be one to remove");
		}
		if (freeAwayCells.isEmpty()) {
			throw new NoAwayCellsToMoveAMineToException("no free away cells");
		}
		Collections.shuffle(interestingSpots);
		for (int pos = 0; pos < interestingMines - 1; ++pos) {
			final int i = interestingSpots.get(pos).first;
			final int j = interestingSpots.get(pos).second;
			changeMineStatus(i, j, true);
		}
		Collections.shuffle(freeAwayCells);
		int i = freeAwayCells.get(0).first;
		int j = freeAwayCells.get(0).second;
		changeMineStatus(i, j, true);

		for (i = 0; i < rows; ++i) {
			for (j = 0; j < cols; ++j) {
				if (getCell(i, j).isVisible) {
					revealCell(i, j);
				}
			}
		}
	}

	//TODO: this doesn't take into account the guaranteed 8
	public void changeMineLocationsWithoutChangingVisibleClues(boolean[][] newMineLocations) throws Exception {
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

	public void updateLogicalStuff(VisibleTile[][] visibleBoard) throws Exception {
		if (visibleBoard.length != rows) {
			throw new Exception("visibleBoard has wrong dimensions");
		}
		for (int i = 0; i < rows; ++i) {
			if (visibleBoard[i].length != cols) {
				throw new Exception("visibleBoard has wrong dimensions");
			}
			for (int j = 0; j < cols; ++j) {
				if (visibleBoard[i][j].isLogicalFree || visibleBoard[i][j].isLogicalMine) {
					if (getCell(i, j).isVisible) {
						throw new Exception("visible cells can't be logical");
					}
				}
				getCell(i, j).isLogicalFree = visibleBoard[i][j].isLogicalFree;
				getCell(i, j).isLogicalMine = visibleBoard[i][j].isLogicalMine;
				getCell(i, j).numberOfMineConfigs.setValue(visibleBoard[i][j].numberOfMineConfigs);
				getCell(i, j).numberOfTotalConfigs.setValue(visibleBoard[i][j].numberOfTotalConfigs);
			}
		}
	}

	private void changeMineStatus(int i, int j, boolean isMine) {
		if (getCell(i, j).isMine == isMine) {
			return;
		}
		getCell(i, j).isMine = isMine;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (isMine) {
				getCell(adjI, adjJ).numberSurroundingMines++;
			} else {
				getCell(adjI, adjJ).numberSurroundingMines--;
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
			isLogicalFree = false;
			if (isMine) {
				throw new Exception("can't reveal a mine");
			}
			if (isLogicalMine) {
				throw new Exception("can't reveal a logical mine");
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
