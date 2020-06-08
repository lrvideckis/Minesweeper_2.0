package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;

public interface MinesweeperSolver {
	void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception;

	boolean[][] getMineConfiguration(
			VisibleTile[][] board,
			int numberOfMines,
			int spotI,
			int spotJ,
			boolean wantMine
	) throws Exception;

	int getNumberOfIterations();

	class VisibleTile {
		BigFraction numberOfMineConfigs = new BigFraction(0), numberOfTotalConfigs = new BigFraction(0);
		boolean isVisible, isLogicalMine, isLogicalFree;
		int numberSurroundingMines;

		public VisibleTile() {
			reset();
		}

		private void reset() {
			isLogicalFree = isLogicalMine = isVisible = false;
			numberSurroundingMines = 0;
			try {
				numberOfMineConfigs.setValues(0, 1);
				numberOfTotalConfigs.setValues(0, 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public boolean getIsVisible() {
			return isVisible;
		}

		public boolean getIsLogicalMine() {
			return isLogicalMine;
		}

		public boolean getIsLogicalFree() {
			return isLogicalFree;
		}

		public BigFraction getNumberOfMineConfigs() {
			return numberOfMineConfigs;
		}

		public BigFraction getNumberOfTotalConfigs() {
			return numberOfTotalConfigs;
		}

		public int getNumberSurroundingMines() {
			return numberSurroundingMines;
		}

		public void updateVisibilityAndSurroundingMines(MinesweeperGame.Tile tile) {
			reset();
			isVisible = tile.isVisible;
			if (isVisible) {
				numberSurroundingMines = tile.numberSurroundingMines;
			}
		}

		public void updateVisibilityAndSurroundingMines(boolean isVisible, int numberSurroundingMines) {
			reset();
			this.isVisible = isVisible;
			if (isVisible) {
				this.numberSurroundingMines = numberSurroundingMines;
			}
		}
	}
}
