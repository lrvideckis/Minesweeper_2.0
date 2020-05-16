package com.example.minesweeper20.minesweeperStuff;

import java.util.ArrayList;

public interface MinesweeperSolver {
	public class visibleTile {
		public Boolean isVisible, isLogicalBomb, isLogicalFree;
		public Integer numberSurroundingBombs, numberOfFreeConfigs, numberOfTotalConfigs;
		visibleTile() {
			isLogicalFree = isLogicalBomb = isVisible = false;
			numberSurroundingBombs = numberOfFreeConfigs = numberOfTotalConfigs = 0;
		}
	}
	public void solvePosition(ArrayList<ArrayList<visibleTile>> board) throws Exception;
}
