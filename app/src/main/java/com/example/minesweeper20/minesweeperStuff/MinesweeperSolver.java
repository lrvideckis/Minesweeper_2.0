package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.helpers.FractionThenDouble;

public interface MinesweeperSolver {
	void solvePosition(VisibleTile[][] board, int numberOfBombs) throws Exception;

	boolean[][] getBombConfiguration(
			VisibleTile[][] _board,
			int _numberOfBombs,
			int _spotI,
			int _spotJ,
			boolean _wantBomb
	) throws Exception;

	class VisibleTile {
		final FractionThenDouble numberOfBombConfigs = new FractionThenDouble(0), numberOfTotalConfigs = new FractionThenDouble(0);
		boolean isVisible, isLogicalBomb, isLogicalFree;
		int numberSurroundingBombs;

		public VisibleTile() {
			reset();
		}

		private void reset() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = 0;
			try {
				numberOfBombConfigs.setValues(0, 1);
				numberOfTotalConfigs.setValues(0, 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public boolean getIsVisible() {
			return isVisible;
		}

		public boolean getIsLogicalBomb() {
			return isLogicalBomb;
		}

		public boolean getIsLogicalFree() {
			return isLogicalFree;
		}

		public FractionThenDouble getNumberOfBombConfigs() {
			return numberOfBombConfigs;
		}

		public FractionThenDouble getNumberOfTotalConfigs() {
			return numberOfTotalConfigs;
		}

		public int getNumberSurroundingBombs() {
			return numberSurroundingBombs;
		}

		public void updateVisibilityAndSurroundingBombs(MinesweeperGame.Tile tile) {
			reset();
			isVisible = tile.isVisible;
			if (isVisible) {
				numberSurroundingBombs = tile.numberSurroundingBombs;
			}
		}
	}
}
