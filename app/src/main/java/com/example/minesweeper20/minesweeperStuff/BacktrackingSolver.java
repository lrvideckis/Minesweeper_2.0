package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.Fraction;
import com.example.minesweeper20.helpers.GetConnectedComponents;
import com.example.minesweeper20.helpers.MutableInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: also break out early the moment we find a (conditioned) solution
//TODO: split components by cells we know: like if we know these 4 cells in a row, then we can split it into 2 components
//TODO: handle away cells
//TODO: extra pruning idea: prune out if there's only n spots left adjacent to a clue, and (clue - bombs placed) > n
//TODO: hard code in rules, then split components by logical cells
//TODO: write non-component non-away cell backtracking solver to test
public class BacktrackingSolver implements MinesweeperSolver {

	private Integer rows, cols;
	private final static Integer iterationLimit = 20000;

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
	private final ArrayList<TreeMap<Integer,MutableInt>> bombConfig;
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

		if(GetConnectedComponents.allCellsAreHidden(board)) {
			for(int i = 0; i < rows; ++i) {
				for(int j = 0; j < cols; ++j) {
					board.get(i).get(j).numberOfBombConfigs = numberOfBombs;
					board.get(i).get(j).numberOfTotalConfigs = rows * cols;
				}
			}
			return;
		}

