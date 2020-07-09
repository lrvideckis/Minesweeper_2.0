package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import android.util.Pair;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class EdgePair {
	public static Pair<MyPair, MyPair> getPairOfEdges(
			final SortedSet<Integer> subComponent,
			final int componentPos,
			final boolean[] isRemoved,
			final ArrayList<ArrayList<SortedSet<Integer>>> adjList
	) throws Exception {
		TreeSet<MyPair> edges = new TreeSet<>();
		for (int node : subComponent) {
			for (int next : adjList.get(componentPos).get(node)) {
				if (!subComponent.contains(next)) {
					continue;
				}
				int u = node;
				int v = next;
				if (u > v) {
					int temp = u;
					u = v;
					v = temp;
				}
				edges.add(new MyPair(u, v));
			}
		}
		Pair<MyPair, MyPair> edgePairWithSmallestLargestComponent = null;
		int smallestLargestComponentSize = (int) 1e9;
		for (MyPair edge1 : edges) {
			for (MyPair edge2 : edges) {
				if (isRemoved[edge1.first] ||
						isRemoved[edge1.second] ||
						isRemoved[edge2.first] ||
						isRemoved[edge2.second]
				) {
					continue;
				}
				isRemoved[edge1.first] = isRemoved[edge1.second] = isRemoved[edge2.first] = isRemoved[edge2.second] = true;
				TreeSet<Integer> visited = new TreeSet<>();
				int numberOfComponents = 0;
				int maxComponentSize = 0;
				for (int node : subComponent) {
					if (isRemoved[node] || visited.contains(node)) {
						continue;
					}
					++numberOfComponents;
					MutableInt componentSize = new MutableInt(0);
					dfs(node, subComponent, componentPos, isRemoved, visited, adjList, componentSize);
					maxComponentSize = Math.max(maxComponentSize, componentSize.get());
				}
				isRemoved[edge1.first] = isRemoved[edge1.second] = isRemoved[edge2.first] = isRemoved[edge2.second] = false;
				if (numberOfComponents == 0) {
					throw new Exception("0 components, but there should be at least 1");
				}
				if (numberOfComponents > 1 && smallestLargestComponentSize > maxComponentSize) {
					smallestLargestComponentSize = maxComponentSize;
					edgePairWithSmallestLargestComponent = new Pair<>(edge1, edge2);
				}
			}
		}
		return edgePairWithSmallestLargestComponent;
	}

	private static void dfs(
			int node,
			final SortedSet<Integer> subComponent,
			final int componentPos,
			final boolean[] isRemoved,
			final TreeSet<Integer> visited,
			final ArrayList<ArrayList<SortedSet<Integer>>> adjList,
			final MutableInt componentSize
	) {
		componentSize.addWith(1);
		if (isRemoved[node]) {
			return;
		}
		visited.add(node);
		for (int next : adjList.get(componentPos).get(node)) {
			if (visited.contains(next) || !subComponent.contains(next)) {
				continue;
			}
			dfs(next, subComponent, componentPos, isRemoved, visited, adjList, componentSize);
		}
	}
}
