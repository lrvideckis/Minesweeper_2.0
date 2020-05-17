package com.example.minesweeper20.minesweeperStuff.solvers;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.helpers.ArrayBounds;
import com.example.minesweeper20.minesweeperStuff.helpers.GetConnectedComponents;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;
import java.util.Collections;

public class BacktrackingSolver implements MinesweeperSolver {

	private Integer rows, cols;

	private final ArrayList<ArrayList<Boolean>> isBomb;
	private final ArrayList<ArrayList<Integer>> cntSurroundingBombs;
	private ArrayList<ArrayList<VisibleTile>> board;
	private Integer numberOfBombs;

	public BacktrackingSolver(int rows, int cols) {
		isBomb = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Boolean> currRow = new ArrayList<>(Collections.nCopies(cols, false));
			isBomb.add(currRow);
		}

		cntSurroundingBombs = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Integer> currRow = new ArrayList<>(Collections.nCopies(cols, 0));
			cntSurroundingBombs.add(currRow);
		}
	}

	@Override
	public void solvePosition(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs) throws Exception {
		board = _board;
		numberOfBombs = _numberOfBombs;
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				isBomb.get(i).set(j,false);
				cntSurroundingBombs.get(i).set(j,0);
			}
		}

		ArrayList<ArrayList<Pair<Integer,Integer>>> components = GetConnectedComponents.getComponents(board);
		for(ArrayList<Pair<Integer,Integer>> component : components) {
			solveComponent(0, component);
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				VisibleTile curr = board.get(i).get(j);
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

	//TODO: only re-run component solve if the component has changed
	private void solveComponent(int pos, ArrayList<Pair<Integer,Integer>> component) throws Exception {
		if(pos == component.size()) {
			checkSolution(component);
			return;
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		isBomb.get(i).set(j, true);
		if(checkSurroundingConditions(i,j)) {
			updateSurroundingBombCnt(i,j,1);
			solveComponent(pos+1, component);
			updateSurroundingBombCnt(i,j,-1);
		}

		//try free
		isBomb.get(i).set(j, false);
		solveComponent(pos+1, component);
	}

	private void updateSurroundingBombCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				if(board.get(adjI).get(adjJ).isVisible) {
					foundAdjVis = true;
					final int cnt = cntSurroundingBombs.get(adjI).get(adjJ);
					cntSurroundingBombs.get(adjI).set(adjJ, cnt + delta);
				}
			}
		}
		if(!foundAdjVis) {
			throw new Exception("hidden cell with no adjacent visible cell");
		}
	}

	private boolean checkSurroundingConditions(int i, int j) {
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if(ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				MinesweeperSolver.VisibleTile adjTile = board.get(adjI).get(adjJ);
				if(!adjTile.isVisible) {
					continue;
				}
				if(cntSurroundingBombs.get(adjI).get(adjJ) + 1 > adjTile.numberSurroundingBombs) {
					return false;
				}
				//TODO: add check for equality if cell (i,j) is the last cell adjacent to (adjI,adjJ)
			}
		}
		return true;
	}

	private void printBoardDebug() {
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(board.get(i).get(j).isVisible) {
					if(board.get(i).get(j).numberSurroundingBombs == 0) {
						System.out.print('.');
					} else {
						System.out.print(board.get(i).get(j).numberSurroundingBombs);
					}
				} else if(isBomb.get(i).get(j)){
					System.out.print('B');
				} else {
					System.out.print('F');
				}
			}
			System.out.println();
		}
		System.out.println();
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
					final int adjI = i + di;
					final int adjJ = j + dj;
					if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
						continue;
					}
					VisibleTile adjTile = board.get(adjI).get(adjJ);
					if(!adjTile.isVisible) {
						continue;
					}
					if(!cntSurroundingBombs.get(adjI).get(adjJ).equals(adjTile.numberSurroundingBombs)) {
						return;
					}
				}
			}
		}
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if(!isBomb.get(i).get(j)) {
				++board.get(i).get(j).numberOfFreeConfigs;
			}
			++board.get(i).get(j).numberOfTotalConfigs;
		}
	}
}
