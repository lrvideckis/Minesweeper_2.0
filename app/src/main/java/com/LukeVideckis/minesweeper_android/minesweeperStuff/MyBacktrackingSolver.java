package com.LukeVideckis.minesweeper_android.minesweeperStuff;

import android.util.Pair;

import com.LukeVideckis.minesweeper_android.customExceptions.GameLostException;
import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AllCellsAreHidden;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.GetConnectedComponents;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MutableInt;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: also break out early the moment we find a (conditioned) solution
public class MyBacktrackingSolver implements BacktrackingSolver {

	public final static int iterationLimit = 10000;

	private final int rows, cols;
	private final boolean[][] isMine;
	private final int[][] cntSurroundingMines, updatedNumberSurroundingMines;
	private final ArrayList<ArrayList<ArrayList<Pair<Integer, Integer>>>> lastUnvisitedSpot;
	private final ArrayList<TreeMap<Integer, ArrayList<Pair<Integer, Integer>>>> savePositionsOfMinesPerCompPerCountMines = new ArrayList<>();
	private final TreeMap<Integer, ArrayList<Pair<Integer, Integer>>> saveGoodMineConfigurations = new TreeMap<>();
	private final ArrayList<TreeMap<Integer, MutableInt>> mineConfig = new ArrayList<>();
	private final ArrayList<TreeMap<Integer, ArrayList<MutableInt>>> mineProbPerCompPerNumMines = new ArrayList<>();
	private final ArrayList<TreeMap<Integer, TreeMap<Integer, BigFraction>>> numberOfConfigsForCurrent = new ArrayList<>();
	private final VisibleTileWithProbability[][] tempBoardWithProbability;
	private int numberOfMines;
	private VisibleTile[][] board;
	private ArrayList<ArrayList<Pair<Integer, Integer>>> components;
	private boolean performCheckPositionValidity;

