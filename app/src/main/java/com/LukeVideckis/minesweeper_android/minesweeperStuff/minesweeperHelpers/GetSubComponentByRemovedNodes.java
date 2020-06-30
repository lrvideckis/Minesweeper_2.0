package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class GetSubComponentByRemovedNodes {

	private final SortedSet<Integer> nodes;
	private final ArrayList<SortedSet<Integer>> adjList;
	private final boolean[] isRemoved;
	private final boolean[] visited;

	public GetSubComponentByRemovedNodes(SortedSet<Integer> nodes, ArrayList<SortedSet<Integer>> adjList, boolean[] isRemoved) {
		this.nodes = nodes;
		this.adjList = adjList;
		this.isRemoved = isRemoved;
		visited = new boolean[isRemoved.length];
		for (int i = 0; i < isRemoved.length; ++i) {
			visited[i] = false;
		}
	}

	//if called twice on the same component, this will return an empty array
	public SortedSet<Integer> getSubComponent(final int startNode) throws Exception {
		if (isRemoved[startNode]) {
			throw new Exception("start node is removed");
		}
		if (!nodes.contains(startNode)) {
			throw new Exception("start node isn't in list of nodes");
		}
		TreeSet<Integer> component = new TreeSet<>();
		if (visited[startNode]) {
			return Collections.unmodifiableSortedSet(component);
		}
		dfs(startNode, component, nodes);
		for (int node : component) {
			if (isRemoved[node]) {
				visited[node] = false;
			}
		}
		return Collections.unmodifiableSortedSet(component);
	}

	private void dfs(final int node, TreeSet<Integer> component, SortedSet<Integer> nodes) {
		component.add(node);
		visited[node] = true;
		if (isRemoved[node]) {
			return;
		}
		for (int to : adjList.get(node)) {
			if (nodes.contains(to) && !visited[to]) {
				dfs(to, component, nodes);
			}
		}
	}
}
