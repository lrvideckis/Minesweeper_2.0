package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetAdjacentCells;
import com.example.minesweeper20.helpers.GetConnectedComponents;
import com.example.minesweeper20.helpers.MutableInt;
import com.example.minesweeper20.helpers.MyMath;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class BacktrackingSolverWithBigint {
	private static class MyBIGPair {
		BigInteger first, second;

		MyBIGPair(BigInteger _first) {
			first = _first;
			second = BigInteger.ONE;
		}
		void addWith(MyBIGPair other) {
			first = first.multiply(other.second).add(second.multiply(other.first));
			second = second.multiply(other.second);
		}
	}

	private final BigInteger[][] BIG_numberOfBombConfigs, BIG_numberOfTotalConfigs;

	private int rows, cols;
	private final static int iterationLimit = 20000;

	private final boolean[][] isBomb;
	private final int[][] cntSurroundingBombs;
	private VisibleTile[][] board;
	private final int[][][] lastUnvisitedSpot;
	private ArrayList<ArrayList<Pair<Integer,Integer>>> components;
	private int numberOfBombs;

	//one hash-map for each component:
	//for each component: we map the number of bombs to a configuration of bombs for that component
	private final ArrayList<TreeMap<Integer, MutableInt>> bombConfig;
	private final ArrayList<TreeMap<Integer, BigInteger>> numberOfConfigsForCurrent;

	public BigInteger getNumberOfBombConfigs(int i, int j) {
		return BIG_numberOfBombConfigs[i][j];
	}
	public BigInteger getNumberOfTotalConfigs(int i, int j) {
		return BIG_numberOfTotalConfigs[i][j];
	}

	public BacktrackingSolverWithBigint(int rows, int cols) {
		isBomb = new boolean[rows][cols];
		cntSurroundingBombs = new int[rows][cols];
		lastUnvisitedSpot = new int[rows][cols][2];


		BIG_numberOfBombConfigs = new BigInteger[rows][cols];
		BIG_numberOfTotalConfigs = new BigInteger[rows][cols];
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				BIG_numberOfBombConfigs[i][j] = BigInteger.ZERO;
				BIG_numberOfTotalConfigs[i][j] = BigInteger.ZERO;
			}
		}

		bombConfig = new ArrayList<>();
		numberOfConfigsForCurrent = new ArrayList<>();
	}

	public void solvePosition(VisibleTile[][] _board, int _numberOfBombs) throws Exception {
		initialize(_board, _numberOfBombs);
		components = GetConnectedComponents.getComponents(board);
		initializeLastUnvisitedSpot(components);

		if(_board.length != rows || _board[0].length != cols) {
			throw new Exception("board dimensions don't match what was passed in the constructor");
		}

		if(GetConnectedComponents.allCellsAreHidden(board)) {
			for(int i = 0; i < rows; ++i) {
				for(int j = 0; j < cols; ++j) {
					BIG_numberOfBombConfigs[i][j] = BigInteger.valueOf(numberOfBombs);
					BIG_numberOfTotalConfigs[i][j] = BigInteger.valueOf(rows*cols);
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
		MyBIGPair awayBombProbability = null;
		if(GetConnectedComponents.getNumberOfAwayCells(board) > 0) {
			awayBombProbability = calculateAwayBombProbability();
		}
		updateNumberOfConfigsForCurrent(GetConnectedComponents.getNumberOfAwayCells(board));

		for(int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfBombs = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfBombs, true);
		}

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(GetConnectedComponents.isAwayCell(board, i, j, rows, cols)) {
					if(awayBombProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					BIG_numberOfBombConfigs[i][j] = new BigInteger(awayBombProbability.first.toString());
					BIG_numberOfTotalConfigs[i][j] = new BigInteger(awayBombProbability.second.toString());
				}
				VisibleTile curr = board[i][j];
				if(curr.getIsVisible()) {
					continue;
				}
				if(BIG_numberOfTotalConfigs[i][j].equals(BigInteger.ZERO)) {
					throw new Exception("There should be at least one bomb configuration for non-visible cells");
				}
				if(BIG_numberOfBombConfigs[i][j].equals(BigInteger.ZERO)) {
					curr.isLogicalFree = true;
				} else if(BIG_numberOfBombConfigs[i][j].equals(BIG_numberOfTotalConfigs[i][j])) {
					curr.isLogicalBomb = true;
				}
			}
		}
	}

	private void updateNumberOfConfigsForCurrent(int numberOfAwayCells) throws Exception {
		for(int i = 0; i < components.size(); ++i) {
			TreeMap<Integer,MutableInt> saveBombConfigs = new TreeMap<>(bombConfig.get(i));
			for(TreeMap.Entry<Integer,MutableInt> entry : saveBombConfigs.entrySet()) {
				BigInteger totalConfigs = new BigInteger("0");
				bombConfig.get(i).clear();
				bombConfig.get(i).put(entry.getKey(), new MutableInt(1));
				TreeMap<Integer,BigInteger> configsPerBombCount = calculateNumberOfBombConfigs();
				for(TreeMap.Entry<Integer,BigInteger> total : configsPerBombCount.entrySet()) {
					BigInteger currConfigs = BinomialCoefficientBIG(numberOfAwayCells, numberOfBombs - total.getKey());
					currConfigs = currConfigs.multiply(total.getValue());
					totalConfigs = totalConfigs.add(currConfigs);
				}
				numberOfConfigsForCurrent.get(i).put(entry.getKey(), totalConfigs);
			}
			bombConfig.get(i).clear();
			bombConfig.set(i, saveBombConfigs);
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

	private MyBIGPair calculateAwayBombProbability() throws Exception {
		final int numberOfAwayCells = GetConnectedComponents.getNumberOfAwayCells(board);

		TreeMap<Integer, BigInteger> configsPerBombCount = calculateNumberOfBombConfigs();
		BigInteger totalNumberOfConfigs = BigInteger.ZERO;
		for(TreeMap.Entry<Integer,BigInteger> val : configsPerBombCount.entrySet()) {
			BigInteger newConfigs = BinomialCoefficientBIG(numberOfAwayCells, numberOfBombs - val.getKey());
			newConfigs = newConfigs.multiply(val.getValue());
			totalNumberOfConfigs = totalNumberOfConfigs.add(newConfigs);
		}
		MyBIGPair awayBombProbability = new MyBIGPair(BigInteger.ZERO);
		for(TreeMap.Entry<Integer,BigInteger> entry : configsPerBombCount.entrySet()) {
			final int currNumberOfBombs = entry.getKey();
			if(numberOfBombs - currNumberOfBombs < 0 || numberOfBombs - currNumberOfBombs > numberOfAwayCells) {
				throw new Exception("number of remaining bombs is more than number of away cells (or negative)");
			}
			MyBIGPair numberOfConfigs = new MyBIGPair(BinomialCoefficientBIG(numberOfAwayCells, numberOfBombs - entry.getKey()));
			numberOfConfigs.first = numberOfConfigs.first.multiply(entry.getValue());
			numberOfConfigs.second = numberOfConfigs.second.multiply(totalNumberOfConfigs);

			numberOfConfigs.first = numberOfConfigs.first.multiply(BigInteger.valueOf(numberOfBombs - currNumberOfBombs));
			numberOfConfigs.second = numberOfConfigs.second.multiply(BigInteger.valueOf(numberOfAwayCells));

			awayBombProbability.addWith(numberOfConfigs);
		}
		return awayBombProbability;
	}

	private TreeMap<Integer,BigInteger> calculateNumberOfBombConfigs() {
		TreeMap<Integer,BigInteger> prevWays = new TreeMap<>(), newWays = new TreeMap<>();
		prevWays.put(0, BigInteger.ONE);
		for(int i = 0; i < components.size(); ++i) {
			for(TreeMap.Entry<Integer,MutableInt> bombVal : bombConfig.get(i).entrySet()) {
				for(TreeMap.Entry<Integer,BigInteger> waysVal : prevWays.entrySet()) {
					final int nextKey = bombVal.getKey() + waysVal.getKey();
					BigInteger nextValueDiff = BigInteger.valueOf(bombVal.getValue().get());
					nextValueDiff = nextValueDiff.multiply(waysVal.getValue());

					BigInteger nextVal = newWays.get(nextKey);
					if(nextVal == null) {
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

	private void initialize(VisibleTile[][] _board, int _numberOfBombs) throws Exception {
		board = _board;
		numberOfBombs = _numberOfBombs;
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				isBomb[i][j] = false;
				cntSurroundingBombs[i][j] = 0;
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<ArrayList<Pair<Integer,Integer>>> components) {
		bombConfig.clear();
		numberOfConfigsForCurrent.clear();
		for(ArrayList<Pair<Integer,Integer>> component : components) {
			bombConfig.add(new TreeMap<Integer,MutableInt>());
			numberOfConfigsForCurrent.add(new TreeMap<Integer,BigInteger>());
			for (Pair<Integer, Integer> spot : component) {
				final int[][] adjCells = GetAdjacentCells.getAdjacentCells(spot.first,spot.second,rows,cols);
				for(int[] adj : adjCells) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].isVisible) {
						lastUnvisitedSpot[adjI][adjJ][0] = spot.first;
						lastUnvisitedSpot[adjI][adjJ][1] = spot.second;
					}
				}
			}
		}
	}

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
		isBomb[i][j] = true;
		if(checkSurroundingConditions(i,j,component.get(pos),1)) {
			currNumberOfBombs.addWith(1);
			updateSurroundingBombCnt(i,j,1);
			solveComponent(pos+1, componentPos, currIterations, currNumberOfBombs, isSecondPass);
			updateSurroundingBombCnt(i,j,-1);
			currNumberOfBombs.addWith(-1);
		}

		//try free
		isBomb[i][j] = false;
		if(checkSurroundingConditions(i,j,component.get(pos),0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfBombs, isSecondPass);
		}
	}

	private void updateSurroundingBombCnt(int i, int j, int delta) throws Exception {
		boolean foundAdjVis = false;
		final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i,j,rows,cols);
		for(int[] adj : adjCells) {
			final int adjI = adj[0], adjJ = adj[1];
			if(board[adjI][adjJ].isVisible) {
				foundAdjVis = true;
				cntSurroundingBombs[adjI][adjJ] += delta;
			}
		}
		if(!foundAdjVis) {
			throw new Exception("hidden cell with no adjacent visible cell");
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer,Integer> currSpot, int arePlacingABomb) {
		final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i,j,rows,cols);
		for(int[] adj : adjCells) {
			final int adjI = adj[0], adjJ = adj[1];
			VisibleTile adjTile = board[adjI][adjJ];
			if(!adjTile.isVisible) {
				continue;
			}
			final int currBacktrackingCount = cntSurroundingBombs[adjI][adjJ];
			if(currBacktrackingCount + arePlacingABomb > adjTile.numberSurroundingBombs) {
				return false;
			}
			if(
					lastUnvisitedSpot[adjI][adjJ][0] == currSpot.first &&
					lastUnvisitedSpot[adjI][adjJ][1] == currSpot.second &&
					currBacktrackingCount + arePlacingABomb != adjTile.numberSurroundingBombs
			) {
				return false;
			}
		}
		return true;
	}

	private void checkSolutionFirstPass(int componentPos, int currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		checkPositionValidity(component, currNumberOfBombs);

		MutableInt count = bombConfig.get(componentPos).get(currNumberOfBombs);
		if(count == null) {
			bombConfig.get(componentPos).put(currNumberOfBombs, new MutableInt(1));
		} else {
			count.addWith(1);
		}
	}

	private void checkSolutionSecondPass(int componentPos, int currNumberOfBombs) throws Exception {
		ArrayList<Pair<Integer,Integer>> component = components.get(componentPos);
		checkPositionValidity(component, currNumberOfBombs);

		if(!bombConfig.get(componentPos).containsKey(currNumberOfBombs)) {
			return;
		}
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			BigInteger currConfigs = numberOfConfigsForCurrent.get(componentPos).get(currNumberOfBombs);
			if(currConfigs == null) {
				throw new Exception("number of configs value is null");
			}
			if (isBomb[i][j]) {
				BIG_numberOfBombConfigs[i][j] = BIG_numberOfBombConfigs[i][j].add(currConfigs);
			}
			BIG_numberOfTotalConfigs[i][j] = BIG_numberOfTotalConfigs[i][j].add(currConfigs);
		}
	}

	private void checkPositionValidity(ArrayList<Pair<Integer,Integer>> component, int currNumberOfBombs) throws Exception {
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i,j,rows,cols);
			for(int[] adj : adjCells) {
				final int adjI = adj[0], adjJ = adj[1];
				VisibleTile adjTile = board[adjI][adjJ];
				if(!adjTile.isVisible) {
					continue;
				}
				if(cntSurroundingBombs[adjI][adjJ] != adjTile.numberSurroundingBombs) {
					throw new Exception("found bad solution - # bombs doesn't match, but this should be pruned out");
				}
			}
		}
		int prevNumberOfBombs = 0;
		for(int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if(isBomb[i][j]) {
				++prevNumberOfBombs;
			}
		}
		if(prevNumberOfBombs != currNumberOfBombs) {
			throw new Exception("number of bombs doesn't match");
		}
	}

	private BigInteger BinomialCoefficientBIG(int n, int k) throws Exception {
		if(k < 0 || k > n) {
			throw new Exception("invalid input");
		}
		if((n == k) || (k == 0)) {
			return BigInteger.ONE;
		}
		if((k == 1) || (k == n - 1)) {
			return BigInteger.valueOf(n);
		}
		if(k > n / 2) {
			return BinomialCoefficientBIG(n, n - k);
		}

		BigInteger result = BigInteger.ONE;
		int i = n - k + 1;
		for (int j = 1; j <= k; j++) {
			final int gcd = MyMath.gcd(i,j);
			result = result.divide(BigInteger.valueOf(j/gcd));
			result = result.multiply(BigInteger.valueOf(i/gcd));
			i++;
		}
		return result;
	}
}
