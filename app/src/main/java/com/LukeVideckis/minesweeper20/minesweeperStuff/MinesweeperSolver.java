package com.LukeVideckis.minesweeper20.minesweeperStuff;

import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;

public interface MinesweeperSolver {
	void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception;

	class VisibleTile {
		BigFraction numberOfMineConfigs = new BigFraction(0), numberOfTotalConfigs = new BigFraction(0);
		boolean isVisible, isLogicalMine, isLogicalFree;
		int numberSurroundingMines;

		public VisibleTile() {
			reset();
		}

		public boolean isNonLogicalStuffEqual(VisibleTile other) {
			return isVisible == other.isVisible &&
					numberSurroundingMines == other.numberSurroundingMines;
		}

		public boolean isEverythingEqual(VisibleTile other) {
			return isVisible == other.isVisible &&
					isLogicalMine == other.isLogicalMine &&
					isLogicalFree == other.isLogicalFree &&
					numberSurroundingMines == other.numberSurroundingMines &&
					numberOfMineConfigs.equals(other.numberOfMineConfigs) &&
					numberOfTotalConfigs.equals(other.numberOfTotalConfigs);
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
			numberSurroundingMines = tile.numberSurroundingMines;
		}

		public void updateVisibilitySurroundingMinesAndLogicalStuff(MinesweeperGame.Tile tile) throws Exception {
			if (tile.isLogicalFree && tile.isLogicalMine) {
				throw new Exception("tile can't be both logical free and mine");
			}
			if (tile.isVisible) {
				if (tile.isLogicalFree || tile.isLogicalMine) {
					throw new Exception("visible tiles can't be logical stuff");
				}
			}
			reset();
			isVisible = tile.isVisible;
			numberSurroundingMines = tile.numberSurroundingMines;
			isLogicalMine = tile.isLogicalMine;
			isLogicalFree = tile.isLogicalFree;
			numberOfMineConfigs.setValue(tile.numberOfMineConfigs);
			numberOfTotalConfigs.setValue(tile.numberOfTotalConfigs);
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
