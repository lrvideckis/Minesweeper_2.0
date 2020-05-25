package com.example.minesweeper20.minesweeperStuff;

import java.util.ArrayList;

public interface MinesweeperSolver {
	class VisibleTile {
		Boolean isVisible, isLogicalBomb, isLogicalFree;
		Integer numberSurroundingBombs;
		Long numberOfBombConfigs, numberOfTotalConfigs;
		public VisibleTile() {
			reset();
		}
		private void reset() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = 0;
			numberOfBombConfigs = numberOfTotalConfigs = 0L;
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
		public long getNumberOfBombConfigs() {
			return numberOfBombConfigs;
		}
		public long getNumberOfTotalConfigs() {
			return numberOfTotalConfigs;
		}
		public int getNumberSurroundingBombs() {
			return numberSurroundingBombs;
		}
		public void updateVisibilityAndSurroundingBombs(MinesweeperGame.Tile tile) {
			reset();
			isVisible = tile.isVisible;
			if(isVisible) {
				numberSurroundingBombs = tile.numberSurroundingBombs;
			}
		}
	}
	void solvePosition(ArrayList<ArrayList<VisibleTile>> board, int numberOfBombs) throws Exception;

	ArrayList<ArrayList<Boolean>> getBombConfiguration(
			ArrayList<ArrayList<VisibleTile>> _board,
			int _numberOfBombs,
			int _spotI,
			int _spotJ,
			boolean _wantBomb
	) throws Exception;
}
