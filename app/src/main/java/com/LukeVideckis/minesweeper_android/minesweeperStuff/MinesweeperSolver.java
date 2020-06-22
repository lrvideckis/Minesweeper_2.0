package com.LukeVideckis.minesweeper_android.minesweeperStuff;

import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;

public interface MinesweeperSolver {
	void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception;

	class VisibleTile {
		BigFraction mineProbability = new BigFraction(0);
		boolean isVisible, isLogicalMine, isLogicalFree;
		int numberSurroundingMines;

		public VisibleTile() throws Exception {
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
					mineProbability.equals(other.mineProbability);
		}

		private void reset() throws Exception {
			isLogicalFree = isLogicalMine = isVisible = false;
			numberSurroundingMines = 0;
			mineProbability.setValues(0, 1);
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

		public BigFraction getMineProbability() {
			return mineProbability;
		}

		public int getNumberSurroundingMines() {
			return numberSurroundingMines;
		}

		public void updateVisibilityAndSurroundingMines(MinesweeperGame.Tile tile) throws Exception {
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
			/*
			if (tile.isLogicalFree && !tile.mineProbability.equals(0)) {
				throw new Exception("logical free tile with non-zero probability");
			}
			if (tile.isLogicalMine && !tile.mineProbability.equals(1)) {
				throw new Exception("logical mine tile with non-1 probability");
			}
			 */
			reset();
			isVisible = tile.isVisible;
			numberSurroundingMines = tile.numberSurroundingMines;
			isLogicalMine = tile.isLogicalMine;
			isLogicalFree = tile.isLogicalFree;
			mineProbability.setValue(tile.mineProbability);
		}

		public void updateVisibilityAndSurroundingMines(boolean isVisible, int numberSurroundingMines) throws Exception {
			reset();
			this.isVisible = isVisible;
			this.numberSurroundingMines = numberSurroundingMines;
		}

		//TODO: remove this eventually
		public void setIsLogicalMine() {
			isLogicalMine = true;
		}
	}
}
