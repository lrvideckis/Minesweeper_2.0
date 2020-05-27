package com.example.minesweeper20.helpers;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class GetConnectedComponents {
	private static class Dsu {
		private final int[] parent;
		Dsu(int size) {
			parent = new int[size];
			Arrays.fill(parent, -1);
		}
		int find(int node) {
			if(parent[node] < 0) {
				return node;
			}
			return parent[node] = find(parent[node]);
		}
		void merge(int x, int y) {
			if((x=find(x)) == (y=find(y))) return;
			if(parent[y] < parent[x]) {
				int temp = x;
				//noinspection SuspiciousNameCombination
				x = y;
				y = temp;
			}
			parent[x] += parent[y];
			parent[y] = x;
		}
	}

	private static int rows, cols;

	private static int getNode(int i, int j) {
		if(ArrayBounds.outOfBounds(i,j,rows,cols)) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i*cols + j;
	}

	//TODO: play around with the order of cells in a single CC to make the backtracking faster
	public static ArrayList<ArrayList<Pair<Integer,Integer>>> getComponents(VisibleTile[][] board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		Dsu disjointSet = new Dsu(rows * cols);
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(!board[i][j].getIsVisible()) {
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
						VisibleTile adjTile = board[adjI][adjJ];
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
				VisibleTile currTile = board[i][j];
				if (currTile.getIsVisible()) {
					continue;
				}
				if(!isAwayCell(board, i, j, rows, cols)) {
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

	public static int getNumberOfAwayCells(VisibleTile[][] board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		int cntAwayCells = 0;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(isAwayCell(board, i, j, rows, cols)) {
					++cntAwayCells;
				}
			}
		}
		return cntAwayCells;
	}

	//returns true if cell has no visible neighbors
	public static boolean isAwayCell(VisibleTile[][] board, int row, int col, int rows, int cols) {
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				final int adjI = row+di;
				final int adjJ = col+dj;
				if(ArrayBounds.outOfBounds(adjI,adjJ,rows,cols)) {
					continue;
				}
				if(board[adjI][adjJ].getIsVisible()) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean allCellsAreHidden(VisibleTile[][] board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for(int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if(board[i][j].getIsVisible()) {
					return false;
				}
			}
		}
		return true;
	}
}
