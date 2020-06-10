package com.example.minesweeper20.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.minesweeper20.activity.GameActivity;
import com.example.minesweeper20.activity.ScaleListener;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.BigFraction;

import static com.example.minesweeper20.minesweeperStuff.MinesweeperSolver.VisibleTile;

public class GameCanvas extends View {

	private ScaleListener scaleListener;
	private final Paint black = new Paint();
	private final DrawCellHelpers drawCellHelpers;
	private final BigFraction mineProbability = new BigFraction(0);
	private final RectF tempCellRect = new RectF();

	public GameCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		black.setColor(Color.BLACK);
		black.setStrokeWidth(3);
		final GameActivity gameActivity = (GameActivity) getContext();
		scaleListener = new ScaleListener(context, this, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
		setOnTouchListener(scaleListener);
		drawCellHelpers = new DrawCellHelpers(context, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		scaleListener.setScreenWidthAndHeight(getWidth(), getHeight());
	}

	private void drawCell(Canvas canvas, VisibleTile solverCell, MinesweeperGame.Tile gameCell, int i, int j, boolean drawRedBackground) throws Exception {
		final int startX = j * GameActivity.cellPixelLength;
		final int startY = i * GameActivity.cellPixelLength;

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
		if (gameActivity.getMinesweeperGame().getIsGameWon() && gameActivity.getMinesweeperGame().getIsGameLost()) {
			throw new Exception("game is both won and lost");
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
			if (gameActivity.getMinesweeperGame().getIsGameLost() && !gameCell.isMine()) {
				drawCellHelpers.drawBlackX(canvas, startX, startY);
			} else if (solverCell.getIsLogicalFree() && (showHints || gameActivity.getToggleMineProbabilityOn())) {
				drawCellHelpers.drawBlackX(canvas, startX, startY);
			}
		} else if (gameCell.isMine() && (gameActivity.getMinesweeperGame().getIsGameLost() || gameActivity.getToggleMinesOn())) {
			drawCellHelpers.drawMine(canvas, startX, startY);
		}

		if (gameActivity.getToggleMineProbabilityOn() && !solverCell.getIsVisible() && !displayedLogicalStuff && !gameCell.isFlagged()) {
			mineProbability.setValue(solverCell.getNumberOfMineConfigs());
			mineProbability.divideWith(solverCell.getNumberOfTotalConfigs());
			drawCellHelpers.drawMineProbability(canvas, startX, startY, mineProbability, getResources());
		}
	}

	private boolean cellIsOffScreen(int startX, int startY) {
		tempCellRect.set(startX, startY, startX + GameActivity.cellPixelLength, startY + GameActivity.cellPixelLength);
		scaleListener.getMatrix().mapRect(tempCellRect);
		return (tempCellRect.right < 0 || tempCellRect.bottom < 0 || tempCellRect.top > getHeight() || tempCellRect.left > getWidth());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.setMatrix(scaleListener.getMatrix());

		GameActivity gameActivity = (GameActivity) getContext();

		final int numberOfRows = gameActivity.getMinesweeperGame().getNumberOfRows();
		final int numberOfCols = gameActivity.getMinesweeperGame().getNumberOfCols();
		for (int i = 0; i < numberOfRows; ++i) {
			for (int j = 0; j < numberOfCols; ++j) {
				try {
					drawCell(canvas, gameActivity.getBoard()[i][j], gameActivity.getMinesweeperGame().getCell(i, j), i, j, (gameActivity.getMinesweeperGame().getIsGameLost() && i == gameActivity.getLastTapRow() && j == gameActivity.getLastTapCol()));
				} catch (Exception e) {
					gameActivity.displayStackTracePopup(e);
					e.printStackTrace();
				}
			}
		}
		for (int j = 0; j <= numberOfCols; ++j) {
			canvas.drawLine(j * GameActivity.cellPixelLength, 0, j * GameActivity.cellPixelLength, numberOfRows * GameActivity.cellPixelLength, black);
		}
		for (int i = 0; i <= numberOfRows; ++i) {
			canvas.drawLine(0, i * GameActivity.cellPixelLength, numberOfCols * GameActivity.cellPixelLength, i * GameActivity.cellPixelLength, black);
		}
		if (gameActivity.getMinesweeperGame().getIsGameWon()) {
			gameActivity.disableSwitchesAndButtons();
			gameActivity.setNewGameButtonWinFace();
			gameActivity.stopTimerThread();
		} else if (gameActivity.getMinesweeperGame().getIsGameLost()) {
			gameActivity.disableSwitchesAndButtons();
			gameActivity.setNewGameButtonDeadFace();
			gameActivity.stopTimerThread();
		}
	}
}
