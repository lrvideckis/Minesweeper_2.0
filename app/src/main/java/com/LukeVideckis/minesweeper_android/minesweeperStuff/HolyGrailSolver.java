package com.LukeVideckis.minesweeper_android.minesweeperStuff;

public class HolyGrailSolver implements BacktrackingSolver {

	//TODO: try to change MyBacktrackingSolver back to BacktrackingSolver interface
	private final MyBacktrackingSolver myBacktrackingSolver;
	private final MinesweeperSolver gaussSolver;
	private final VisibleTileWithProbability[][] tempBoardWithProbability;
	private final int rows, cols;

	public HolyGrailSolver(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		gaussSolver = new GaussianEliminationSolver(rows, cols);
		tempBoardWithProbability = new VisibleTileWithProbability[rows][cols];
	}

	@Override
	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {
		throw new Exception("not implemented");
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
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
		gaussSolver.solvePosition(board, numberOfMines);
		myBacktrackingSolver.solvePosition(board, numberOfMines);
	}

	public void doPerformCheckPositionValidity() {
		myBacktrackingSolver.doPerformCheckPositionValidity();
	}
}
