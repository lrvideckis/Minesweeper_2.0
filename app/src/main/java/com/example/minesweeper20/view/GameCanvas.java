package com.example.minesweeper20.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;

import com.example.minesweeper20.HitIterationLimitException;
import com.example.minesweeper20.R;
import com.example.minesweeper20.activity.GameActivity;
import com.example.minesweeper20.activity.ScaleListener;
import com.example.minesweeper20.helpers.ConvertGameBoardFormat;
import com.example.minesweeper20.helpers.Fraction;
import com.example.minesweeper20.helpers.PopupHelper;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;

import java.util.ArrayList;

public class GameCanvas extends View {

	private final ScaleListener scaleListener;
	private final MinesweeperGame minesweeperGame;
	private final Integer cellPixelLength = 150;
	private final Paint black = new Paint();
	private PopupWindow endGamePopup;
	private final Matrix canvasTransitionScale = new Matrix();
	private final ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board;
	private final MinesweeperSolver backtrackingSolver;
	private final DrawCellHelpers drawCellHelpers;
	private final Fraction bombProbability = new Fraction(0);

	public GameCanvas(Context context, AttributeSet attrs) throws Exception {
		super(context, attrs);
		black.setColor(Color.BLACK);
		black.setStrokeWidth(3);
		final GameActivity gameActivity = (GameActivity) getContext();
		Integer screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		Integer screenHeight = context.getResources().getDisplayMetrics().heightPixels;
		scaleListener = new ScaleListener(context, this, screenWidth, screenHeight);
		setOnTouchListener(scaleListener);
		minesweeperGame = new MinesweeperGame(
				gameActivity.getNumberOfRows(),
				gameActivity.getNumberOfCols(),
				gameActivity.getNumberOfBombs());
		drawCellHelpers = new DrawCellHelpers(context, gameActivity.getNumberOfRows(), gameActivity.getNumberOfCols());
		board = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
		backtrackingSolver = new BacktrackingSolver(
				minesweeperGame.getNumberOfRows(),
				minesweeperGame.getNumberOfCols());
		setUpEndGamePopup();
	}

