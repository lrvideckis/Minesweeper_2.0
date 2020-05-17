package com.example.minesweeper20.minesweeperStuff;

import java.util.ArrayList;

public interface MinesweeperSolver {
	class VisibleTile {
		public Boolean isVisible, isLogicalBomb, isLogicalFree;
		public Integer numberSurroundingBombs, numberOfFreeConfigs, numberOfTotalConfigs;
		public VisibleTile() {
		    reset();
		}
		public void reset() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = numberOfFreeConfigs = numberOfTotalConfigs = 0;
		}
	}
	void solvePosition(ArrayList<ArrayList<VisibleTile>> board, int numberOfBombs) throws Exception;
}
