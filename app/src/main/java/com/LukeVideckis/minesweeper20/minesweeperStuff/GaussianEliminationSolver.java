package com.LukeVideckis.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.ArrayBounds;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.AwayCell;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.GetAdjacentCells;
import com.LukeVideckis.minesweeper20.minesweeperStuff.minesweeperHelpers.MyMath;

import java.util.ArrayList;
import java.util.Arrays;

//TODO: make this account for number of mines (by adding a single row: 1,1,1,1,..., #mines) to the matrix
public class GaussianEliminationSolver implements MinesweeperSolver {

	private final int rows, cols;
	private final int[][] hiddenNodeToId, idToHiddenNode, newSurroundingMineCounts;

	public GaussianEliminationSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		hiddenNodeToId = new int[rows][cols];
		idToHiddenNode = new int[rows * cols][2];
		newSurroundingMineCounts = new int[rows][cols];
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		Pair<Integer, Integer> dimensions = ArrayBounds.getArrayBounds(board);
		if (rows != dimensions.first || cols != dimensions.second) {
			throw new Exception("dimensions of board doesn't match what was passed in the constructor");
		}

		//noinspection StatementWithEmptyBody
		while (runGaussSolverOnce(board))
			;
	}

	//returns true if extra stuff is found
	private boolean runGaussSolverOnce(VisibleTile[][] board) throws Exception {
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final VisibleTile cell = board[i][j];
				if (cell.isLogicalMine && cell.isLogicalFree) {
					throw new Exception("cell can't be both logical mine and free");
				}
				if (!cell.getIsVisible() && cell.getNumberSurroundingMines() > 0) {
					throw new Exception("non-visible cells should have 0 surrounding mines, but it doesn't");
				}
				newSurroundingMineCounts[i][j] = cell.getNumberSurroundingMines();
				hiddenNodeToId[i][j] = -1;
			}
		}

		int numberOfHiddenNodes = 0, numberOfClues = 0;
		ArrayList<Pair<Integer, Integer>> clueSpots = new ArrayList<>();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final VisibleTile cell = board[i][j];
				if (cell.getIsVisible()) {
					boolean foundAdjacentUnknown = false;
					for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].isLogicalMine) {
							--newSurroundingMineCounts[i][j];
						}
						if (!board[adjI][adjJ].isLogicalMine && !board[adjI][adjJ].isLogicalFree) {
							foundAdjacentUnknown = true;
						}
					}
					if (newSurroundingMineCounts[i][j] > 0 && foundAdjacentUnknown) {
						++numberOfClues;
						clueSpots.add(new Pair<>(i, j));
					}
					continue;
				}
				if (AwayCell.isAwayCell(board, i, j, rows, cols)) {
					continue;
				}
				if (cell.isLogicalFree || cell.isLogicalMine) {
					continue;
				}
				hiddenNodeToId[i][j] = numberOfHiddenNodes;
				idToHiddenNode[numberOfHiddenNodes][0] = i;
				idToHiddenNode[numberOfHiddenNodes][1] = j;
				numberOfHiddenNodes++;
			}
		}

		double[][] matrix = new double[numberOfClues][numberOfHiddenNodes + 1];
		for (int currentClue = 0; currentClue < clueSpots.size(); ++currentClue) {
			final int i = clueSpots.get(currentClue).first;
			final int j = clueSpots.get(currentClue).second;
			for (int[] adj : GetAdjacentCells.getAdjacentCells(i, j, rows, cols)) {
				final int adjI = adj[0], adjJ = adj[1];
				if (board[adjI][adjJ].getIsVisible() || board[adjI][adjJ].isLogicalMine || board[adjI][adjJ].isLogicalFree) {
					continue;
				}
				if (hiddenNodeToId[adjI][adjJ] == -1) {
					throw new Exception("adjacent node should have an id");
				}
				matrix[currentClue][hiddenNodeToId[adjI][adjJ]] = 1;
			}
			matrix[currentClue][numberOfHiddenNodes] = newSurroundingMineCounts[i][j];
		}

		MyMath.performGaussianElimination(matrix);

		boolean foundNewStuff = false;
		boolean[] isMine = new boolean[numberOfHiddenNodes];
		boolean[] isFree = new boolean[numberOfHiddenNodes];
		for (double[] currRow : matrix) {
			Arrays.fill(isMine, false);
			Arrays.fill(isFree, false);
			checkRowForSolvableStuff(currRow, isMine, isFree);
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (isMine[i] && isFree[i]) {
					throw new Exception("can't be both a mine and free");
				}
				final int gridI = idToHiddenNode[i][0];
				final int gridJ = idToHiddenNode[i][1];
				if (isMine[i] && !board[gridI][gridJ].isLogicalMine) {
					foundNewStuff = true;
					board[gridI][gridJ].isLogicalMine = true;
				}
				if (isFree[i] && !board[gridI][gridJ].isLogicalFree) {
					foundNewStuff = true;
					board[gridI][gridJ].isLogicalFree = true;
				}
			}
		}

		return (foundNewStuff || checkForTrivialStuff(board));
	}

	//TODO: pull this out into static helper
	private boolean checkForTrivialStuff(VisibleTile[][] board) {
		boolean foundNewStuff = false;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				VisibleTile cell = board[i][j];
				if (!cell.getIsVisible()) {
					continue;
				}
				final int[][] adjCells = GetAdjacentCells.getAdjacentCells(i, j, rows, cols);
				int cntAdjacentMines = 0, cntAdjacentFrees = 0, cntTotalAdjacentCells = 0;
				for (int[] adj : adjCells) {
					final int adjI = adj[0], adjJ = adj[1];
					if (board[adjI][adjJ].getIsVisible()) {
						continue;
					}
					++cntTotalAdjacentCells;
					if (board[adjI][adjJ].isLogicalMine) {
						++cntAdjacentMines;
					}
					if (board[adjI][adjJ].isLogicalFree) {
						++cntAdjacentFrees;
					}
				}
				if (cntTotalAdjacentCells == 0) {
					continue;
				}
				if (cntAdjacentMines == cell.getNumberSurroundingMines()) {
					//anything that's not a mine is free
					for (int[] adj : adjCells) {
						final int adjI = adj[0], adjJ = adj[1];
						if (board[adjI][adjJ].getIsVisible()) {
							continue;
						}
						if (board[adjI][adjJ].isLogicalMine) {
							continue;
						}
						if (!board[adjI][adjJ].isLogicalFree) {
							foundNewStuff = true;
							board[adjI][adjJ].isLogicalFree = true;
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
						if (board[adjI][adjJ].isLogicalFree) {
							continue;
						}
						if (!board[adjI][adjJ].isLogicalMine) {
							foundNewStuff = true;
							board[adjI][adjJ].isLogicalMine = true;
						}
					}
				}
			}
		}
		return foundNewStuff;
	}

	private void checkRowForSolvableStuff(double[] currRow, boolean[] isMine, boolean[] isFree) {
		if (Math.abs(currRow[currRow.length - 1]) < MyMath.EPSILON) {
			return;
		}
		double sumPos = 0, sumNeg = 0;
		for (int i = 0; i + 1 < currRow.length; ++i) {
			if (Math.abs(currRow[i]) < MyMath.EPSILON) {
				continue;
			}
			if (currRow[i] > 0.0) {
				sumPos += currRow[i];
			}
			if (currRow[i] < 0.0) {
				sumNeg += currRow[i];
			}
		}
		if (Math.abs(sumPos - currRow[currRow.length - 1]) < MyMath.EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < MyMath.EPSILON) {
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
		if (Math.abs(sumNeg - currRow[currRow.length - 1]) < MyMath.EPSILON) {
			for (int i = 0; i + 1 < currRow.length; ++i) {
				if (Math.abs(currRow[i]) < MyMath.EPSILON) {
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
}
