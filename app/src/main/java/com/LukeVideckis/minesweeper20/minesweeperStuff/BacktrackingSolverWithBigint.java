package com.LukeVideckis.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper20.customExceptions.NoSolutionFoundException;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.GetConnectedComponents;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.MyMath;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

public class BacktrackingSolverWithBigint implements BacktrackingSolver {
	private final static int iterationLimit = 20000;
	private final BigInteger[][] BIG_numberOfMineConfigs, BIG_numberOfTotalConfigs;
	private final boolean[][] isMine;
	private final int[][] cntSurroundingMines;
	private final int[][][] lastUnvisitedSpot;
	//one hash-map for each component:
	//for each component: we map the number of mines to a configuration of mines for that component
	private final ArrayList<TreeMap<Integer, MutableInt>> mineConfig;
	private final ArrayList<TreeMap<Integer, BigInteger>> numberOfConfigsForCurrent;
	private int rows, cols;
	private VisibleTile[][] board;
	private ArrayList<ArrayList<Pair<Integer, Integer>>> components;
	private int numberOfMines, numberOfIterations;

	public BacktrackingSolverWithBigint(int rows, int cols) {
		isMine = new boolean[rows][cols];
		cntSurroundingMines = new int[rows][cols];
		lastUnvisitedSpot = new int[rows][cols][2];


		BIG_numberOfMineConfigs = new BigInteger[rows][cols];
		BIG_numberOfTotalConfigs = new BigInteger[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				BIG_numberOfMineConfigs[i][j] = BigInteger.ZERO;
				BIG_numberOfTotalConfigs[i][j] = BigInteger.ZERO;
			}
		}

