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
//TODO: extra pruning idea: prune out if there's only n spots left adjacent to a clue, and (clue - bombs placed) > n
//TODO: hard code in rules, then split components by logical cells
public class BacktrackingSolver implements MinesweeperSolver {

	private final static int iterationLimit = 20000;
	private final int rows, cols;
	private final boolean[][] isBomb;
	private final int[][] cntSurroundingBombs, updatedNumberSurroundingBombs;
	private final int[][][] lastUnvisitedSpot;
	private final ArrayList<TreeMap<Integer, MutableInt>> bombConfig;
	//TODO: remove bombProbPerCompPerNumBombs denominator, and use bombConfig instead
	private final ArrayList<TreeMap<Integer, ArrayList<Pair<MutableInt, MutableInt>>>> bombProbPerCompPerNumBombs;
	private final ArrayList<TreeMap<Integer, BigFraction>> numberOfConfigsForCurrent;
	private final GaussianEliminationSolver gaussianEliminationSolver;
	private int totalIterations;
	private VisibleTile[][] board;
	private ArrayList<ArrayList<Pair<Integer, Integer>>> components;
	private int numberOfBombs;

	public BacktrackingSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		isBomb = new boolean[rows][cols];
		cntSurroundingBombs = new int[rows][cols];
		updatedNumberSurroundingBombs = new int[rows][cols];
		lastUnvisitedSpot = new int[rows][cols][2];
		bombConfig = new ArrayList<>();
		numberOfConfigsForCurrent = new ArrayList<>();
		gaussianEliminationSolver = new GaussianEliminationSolver(rows, cols);
		bombProbPerCompPerNumBombs = new ArrayList<>();
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfBombs) throws Exception {

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					board[i][j].numberOfBombConfigs.setValues(numberOfBombs, 1);
					board[i][j].numberOfTotalConfigs.setValues(rows * cols, 1);
				}
			}
			return;
		}

		gaussianEliminationSolver.solvePosition(board, numberOfBombs);

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible() && (board[i][j].getIsLogicalBomb() || board[i][j].getIsLogicalFree())) {
					throw new Exception("visible cells can't be logical frees/bombs");
				}
				if (board[i][j].getIsLogicalBomb()) {
					--numberOfBombs;
					board[i][j].numberOfBombConfigs.setValues(1, 1);
					board[i][j].numberOfTotalConfigs.setValues(1, 1);
				} else if (board[i][j].getIsLogicalFree()) {
					board[i][j].numberOfBombConfigs.setValues(0, 1);
					board[i][j].numberOfTotalConfigs.setValues(1, 1);
				}
				if (board[i][j].getIsVisible()) {
					updatedNumberSurroundingBombs[i][j] = board[i][j].getNumberSurroundingBombs();
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						VisibleTile adjCell = board[adj[0]][adj[1]];
						if (adjCell.getIsLogicalBomb()) {
							--updatedNumberSurroundingBombs[i][j];
						}
					}
				}
			}
		}

		initialize(board, numberOfBombs);
		components = GetConnectedComponents.getComponentsWithKnownCells(board);

		initializeLastUnvisitedSpot(components);

		//TODO: look into running this loop in parallel
		totalIterations = 0;
		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs);
			totalIterations += currIterations.get();
		}

		removeBombNumbersFromComponent();
		BigFraction awayBombProbability = null;
		if (AwayCell.getNumberOfAwayCells(board) > 0) {
			awayBombProbability = calculateAwayBombProbability();
		}
		updateNumberOfConfigsForCurrent(AwayCell.getNumberOfAwayCells(board));

		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

		BigFraction totalConfigs = new BigFraction(0);
		TreeMap<Integer, BigFraction> configsPerBombCount = calculateNumberOfBombConfigs();
		for (TreeMap.Entry<Integer, BigFraction> total : configsPerBombCount.entrySet()) {
			BigFraction currConfigs = MyMath.BinomialCoefficient(numberOfAwayCells, numberOfBombs - total.getKey());
			currConfigs.multiplyWith(total.getValue());
			totalConfigs.addWith(currConfigs);
		}

		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, ArrayList<Pair<MutableInt, MutableInt>>> entry : bombProbPerCompPerNumBombs.get(i).entrySet()) {
				final int bombs = entry.getKey();
				final ArrayList<Pair<MutableInt, MutableInt>> bombProbPerSpot = entry.getValue();

				BigFraction currWeight = numberOfConfigsForCurrent.get(i).get(bombs);

				for (int j = 0; j < components.get(i).size(); ++j) {
					final int numerator = bombProbPerSpot.get(j).first.get();
					final int row = components.get(i).get(j).first;
					final int col = components.get(i).get(j).second;

					BigFraction delta = new BigFraction(numerator);
					delta.multiplyWith(currWeight);
					board[row][col].numberOfBombConfigs.addWith(delta);
				}
			}
		}


		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile curr = board[i][j];
				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					if (awayBombProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					curr.numberOfBombConfigs.setValue(awayBombProbability);
					curr.numberOfTotalConfigs.setValues(1, 1);
					continue;
				}
				if (curr.getIsVisible() || curr.getIsLogicalBomb() || curr.getIsLogicalFree()) {
					continue;
				}
				curr.numberOfBombConfigs.divideWith(totalConfigs);
				curr.numberOfTotalConfigs.setValues(1, 1);
				if (curr.numberOfBombConfigs.equals(0)) {
					curr.isLogicalFree = true;
				} else if (curr.numberOfBombConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalBomb = true;
				}
			}
		}
	}

	private void updateNumberOfConfigsForCurrent(int numberOfAwayCells) throws Exception {
		for (int i = 0; i < components.size(); ++i) {
			TreeMap<Integer, MutableInt> saveBombConfigs = new TreeMap<>(bombConfig.get(i));
			for (TreeMap.Entry<Integer, MutableInt> entry : saveBombConfigs.entrySet()) {
				BigFraction totalConfigs = new BigFraction(0);
				bombConfig.get(i).clear();
				bombConfig.get(i).put(entry.getKey(), new MutableInt(1));
				TreeMap<Integer, BigFraction> configsPerBombCount = calculateNumberOfBombConfigs();
				for (TreeMap.Entry<Integer, BigFraction> total : configsPerBombCount.entrySet()) {
					BigFraction currConfigs = MyMath.BinomialCoefficient(numberOfAwayCells, numberOfBombs - total.getKey());
					currConfigs.multiplyWith(total.getValue());
					totalConfigs.addWith(currConfigs);
				}
				numberOfConfigsForCurrent.get(i).put(entry.getKey(), totalConfigs);
			}
			bombConfig.get(i).clear();
			bombConfig.set(i, saveBombConfigs);
		}
	}

	private void removeBombNumbersFromComponent() throws Exception {
		ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size() + 1);
		for (int i = 0; i <= components.size(); ++i) {
			dpTable.add(new TreeSet<Integer>());
		}

		dpTable.get(0).add(0);
		for (int i = 0; i < components.size(); ++i) {
			for (int entry : bombConfig.get(i).keySet()) {
				for (int val : dpTable.get(i)) {
					dpTable.get(i + 1).add(val + entry);
				}
			}
		}
		TreeSet<Integer> validSpots = new TreeSet<>();
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		for (int bombCnt : dpTable.get(components.size())) {
			if (bombCnt <= numberOfBombs && numberOfBombs <= bombCnt + numberOfAwayCells) {
				validSpots.add(bombCnt);
			}
		}
		dpTable.get(components.size()).clear();
		dpTable.set(components.size(), validSpots);

		for (int i = components.size() - 1; i >= 0; --i) {
			TreeSet<Integer> spotsToRemove = new TreeSet<>();
			for (int entry : bombConfig.get(i).keySet()) {
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
				bombConfig.get(i).remove(val);
				bombProbPerCompPerNumBombs.get(i).remove(val);
			}

			spotsToRemove.clear();
			for (int val : dpTable.get(i)) {
				boolean found = false;
				for (int entry : bombConfig.get(i).keySet()) {
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

	private BigFraction calculateAwayBombProbability() throws Exception {
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

		TreeMap<Integer, BigFraction> configsPerBombCount = calculateNumberOfBombConfigs();
		BigFraction totalNumberOfConfigs = new BigFraction(0);
		for (TreeMap.Entry<Integer, BigFraction> val : configsPerBombCount.entrySet()) {
			if (numberOfBombs - val.getKey() < 0 || numberOfBombs - val.getKey() > numberOfAwayCells) {
				throw new Exception("number of remaining bombs is more than number of away cells (or negative)");
			}
			//choose where the extra bombs go
			BigFraction newConfigs = MyMath.BinomialCoefficient(numberOfAwayCells, numberOfBombs - val.getKey());
			//multiply with number of bomb configs with current # of bombs
			newConfigs.multiplyWith(val.getValue());
			totalNumberOfConfigs.addWith(newConfigs);
		}
		BigFraction awayBombProbability = new BigFraction(0);
		for (TreeMap.Entry<Integer, BigFraction> entry : configsPerBombCount.entrySet()) {
			final int currNumberOfBombs = entry.getKey();
			if (numberOfBombs - currNumberOfBombs < 0 || numberOfBombs - currNumberOfBombs > numberOfAwayCells) {
				throw new Exception("number of remaining bombs is more than number of away cells (or negative)");
			}
			//again, choose which away cells to place remaining bombs
			BigFraction numberOfConfigs = MyMath.BinomialCoefficient(numberOfAwayCells, numberOfBombs - entry.getKey());
			//again multiply with number of bomb configs with current # of bombs
			numberOfConfigs.multiplyWith(entry.getValue());

			//divide with total number of configurations
			numberOfConfigs.divideWith(totalNumberOfConfigs);

			//actual probability that a single away cell is a bomb, the above is just a weight - "how often is this probability the case - # configs / # total configs"
			numberOfConfigs.multiplyWith(numberOfBombs - currNumberOfBombs, numberOfAwayCells);

			awayBombProbability.addWith(numberOfConfigs);
		}
		return awayBombProbability;
	}

	private TreeMap<Integer, BigFraction> calculateNumberOfBombConfigs() throws Exception {
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);
		TreeMap<Integer, BigFraction> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
		prevWays.put(0, new BigFraction(1));
		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, MutableInt> bombVal : bombConfig.get(i).entrySet()) {
				for (TreeMap.Entry<Integer, BigFraction> waysVal : prevWays.entrySet()) {
					final int nextKey = bombVal.getKey() + waysVal.getKey();
					BigFraction nextValueDiff = new BigFraction(bombVal.getValue().get());
					nextValueDiff.multiplyWith(waysVal.getValue());
					if (i + 1 == components.size() && (nextKey > numberOfBombs || nextKey + numberOfAwayCells < numberOfBombs)) {
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
	public boolean[][] getBombConfiguration(VisibleTile[][] board, int numberOfBombs, int spotI, int spotJ, boolean wantBomb) throws Exception {
		throw new Exception("not implemented");
		/*
		initialize(board, numberOfBombs);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		this.spotI = spotI;
		this.spotJ = spotJ;
		this.wantBomb = wantBomb;
		foundBombConfiguration = false;

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
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs, false);
		}
		return (foundBombConfiguration ? saveIsBomb : null);
		 */
	}

	private void initialize(VisibleTile[][] board, int numberOfBombs) throws Exception {
		this.board = board;
		this.numberOfBombs = numberOfBombs;
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				isBomb[i][j] = false;
				cntSurroundingBombs[i][j] = 0;
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer, Integer>>> components) {
		bombConfig.clear();
		numberOfConfigsForCurrent.clear();
		bombProbPerCompPerNumBombs.clear();
		for (ArrayList<Pair<Integer, Integer>> component : components) {
			bombConfig.add(new TreeMap<Integer, MutableInt>());
			numberOfConfigsForCurrent.add(new TreeMap<Integer, BigFraction>());
			bombProbPerCompPerNumBombs.add(new TreeMap<Integer, ArrayList<Pair<MutableInt, MutableInt>>>());
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
	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (pos == component.size()) {
			handleSolution(componentPos, currNumberOfBombs.get());
			return;
		}
		currIterations.addWith(1);
		if (currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException();
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		isBomb[i][j] = true;
		if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
			currNumberOfBombs.addWith(1);
			updateSurroundingBombCnt(i, j, 1);
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfBombs);
			updateSurroundingBombCnt(i, j, -1);
			currNumberOfBombs.addWith(-1);
		}

		//try free
		isBomb[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfBombs);
		}
	}

	private void updateSurroundingBombCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			if (board[adj[0]][adj[1]].isVisible) {
				foundAdjVis = true;
				cntSurroundingBombs[adj[0]][adj[1]] += delta;
			}
		}
		if (!foundAdjVis) {
			throw new Exception("hidden cell with no adjacent visible cell");
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingABomb) {
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			VisibleTile adjTile = board[adjI][adjJ];
			if (!adjTile.isVisible) {
				continue;
			}
			final int currBacktrackingCount = cntSurroundingBombs[adjI][adjJ];
			if (currBacktrackingCount + arePlacingABomb > updatedNumberSurroundingBombs[adjI][adjJ]) {
				return false;
			}
			if (lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
					lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
					currBacktrackingCount + arePlacingABomb != updatedNumberSurroundingBombs[adjI][adjJ]) {
				return false;
			}
		}
		return true;
	}

	private void handleSolution(int componentPos, int currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		//TODO: remove this extra computation once there is sufficient testing
		checkPositionValidity(component, currNumberOfBombs);

		MutableInt count = bombConfig.get(componentPos).get(currNumberOfBombs);
		if (count == null) {
			bombConfig.get(componentPos).put(currNumberOfBombs, new MutableInt(1));
		} else {
			count.addWith(1);
		}

		if (!bombProbPerCompPerNumBombs.get(componentPos).containsKey(currNumberOfBombs)) {
			ArrayList<Pair<MutableInt, MutableInt>> currSpotsArray = new ArrayList<>(component.size());
			for (int i = 0; i < component.size(); ++i) {
				currSpotsArray.add(new Pair<>(new MutableInt(0), new MutableInt(0)));
			}
			bombProbPerCompPerNumBombs.get(componentPos).put(currNumberOfBombs, currSpotsArray);
		}
		ArrayList<Pair<MutableInt, MutableInt>> currArrayList = Objects.requireNonNull(bombProbPerCompPerNumBombs.get(componentPos).get(currNumberOfBombs));
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			//VisibleTile curr = board[i][j];
			Pair<MutableInt, MutableInt> curr = currArrayList.get(pos);

			if (isBomb[i][j]) {
				curr.first.addWith(1);
			}
			curr.second.addWith(1);
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer, Integer>> component, int currNumberOfBombs) throws Exception {
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				VisibleTile adjTile = board[adjI][adjJ];
				if (!adjTile.isVisible) {
					continue;
				}
				if (cntSurroundingBombs[adjI][adjJ] != updatedNumberSurroundingBombs[adjI][adjJ]) {
					throw new Exception("found bad solution - # bombs doesn't match, but this should be pruned out");
				}
			}
		}
		int prevNumberOfBombs = 0;
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if (isBomb[i][j]) {
				++prevNumberOfBombs;
			}
		}
		if (prevNumberOfBombs != currNumberOfBombs) {
			throw new Exception("number of bombs doesn't match");
		}
	}

	public int getNumberOfIterations() {
		return totalIterations;
	}
}
