package com.example.minesweeper20.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.minesweeper20.R;
import com.example.minesweeper20.activity.GameActivity;
import com.example.minesweeper20.activity.ScaleListener;
import com.example.minesweeper20.customExceptions.HitIterationLimitException;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class GameCanvas extends View {

	static final int cellPixelLength = 150;

	private final ScaleListener scaleListener;
	private final MinesweeperGame minesweeperGame;
	private final Paint black = new Paint();
	private final VisibleTile[][] board;
	private final MinesweeperSolver backtrackingSolver, gaussSolver;
	private final DrawCellHelpers drawCellHelpers;
	private final BigFraction mineProbability = new BigFraction(0);
	private int lastTapRow, lastTapCol;

	public GameCanvas(Context context, AttributeSet attrs) throws Exception {
		super(context, attrs);
		black.setColor(Color.BLACK);
		black.setStrokeWidth(3);
		final GameActivity gameActivity = (GameActivity) getContext();
		final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		final int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
		scaleListener = new ScaleListener(context, this, screenWidth, screenHeight);
		setOnTouchListener(scaleListener);
		minesweeperGame = new MinesweeperGame(
				gameActivity.getNumberOfRows(),
				gameActivity.getNumberOfCols(),
				gameActivity.getNumberOfMines());
		drawCellHelpers = new DrawCellHelpers(context, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
		board = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
		backtrackingSolver = new BacktrackingSolver(
				minesweeperGame.getNumberOfRows(),
				minesweeperGame.getNumberOfCols());
		gaussSolver = new GaussianEliminationSolver(
				minesweeperGame.getNumberOfRows(),
				minesweeperGame.getNumberOfCols());
	}

	public void handleTap(float tapX, float tapY) {
		//TODO: have grid always fill screen
		//eventually I won't need this check, as the grid always fills the screen
		if (tapX < 0f ||
				tapY < 0f ||
				tapX > minesweeperGame.getNumberOfCols() * cellPixelLength ||
				tapY > minesweeperGame.getNumberOfRows() * cellPixelLength) {
			return;
		}
		final int row = (int) (tapY / cellPixelLength);
		final int col = (int) (tapX / cellPixelLength);

		if (!minesweeperGame.getIsGameLost()) {
			lastTapRow = row;
			lastTapCol = col;
		}

		GameActivity gameActivity = (GameActivity) getContext();
		String[] gameChoices = getResources().getStringArray(R.array.game_type);

		try {
			//TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
			if (!gameActivity.getGameMode().equals(gameChoices[0])) {
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				boolean wantMine = gameActivity.getGameMode().equals(gameChoices[1]);
				//TODO: bug: when toggle flags is on + hard mode: this will always put a mine under the cell you just flagged
				boolean[][] newMineLocations = backtrackingSolver.getMineConfiguration(board, 0, row, col, wantMine);
				if (newMineLocations != null) {
					minesweeperGame.changeMineLocations(newMineLocations);
				}
			}

			minesweeperGame.clickCell(row, col, gameActivity.getToggleFlagModeOn());
			if (!minesweeperGame.getIsGameLost() && !(gameActivity.getToggleFlagModeOn() && !minesweeperGame.getCell(row, col).getIsVisible())) {
				if (gameActivity.getToggleBacktrackingHintsOn() || gameActivity.getToggleMineProbabilityOn()) {
					updateSolvedBoardWithBacktrackingSolver();
				} else if (gameActivity.getToggleGaussHintsOn()) {
					updateSolvedBoardWithGaussSolver();
				}
			}
		} catch (Exception e) {
			gameActivity.displayStackTracePopup(e);
			e.printStackTrace();
		}

		gameActivity.updateNumberOfMines(minesweeperGame.getNumberOfMines() - minesweeperGame.getNumberOfFlags());
		invalidate();
	}

	public void updateSolvedBoardWithBacktrackingSolver() throws Exception {
		//TODO: only run solver if board has changed since last time
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		GameActivity gameActivity = (GameActivity) getContext();
		try {
			backtrackingSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
			gameActivity.updateNumberOfSolverIterations(backtrackingSolver.getNumberOfIterations());
		} catch (HitIterationLimitException e) {
			gameActivity.solverHitIterationLimit();
		}
	}

	public void updateSolvedBoardWithGaussSolver() throws Exception {
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		try {
			gaussSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
		} catch (Exception e) {
			GameActivity gameActivity = (GameActivity) getContext();
			gameActivity.displayStackTracePopup(e);
			e.printStackTrace();
		}
	}

	private void drawCell(Canvas canvas, VisibleTile solverCell, MinesweeperGame.Tile gameCell, int i, int j, boolean drawRedBackground) throws Exception {
		final int startX = j * cellPixelLength;
		final int startY = i * cellPixelLength;

		if (cellIsOffScreen(startX, startY)) {
			return;
		}

		if (gameCell.getIsVisible()) {
			drawCellHelpers.drawNumberedCell(canvas, gameCell.getNumberSurroundingMines(), i, j, startX, startY);
			return;
		}

		GameActivity gameActivity = (GameActivity) getContext();


		if (gameActivity.getToggleBacktrackingHintsOn() && gameActivity.getToggleGaussHintsOn()) {
			throw new Exception("can't have both solvers on at once");
		}
		if (gameActivity.getToggleGaussHintsOn() && gameActivity.getToggleMineProbabilityOn()) {
			throw new Exception("can't have gauss hints and probability on");
		}
		if (solverCell.getIsLogicalMine() && !gameCell.isMine()) {
			throw new Exception("solver says: logical mine, but it's not a mine");
		}
		if (solverCell.getIsLogicalFree() && gameCell.isMine()) {
			throw new Exception("gauss solver says: logical free, but it's not free");
		}

		final boolean showHints = (gameActivity.getToggleBacktrackingHintsOn() || gameActivity.getToggleGaussHintsOn());

		boolean displayedLogicalStuff = false;
		if (drawRedBackground) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawEndGameTap(canvas, i, j);
		} else if (solverCell.getIsLogicalMine() && showHints && !gameCell.isFlagged()) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawLogicalMine(canvas, i, j, getResources());
		} else if (solverCell.getIsLogicalFree() && showHints && !gameCell.isFlagged()) {
			displayedLogicalStuff = true;
			drawCellHelpers.drawLogicalFree(canvas, i, j, getResources());
		} else {
			drawCellHelpers.drawBlankCell(canvas, i, j, getResources());
		}

		if (gameCell.isFlagged()) {
			drawCellHelpers.drawFlag(canvas, startX, startY);
			if (minesweeperGame.getIsGameLost() && !gameCell.isMine()) {
				drawCellHelpers.drawBlackX(canvas, startX, startY);
			} else if (solverCell.getIsLogicalFree() && (showHints || gameActivity.getToggleMineProbabilityOn())) {
				drawCellHelpers.drawBlackX(canvas, startX, startY);
			}
		} else if (gameCell.isMine() && (minesweeperGame.getIsGameLost() || gameActivity.getToggleMinesOn())) {
			drawCellHelpers.drawMine(canvas, startX, startY);
		}

		if (gameActivity.getToggleMineProbabilityOn() && !solverCell.getIsVisible() && !displayedLogicalStuff && !gameCell.isFlagged()) {
			mineProbability.setValue(solverCell.getNumberOfMineConfigs());
			mineProbability.divideWith(solverCell.getNumberOfTotalConfigs());
			drawCellHelpers.drawMineProbability(canvas, startX, startY, mineProbability, getResources());
		}
	}

	//TODO: change this logic back to using matrices
	private boolean cellIsOffScreen(int startX, int startY) {
		final float absoluteX = scaleListener.getAbsoluteX();
		final float absoluteY = scaleListener.getAbsoluteY();
		final float scale = scaleListener.getScale();
		final float halfOfScreenWidth = scaleListener.getHalfOfScreenWidth();
		final float halfOfScreenHeight = scaleListener.getHalfOfScreenHeight();

		if ((startX + cellPixelLength + absoluteX - halfOfScreenWidth) * scale + halfOfScreenWidth < 0) {
			return true;
		}
		if ((startY + cellPixelLength + absoluteY - halfOfScreenHeight) * scale + halfOfScreenHeight < 0) {
			return true;
		}
		if ((startX + absoluteX - halfOfScreenWidth) * scale + halfOfScreenWidth > getWidth()) {
			return true;
		}
		return ((startY + absoluteY - halfOfScreenHeight) * scale + halfOfScreenHeight > getHeight());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.setMatrix(scaleListener.getMatrix());

		final int numberOfRows = minesweeperGame.getNumberOfRows();
		final int numberOfCols = minesweeperGame.getNumberOfCols();
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				try {
					drawCell(canvas, board[i][j], minesweeperGame.getCell(i, j), i, j, (minesweeperGame.getIsGameLost() && i == lastTapRow && j == lastTapCol));
				} catch (Exception e) {
					GameActivity gameActivity = (GameActivity) getContext();
					gameActivity.displayStackTracePopup(e);
					e.printStackTrace();
				}
			}
		}
		for (int j = 0; j <= numberOfCols; ++j) {
			canvas.drawLine(j * cellPixelLength, 0, j * cellPixelLength, numberOfRows * cellPixelLength, black);
		}
		for (int i = 0; i <= numberOfRows; ++i) {
			canvas.drawLine(0, i * cellPixelLength, numberOfCols * cellPixelLength, i * cellPixelLength, black);
		}
		if (minesweeperGame.getIsGameWon()) {
			setOnTouchListener(null);
			((GameActivity) getContext()).displayGameWonPopup();
		}
	}
}
