package com.example.minesweeper20.helpers;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class GetConnectedComponents {
	private static int rows, cols;

	private static int getNode(int i, int j) {
		if (ArrayBounds.outOfBounds(i, j, rows, cols)) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i * cols + j;
	}

	public static ArrayList<ArrayList<Pair<Integer, Integer>>> getComponents(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		Dsu disjointSet = new Dsu(rows * cols);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!board[i][j].getIsVisible()) {
					continue;
				}
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					VisibleTile adjTile = board[adjI][adjJ];
					if (adjTile.getIsVisible()) {
						continue;
					}
					disjointSet.merge(getNode(i, j), getNode(adjI, adjJ));
				}
			}
		}
		ArrayList<ArrayList<Pair<Integer, Integer>>> tempComponents = new ArrayList<>(rows * cols);
		for (int i = 0; i < rows * cols; ++i) {
			tempComponents.add(new ArrayList<Pair<Integer, Integer>>());
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile currTile = board[i][j];
				if (currTile.getIsVisible()) {
					continue;
				}
				if (!AwayCell.isAwayCell(board, i, j, rows, cols)) {
					tempComponents.get(disjointSet.find(getNode(i, j))).add(new Pair<>(i, j));
				}
			}
		}
		ArrayList<ArrayList<Pair<Integer, Integer>>> components = new ArrayList<>();
		for (ArrayList<Pair<Integer, Integer>> component : tempComponents) {
			if (!component.isEmpty()) {
				components.add(component);
			}
		}
		return components;
	}

	public static ArrayList<ArrayList<Pair<Integer, Integer>>> getComponentsWithKnownCells(VisibleTile[][] board) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		Dsu disjointSet = new Dsu(rows * cols);
		boolean[][] unknownStatusSpot = new boolean[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (!board[i][j].getIsVisible()) {
					continue;
				}
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					VisibleTile adjTile = board[adjI][adjJ];
					if (adjTile.getIsVisible() || adjTile.getIsLogicalBomb() || adjTile.getIsLogicalFree()) {
						continue;
					}
					disjointSet.merge(getNode(i, j), getNode(adjI, adjJ));
					unknownStatusSpot[adjI][adjJ] = true;
				}
			}
		}
		boolean[][] visited = new boolean[rows][cols];
		ArrayList<ArrayList<Pair<Integer, Integer>>> components = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (visited[i][j] || !unknownStatusSpot[i][j]) {
					continue;
				}
				ArrayList<Pair<Integer, Integer>> component = new ArrayList<>();
				dfs(i, j, component, visited, unknownStatusSpot, disjointSet.find(getNode(i, j)), disjointSet);
				components.add(component);
			}
		}
		return components;
	}

	private static void dfs(
			int i,
			int j,
			ArrayList<Pair<Integer, Integer>> component,
			boolean[][] visited,
			boolean[][] unknownStatusSpot,
			int ccId,
			Dsu disjointSet
	) {
		component.add(new Pair<>(i, j));
		visited[i][j] = true;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccId != disjointSet.find(getNode(adjI, adjJ))) {
				continue;
			}
			dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccId, disjointSet);
		}
		for (int di = -2; di <= 2; ++di) {
			for (int dj = -2; dj <= 2; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				if (visited[adjI][adjJ] || !unknownStatusSpot[adjI][adjJ] || ccId != disjointSet.find(getNode(adjI, adjJ))) {
					continue;
				}
				dfs(adjI, adjJ, component, visited, unknownStatusSpot, ccId, disjointSet);
			}
		}
	}

	private static class Dsu {
		private final int[] parent;

		Dsu(int size) {
			parent = new int[size];
			Arrays.fill(parent, -1);
		}

		int find(int node) {
			if (parent[node] < 0) {
				return node;
			}
			return parent[node] = find(parent[node]);
		}

		void merge(int x, int y) {
			if ((x = find(x)) == (y = find(y))) return;
			if (parent[y] < parent[x]) {
				int temp = x;
				//noinspection SuspiciousNameCombination
				x = y;
				y = temp;
			}
			parent[x] += parent[y];
			parent[y] = x;
		}
	}
}
