package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetConnectedComponents;

import java.util.Arrays;

public class GaussianEliminationSolver implements MinesweeperSolver {

	private static final double EPSILON = 0.00000001;
	private final int rows, cols;
	private final int[][] hiddenNodeToId;

	public GaussianEliminationSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		hiddenNodeToId = new int[rows][cols];
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
				hiddenNodeToId[i][j] = numberOfHiddenNodes++;
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
		System.out.println("before matrix is:");
		for (double[] doubles : matrix) {
			System.out.println(Arrays.toString(doubles));
		}

		performGaussianElimination(matrix);

		System.out.println("before matrix is:");
		for (double[] doubles : matrix) {
			System.out.println(Arrays.toString(doubles));
		}
	}

	private void performGaussianElimination(double[][] matrix) {
		int n = matrix.length;
		int m = matrix[0].length-1;
		for(int col = 0, row = 0; col < m && row < n; ++col) {
			int sel = row;
			for(int i = row+1; i < n; ++i) {
				if (Math.abs(matrix[i][col]) > Math.abs(matrix[sel][col])) {
					sel = i;
				}
			}
			if(Math.abs(matrix[sel][col]) < EPSILON) {
				continue;
			}
			for(int j = 0; j < m; ++j) {
				double temp = matrix[sel][j];
				matrix[sel][j] = matrix[row][j];
				matrix[row][j] = temp;
			}
			double s = (1.0 / matrix[row][col]);
			for(int j = 0; j < m; ++j) {
				matrix[row][j] = matrix[row][j] * s;
			}
			for(int i = 0; i < n; ++i) {
				if (i != row && Math.abs(matrix[i][col]) > EPSILON) {
					double t = matrix[i][col];
					for(int j = 0; j < m; ++j) {
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
