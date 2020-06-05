package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.customExceptions.HitIterationLimitException;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.GetConnectedComponents;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: also break out early the moment we find a (conditioned) solution
//TODO: split components by cells we know: like if we know these 4 cells in a row, then we can split it into 2 components
//TODO: extra pruning idea: prune out if there's only n spots left adjacent to a clue, and (clue - mines placed) > n
//TODO: hard code in rules, then split components by logical cells
public class BacktrackingSolver implements MinesweeperSolver {

	private final static int iterationLimit = 20000;
	private final int rows, cols;
	private final boolean[][] isMine;
	private final int[][] cntSurroundingMines, updatedNumberSurroundingMines;
	private final int[][][] lastUnvisitedSpot;
	private final ArrayList<TreeMap<Integer, MutableInt>> mineConfig;
	//TODO: remove mineProbPerCompPerNumMines denominator, and use mineConfig instead
	private final ArrayList<TreeMap<Integer, ArrayList<Pair<MutableInt, MutableInt>>>> mineProbPerCompPerNumMines;
	private final ArrayList<TreeMap<Integer, TreeMap<Integer, BigFraction>>> numberOfConfigsForCurrent;
	private final GaussianEliminationSolver gaussianEliminationSolver;
	private int totalIterations;
	private VisibleTile[][] board;
	private ArrayList<ArrayList<Pair<Integer, Integer>>> components;
	private int numberOfMines;

