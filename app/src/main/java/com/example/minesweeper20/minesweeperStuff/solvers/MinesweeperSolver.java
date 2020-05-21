package com.example.minesweeper20.minesweeperStuff.solvers;

import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;

import java.util.ArrayList;

public interface MinesweeperSolver {
	class VisibleTile {
		Boolean isVisible, isLogicalBomb, isLogicalFree;
		Integer numberSurroundingBombs, numberOfBombConfigs, numberOfTotalConfigs;
		public VisibleTile() {
			reset();
		}
		public void reset() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = numberOfBombConfigs = numberOfTotalConfigs = 0;
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
		public int getNumberOfBombConfigs() {
			return numberOfBombConfigs;
		}
		public int getNumberOfTotalConfigs() {
			return numberOfTotalConfigs;
		}
		public void updateCell(MinesweeperGame.Tile gameTile) {
			isVisible = gameTile.isRevealed();
			if(gameTile.isRevealed()) {
				numberSurroundingBombs = gameTile.getNumberSurroundingBombs();
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