		for(int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs, false);
		}

		removeBombNumbersFromComponent();
		Fraction awayBombProbability = null;
		if(GetConnectedComponents.getNumberOfAwayCells(board) > 0) {
			awayBombProbability = calculateAwayBombProbability();
		}

		for(int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs, true);
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(GetConnectedComponents.isAwayCell(board, i, j)) {
					assert awayBombProbability != null;
					board.get(i).get(j).numberOfBombConfigs = awayBombProbability.getNumerator();
					board.get(i).get(j).numberOfTotalConfigs = awayBombProbability.getDenominator();
				}
				VisibleTile curr = board.get(i).get(j);
				if(curr.getIsVisible()) {
					continue;
				}
				if(curr.numberOfTotalConfigs == 0) {
					throw new Exception("There should be at least one bomb configuration for non-visible cells");
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
			for(int entry : bombConfig.get(i).keySet()) {
				for(int val : dpTable.get(i)) {
					dpTable.get(i+1).add(val + entry);
				}
			}
		}
		TreeSet<Integer> validSpots = new TreeSet<>();
		final int numberOfAwayCells = GetConnectedComponents.getNumberOfAwayCells(board);
		for(int bombCnt : dpTable.get(components.size())) {
			if (bombCnt <= numberOfBombs && numberOfBombs <= bombCnt + numberOfAwayCells) {
				validSpots.add(bombCnt);
			}
		}
		dpTable.get(components.size()).clear();
		dpTable.set(components.size(), validSpots);

		for(int i = components.size()-1; i >= 0; --i) {
			TreeSet<Integer> spotsToRemove = new TreeSet<>();
			for(int entry : bombConfig.get(i).keySet()) {
				boolean found = false;
				for(int val : dpTable.get(i)) {
					if(dpTable.get(i+1).contains(val + entry)) {
						found = true;
						break;
					}
				}
				if(!found) {
					spotsToRemove.add(entry);
				}
			}
			for(int val : spotsToRemove) {
				bombConfig.get(i).remove(val);
			}

			spotsToRemove.clear();
			for(int val : dpTable.get(i)) {
				boolean found = false;
				for(int entry : bombConfig.get(i).keySet()) {
					if(dpTable.get(i+1).contains(val + entry)) {
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

	private Fraction calculateAwayBombProbability() throws Exception {
		TreeMap<Integer,MutableInt> configsPerBombCount = calculateNumberOfBombConfigs();
		int totalNumberOfConfigs = 0;
		for(MutableInt val : configsPerBombCount.values()) {
			totalNumberOfConfigs += val.get();
		}
		Fraction awayBombProbability = new Fraction(0);
		final int numberOfAwayCells = GetConnectedComponents.getNumberOfAwayCells(board);
		for(TreeMap.Entry<Integer,MutableInt> entry : configsPerBombCount.entrySet()) {
			final int currNumberOfBombs = entry.getKey();
			final int numberOfConfigs = entry.getValue().get();
			if(numberOfBombs - currNumberOfBombs < 0 || numberOfBombs - currNumberOfBombs > numberOfAwayCells) {
				throw new Exception("number of remaining bombs is more than number of away cells (or negative)");
			}
			Fraction delta = new Fraction(numberOfConfigs, totalNumberOfConfigs);
			delta.multiplyWith(new Fraction(numberOfBombs - currNumberOfBombs, numberOfAwayCells));
			awayBombProbability.addWith(delta);
		}
		return awayBombProbability;
	}

	private TreeMap<Integer,MutableInt> calculateNumberOfBombConfigs() {
		TreeMap<Integer,MutableInt> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
		prevWays.put(0, new MutableInt(1));

		for(int i = 0; i < components.size(); ++i) {
			for(TreeMap.Entry<Integer,MutableInt> bombVal : bombConfig.get(i).entrySet()) {
				for(TreeMap.Entry<Integer,MutableInt> waysVal : prevWays.entrySet()) {
					final int nextKey = bombVal.getKey() + waysVal.getKey();
					final int nextValueDiff = Math.multiplyExact(bombVal.getValue().get(), waysVal.getValue().get());
					MutableInt nextVal = newWays.get(nextKey);
					if(nextVal == null) {
						newWays.put(nextKey, new MutableInt(nextValueDiff));
					} else {
						nextVal.addWith(nextValueDiff);
					}
				}
			}
			prevWays.clear();
			prevWays.putAll(newWays);
			newWays.clear();
		}
		return prevWays;
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
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs, false);
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
			bombConfig.add(new TreeMap<Integer,MutableInt>());
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
	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfBombs, boolean isSecondPass) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		if(pos == component.size()) {
			if(isSecondPass) {
				checkSolutionSecondPass(componentPos, currNumberOfBombs.get());
			} else {
				checkSolutionFirstPass(componentPos, currNumberOfBombs.get());
			}
			return;
		}
		currIterations.addWith(1);
		if(currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException();
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		isBomb.get(i).set(j, true);
		if(checkSurroundingConditions(i,j,component.get(pos),1)) {
			currNumberOfBombs.addWith(1);
			updateSurroundingBombCnt(i,j,1);
			solveComponent(pos+1, componentPos, currIterations, currNumberOfBombs, isSecondPass);
			updateSurroundingBombCnt(i,j,-1);
			currNumberOfBombs.addWith(-1);
		}

		//try free
		isBomb.get(i).set(j, false);
		if(checkSurroundingConditions(i,j,component.get(pos),0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfBombs, isSecondPass);
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

	private void checkSolutionFirstPass(int componentPos, int currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		//TODO: remove this extra computation once there is sufficient testing
		checkPositionValidity(component, currNumberOfBombs);

		MutableInt count = bombConfig.get(componentPos).get(currNumberOfBombs);
		if(count == null) {
			bombConfig.get(componentPos).put(currNumberOfBombs, new MutableInt(1));
		} else {
			count.addWith(1);
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
	private void checkSolutionSecondPass(int componentPos, int currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		//TODO: remove this extra computation once there is sufficient testing
		checkPositionValidity(component, currNumberOfBombs);

		if(!bombConfig.get(componentPos).containsKey(currNumberOfBombs)) {
			return;
		}
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if (isBomb.get(i).get(j)) {
				++board.get(i).get(j).numberOfBombConfigs;
			}
			++board.get(i).get(j).numberOfTotalConfigs;
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer,Integer>> component, int currNumberOfBombs) throws Exception {
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
		int prevNumberOfBombs = 0;
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if(isBomb.get(i).get(j)) {
				++prevNumberOfBombs;
			}
		}
		if(prevNumberOfBombs != currNumberOfBombs) {
			throw new Exception("number of bombs doesn't match");
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
