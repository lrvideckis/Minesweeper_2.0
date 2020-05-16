package com.example.minesweeper20.minesweeperStuff.helpers;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;
import java.util.Collections;

public class GetConnectedComponents {
	private static class dsu {
		dsu(int size) {
			parent = new ArrayList<>(Collections.nCopies(size, -1));
		}
		ArrayList<Integer> parent;
		int find(int node) {
			return parent.get(node) < 0 ? node : parent.set(node, find(parent.get(node)));
		}
		void merge(int x, int y) {
			if((x=find(x)) == (y=find(y))) return;
			if(parent.get(y) < parent.get(x)) {
				int temp = x;
				x = y;
				y = temp;
			}
			parent.set(x, parent.get(x) + parent.get(y));
			parent.set(y,x);
		}
	}

	private static Integer rows, cols;

	private static int getNode(int i, int j) {
		if(i < 0 || j < 0 || i >= rows || j >= cols) {
			throw new ArrayIndexOutOfBoundsException("throwing from getConnectedComponents.getNode()");
		}
		return i*cols + j;
	}

	//TODO: play around with the order of cells in a single CC to make the backtracking faster
	public static ArrayList<ArrayList<Pair<Integer,Integer>>> getComponents(ArrayList<ArrayList<MinesweeperSolver.visibleTile>> board) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		dsu disjointSet = new dsu(rows * cols);
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(!board.get(i).get(j).isVisible) {
					continue;
				}
				for(int di = -1; di <= 1; ++di) {
					for(int dj = -1; dj <= 1; ++dj) {
						if(di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i + di;
						final int adjJ = j + dj;
						if(!ArrayBounds.inBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						MinesweeperSolver.visibleTile adjTile = board.get(adjI).get(adjJ);
						if(adjTile.isVisible) {
							continue;
						}
						disjointSet.merge(getNode(i,j),getNode(adjI,adjJ));
					}
				}
			}
		}
		ArrayList<ArrayList<Pair<Integer,Integer>>> tempComponents = new ArrayList<>(rows * cols);
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				MinesweeperSolver.visibleTile currTile = board.get(i).get(j);
				if (!currTile.isVisible) {
					continue;
				}
				boolean foundAdjVis = false;
				for(int di = -1; di <= 1 && !foundAdjVis; ++di) {
					for(int dj = -1; dj <= 1 && !foundAdjVis; ++dj) {
						if(di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i + di;
						final int adjJ = j + dj;
						if(!ArrayBounds.inBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						if(board.get(adjI).get(adjJ).isVisible) {
							foundAdjVis = true;
						}
					}
				}
				if(!foundAdjVis) {
					continue;
				}
				tempComponents.get(disjointSet.find(getNode(i,j))).add(new Pair<>(i,j));
			}
		}
		ArrayList<ArrayList<Pair<Integer,Integer>>> components = new ArrayList<>();
		for(int i = 0; i < rows*cols; ++i) {
			if(tempComponents.get(i).isEmpty()) {
				continue;
			}
			components.add(tempComponents.get(i));
		}
		return components;
	}
}
