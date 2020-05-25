package com.example.minesweeper20.minesweeperStuff;

import android.util.Pair;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.helpers.ArrayBounds;
import com.example.minesweeper20.helpers.GetConnectedComponents;
import com.example.minesweeper20.helpers.MutableInt;

import java.util.ArrayList;
import java.util.Collections;

public class SlowBacktrackingSolver implements MinesweeperSolver {

	private Integer rows, cols;
	private final static Integer iterationLimit = 500000;

	private final ArrayList<ArrayList<Pair<Integer,Integer>>> lastUnvisitedSpot;
	private final ArrayList<ArrayList<Boolean>> isBomb;
	private final ArrayList<ArrayList<Integer>> cntSurroundingBombs;
	private ArrayList<ArrayList<VisibleTile>> board;
	private Integer numberOfBombs;

	public SlowBacktrackingSolver(int rows, int cols) {
		isBomb = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Boolean> currRow = new ArrayList<>(Collections.nCopies(cols, false));
			isBomb.add(currRow);
		}

		cntSurroundingBombs = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Integer> currRow = new ArrayList<>(Collections.nCopies(cols, 0));
			cntSurroundingBombs.add(currRow);
		}
		lastUnvisitedSpot = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<Pair<Integer,Integer>> currRow = new ArrayList<>(cols);
			for(int j = 0; j < cols; ++j) {
				currRow.add(null);
			}
			lastUnvisitedSpot.add(currRow);
		}
	}

	@Override
	public void solvePosition(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs) throws Exception {
		initialize(_board, _numberOfBombs);

		if(GetConnectedComponents.allCellsAreHidden(board)) {
			for(int i = 0; i < rows; ++i) {
				for(int j = 0; j < cols; ++j) {
					board.get(i).get(j).numberOfBombConfigs.setValues(numberOfBombs, 1);
					board.get(i).get(j).numberOfTotalConfigs.setValues(rows*cols, 1);
				}
			}
			return;
		}

		ArrayList<Pair<Integer,Integer>> component = new ArrayList<>();
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				if(!board.get(i).get(j).getIsVisible()) {
					component.add(new Pair<>(i,j));
				}
			}
		}
		initializeLastUnvisitedSpot(component);

		MutableInt currIterations = new MutableInt(0);
		MutableInt currNumberOfBombs = new MutableInt(0);
		solveComponent(0, component, currIterations, currNumberOfBombs);

		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				VisibleTile curr = board.get(i).get(j);
				if(curr.getIsVisible()) {
					continue;
				}
				if(curr.numberOfTotalConfigs.equals(0)) {
					throw new Exception("There should be at least one bomb configuration for non-visible cells");
				}
				if(curr.numberOfBombConfigs.equals(0)) {
					curr.isLogicalFree = true;
				} else if(curr.numberOfBombConfigs.equals(curr.numberOfTotalConfigs)) {
					curr.isLogicalBomb = true;
				}
			}
		}
	}

	private void initializeLastUnvisitedSpot(ArrayList<Pair<Integer,Integer>> component) {
		for (Pair<Integer,Integer> spot : component) {
			for (int di = -1; di <= 1; ++di) {
				for (int dj = -1; dj <= 1; ++dj) {
					if (di == 0 && dj == 0) {
						continue;
					}
					final int adjI = spot.first + di;
					final int adjJ = spot.second + dj;
					if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
						continue;
					}
					if (board.get(adjI).get(adjJ).isVisible) {
						lastUnvisitedSpot.get(adjI).set(adjJ, spot);
					}
				}
			}
		}
	}

	public ArrayList<ArrayList<Boolean>> getBombConfiguration(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs, int _spotI, int _spotJ, boolean _wantBomb) throws Exception {
		throw new Exception("to make warning go away");
	}

	private void initialize(ArrayList<ArrayList<VisibleTile>> _board, int _numberOfBombs) throws Exception {
		board = _board;
		numberOfBombs = _numberOfBombs;
		Pair<Integer,Integer> dimensions = ArrayBounds.getArrayBounds(board);
		rows = dimensions.first;
		cols = dimensions.second;
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				isBomb.get(i).set(j,false);
				cntSurroundingBombs.get(i).set(j,0);
			}
		}
	}

	private void solveComponent(int pos, ArrayList<Pair<Integer,Integer>> component, MutableInt currIterations, MutableInt currNumberOfBombs) throws Exception {
		if(pos == component.size()) {
			checkSolution(currNumberOfBombs.get());
			return;
		}
		currIterations.addWith(1);
		if(currIterations.get() >= iterationLimit) {
			throw new HitIterationLimitException();
		}
		final int i = component.get(pos).first;
		final int j = component.get(pos).second;

		//try bomb
		isBomb.get(i).set(j, true);
		if(checkSurroundingConditions(i,j, component.get(pos), 1)) {
			currNumberOfBombs.addWith(1);
			updateSurroundingBombCnt(i,j,1);
			solveComponent(pos+1, component, currIterations, currNumberOfBombs);
			updateSurroundingBombCnt(i,j,-1);
			currNumberOfBombs.addWith(-1);
		}

		//try free
		isBomb.get(i).set(j, false);
		if(checkSurroundingConditions(i,j,component.get(pos), 0)) {
			solveComponent(pos+1, component, currIterations, currNumberOfBombs);
		}
	}

	private void updateSurroundingBombCnt(int i, int j, int delta) {
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				if(board.get(adjI).get(adjJ).isVisible) {
					final int cnt = cntSurroundingBombs.get(adjI).get(adjJ);
					cntSurroundingBombs.get(adjI).set(adjJ, cnt + delta);
				}
			}
		}
	}

	private boolean checkSurroundingConditions(int i, int j, Pair<Integer,Integer> currSpot, int arePlacingABomb) {
		for(int di = -1; di <= 1; ++di) {
			for(int dj = -1; dj <= 1; ++dj) {
				if (di == 0 && dj == 0) {
					continue;
				}
				final int adjI = i + di;
				final int adjJ = j + dj;
				if(ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
					continue;
				}
				VisibleTile adjTile = board.get(adjI).get(adjJ);
				if(!adjTile.isVisible) {
					continue;
				}
				final int currBacktrackingCount = cntSurroundingBombs.get(adjI).get(adjJ);
				if(currBacktrackingCount + arePlacingABomb > adjTile.numberSurroundingBombs) {
					return false;
				}
				if(lastUnvisitedSpot.get(adjI).get(adjJ).equals(currSpot) && currBacktrackingCount + arePlacingABomb != adjTile.numberSurroundingBombs) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkSolution(int currNumberOfBombs) throws Exception {
		if(!checkPositionValidity(currNumberOfBombs)) {
			return;
		}

		if(currNumberOfBombs != numberOfBombs) {
			return;
		}
		//printBoardDebug();
		for(int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if(board.get(i).get(j).getIsVisible()) {
					continue;
				}
				if (isBomb.get(i).get(j)) {
					board.get(i).get(j).numberOfBombConfigs.addWith(1);
				}
				board.get(i).get(j).numberOfTotalConfigs.addWith(1);
			}
		}
	}

	//returns true if valid
	private boolean checkPositionValidity(int currNumberOfBombs) throws Exception {
		for(int i = 0; i < rows; ++i) {
			for(int j = 0; j < cols; ++j) {
				for (int di = -1; di <= 1; ++di) {
					for (int dj = -1; dj <= 1; ++dj) {
						if (di == 0 && dj == 0) {
							continue;
						}
						final int adjI = i + di;
						final int adjJ = j + dj;
						if (ArrayBounds.outOfBounds(adjI, adjJ, rows, cols)) {
							continue;
						}
						VisibleTile adjTile = board.get(adjI).get(adjJ);
						if (!adjTile.isVisible) {
							continue;
						}
						if (!cntSurroundingBombs.get(adjI).get(adjJ).equals(adjTile.numberSurroundingBombs)) {
							return false;
						}
					}
				}
			}
		}
		int prevNumberOfBombs = 0;
		for(int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				if (isBomb.get(i).get(j)) {
					++prevNumberOfBombs;
				}
			}
		}
		if(prevNumberOfBombs != currNumberOfBombs) {
			throw new Exception("number of bombs doesn't match");
		}

		return true;
	}
}
