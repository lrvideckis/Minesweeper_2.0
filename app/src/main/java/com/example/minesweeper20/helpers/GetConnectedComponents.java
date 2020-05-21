package com.example.minesweeper20.helpers;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.solvers.MinesweeperSolver;

import java.util.ArrayList;
import java.util.Collections;

public class GetConnectedComponents {
	private static class Dsu {
		private final ArrayList<Integer> parent;
		Dsu(int size) {
			parent = new ArrayList<>(Collections.nCopies(size, -1));
		}
		int find(int node) {
			if(parent.get(node) < 0) {
				return node;
			}
			parent.set(node, find(parent.get(node)));
			return parent.get(node);
		}
		void merge(int x, int y) {
			if((x=find(x)) == (y=find(y))) return;
			if(parent.get(y) < parent.get(x)) {
				int temp = x;
				//noinspection SuspiciousNameCombination
				x = y;
				y = temp;
			}
			parent.set(x, parent.get(x) + parent.get(y));
			parent.set(y,x);
		}
	}

	private static Integer rows, cols;

	private static int getNode(int i, int j) {
		if(ArrayBounds.outOfBounds(i,j,rows,cols)) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i*cols + j;
	}

	//TODO: play around with the order of cells in a single CC to make the backtracking faster
	public static ArrayList<ArrayList<Pair<Integer,Integer>>> getComponents(ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		Dsu disjointSet = new Dsu(rows * cols);
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(!board.get(i).get(j).getIsVisible()) {
					continue;
				}
				for(int di = -1; di <= 1; ++di) {
					for(int dj = -1; dj <= 1; ++dj) {
						if(di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i + di;
						final int adjJ = j + dj;
						if(ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						MinesweeperSolver.VisibleTile adjTile = board.get(adjI).get(adjJ);
						if(adjTile.getIsVisible()) {
							continue;
						}
						disjointSet.merge(getNode(i,j),getNode(adjI,adjJ));
					}
				}
			}
		}
		ArrayList<ArrayList<Pair<Integer,Integer>>> tempComponents = new ArrayList<>(rows * cols);
		for(int i = 0; i < rows*cols; ++i) {
			tempComponents.add(new ArrayList<Pair<Integer,Integer>>());
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.VisibleTile currTile = board.get(i).get(j);
				if (currTile.getIsVisible()) {
					continue;
				}
				boolean foundAdjVis = false;
				for(int di = -1; di <= 1 && !foundAdjVis; ++di) {
					for(int dj = -1; dj <= 1; ++dj) {
						if(di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i + di;
						final int adjJ = j + dj;
						if(ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						if(board.get(adjI).get(adjJ).getIsVisible()) {
							foundAdjVis = true;
							break;
						}
					}
				}
				if (foundAdjVis) {
					tempComponents.get(disjointSet.find(getNode(i, j))).add(new Pair<>(i, j));
				}
			}
		}
		ArrayList<ArrayList<Pair<Integer,Integer>>> components = new ArrayList<>();
		for(ArrayList<Pair<Integer,Integer>> component : tempComponents) {
			if(!component.isEmpty()) {
				components.add(component);
			}
		}
		return components;
	}

	public static Integer getNumberOfAwayCells(ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		int cntAwayCells = 0;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(isAwayCell(board, i, j)) {
					++cntAwayCells;
				}
			}
		}
		return cntAwayCells;
	}

	//returns true if cell has no visible neighbors
	public static Boolean isAwayCell(ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board, int row, int col) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if(di == 0 && dj == 0) {
					continue;
				}
				final int adjI = row+di;
				final int adjJ = col+dj;
				if(ArrayBounds.outOfBounds(adjI,adjJ,rows,cols)) {
					continue;
				}
				if(board.get(adjI).get(adjJ).getIsVisible()) {
					return false;
				}
			}
		}
		return true;
	}
}
