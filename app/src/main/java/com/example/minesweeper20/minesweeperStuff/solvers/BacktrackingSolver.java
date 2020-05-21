package com.example.minesweeper20.minesweeperStuff.solvers;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.HitIterationLimitException;
import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetConnectedComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: also break out early the moment we find a (conditioned) solution
//TODO: split components by cells we know: like if we know these 4 cells in a row, then we can split it into 2 components
//TODO: handle away cells
//TODO: extra pruning idea: prune out if there's only n spots left adjacent to a clue, and (clue - bombs placed) > n
public class BacktrackingSolver implements MinesweeperSolver {

	private Integer rows, cols;
	private final static AtomicInteger iterationLimit = new AtomicInteger(20000);

	private final ArrayList<ArrayList<Boolean>> isBomb;
	private final ArrayList<ArrayList<Integer>> cntSurroundingBombs;
	private ArrayList<ArrayList<VisibleTile>> board;
	private final ArrayList<ArrayList<Pair<Integer,Integer>>> lastUnvisitedSpot;
	private ArrayList<ArrayList<Pair<Integer,Integer>>> components;
	private Integer numberOfBombs;

	//variables for saving specific bomb position
	private final ArrayList<ArrayList<Boolean>> saveIsBomb;
	//one hash-map for each component:
	//for each component: we map the number of bombs to a configuration of bombs for that component
	private final ArrayList<HashMap<Integer,ArrayList<Pair<Integer,Integer>>>> bombConfig;
	private Boolean needToCheckSpotCondition, wantBomb, foundBombConfiguration;
	private Integer spotI, spotJ;

	public BacktrackingSolver(int rows, int cols) {
		isBomb = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Boolean> currRow = new ArrayList<>(Collections.nCopies(cols, false));
			isBomb.add(currRow);
		}

