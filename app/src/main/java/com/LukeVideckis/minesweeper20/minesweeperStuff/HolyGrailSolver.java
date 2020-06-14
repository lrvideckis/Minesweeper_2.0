package com.LukeVideckis.minesweeper20.minesweeperStuff;

public class HolyGrailSolver implements BacktrackingSolver {

	private int numberOfIterations = 0;
	//TODO: try to change MyBacktrackingSolver back to BacktrackingSolver interface
	private final MyBacktrackingSolver myBacktrackingSolver;
	private final MinesweeperSolver gaussSolver;

	public HolyGrailSolver(int rows, int cols) {
		myBacktrackingSolver = new MyBacktrackingSolver(rows, cols);
		gaussSolver = new GaussianEliminationSolver(rows, cols);
	}

	@Override
	public boolean[][] getMineConfiguration(VisibleTile[][] board, int numberOfMines, int spotI, int spotJ, boolean wantMine) throws Exception {
		throw new Exception("not implemented");
	}

	@Override
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	@Override
	public void solvePosition(VisibleTile[][] board, int numberOfMines) throws Exception {
		gaussSolver.solvePosition(board, numberOfMines);
		myBacktrackingSolver.solvePosition(board, numberOfMines);
		numberOfIterations = myBacktrackingSolver.getNumberOfIterations();
	}

	public void doPerformCheckPositionValidity() {
		myBacktrackingSolver.doPerformCheckPositionValidity();
	}
}