	public BacktrackingSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		isMine = new boolean[rows][cols];
		cntSurroundingMines = new int[rows][cols];
		updatedNumberSurroundingMines = new int[rows][cols];
		lastUnvisitedSpot = new int[rows][cols][2];
		mineConfig = new ArrayList<>();
		numberOfConfigsForCurrent = new ArrayList<>();
		gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);
		mineProbPerCompPerNumMines = new ArrayList<>();
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					board[i][j].numberOfMineConfigs.setValues(numberOfMines, 1);
					board[i][j].numberOfTotalConfigs.setValues(rows * cols, 1);
				}
			}
			return;
		}

		gaussianEliminationSolver.solvePosition(board, numberOfMines);

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible() && (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree())) {
					throw new Exception("visible cells can't be logical frees/mines");
				}
				if (board[i][j].getIsLogicalMine()) {
					--numberOfMines;
					board[i][j].numberOfMineConfigs.setValues(1, 1);
					board[i][j].numberOfTotalConfigs.setValues(1, 1);
				} else if (board[i][j].getIsLogicalFree()) {
					board[i][j].numberOfMineConfigs.setValues(0, 1);
					board[i][j].numberOfTotalConfigs.setValues(1, 1);
				}
				if (board[i][j].getIsVisible()) {
					updatedNumberSurroundingMines[i][j] = board[i][j].getNumberSurroundingMines();
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						VisibleTile adjCell = board[adj[0]][adj[1]];
						if (adjCell.getIsLogicalMine()) {
							--updatedNumberSurroundingMines[i][j];
						}
					}
				}
			}
		}

		initialize(board, numberOfMines);
		components = GetConnectedComponents.getComponentsWithKnownCells(board);
		initializeLastUnvisitedSpot(components);

		//TODO: look into running this loop in parallel
		totalIterations = 0;
		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines);
			totalIterations += currIterations.get();
		}

		removeMineNumbersFromComponent();
		BigFraction awayMineProbability = null;
		if (AwayCell.getNumberOfAwayCells(board) > 0) {
			awayMineProbability = calculateAwayMineProbability();
		}
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		updateNumberOfConfigsForCurrent();


		TreeMap<Integer, BigFraction> configsPerMineCount = calculateNumberOfMineConfigs();

		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, ArrayList<Pair<MutableInt, MutableInt>>> entry : mineProbPerCompPerNumMines.get(i).entrySet()) {
				final int mines = entry.getKey();
				final ArrayList<Pair<MutableInt, MutableInt>> mineProbPerSpot = entry.getValue();

				TreeMap<Integer, BigFraction> configsPerMine = numberOfConfigsForCurrent.get(i).get(mines);
				BigFraction currWeight = new BigFraction(0);
				for (TreeMap.Entry<Integer, BigFraction> currEntry : Objects.requireNonNull(configsPerMine).entrySet()) {
					BigFraction currTerm = new BigFraction(0);
					for (TreeMap.Entry<Integer, BigFraction> total : configsPerMineCount.entrySet()) {
						BigFraction delta = MyMath.BinomialCoefficientFraction(numberOfAwayCells, numberOfMines - total.getKey(), numberOfMines - currEntry.getKey());
						delta.multiplyWith(total.getValue());

						currTerm.addWith(delta);
					}

					currTerm.invert();
					currTerm.multiplyWith(currEntry.getValue());

					currWeight.addWith(currTerm);
				}

				for (int j = 0; j < components.get(i).size(); ++j) {
					final int numerator = mineProbPerSpot.get(j).first.get();
					final int row = components.get(i).get(j).first;
					final int col = components.get(i).get(j).second;

					BigFraction delta = new BigFraction(numerator);
					delta.multiplyWith(currWeight);
					board[row][col].numberOfMineConfigs.addWith(delta);
				}
			}
		}


		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile curr = board[i][j];
				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					if (awayMineProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					curr.numberOfMineConfigs.setValue(awayMineProbability);
					curr.numberOfTotalConfigs.setValues(1, 1);
					continue;
				}
				if (curr.getIsVisible() || curr.getIsLogicalMine() || curr.getIsLogicalFree()) {
					continue;
				}
				//TODO: remove total configs, and just have a single BigFraction
				curr.numberOfTotalConfigs.setValues(1, 1);
				if (curr.numberOfMineConfigs.equals(0)) {
					curr.isLogicalFree = true;
				} else if (curr.numberOfMineConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalMine = true;
				}
			}
		}
	}

	//TODO: this can be optimized a dimension by storing prefixes and suffixes of the dp table
	//for each component, and for each # mines: this calculates the number of mine configurations, and saves it in numberOfConfigsForCurrent
	private void updateNumberOfConfigsForCurrent() throws Exception {
		for (int i = 0; i < components.size(); ++i) {
			TreeMap<Integer, MutableInt> saveMineConfigs = new TreeMap<>(mineConfig.get(i));
			if (!numberOfConfigsForCurrent.get(i).isEmpty()) {
				throw new Exception("numberOfConfigsForCurrent should be cleared from previous run, but isn't");
			}
			for (TreeMap.Entry<Integer, MutableInt> entry : saveMineConfigs.entrySet()) {
				mineConfig.get(i).clear();
				mineConfig.get(i).put(entry.getKey(), new MutableInt(1));
				numberOfConfigsForCurrent.get(i).put(entry.getKey(), calculateNumberOfMineConfigs());
			}
			mineConfig.get(i).clear();
			mineConfig.set(i, saveMineConfigs);
		}
	}

	private void removeMineNumbersFromComponent() throws Exception {
		ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size() + 1);
		for (int i = 0; i <= components.size(); ++i) {
			dpTable.add(new TreeSet<Integer>());
		}

		dpTable.get(0).add(0);
		for (int i = 0; i < components.size(); ++i) {
			for (int entry : mineConfig.get(i).keySet()) {
				for (int val : dpTable.get(i)) {
					dpTable.get(i + 1).add(val + entry);
				}
			}
		}
		TreeSet<Integer> validSpots = new TreeSet<>();
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		for (int mineCnt : dpTable.get(components.size())) {
			if (mineCnt <= numberOfMines && numberOfMines <= mineCnt + numberOfAwayCells) {
				validSpots.add(mineCnt);
			}
		}
		dpTable.get(components.size()).clear();
		dpTable.set(components.size(), validSpots);

		for (int i = components.size() - 1; i >= 0; --i) {
			TreeSet<Integer> spotsToRemove = new TreeSet<>();
			for (int entry : mineConfig.get(i).keySet()) {
				boolean found = false;
				for (int val : dpTable.get(i)) {
					if (dpTable.get(i + 1).contains(val + entry)) {
						found = true;
						break;
					}
				}
				if (!found) {
					spotsToRemove.add(entry);
				}
			}
			for (int val : spotsToRemove) {
				mineConfig.get(i).remove(val);
				mineProbPerCompPerNumMines.get(i).remove(val);
			}

			spotsToRemove.clear();
			for (int val : dpTable.get(i)) {
				boolean found = false;
				for (int entry : mineConfig.get(i).keySet()) {
					if (dpTable.get(i + 1).contains(val + entry)) {
						found = true;
						break;
					}
				}
				if (!found) {
					spotsToRemove.add(val);
				}
			}
			for (int val : spotsToRemove) {
				dpTable.get(i).remove(val);
			}
		}
	}

	private BigFraction calculateAwayMineProbability() throws Exception {
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		TreeMap<Integer, BigFraction> configsPerMineCount = calculateNumberOfMineConfigs();
		BigFraction awayMineProbability = new BigFraction(0);
		for (TreeMap.Entry<Integer, BigFraction> entry : configsPerMineCount.entrySet()) {
			if (numberOfMines - entry.getKey() < 0 || numberOfMines - entry.getKey() > numberOfAwayCells) {
				throw new Exception("number of remaining mines is more than number of away cells (or negative)");
			}

			//calculate # configs / # total configs - the probability that the configuration of mines has the current specific # of mines
			BigFraction numberOfConfigs = new BigFraction(0);
			for (TreeMap.Entry<Integer, BigFraction> val : configsPerMineCount.entrySet()) {
				BigFraction currDelta = MyMath.BinomialCoefficientFraction(numberOfAwayCells, numberOfMines - val.getKey(), numberOfMines - entry.getKey());
				currDelta.multiplyWith(val.getValue());
				numberOfConfigs.addWith(currDelta);
			}
			numberOfConfigs.invert();
			numberOfConfigs.multiplyWith(entry.getValue());

			//actual probability that a single away cell is a mine, the above is just a weight - "how often is this probability the case - # configs / # total configs"
			numberOfConfigs.multiplyWith(numberOfMines - entry.getKey(), numberOfAwayCells);

			awayMineProbability.addWith(numberOfConfigs);
		}
		return awayMineProbability;
	}

	private TreeMap<Integer, BigFraction> calculateNumberOfMineConfigs() throws Exception {
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		TreeMap<Integer, BigFraction> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
		prevWays.put(0, new BigFraction(1));
		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, MutableInt> mineVal : mineConfig.get(i).entrySet()) {
				for (TreeMap.Entry<Integer, BigFraction> waysVal : prevWays.entrySet()) {
					final int nextKey = mineVal.getKey() + waysVal.getKey();
					BigFraction nextValueDiff = new BigFraction(mineVal.getValue().get());
					nextValueDiff.multiplyWith(waysVal.getValue());
					if (i + 1 == components.size() && (nextKey > numberOfMines || nextKey + numberOfAwayCells < numberOfMines)) {
						continue;
					}
					BigFraction nextVal = newWays.get(nextKey);
					if (nextVal == null) {
						newWays.put(nextKey, nextValueDiff);
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
	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {
		throw new Exception("not implemented");
		/*
		initialize(board, numberOfMines);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		this.spotI = spotI;
		this.spotJ = spotJ;
		this.wantMine = wantMine;
		foundMineConfiguration = false;

		for (int i = 0; i < components.size(); ++i) {
			ArrayList<Pair<Integer, Integer>> component = components.get(i);
			needToCheckSpotCondition = false;
			for (Pair<Integer, Integer> spot : component) {
				if (spot.first.equals(spotI) && spot.second.equals(spotJ)) {
					needToCheckSpotCondition = true;
					break;
				}
			}
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines, false);
		}
		return (foundMineConfiguration ? saveIsMine : null);
		 */
	}

	private void initialize(VisibleTile[][] board, int numberOfMines) throws Exception {
		this.board = board;
		this.numberOfMines = numberOfMines;
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				isMine[i][j] = false;
				cntSurroundingMines[i][j] = 0;
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer, Integer>>> components) {
		mineConfig.clear();
		numberOfConfigsForCurrent.clear();
		mineProbPerCompPerNumMines.clear();
		for (ArrayList<Pair<Integer, Integer>> component : components) {
			mineConfig.add(new TreeMap<Integer, MutableInt>());
			numberOfConfigsForCurrent.add(new TreeMap<Integer, TreeMap<Integer, BigFraction>>());
			mineProbPerCompPerNumMines.add(new TreeMap<Integer, ArrayList<Pair<MutableInt, MutableInt>>>());
			for (Pair<Integer, Integer> spot : component) {
				for (int[] adj : GetAdjacentCells.getAdjacentCells(spot.first, spot.second, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].isVisible) {
						lastUnvisitedSpot[adjI][adjJ][0] = spot.first;
						lastUnvisitedSpot[adjI][adjJ][1] = spot.second;
					}
				}
			}
		}
	}

	//TODO: only re-run component solve if the component has changed
	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (pos == component.size()) {
			handleSolution(componentPos, currNumberOfMines.get());
			return;
		}
		currIterations.addWith(1);
		if (currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException();
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try mine
		isMine[i][j] = true;
		if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
			currNumberOfMines.addWith(1);
			updateSurroundingMineCnt(i, j, 1);
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines);
			updateSurroundingMineCnt(i, j, -1);
			currNumberOfMines.addWith(-1);
		}

		//try free
		isMine[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines);
		}
	}

	private void updateSurroundingMineCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			if (board[adj[0]][adj[1]].isVisible) {
				foundAdjVis = true;
				cntSurroundingMines[adj[0]][adj[1]] += delta;
			}
		}
		if (!foundAdjVis) {
			throw new Exception("hidden cell with no adjacent visible cell");
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingAMine) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			VisibleTile adjTile = board[adjI][adjJ];
			if (!adjTile.isVisible) {
				continue;
			}
			final int currBacktrackingCount = cntSurroundingMines[adjI][adjJ];
			if (currBacktrackingCount + arePlacingAMine > updatedNumberSurroundingMines[adjI][adjJ]) {
				return false;
			}
			if (lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
					lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
					currBacktrackingCount + arePlacingAMine != updatedNumberSurroundingMines[adjI][adjJ]) {
				return false;
			}
		}
		return true;
	}

	private void handleSolution(int componentPos, int currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		//TODO: remove this extra computation once there is sufficient testing
		checkPositionValidity(component, currNumberOfMines);

		MutableInt count = mineConfig.get(componentPos).get(currNumberOfMines);
		if (count == null) {
			mineConfig.get(componentPos).put(currNumberOfMines, new MutableInt(1));
		} else {
			count.addWith(1);
		}

		if (!mineProbPerCompPerNumMines.get(componentPos).containsKey(currNumberOfMines)) {
			ArrayList<Pair<MutableInt, MutableInt>> currSpotsArray = new ArrayList<>(component.size());
			for (int i = 0; i < component.size(); ++i) {
				currSpotsArray.add(new Pair<>(new MutableInt(0), new MutableInt(0)));
			}
			mineProbPerCompPerNumMines.get(componentPos).put(currNumberOfMines, currSpotsArray);
		}
		ArrayList<Pair<MutableInt, MutableInt>> currArrayList = Objects.requireNonNull(mineProbPerCompPerNumMines.get(componentPos).get(currNumberOfMines));
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			//VisibleTile curr = board[i][j];
			Pair<MutableInt, MutableInt> curr = currArrayList.get(pos);

			if (isMine[i][j]) {
				curr.first.addWith(1);
			}
			curr.second.addWith(1);
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer, Integer>> component, int currNumberOfMines) throws Exception {
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				VisibleTile adjTile = board[adjI][adjJ];
				if (!adjTile.isVisible) {
					continue;
				}
				if (cntSurroundingMines[adjI][adjJ] != updatedNumberSurroundingMines[adjI][adjJ]) {
					throw new Exception("found bad solution - # mines doesn't match, but this should be pruned out");
				}
			}
		}
		int prevNumberOfMines = 0;
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if (isMine[i][j]) {
				++prevNumberOfMines;
			}
		}
		if (prevNumberOfMines != currNumberOfMines) {
			throw new Exception("number of mines doesn't match");
		}
	}

	public int getNumberOfIterations() {
		return totalIterations;
	}
}