	private void setUpEndGamePopup() {
		endGamePopup = PopupHelper.initializePopup(getContext().getApplicationContext(), R.layout.end_game_popup);
		Button okButton = endGamePopup.getContentView().findViewById(R.id.closeEndGamePopup);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				endGamePopup.dismiss();
				GameActivity gameActivity = (GameActivity) getContext();
				gameActivity.onClick(null);
			}
		});
	}

	public void handleTap(Float tapX, Float tapY) {
		//TODO: have grid always fill screen
		//eventually I won't need this check, as the grid always fills the screen
		if(tapX < 0f ||
				tapY < 0f ||
				tapX > minesweeperGame.getNumberOfCols() * cellPixelLength ||
				tapY > minesweeperGame.getNumberOfRows() * cellPixelLength) {
			return;
		}
		final int row = (int) (tapY / cellPixelLength);
		final int col = (int) (tapX / cellPixelLength);

		GameActivity gameActivity = (GameActivity) getContext();
		String[] gameChoices = getResources().getStringArray(R.array.game_type);

		try {
			//TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
			if (!gameActivity.getGameMode().equals(gameChoices[0])) {
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				boolean wantBomb = gameActivity.getGameMode().equals(gameChoices[1]);
				//TODO: bug: when toggle flags is on + hard mode: this will always put a bomb under the cell you just flagged
				ArrayList<ArrayList<Boolean>> newBombLocations = backtrackingSolver.getBombConfiguration(board, 0, row, col, wantBomb);
				if (newBombLocations != null) {
					minesweeperGame.changeBombLocations(newBombLocations);
				}
			}

			minesweeperGame.clickCell(row, col, gameActivity.getToggleFlagModeOn());
			if (!minesweeperGame.getIsGameOver() && gameActivity.getToggleHintsOn() && !(gameActivity.getToggleFlagModeOn() && !minesweeperGame.getCell(row,col).getIsVisible())) {
				updateSolvedBoard();
			}
		} catch(Exception e) {
			gameActivity.displayStackTracePopup(e);
			e.printStackTrace();
		}

		gameActivity.updateNumberOfBombs(minesweeperGame.getNumberOfBombs() - minesweeperGame.getNumberOfFlags());
		invalidate();
	}

	public void updateSolvedBoard() throws Exception {
		//TODO: only run solver if board has changed since last time
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		try {
			backtrackingSolver.solvePosition(board, minesweeperGame.getNumberOfBombs());
		} catch (HitIterationLimitException e) {
			GameActivity gameActivity = (GameActivity) getContext();
			gameActivity.solverHasJustHitIterationLimit();
		}
	}

	private void drawCell(Canvas canvas, MinesweeperSolver.VisibleTile solverCell, MinesweeperGame.Tile gameCell, int i, int j) throws Exception {
		final int startX = j * cellPixelLength;
		final int startY = i * cellPixelLength;

		//it seems this is making it more laggy
		if(cellIsOffScreen(startX, startY)) {
			return;
		}

		if(gameCell.getIsVisible()) {
			drawCellHelpers.drawNumberedCell(canvas, gameCell.getNumberSurroundingBombs(), i, j);
			return;
		}
		drawCellHelpers.drawBlankCell(canvas, i, j);

		GameActivity gameActivity = (GameActivity) getContext();

		if(gameCell.isFlagged()) {
			drawCellHelpers.drawFlag(canvas, startX, startY);
			if(minesweeperGame.getIsGameOver() && !gameCell.isBomb()) {
				drawCellHelpers.drawRedX(canvas, startX, startY);
			}
		} else if(gameCell.isBomb() && (minesweeperGame.getIsGameOver() || gameActivity.getToggleBombsOn())) {
			drawCellHelpers.drawBomb(canvas, startX, startY);
		}

		if(gameActivity.getToggleHintsOn()) {
			if(solverCell.getIsLogicalBomb()) {
				if(!gameCell.isBomb()) {
					throw new Exception("solver says: logical bomb, but it's not a bomb");
				}
				drawCellHelpers.drawLogicalBomb(canvas, startX, startY);
			} else if(solverCell.getIsLogicalFree()) {
				if(gameCell.isBomb()) {
					throw new Exception("solver says: logical free, but it's not free");
				}
				drawCellHelpers.drawLogicalFree(canvas, startX, startY);
			} else if(gameActivity.getToggleBombProbabilityOn()) {
				if(!solverCell.getIsVisible()) {
					bombProbability.setValues(solverCell.getNumberOfBombConfigs(), solverCell.getNumberOfTotalConfigs());
					drawCellHelpers.drawBombProbability(canvas, startX, startY, bombProbability, getResources());
				}
			}
		}
	}

	private boolean cellIsOffScreen(int startX, int startY) {
		final float topNavBarHeight = getTop() + getStatusBarHeight();
		final float absoluteX = scaleListener.getAbsoluteX();
		final float absoluteY = scaleListener.getAbsoluteY();
		final float scale = scaleListener.getScale();
		final float halfOfScreenWidth = scaleListener.getHalfOfScreenWidth();
		final float halfOfScreenHeight = scaleListener.getHalfOfScreenHeight();

		if((startX + cellPixelLength + absoluteX - halfOfScreenWidth) * scale + halfOfScreenWidth < 0) {
			return true;
		}
		if((startY + cellPixelLength + topNavBarHeight + absoluteY - halfOfScreenHeight) * scale + halfOfScreenHeight - topNavBarHeight < 0) {
			return true;
		}
		if((startX + absoluteX - halfOfScreenWidth) * scale + halfOfScreenWidth > getWidth()) {
			return true;
		}
		return ((startY + absoluteY + topNavBarHeight - halfOfScreenHeight) * scale + halfOfScreenHeight - topNavBarHeight > getHeight());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		final float topNavBarHeight = getTop() + getStatusBarHeight();
		scaleListener.setTopNavBarHeight(topNavBarHeight);
		canvasTransitionScale.set(scaleListener.getMatrix());
		canvasTransitionScale.preTranslate(0, topNavBarHeight);
		canvas.setMatrix(canvasTransitionScale);

		final int numberOfRows = minesweeperGame.getNumberOfRows();
		final int numberOfCols = minesweeperGame.getNumberOfCols();
		for(int i = 0; i < numberOfRows; ++i) {
			for(int j = 0; j < numberOfCols; ++j) {
				try {
					drawCell(canvas, board.get(i).get(j), minesweeperGame.getCell(i,j), i, j);
				} catch (Exception e) {
					GameActivity gameActivity = (GameActivity) getContext();
					gameActivity.displayStackTracePopup(e);
					e.printStackTrace();
				}
			}
		}
		for(int j = 0; j <= numberOfCols; ++j) {
			canvas.drawLine(j*cellPixelLength, 0, j*cellPixelLength, numberOfRows*cellPixelLength, black);
		}
		for(int i = 0; i <= numberOfRows; ++i) {
			canvas.drawLine(0, i*cellPixelLength, numberOfCols*cellPixelLength, i*cellPixelLength, black);
		}

		if(minesweeperGame.getIsGameOver()) {
			PopupHelper.displayPopup(endGamePopup, findViewById(R.id.gridCanvas), getResources());
		}
	}

	private int getStatusBarHeight() {
		int statusBarHeight = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if(resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}
		return statusBarHeight;
	}
}