		saveIsBomb = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Boolean> currRow = new ArrayList<>(Collections.nCopies(cols, false));
			saveIsBomb.add(currRow);
		}

		cntSurroundingBombs = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Integer> currRow = new ArrayList<>(Collections.nCopies(cols, 0));
			cntSurroundingBombs.add(currRow);
		}

		lastUnvisitedSpot = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Pair<Integer,Integer>> currRow = new ArrayList<>(cols);
			for(int j = 0; j < cols; ++j) {
				currRow.add(null);
			}
			lastUnvisitedSpot.add(currRow);
		}

		bombConfig = new ArrayList<>();
	}

	@Override
	public void solvePosition(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs) throws Exception {
		initialize(_board, _numberOfBombs);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		for(int i = 0; i < components.size(); ++i) {
			//TODO: make these not member variables
			AtomicInteger currIterations = new AtomicInteger(0);
			solveComponent(0, i, currIterations, false);
			System.out.println("here, iterations: " + currIterations);
		}

		removeBombNumbersFromComponent();

		for(int i = 0; i < components.size(); ++i) {
			AtomicInteger currIterations = new AtomicInteger(0);
			solveComponent(0, i, currIterations, true);
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				VisibleTile curr = board.get(i).get(j);
				if(curr.numberOfTotalConfigs == 0) {
					continue;
				}
				if(curr.numberOfBombConfigs == 0) {
					curr.isLogicalFree = true;
				} else if(curr.numberOfBombConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalBomb = true;
				}
			}
		}
	}

	private void removeBombNumbersFromComponent() throws Exception {
		ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size()+1);
		for(int i = 0; i <= components.size(); ++i) {
			dpTable.add(new TreeSet<Integer>());
		}

		dpTable.get(0).add(0);
		for(int i = 0; i < components.size(); ++i) {
			for(Map.Entry<Integer,ArrayList<Pair<Integer,Integer>>> entry : bombConfig.get(i).entrySet()) {
				for(Integer val : dpTable.get(i)) {
					dpTable.get(i+1).add(val + entry.getKey());
				}
			}
		}
		TreeSet<Integer> validSpots = new TreeSet<>();
		for(int bombCnt : dpTable.get(components.size())) {
			if (bombCnt <= numberOfBombs && numberOfBombs <= bombCnt + GetConnectedComponents.getNumberOfAwayCells(board)) {
				validSpots.add(bombCnt);
			}
		}
		dpTable.get(components.size()).clear();
		dpTable.set(components.size(), validSpots);

		for(int i = components.size()-1; i >= 0; --i) {
			TreeSet<Integer> spotsToRemove = new TreeSet<>();
			for(Map.Entry<Integer,ArrayList<Pair<Integer,Integer>>> entry : bombConfig.get(i).entrySet()) {
				boolean found = false;
				for(int val : dpTable.get(i)) {
					if(dpTable.get(i+1).contains(val + entry.getKey())) {
						found = true;
						break;
					}
				}
				if(!found) {
					spotsToRemove.add(entry.getKey());
				}
			}
			for(int val : spotsToRemove) {
				bombConfig.get(i).remove(val);
			}

			spotsToRemove.clear();
			for(int val : dpTable.get(i)) {
				boolean found = false;
				for(Map.Entry<Integer, ArrayList<Pair<Integer, Integer>>> entry : bombConfig.get(i).entrySet()) {
					if(dpTable.get(i+1).contains(val + entry.getKey())) {
						found = true;
						break;
					}
				}
				if(!found) {
					spotsToRemove.add(val);
				}
			}
			for(int val : spotsToRemove) {
				dpTable.get(i).remove(val);
			}
		}
	}

	//TODO: update this to do backtracking twice with dp in the middle
	public ArrayList<ArrayList<Boolean>> getBombConfiguration(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs, int _spotI, int _spotJ, boolean _wantBomb) throws Exception {
		initialize(_board, _numberOfBombs);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		spotI = _spotI;
		spotJ = _spotJ;
		wantBomb = _wantBomb;
		foundBombConfiguration = false;

		for(int i = 0; i < components.size(); ++i) {
			ArrayList<Pair<Integer,Integer>> component = components.get(i);
			needToCheckSpotCondition = false;
			for(Pair<Integer,Integer> spot : component) {
				if(spot.first.equals(spotI) && spot.second.equals(spotJ)) {
					needToCheckSpotCondition = true;
					break;
				}
			}
			AtomicInteger currIterations = new AtomicInteger(0);
			solveComponent(0, i, currIterations, false);
		}
		return (foundBombConfiguration ? saveIsBomb : null);
	}

	private void initialize(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs) throws Exception {
		board = _board;
		numberOfBombs = _numberOfBombs;
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				isBomb.get(i).set(j,false);
				saveIsBomb.get(i).set(j,false);
				cntSurroundingBombs.get(i).set(j,0);
			}
		}
		needToCheckSpotCondition = false;
	}

	private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer,Integer>>> components) {
		bombConfig.clear();
		for(ArrayList<Pair<Integer,Integer>> component : components) {
			bombConfig.add(new HashMap<Integer, ArrayList<Pair<Integer, Integer>>>());
			for (Pair<Integer, Integer> spot : component) {
				for (int di = -1; di <= 1; ++di) {
					for (int dj = -1; dj <= 1; ++dj) {
						if (di == 0 && dj == 0) {
							continue;
						}
						final int adjI = spot.first + di;
						final int adjJ = spot.second + dj;
						if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						if (board.get(adjI).get(adjJ).isVisible) {
							lastUnvisitedSpot.get(adjI).set(adjJ, spot);
						}
					}
				}
			}
		}
	}

	//TODO: only re-run component solve if the component has changed
	private void solveComponent(int pos, int componentPos, AtomicInteger currIterations, boolean isSecondPass) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		if(pos == component.size()) {
			checkSolution(componentPos, isSecondPass);
			return;
		}
		if(currIterations.incrementAndGet() >= iterationLimit.get()) {
			throw new HitIterationLimitException("too many iterations");
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		isBomb.get(i).set(j, true);
		if(checkSurroundingConditions(i,j,component.get(pos),1)) {
			updateSurroundingBombCnt(i,j,1);
			solveComponent(pos+1, componentPos, currIterations, isSecondPass);
			updateSurroundingBombCnt(i,j,-1);
		}

		//try free
		isBomb.get(i).set(j, false);
		if(checkSurroundingConditions(i,j,component.get(pos),0)) {
			solveComponent(pos + 1, componentPos, currIterations, isSecondPass);
		}
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

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer,Integer> currSpot, int arePlacingABomb) {
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
				VisibleTile adjTile = board.get(adjI).get(adjJ);
				if(!adjTile.isVisible) {
					continue;
				}
				final int currBacktrackingCount = cntSurroundingBombs.get(adjI).get(adjJ);
				if(currBacktrackingCount + arePlacingABomb > adjTile.numberSurroundingBombs) {
					return false;
				}
				if(lastUnvisitedSpot.get(adjI).get(adjJ).equals(currSpot) && currBacktrackingCount + arePlacingABomb != adjTile.numberSurroundingBombs) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkSolution(int componentPos, boolean isSecondPass) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		checkPositionValidity(component);

		//TODO: keep a running total of bombs instead of doing this
		int currNumberOfBombs = 0;
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if(isBomb.get(i).get(j)) {
				++currNumberOfBombs;
			}
		}

		if(isSecondPass && !bombConfig.get(componentPos).containsKey(currNumberOfBombs)) {
			return;
		}

		if(isSecondPass) {
			for (int pos = 0; pos < component.size(); ++pos) {
				final int i = component.get(pos).first;
				final int j = component.get(pos).second;
				if (isBomb.get(i).get(j)) {
					++board.get(i).get(j).numberOfBombConfigs;
				}
				++board.get(i).get(j).numberOfTotalConfigs;
			}
		}

		if(isSecondPass) {
			return;
		}

		if(!bombConfig.get(componentPos).containsKey(currNumberOfBombs)) {
			ArrayList<Pair<Integer,Integer>> currBombConfig = new ArrayList<>();
			for(int pos = 0; pos < component.size(); ++pos) {
				final int i = component.get(pos).first;
				final int j = component.get(pos).second;
				if(isBomb.get(i).get(j)) {
					currBombConfig.add(component.get(pos));
				}
			}
			bombConfig.get(componentPos).put(currNumberOfBombs, currBombConfig);
		}

		if(!needToCheckSpotCondition || isBomb.get(spotI).get(spotJ) == wantBomb) {
			if(needToCheckSpotCondition) {
				foundBombConfiguration = true;
			}
			for(int pos = 0; pos < component.size(); ++pos) {
				final int i = component.get(pos).first;
				final int j = component.get(pos).second;
				saveIsBomb.get(i).set(j, isBomb.get(i).get(j));
			}
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer,Integer>> component) throws Exception {
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
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
					VisibleTile adjTile = board.get(adjI).get(adjJ);
					if(!adjTile.isVisible) {
						continue;
					}
					if(!cntSurroundingBombs.get(adjI).get(adjJ).equals(adjTile.numberSurroundingBombs)) {
						throw new Exception("found bad solution - # bombs doesn't match, but this should be pruned out");
					}
				}
			}
		}
	}

	/*
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
	 */
}
