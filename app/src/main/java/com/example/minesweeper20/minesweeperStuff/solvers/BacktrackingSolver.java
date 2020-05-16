package com.example.minesweeper20.minesweeperStuff.solvers;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.helpers.ArrayBounds;
import com.example.minesweeper20.minesweeperStuff.helpers.GetConnectedComponents;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;
import java.util.Collections;

public class BacktrackingSolver implements MinesweeperSolver {

	private Integer rows, cols;

	private ArrayList<ArrayList<Integer>> numberOfBombPositions;
	private ArrayList<ArrayList<Boolean>> isBomb;
	private ArrayList<ArrayList<visibleTile>> board;

	@Override
	public void solvePosition(ArrayList<ArrayList<visibleTile>> _board) throws Exception {
		board = _board;
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		numberOfBombPositions = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			numberOfBombPositions.set(i, new ArrayList<>(Collections.nCopies(cols, 0)));
		}

		isBomb = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			isBomb.set(i, new ArrayList<>(Collections.nCopies(cols, false)));
		}

		ArrayList<ArrayList<Pair<Integer,Integer>>> components = GetConnectedComponents.getComponents(board);
		for(ArrayList<Pair<Integer,Integer>> component : components) {
			solveComponent(0, component);
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.visibleTile curr = board.get(i).get(j);
				if(curr.numberOfTotalConfigs == 0) {
					continue;
				}
				if(curr.numberOfFreeConfigs == 0) {
					curr.isLogicalBomb = true;
				} else if(curr.numberOfFreeConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalFree = true;
				}
			}
		}
	}

	private void solveComponent(int pos, ArrayList<Pair<Integer,Integer>> component) {
		if(pos == component.size()) {
			checkSolution(component);
			return;
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;
		MinesweeperSolver.visibleTile tile = board.get(i).get(j);

		//try bomb
		isBomb.get(i).set(j, true);
		if(getSurroundingNumbers(i,j) <= board.get(i).get(j).numberSurroundingBombs) {
			solveComponent(pos+1, component);
		}

		//try free
		isBomb.get(i).set(j, false);
		solveComponent(pos+1, component);
	}

	private void checkSolution(ArrayList<Pair<Integer,Integer>> component) {
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			for(int di = -1; di <= 1; ++di) {
				for(int dj = -1; dj <= 1; ++dj) {
					if (di == 0 && dj == 0) {
						continue;
					}
					int adjI = i + di;
					int adjJ = j + dj;
					if (!ArrayBounds.inBounds(adjI, adjJ, rows, cols)) {
						continue;
					}
					MinesweeperSolver.visibleTile adjTile = board.get(adjI).get(adjJ);
					if(!adjTile.isVisible) {
						continue;
					}
					if(getSurroundingNumbers(adjI,adjJ) != adjTile.numberSurroundingBombs) {
						return;
					}
				}
			}
		}
		System.out.println("here, found solution");
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if(!isBomb.get(i).get(j)) {
				++board.get(i).get(j).numberOfFreeConfigs;
			}
			++board.get(i).get(j).numberOfTotalConfigs;
		}
	}

	private int getSurroundingNumbers(int i, int j) {
		int cntSurroundingBombs = 0;
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if(di == 0 && dj == 0) {
					continue;
				}
				int adjI = i + di;
				int adjJ = j + dj;
				if(!ArrayBounds.inBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				if(isBomb.get(adjI).get(adjJ)) {
					++cntSurroundingBombs;
				}
			}
		}
		return cntSurroundingBombs;
	}
}