	public MyBacktrackingSolver(int rows, int cols, boolean performCheckPositionValidity) {
		this.rows = rows;
		this.cols = cols;
		isMine = new boolean[rows][cols];
		cntSurroundingMines = new int[rows][cols];
		updatedNumberSurroundingMines = new int[rows][cols];
		lastUnvisitedSpot = new ArrayList<>(rows);
		tempBoardWithProbability = new VisibleTileWithProbability[rows][cols];
		for (int i = 0; i < rows; ++i) {
			ArrayList<ArrayList<Pair<Integer, Integer>>> currRow = new ArrayList<>(cols);
			for (int j = 0; j < cols; ++j) {
				ArrayList<Pair<Integer, Integer>> currSpot = new ArrayList<>();
				currRow.add(currSpot);
			}
			lastUnvisitedSpot.add(currRow);
		}
		this.performCheckPositionValidity = performCheckPositionValidity;
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		//TODO: this can be optimized: have the option to not calculate probability, this option can be used for getMineConfiguration
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				tempBoardWithProbability[i][j] = new VisibleTileWithProbability(board[i][j]);
			}
		}
		solvePosition(tempBoardWithProbability, numberOfMines);
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				board[i][j].set(tempBoardWithProbability[i][j]);
			}
		}
	}

	@Override
	public void solvePosition(VisibleTileWithProbability[][] board, int numberOfMines) throws Exception {

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			for (int i = 0; i < rows; ++i) {
				for (int j = 0; j < cols; ++j) {
					board[i][j].mineProbability.setValues(numberOfMines, rows * cols);
				}
			}
			return;
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible() && (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree())) {
					throw new Exception("visible cells can't be logical frees/mines");
				}
				if (board[i][j].getIsLogicalMine() && board[i][j].getIsLogicalFree()) {
					throw new Exception("cell can't be both logical free and logical mine");
				}
				if (board[i][j].getIsLogicalMine()) {
					if (!AwayCell.isAwayCell(board, i, j, rows, cols)) {
						--numberOfMines;
					}
					board[i][j].mineProbability.setValues(1, 1);
				} else if (board[i][j].getIsLogicalFree()) {
					board[i][j].mineProbability.setValues(0, 1);
				} else {
					board[i][j].mineProbability.setValues(0, 1);
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

		performBacktrackingSequentially(null);

		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

		removeMineNumbersFromComponent();
		BigFraction awayMineProbability = null;
		if (numberOfAwayCells > 0) {
			awayMineProbability = calculateAwayMineProbability();
		}
		updateNumberOfConfigsForCurrent();


		TreeMap<Integer, BigFraction> configsPerMineCount = calculateNumberOfMineConfigs();

		for (int i = 0; i < components.size(); ++i) {
			for (TreeMap.Entry<Integer, ArrayList<MutableInt>> entry : mineProbPerCompPerNumMines.get(i).entrySet()) {
				final int mines = entry.getKey();
				final ArrayList<MutableInt> mineProbPerSpot = entry.getValue();

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
					final int numerator = mineProbPerSpot.get(j).get();
					final int row = components.get(i).get(j).first;
					final int col = components.get(i).get(j).second;

					BigFraction delta = new BigFraction(numerator);
					delta.multiplyWith(currWeight);
					board[row][col].mineProbability.addWith(delta);
				}
			}
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTileWithProbability curr = board[i][j];
				if (curr.getIsVisible() && (curr.isLogicalMine || curr.isLogicalFree)) {
					throw new Exception("visible cells shouldn't be logical");
				}
				if (curr.getIsVisible() && !curr.mineProbability.equals(0)) {
					throw new Exception("found a visible cell with non-zero mine probability: " + i + " " + j);
				}
				if (curr.getIsLogicalMine()) {
					if (!curr.mineProbability.equals(1)) {
						throw new Exception("found logical mine with mine probability != 1: " + i + " " + j);
					}
				}
				if (curr.getIsLogicalFree()) {
					if (!curr.mineProbability.equals(0)) {
						throw new Exception("found logical free cell with mine probability != 0: " + i + " " + j);
					}
				}

				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					if (awayMineProbability == null) {
						throw new Exception("away probability is null, but this was checked above");
					}
					curr.mineProbability.setValue(awayMineProbability);
				}

				if (curr.getIsVisible() || curr.getIsLogicalMine() || curr.getIsLogicalFree()) {
					continue;
				}
				if (curr.mineProbability.equals(0)) {
					curr.isLogicalFree = true;
				} else if (curr.mineProbability.equals(1)) {
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
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				lastUnvisitedSpot.get(i).get(j).clear();
			}
		}
		mineConfig.clear();
		numberOfConfigsForCurrent.clear();
		mineProbPerCompPerNumMines.clear();
		for (ArrayList<Pair<Integer, Integer>> component : components) {
			mineConfig.add(new TreeMap<>());
			numberOfConfigsForCurrent.add(new TreeMap<>());
			mineProbPerCompPerNumMines.add(new TreeMap<>());
			for (Pair<Integer, Integer> spot : component) {
				for (int[] adj : GetAdjacentCells.getAdjacentCells(spot.first, spot.second, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].isVisible) {
						lastUnvisitedSpot.get(adjI).get(adjJ).add(spot);
					}
				}
			}
		}
	}

	//TODO: only re-run component solve if the component has changed
	private void solveComponent(int pos, int componentPos, MutableInt currIterations, MutableInt currNumberOfMines, InterestingCell interestingCell) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (pos == component.size()) {
			handleSolution(componentPos, currNumberOfMines.get(), interestingCell);
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
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines, interestingCell);
			updateSurroundingMineCnt(i, j, -1);
			currNumberOfMines.addWith(-1);
		}

		//try free
		isMine[i][j] = false;
		if (checkSurroundingConditions(i, j, component.get(pos), 0)) {
			solveComponent(pos + 1, componentPos, currIterations, currNumberOfMines, interestingCell);
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

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer, Integer> currSpot, int arePlacingAMine) throws Exception {
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
			ArrayList<Pair<Integer, Integer>> currAdj = lastUnvisitedSpot.get(adjI).get(adjJ);
			int spotsLeft = -1;
			for (int pos = 0; pos < currAdj.size(); ++pos) {
				if (currAdj.get(pos).equals(currSpot)) {
					spotsLeft = currAdj.size() - pos - 1;
					break;
				}
			}
			if (spotsLeft == -1) {
				throw new Exception("didn't find spot in lastUnvisitedSpot, but it should be there");
			}
			if (currBacktrackingCount + arePlacingAMine + spotsLeft < updatedNumberSurroundingMines[adjI][adjJ]) {
				return false;
			}
		}
		return true;
	}

	private void handleSolution(int componentPos, int currNumberOfMines, InterestingCell interestingCell) throws Exception {
		ArrayList<Pair<Integer, Integer>> component = components.get(componentPos);
		if (performCheckPositionValidity) {
			checkPositionValidity(component, currNumberOfMines);
		}

		MutableInt count = mineConfig.get(componentPos).get(currNumberOfMines);
		if (count == null) {
			mineConfig.get(componentPos).put(currNumberOfMines, new MutableInt(1));
		} else {
			count.addWith(1);
		}

		if (!mineProbPerCompPerNumMines.get(componentPos).containsKey(currNumberOfMines)) {
			ArrayList<MutableInt> currSpotsArray = new ArrayList<>(component.size());
			for (int i = 0; i < component.size(); ++i) {
				currSpotsArray.add(new MutableInt(0));
			}
			mineProbPerCompPerNumMines.get(componentPos).put(currNumberOfMines, currSpotsArray);
		}
		ArrayList<MutableInt> currArrayList = Objects.requireNonNull(mineProbPerCompPerNumMines.get(componentPos).get(currNumberOfMines));
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			MutableInt curr = currArrayList.get(pos);

			if (isMine[i][j]) {
				curr.addWith(1);
			}
		}

		if (interestingCell != null) {
			saveCurrentConfiguration(componentPos, currNumberOfMines, component, interestingCell);
		}
	}

	private void saveCurrentConfiguration(int componentPos, int currNumberOfMines, ArrayList<Pair<Integer, Integer>> component, InterestingCell interestingCell) {
		final int spotI = interestingCell.getSpotI();
		final int spotJ = interestingCell.getSpotJ();
		final boolean wantMine = interestingCell.getWantMine();

		boolean goodConfig = false, componentHasInterestingSpot = false;
		for (int pos = 0; pos < component.size(); ++pos) {
			final int i = component.get(pos).first;
			final int j = component.get(pos).second;
			if (i == spotI && j == spotJ) {
				componentHasInterestingSpot = true;
				interestingCell.cellComponent = componentPos;
				if (isMine[i][j] == wantMine) {
					goodConfig = true;
				}
				break;
			}
		}

		if (componentHasInterestingSpot || !savePositionsOfMinesPerCompPerCountMines.get(componentPos).containsKey(currNumberOfMines)) {
			ArrayList<Pair<Integer, Integer>> currMineConfiguration = new ArrayList<>();
			for (int pos = 0; pos < component.size(); ++pos) {
				final int i = component.get(pos).first;
				final int j = component.get(pos).second;
				if (isMine[i][j]) {
					currMineConfiguration.add(new Pair<>(i, j));
				}
			}
			if (componentHasInterestingSpot) {
				if (goodConfig && !saveGoodMineConfigurations.containsKey(currNumberOfMines)) {
					saveGoodMineConfigurations.put(currNumberOfMines, currMineConfiguration);
				}
			} else {
				savePositionsOfMinesPerCompPerCountMines.get(componentPos).put(currNumberOfMines, currMineConfiguration);
			}
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

	private void performBacktrackingSequentially(InterestingCell interestingCell) throws Exception {
		for (int i = 0; i < components.size(); ++i) {
			MutableInt currIterations = new MutableInt(0);
			MutableInt currNumberOfMines = new MutableInt(0);
			solveComponent(0, i, currIterations, currNumberOfMines, interestingCell);
		}
	}

	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {

		if (AllCellsAreHidden.allCellsAreHidden(board)) {
			throw new Exception("not implemented yet");
		}

		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsVisible() && (board[i][j].getIsLogicalMine() || board[i][j].getIsLogicalFree())) {
					throw new Exception("visible cells can't be logical frees/mines");
				}
				if (board[i][j].getIsLogicalMine()) {
					if (i == spotI && j == spotJ) {
						if (wantMine) {
							System.out.println("here 3");
							return null;
						}
						throw new GameLostException("logical mine in spot where free was requested");
					}
					--numberOfMines;
				} else if (board[i][j].getIsLogicalFree()) {
					if (i == spotI && j == spotJ) {
						if (wantMine) {
							throw new GameLostException("logical free in spot where mine was requested");
						}
						System.out.println("return 2");
						return null;
					}
				}
				if (board[i][j].getIsVisible()) {
					if (i == spotI && j == spotJ) {
						throw new Exception("requested (mine/free) cell is visible");
					}
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

		savePositionsOfMinesPerCompPerCountMines.clear();
		for (int i = 0; i < components.size(); ++i) {
			savePositionsOfMinesPerCompPerCountMines.add(new TreeMap<>());
		}

		InterestingCell interestingCell = new InterestingCell(spotI, spotJ, wantMine);

		performBacktrackingSequentially(interestingCell);
		if (interestingCell.cellComponent == -1) {
			throw new Exception("Wanted (interesting) cell is an away cell, I haven't implemented this yet");
		}

		boolean[][] newMines = new boolean[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (board[i][j].getIsLogicalMine()) {
					newMines[i][j] = true;
				}
			}
		}

		ArrayList<TreeSet<Integer>> dpTable = new ArrayList<>(components.size());
		ArrayList<TreeMap<Integer, Integer>> parentTable = new ArrayList<>(components.size());
		for (int i = 0; i < components.size(); ++i) {
			dpTable.add(new TreeSet<>());
			parentTable.add(new TreeMap<>());
		}
		for (int i = 0; i < interestingCell.cellComponent; ++i) {
			if (i == 0) {
				for (int entry : mineConfig.get(i).keySet()) {
					dpTable.get(i).add(entry);
					parentTable.get(i).put(entry, entry);
				}
				continue;
			}
			for (int entry : mineConfig.get(i).keySet()) {
				for (int val : dpTable.get(i - 1)) {
					dpTable.get(i).add(val + entry);
					parentTable.get(i).put(val + entry, entry);
				}
			}
		}
		for (int i = components.size() - 1; i > interestingCell.cellComponent; --i) {
			if (i == components.size() - 1) {
				for (int entry : mineConfig.get(i).keySet()) {
					dpTable.get(i).add(entry);
					parentTable.get(i).put(entry, entry);
				}
				continue;
			}
			for (int entry : mineConfig.get(i).keySet()) {
				for (int val : dpTable.get(i + 1)) {
					dpTable.get(i).add(val + entry);
					parentTable.get(i).put(val + entry, entry);
				}
			}
		}

		final int numberOfAwayCells = AwayCell.getNumberOfAwayCells(board);

		TreeSet<Integer> prev = new TreeSet<>();
		prev.add(0);
		if (interestingCell.cellComponent > 0) {
			prev.clear();
			prev = dpTable.get(interestingCell.cellComponent - 1);
		}
		TreeSet<Integer> after = new TreeSet<>();
		after.add(0);
		if (interestingCell.cellComponent + 1 < components.size()) {
			after.clear();
			after = dpTable.get(interestingCell.cellComponent + 1);
		}
		System.out.println("prev:");
		for (int x : prev) System.out.print(x);
		System.out.println();

		System.out.println("after:");
		for (int x : after) System.out.print(x);
		System.out.println();

		for (TreeMap.Entry<Integer, ArrayList<Pair<Integer, Integer>>> entry : saveGoodMineConfigurations.entrySet()) {
			int minesCurr = entry.getKey();
			for (int minesBefore : prev) {
				System.out.println("numberOfMines: " + numberOfMines);
				System.out.println("lower on: " + (1 + numberOfMines - minesBefore - minesCurr));
				Integer minesAfter = after.lower(1 + numberOfMines - minesBefore - minesCurr);
				if (minesAfter == null || minesBefore + minesAfter + minesCurr < numberOfMines - numberOfAwayCells) {
					continue;
				}
				//found solution
				System.out.println("here, setting curr component mines");

				//set mines of current component
				for (Pair<Integer, Integer> mineSpot : entry.getValue()) {
					System.out.println("here current component, setting: " + mineSpot);
					if (newMines[mineSpot.first][mineSpot.second]) {
						throw new Exception("already a mine, but it shouldn't be");
					}
					newMines[mineSpot.first][mineSpot.second] = true;
				}

				//set mines of all components after current component
				for (int i = interestingCell.cellComponent + 1; i < components.size(); ++i) {
					final int numMinesCurrComponent = Objects.requireNonNull(parentTable.get(i).get(minesAfter));
					for (Pair<Integer, Integer> mineSpot : Objects.requireNonNull(savePositionsOfMinesPerCompPerCountMines.get(i).get(numMinesCurrComponent))) {
						System.out.println("here after, setting: " + mineSpot);
						if (newMines[mineSpot.first][mineSpot.second]) {
							throw new Exception("already a mine, but it shouldn't be");
						}
						newMines[mineSpot.first][mineSpot.second] = true;
					}
					minesAfter -= numMinesCurrComponent;
				}

				//set mines of all components before current component
				for (int i = interestingCell.cellComponent - 1; i >= 0; --i) {
					final int numMinesCurrComponent = Objects.requireNonNull(parentTable.get(i).get(minesBefore));
					for (Pair<Integer, Integer> mineSpot : Objects.requireNonNull(savePositionsOfMinesPerCompPerCountMines.get(i).get(numMinesCurrComponent))) {
						System.out.println("here prev, setting: " + mineSpot);
						if (newMines[mineSpot.first][mineSpot.second]) {
							throw new Exception("already a mine, but it shouldn't be");
						}
						newMines[mineSpot.first][mineSpot.second] = true;
					}
					minesBefore -= numMinesCurrComponent;
				}

				//set mines in away cells
				final int minesLeft = numberOfMines - minesCurr - minesBefore - minesAfter;
				System.out.println("number of away mines: " + minesLeft);
				ArrayList<Pair<Integer, Integer>> allAwayCells = new ArrayList<>();
				for (int i = 0; i < rows; ++i) {
					for (int j = 0; j < cols; ++j) {
						if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
							allAwayCells.add(new Pair<>(i, j));
						}
					}
				}
				if (minesLeft < 0 || minesLeft > allAwayCells.size()) {
					throw new Exception("number of mines left doesn't make a valid configuration");
				}
				//TODO: shuffle away cells array
				for (int i = 0; i < minesLeft; ++i) {
					System.out.println("setting away mine: " + allAwayCells.get(i));
					if (newMines[allAwayCells.get(i).first][allAwayCells.get(i).second]) {
						throw new Exception("already a mine, but it shouldn't be");
					}
					newMines[allAwayCells.get(i).first][allAwayCells.get(i).second] = true;
				}
				System.out.println("return 1");
				return newMines;
			}
		}
		throw new Exception("didn't find solution, but it should exist");
	}

	private static class InterestingCell {
		private final int spotI, spotJ;
		private final boolean wantMine;
		private int cellComponent = -1;

		InterestingCell(int spotI, int spotJ, boolean wantMine) {
			this.spotI = spotI;
			this.spotJ = spotJ;
			this.wantMine = wantMine;
		}

		int getSpotI() {
			return spotI;
		}

		int getSpotJ() {
			return spotJ;
		}

		boolean getWantMine() {
			return wantMine;
		}
	}
}
