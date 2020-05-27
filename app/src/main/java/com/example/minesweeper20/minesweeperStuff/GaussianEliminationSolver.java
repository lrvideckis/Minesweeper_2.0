package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetConnectedComponents;

import java.util.Arrays;

public class GaussianEliminationSolver implements MinesweeperSolver {

	private static final double EPSILON = 0.00000001;
	private final int rows, cols;
	private final int[][] hiddenNodeToId, idToHiddenNode;

	public GaussianEliminationSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		hiddenNodeToId = new int[rows][cols];
		idToHiddenNode = new int[rows*cols][2];
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfBombs) throws Exception {
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if(rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}
		int numberOfHiddenNodes = 0, numberOfClues = 0;
		for(int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if(cell.getIsVisible()) {
					if(cell.getNumberSurroundingBombs() > 0) {
						++numberOfClues;
					}
					continue;
				}
				if(GetConnectedComponents.isAwayCell(board, i, j, rows, cols)) {
					continue;
				}
				hiddenNodeToId[i][j] = numberOfHiddenNodes;
				idToHiddenNode[numberOfHiddenNodes][0] = i;
				idToHiddenNode[numberOfHiddenNodes][1] = j;
				numberOfHiddenNodes++;
			}
		}
		double[][] matrix = new double[numberOfClues][numberOfHiddenNodes + 1];
		int currentClue = 0;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if(!cell.getIsVisible() || cell.getNumberSurroundingBombs() == 0) {
					continue;
				}
				for(int di = -1; di <= 1; ++di) {
					for(int dj = -1; dj <= 1; ++dj) {
						if(di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i+di;
						final int adjJ = j+dj;
						if(ArrayBounds.outOfBounds(adjI,adjJ, rows, cols)) {
							continue;
						}
						if(board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						matrix[currentClue][hiddenNodeToId[adjI][adjJ]] = 1;
					}
				}
				matrix[currentClue][numberOfHiddenNodes] = cell.getNumberSurroundingBombs();
				++currentClue;
			}
		}

		performGaussianElimination(matrix);

		boolean[] isBomb = new boolean[numberOfHiddenNodes + 1];
		boolean[] isFree = new boolean[numberOfHiddenNodes + 1];
		for (double[] currRow : matrix) {
			Arrays.fill(isBomb, false);
			Arrays.fill(isFree, false);
			checkRowForSolvableStuff(currRow, isBomb, isFree);
			for(int i = 0; i+1 < currRow.length; ++i) {
				if(isBomb[i] && isFree[i]) {
					throw new Exception("can't be both a bomb and free");
				}
				if(isBomb[i]) {
					board[idToHiddenNode[i][0]][idToHiddenNode[i][1]].isLogicalBomb = true;
				} else if(isFree[i]) {
					board[idToHiddenNode[i][0]][idToHiddenNode[i][1]].isLogicalFree = true;
				}
			}
		}
	}

	private void checkRowForSolvableStuff(double[] currRow, boolean[] isBomb, boolean[] isFree) {
		double sumPos = 0, sumNeg = 0;
		for(int i = 0; i+1 < currRow.length; ++i) {
			if(currRow[i] > 0.0) {
				sumPos += currRow[i];
			}
			if(currRow[i] < 0.0) {
				sumNeg += currRow[i];
			}
		}
		if(Math.abs(sumPos - currRow[currRow.length-1]) < EPSILON) {
			for(int i = 0; i+1 < currRow.length; ++i) {
				if(currRow[i] > 0.0) {
					isBomb[i] = true;
				}
				if(currRow[i] < 0.0) {
					isFree[i] = true;
				}
			}
			return;
		}
		if(Math.abs(sumNeg - currRow[currRow.length-1]) < EPSILON) {
			for(int i = 0; i+1 < currRow.length; ++i) {
				if(currRow[i] > 0.0) {
					isFree[i] = true;
				}
				if(currRow[i] < 0.0) {
					isBomb[i] = true;
				}
			}
		}
	}

	private void performGaussianElimination(double[][] matrix) {
		if(matrix.length == 0 || matrix[0].length == 0) {
			return;
		}
		final int rows = matrix.length;
		final int cols = matrix[0].length;
		for(int col = 0, row = 0; col < cols && row < rows; ++col) {
			int sel = row;
			for(int i = row+1; i < rows; ++i) {
				if (Math.abs(matrix[i][col]) > Math.abs(matrix[sel][col])) {
					sel = i;
				}
			}
			if(Math.abs(matrix[sel][col]) < EPSILON) {
				continue;
			}
			for(int j = 0; j < cols; ++j) {
				double temp = matrix[sel][j];
				matrix[sel][j] = matrix[row][j];
				matrix[row][j] = temp;
			}
			double s = (1.0 / matrix[row][col]);
			for(int j = 0; j < cols; ++j) {
				matrix[row][j] = matrix[row][j] * s;
			}
			for(int i = 0; i < rows; ++i) {
				if (i != row && Math.abs(matrix[i][col]) > EPSILON) {
					double t = matrix[i][col];
					for(int j = 0; j < cols; ++j) {
						matrix[i][j] = matrix[i][j] - (matrix[row][j] * t);
					}
				}
			}
			++row;
		}
	}

	@Override
	public boolean[][] getBombConfiguration(VisibleTile[][] _board, int _numberOfBombs, int _spotI, int _spotJ, boolean _wantBomb) throws Exception {
		throw new Exception("not implemented yet");
	}
}
