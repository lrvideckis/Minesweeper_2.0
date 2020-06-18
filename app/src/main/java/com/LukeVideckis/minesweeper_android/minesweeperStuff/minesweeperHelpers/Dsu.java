package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.Arrays;

public class Dsu {
	private final int[] parent;

	Dsu(int size) {
		parent = new int[size];
		Arrays.fill(parent, -1);
	}

	public static int getNode(int i, int j, int rows, int cols) {
		if (ArrayBounds.outOfBounds(i, j, rows, cols)) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i * cols + j;
	}

	public int find(int node) {
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
