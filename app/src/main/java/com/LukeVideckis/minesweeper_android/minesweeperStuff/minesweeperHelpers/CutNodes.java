package com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers;

import java.util.ArrayList;
import java.util.SortedSet;

public class CutNodes {
	final MutableInt currTime;
	final GetSubComponentByRemovedNodes getSubComponentByRemovedNodes;
	private final SortedSet<Integer> nodes;
	private final ArrayList<SortedSet<Integer>> adjList;
	private final int[] minTime, timeIn;
	private final boolean[] visited, isRemoved;

	public CutNodes(SortedSet<Integer> nodes, ArrayList<SortedSet<Integer>> adjList, boolean[] isRemoved) {
		this.nodes = nodes;
		this.adjList = adjList;
		this.isRemoved = isRemoved;

		//initialize variables for finding cut nodes
		minTime = new int[isRemoved.length];
		timeIn = new int[isRemoved.length];
		visited = new boolean[isRemoved.length];
		for (int i = 0; i < isRemoved.length; ++i) {
			minTime[i] = timeIn[i] = isRemoved.length + 10;
			visited[i] = false;
		}
		currTime = new MutableInt(0);
		getSubComponentByRemovedNodes = new GetSubComponentByRemovedNodes(nodes, adjList, isRemoved);
	}

	//when calling this multiple times on the same component, this will only give correct results the first time
	//this is because I don't re-initialize the member variables for finding cut nodes
	//this returns an empty ArrayList when called more than once on the same component
	public ArrayList<Integer> getCutNodes(int startNode) throws Exception {
		if (isRemoved[startNode]) {
			throw new Exception("start node is removed");
		}
		if (!nodes.contains(startNode)) {
			throw new Exception("start node isn't in list of nodes");
		}
		ArrayList<Integer> allCutNodes = new ArrayList<>();
		if (visited[startNode]) {
			return allCutNodes;
		}
		SortedSet<Integer> component = getSubComponentByRemovedNodes.getSubComponent(startNode);
		dfsCutNodes(startNode, startNode, allCutNodes, component);
		for (int node : component) {
			if (isRemoved[node]) {
				visited[node] = false;
			}
		}
		return allCutNodes;
	}

	private void dfsCutNodes(final int node, final int prev, ArrayList<Integer> allCutNodes, SortedSet<Integer> component) throws Exception {
		if (!component.contains(node)) {
			throw new Exception("component doesn't contain node");
		}
		visited[node] = true;
		currTime.addWith(1);
		timeIn[node] = minTime[node] = currTime.get();
		int numChildren = 0;
		for (int to : adjList.get(node)) {
			if (!component.contains(to)) {
				continue;
			}
			if (to != prev) {
				minTime[node] = Math.min(minTime[node], timeIn[to]);
			}
			if (visited[to]) continue;
			numChildren++;
			if (node == prev && numChildren > 1) {
				allCutNodes.add(node);
			}
			dfsCutNodes(to, node, allCutNodes, component);
			minTime[node] = Math.min(minTime[node], minTime[to]);
			if (node != prev && minTime[to] >= timeIn[node]) {
				allCutNodes.add(node);
			}
		}
	}
}
