package com.example.minesweeper20.minesweeperStuff;

import com.example.minesweeper20.helpers.FractionThenDouble;

import java.util.ArrayList;

public interface MinesweeperSolver {
	class VisibleTile {
		Boolean isVisible, isLogicalBomb, isLogicalFree;
		Integer numberSurroundingBombs;
		FractionThenDouble numberOfBombConfigs, numberOfTotalConfigs;
		public VisibleTile() {
			reset();
		}
		private void reset() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = 0;
			numberOfBombConfigs = new FractionThenDouble(0);
			numberOfTotalConfigs = new FractionThenDouble(0);
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
