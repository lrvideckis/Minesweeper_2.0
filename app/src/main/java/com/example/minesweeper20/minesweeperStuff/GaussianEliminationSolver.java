package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;

import java.util.Arrays;
import java.util.TreeSet;

//TODO: make this account for number of mines (by adding a single row: 1,1,1,1,..., #mines) to the matrix
public class GaussianEliminationSolver implements MinesweeperSolver {

	private static final double EPSILON = 0.00000001;
	private final int rows, cols;
	private final int[][] hiddenNodeToId, idToHiddenNode;

	public GaussianEliminationSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		hiddenNodeToId = new int[rows][cols];
		idToHiddenNode = new int[rows * cols][2];
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}
		int numberOfHiddenNodes = 0, numberOfClues = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if (cell.getIsVisible()) {
					if (cell.getNumberSurroundingMines() > 0) {
						++numberOfClues;
					}
					continue;
				}
				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					continue;
				}
				hiddenNodeToId[i][j] = numberOfHiddenNodes;
				idToHiddenNode[numberOfHiddenNodes][0] = i;
				idToHiddenNode[numberOfHiddenNodes][1] = j;
				numberOfHiddenNodes++;
			}
		}

		TreeSet<Integer> knownMines = new TreeSet<>(), knownFrees = new TreeSet<>();
		//noinspection StatementWithEmptyBody
		while (runGaussSolverOnce(knownMines, knownFrees, board, numberOfClues, numberOfHiddenNodes))
			;

		for (int i : knownMines) {
			if (knownFrees.contains(i)) {
				throw new Exception("cell can't be both a logical mine, and logical free " + idToHiddenNode[i][0] + " " + idToHiddenNode[i][1]);
			}
			board[idToHiddenNode[i][0]][idToHiddenNode[i][1]].isLogicalMine = true;
		}
		for (int i : knownFrees) {
			board[idToHiddenNode[i][0]][idToHiddenNode[i][1]].isLogicalFree = true;
		}
	}

	private boolean runGaussSolverOnce(
			TreeSet<Integer> knownMines,
			TreeSet<Integer> knownFrees,
			VisibleTile[][] board,
			int numberOfClues,
			int numberOfHiddenNodes
	) throws Exception {
		double[][] matrix = new double[numberOfClues + knownMines.size() + knownFrees.size()][numberOfHiddenNodes + 1];
		int currentClue = 0;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if (!cell.getIsVisible() || cell.getNumberSurroundingMines() == 0) {
					continue;
				}
				for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].getIsVisible()) {
						continue;
					}
					matrix[currentClue][hiddenNodeToId[adjI][adjJ]] = 1;
				}
				matrix[currentClue][numberOfHiddenNodes] = cell.getNumberSurroundingMines();
				++currentClue;
			}
		}

		for (int i : knownMines) {
			matrix[currentClue][i] = 1;
			matrix[currentClue][numberOfHiddenNodes] = 1;
			++currentClue;
		}

		for (int i : knownFrees) {
			matrix[currentClue++][i] = 1;
		}

		performGaussianElimination(matrix);

		boolean foundNewStuff = false;
		boolean[] isMine = new boolean[numberOfHiddenNodes + 1];
		boolean[] isFree = new boolean[numberOfHiddenNodes + 1];
		for (double[] currRow : matrix) {
			Arrays.fill(isMine, false);
			Arrays.fill(isFree, false);
			checkRowForSolvableStuff(currRow, isMine, isFree);
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (isMine[i] && isFree[i]) {
					throw new Exception("can't be both a mine and free");
				}
				if (isMine[i] && !knownMines.contains(i)) {
					foundNewStuff = true;
					knownMines.add(i);
				}
				if (isFree[i] && !knownFrees.contains(i)) {
					foundNewStuff = true;
					knownFrees.add(i);
				}
			}
		}

		return (foundNewStuff || checkForTrivialStuff(knownMines, knownFrees, board));
	}

	private boolean checkForTrivialStuff(TreeSet<Integer> knownMines, TreeSet<Integer> knownFrees, VisibleTile[][] board) {
		boolean foundNewStuff = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if (!cell.getIsVisible() || cell.getNumberSurroundingMines() == 0) {
					continue;
				}
				int cntAdjacentMines = 0, cntAdjacentFrees = 0, cntTotalAdjacentCells = 0;
				final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i, j, rows, cols);
				for (int[] adj : adjCells) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].getIsVisible()) {
						continue;
					}
					++cntTotalAdjacentCells;
					if (knownMines.contains(hiddenNodeToId[adjI][adjJ])) {
						++cntAdjacentMines;
					}
					if (knownFrees.contains(hiddenNodeToId[adjI][adjJ])) {
						++cntAdjacentFrees;
					}
				}
				if (cntAdjacentMines == cell.getNumberSurroundingMines()) {
					//anything that's not a mine is free
					for (int[] adj : adjCells) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						final int currID = hiddenNodeToId[adjI][adjJ];
						if (knownMines.contains(currID)) {
							continue;
						}
						if (!knownFrees.contains(currID)) {
							foundNewStuff = true;
							knownFrees.add(currID);
						}
					}
				}
				if (cntTotalAdjacentCells - cntAdjacentFrees == cell.getNumberSurroundingMines()) {
					//anything that's not free is a mine
					for (int[] adj : adjCells) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						final int currID = hiddenNodeToId[adjI][adjJ];
						if (knownFrees.contains(currID)) {
							continue;
						}
						if (!knownMines.contains(currID)) {
							foundNewStuff = true;
							knownMines.add(currID);
						}
					}
				}
			}
		}
		return foundNewStuff;
	}

	private void checkRowForSolvableStuff(double[] currRow, boolean[] isMine, boolean[] isFree) {
		if (Math.abs(currRow[currRow.length - 1]) < EPSILON) {
			return;
		}
		double sumPos = 0, sumNeg = 0;
		for (int i = 0; i + 1 < currRow.length; ++i) {
			if (Math.abs(currRow[i]) < EPSILON) {
				continue;
			}
			if (currRow[i] > 0.0) {
				sumPos += currRow[i];
			}
			if (currRow[i] < 0.0) {
				sumNeg += currRow[i];
			}
		}
		if (Math.abs(sumPos - currRow[currRow.length - 1]) < EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < EPSILON) {
					continue;
				}
				if (currRow[i] > 0.0) {
					isMine[i] = true;
				}
				if (currRow[i] < 0.0) {
					isFree[i] = true;
				}
			}
			return;
		}
		if (Math.abs(sumNeg - currRow[currRow.length - 1]) < EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < EPSILON) {
					continue;
				}
				if (currRow[i] > 0.0) {
					isFree[i] = true;
				}
				if (currRow[i] < 0.0) {
					isMine[i] = true;
				}
			}
		}
	}

	private void performGaussianElimination(double[][] matrix) {
		if (matrix.length == 0 || matrix[0].length == 0) {
			return;
		}
		final int rows = matrix.length;
		final int cols = matrix[0].length;
		for (int col = 0, row = 0; col < cols && row < rows; ++col) {
			int sel = row;
			for (int i = row + 1; i < rows; ++i) {
				if (Math.abs(matrix[i][col]) > Math.abs(matrix[sel][col])) {
					sel = i;
				}
			}
			if (Math.abs(matrix[sel][col]) < EPSILON) {
				continue;
			}
			for (int j = 0; j < cols; ++j) {
				double temp = matrix[sel][j];
				matrix[sel][j] = matrix[row][j];
				matrix[row][j] = temp;
			}
			double s = (1.0 / matrix[row][col]);
			for (int j = 0; j < cols; ++j) {
				matrix[row][j] = matrix[row][j] * s;
			}
			for (int i = 0; i < rows; ++i) {
				if (i != row && Math.abs(matrix[i][col]) > EPSILON) {
					double t = matrix[i][col];
					for (int j = 0; j < cols; ++j) {
						matrix[i][j] = matrix[i][j] - (matrix[row][j] * t);
					}
				}
			}
			++row;
		}
	}

	@Override
	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {
		throw new Exception("not implemented yet");
	}

	@Override
	public int getNumberOfIterations() {
		return -1;
	}
}