		mineConfig = new ArrayList<>();
		numberOfConfigsForCurrent = new ArrayList<>();
	}

	public BigInteger getNumberOfMineConfigs(int i, int j) {
		return BIG_numberOfMineConfigs[i][j];
	}

	public BigInteger getNumberOfTotalConfigs(int i, int j) {
		return BIG_numberOfTotalConfigs[i][j];
	}

	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		initialize(board, numberOfMines);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		if (board.length != rows || board[0].length != cols) {
			throw new Exception("board dimensions don't match what was passed in the constructor");
		}

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					BIG_numberOfMineConfigs[i][j] = BigInteger.valueOf(numberOfMines);
					BIG_numberOfTotalConfigs[i][j] = BigInteger.valueOf(rows * cols);
				}
			}
			return;
		}

		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines, false);
		}

		removeMineNumbersFromComponent();
		MyBIGPair awayMineProbability = null;
		if (AwayCell.getNumberOfAwayCells(board) > 0) {
			awayMineProbability = calculateAwayMineProbability();
		}
		updateNumberOfConfigsForCurrent(AwayCell.getNumberOfAwayCells(board));

		numberOfIterations = 0;
		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines, true);
			numberOfIterations += currIterations.get();
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					if (awayMineProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					BIG_numberOfMineConfigs[i][j] = new BigInteger(awayMineProbability.first.toString());
					BIG_numberOfTotalConfigs[i][j] = new BigInteger(awayMineProbability.second.toString());
				}
				VisibleTile curr = board[i][j];
				if (curr.getIsVisible()) {
					continue;
				}
				if (BIG_numberOfTotalConfigs[i][j].equals(BigInteger.ZERO)) {
					throw new NoSolutionFoundException("There should be at least one mine configuration for non-visible cells");
				}
				if (BIG_numberOfMineConfigs[i][j].equals(BigInteger.ZERO)) {
					curr.isLogicalFree = true;
				} else if (BIG_numberOfMineConfigs[i][j].equals(BIG_numberOfTotalConfigs[i][j])) {
					curr.isLogicalMine = true;
				}
			}
		}
	}

	@Override
	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {
		throw new Exception("not implemented yet");
	}

	@Override
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	private void updateNumberOfConfigsForCurrent(int numberOfAwayCells) throws Exception {
		for (int i = 0; i < components.size(); ++i) {
			TreeMap<Integer, MutableInt> saveMineConfigs = new TreeMap<>(mineConfig.get(i));
			for (TreeMap.Entry<Integer, MutableInt> entry : saveMineConfigs.entrySet()) {
				BigInteger totalConfigs = new BigInteger("0");
				mineConfig.get(i).clear();
				mineConfig.get(i).put(entry.getKey(), new MutableInt(1));
				TreeMap<Integer, BigInteger> configsPerMineCount = calculateNumberOfMineConfigs();
				for (TreeMap.Entry<Integer, BigInteger> total : configsPerMineCount.entrySet()) {
					BigInteger currConfigs = BinomialCoefficientBIG(numberOfAwayCells, numberOfMines - total.getKey());
					currConfigs = currConfigs.multiply(total.getValue());
					totalConfigs = totalConfigs.add(currConfigs);
				}
				numberOfConfigsForCurrent.get(i).put(entry.getKey(), totalConfigs);
			}
			mineConfig.get(i).clear();
			mineConfig.set(i, saveMineConfigs);
		}
	}

	private void removeMineNumbersFromComponent() throws Exception {
		ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size() + 1);
		for (int i = 0; i <= components.size(); ++i) {
			dpTable.add(new TreeSet<>());
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

	private MyBIGPair calculateAwayMineProbability() throws Exception {
		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

		TreeMap<Integer, BigInteger> configsPerMineCount = calculateNumberOfMineConfigs();
		BigInteger totalNumberOfConfigs = BigInteger.ZERO;
		for (TreeMap.Entry<Integer, BigInteger> val : configsPerMineCount.entrySet()) {
			BigInteger newConfigs = BinomialCoefficientBIG(numberOfAwayCells, numberOfMines - val.getKey());
			newConfigs = newConfigs.multiply(val.getValue());
			totalNumberOfConfigs = totalNumberOfConfigs.add(newConfigs);
		}
		MyBIGPair awayMineProbability = new MyBIGPair(BigInteger.ZERO);
		for (TreeMap.Entry<Integer, BigInteger> entry : configsPerMineCount.entrySet()) {
			final int currNumberOfMines = entry.getKey();
			if (numberOfMines - currNumberOfMines < 0 || numberOfMines - currNumberOfMines > numberOfAwayCells) {
				throw new Exception("number of remaining mines is more than number of away cells (or negative)");
			}
			MyBIGPair numberOfConfigs = new MyBIGPair(BinomialCoefficientBIG(numberOfAwayCells, numberOfMines - entry.getKey()));
			numberOfConfigs.first = numberOfConfigs.first.multiply(entry.getValue());
			numberOfConfigs.second = numberOfConfigs.second.multiply(totalNumberOfConfigs);

			numberOfConfigs.first = numberOfConfigs.first.multiply(BigInteger.valueOf(numberOfMines - currNumberOfMines));
			numberOfConfigs.second = numberOfConfigs.second.multiply(BigInteger.valueOf(numberOfAwayCells));

			awayMineProbability.addWith(numberOfConfigs);
		}
		return awayMineProbability;
	}

	private TreeMap<Integer, BigInteger> calculateNumberOfMineConfigs() {
		TreeMap<Integer, BigInteger> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
		prevWays.put(0, BigInteger.ONE);
		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, MutableInt> mineVal : mineConfig.get(i).entrySet()) {
				for (TreeMap.Entry<Integer, BigInteger> waysVal : prevWays.entrySet()) {
					final int nextKey = mineVal.getKey() + waysVal.getKey();
					BigInteger nextValueDiff = BigInteger.valueOf(mineVal.getValue().get());
					nextValueDiff = nextValueDiff.multiply(waysVal.getValue());

					BigInteger nextVal = newWays.get(nextKey);
					if (nextVal == null) {
						newWays.put(nextKey, nextValueDiff);
					} else {
						newWays.put(nextKey, nextVal.add(nextValueDiff));
					}
				}
			}
			prevWays.clear();
			prevWays.putAll(newWays);
			newWays.clear();
		}
		return prevWays;
	}

	private void initialize(VisibleTile[][] board, int numberOfMines) throws Exception {
		this.board = board;
		this.numberOfMines = numberOfMines;
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
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
		for (ArrayList<Pair<Integer, Integer>> component : components) {
			mineConfig.add(new TreeMap<>());
			numberOfConfigsForCurrent.add(new TreeMap<>());
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

	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfMines, boolean isSecondPass) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (pos == component.size()) {
			if (isSecondPass) {
				checkSolutionSecondPass(componentPos, currNumberOfMines.get());
			} else {
				checkSolutionFirstPass(componentPos, currNumberOfMines.get());
			}
			return;
		}
		currIterations.addWith(1);
		if (currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException("too many iterations");
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try mine
		isMine[i][j] = true;
		if (checkSurroundingConditions(i, j, component.get(pos), 1)) {
			currNumberOfMines.addWith(1);
			updateSurroundingMineCnt(i, j, 1);
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines, isSecondPass);
			updateSurroundingMineCnt(i, j, -1);
			currNumberOfMines.addWith(-1);
		}

		//try free
		isMine[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines, isSecondPass);
		}
	}

	private void updateSurroundingMineCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
			final int adjI = adj[0], adjJ = adj[1];
			if (board[adjI][adjJ].isVisible) {
				foundAdjVis = true;
				cntSurroundingMines[adjI][adjJ] += delta;
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
			if (currBacktrackingCount + arePlacingAMine > adjTile.numberSurroundingMines) {
				return false;
			}
			if (
					lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
							lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
							currBacktrackingCount + arePlacingAMine != adjTile.numberSurroundingMines
			) {
				return false;
			}
		}
		return true;
	}

	private void checkSolutionFirstPass(int componentPos, int currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		checkPositionValidity(component, currNumberOfMines);

		MutableInt count = mineConfig.get(componentPos).get(currNumberOfMines);
		if (count == null) {
			mineConfig.get(componentPos).put(currNumberOfMines, new MutableInt(1));
		} else {
			count.addWith(1);
		}
	}

	private void checkSolutionSecondPass(int componentPos, int currNumberOfMines) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		checkPositionValidity(component, currNumberOfMines);

		if (!mineConfig.get(componentPos).containsKey(currNumberOfMines)) {
			return;
		}
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			BigInteger currConfigs = numberOfConfigsForCurrent.get(componentPos).get(currNumberOfMines);
			if (currConfigs == null) {
				throw new Exception("number of configs value is null");
			}
			if (isMine[i][j]) {
				BIG_numberOfMineConfigs[i][j] = BIG_numberOfMineConfigs[i][j].add(currConfigs);
			}
			BIG_numberOfTotalConfigs[i][j] = BIG_numberOfTotalConfigs[i][j].add(currConfigs);
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
				if (cntSurroundingMines[adjI][adjJ] != adjTile.numberSurroundingMines) {
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

	private BigInteger BinomialCoefficientBIG(int n, int k) throws Exception {
		if (k < 0 || k > n) {
			throw new Exception("invalid input");
		}
		if ((n == k) || (k == 0)) {
			return BigInteger.ONE;
		}
		if ((k == 1) || (k == n - 1)) {
			return BigInteger.valueOf(n);
		}
		if (k > n / 2) {
			return BinomialCoefficientBIG(n, n - k);
		}

		BigInteger result = BigInteger.ONE;
		int i = n - k + 1;
		for (int j = 1; j <= k; j++) {
			final int gcd = MyMath.gcd(i, j);
			result = result.divide(BigInteger.valueOf(j / gcd));
			result = result.multiply(BigInteger.valueOf(i / gcd));
			i++;
		}
		return result;
	}

	private static class MyBIGPair {
		BigInteger first, second;

		MyBIGPair(BigInteger first) {
			this.first = first;
			second = BigInteger.ONE;
		}

		void addWith(MyBIGPair other) {
			first = first.multiply(other.second).add(second.multiply(other.first));
			second = second.multiply(other.second);
		}
	}
}
