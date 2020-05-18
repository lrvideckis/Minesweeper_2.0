package com.example.minesweeper20.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.example.minesweeper20.activity.GameActivity;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.R;
import com.example.minesweeper20.activity.ScaleListener;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.helpers.ConvertGameBoardFormat;
import com.example.minesweeper20.minesweeperStuff.solvers.BacktrackingSolver;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class GameCanvas extends View {

	private final ScaleListener scaleListener;
	private final MinesweeperGame minesweeperGame;
	private final Integer cellPixelLength = 150;
	private final Paint black = new Paint();
	private PopupWindow popupWindow;
	private final float[] matrixValues = new float[9];
	private final Matrix canvasTransitionScale = new Matrix();
	private final ArrayList<ArrayList<MinesweeperSolver.VisibleTile>> board;
	private final BacktrackingSolver backtrackingSolver;
	private final DrawCellHelpers drawCellHelpers;

	public GameCanvas(Context context, AttributeSet attrs) {
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
		drawCellHelpers = new DrawCellHelpers(context);
		board = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);
		backtrackingSolver = new BacktrackingSolver(
				minesweeperGame.getNumberOfRows(),
				minesweeperGame.getNumberOfCols());
		setUpEndGamePopup();
	}

	private void setUpEndGamePopup() {
		Context mContext = getContext().getApplicationContext();
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		@SuppressLint("InflateParams") View endGamePopup = inflater.inflate(R.layout.end_game_popup,null);
		popupWindow = new PopupWindow(
				endGamePopup,
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		popupWindow.setElevation(5.0f);
		Button okButton = endGamePopup.findViewById(R.id.closeEndGamePopup);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				popupWindow.dismiss();
				GameActivity gameActivity = (GameActivity) getContext();
				gameActivity.onClick(null);
			}
		});
	}


	public void handleTap(Float tapX, Float tapY) throws Exception {
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

		//TODO: bug here: when you click a visible cell which results in revealing extra cells in eary/hard mode - make sure you win/lose
		if(!gameActivity.getGameMode().equals(gameChoices[0])) {
			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
			boolean wantBomb = gameActivity.getGameMode().equals(gameChoices[1]);
			//TODO: bug: when toggle flags is on + hard mode: this will always put a bomb under the cell you just flagged
			ArrayList<ArrayList<Boolean>> newBombLocations = backtrackingSolver.getBombConfiguration(board, 0, row, col, wantBomb);
			if(newBombLocations != null) {
				minesweeperGame.changeBombLocations(newBombLocations);
			}
		}

		minesweeperGame.clickCell(row, col, gameActivity.getToggleFlagModeOn());

		if(!minesweeperGame.getIsGameOver() && gameActivity.getToggleHintsOn()) {
			updateSolvedBoard();
		}

		gameActivity.updateNumberOfBombs(minesweeperGame.getNumberOfBombs() - minesweeperGame.getNumberOfFlags());
		invalidate();
	}

	public void updateSolvedBoard() throws Exception {
		//TODO: only run solver if board has changed since last time
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		backtrackingSolver.solvePosition(board, minesweeperGame.getNumberOfBombs());
	}

	private void drawCell(Canvas canvas, MinesweeperSolver.VisibleTile solverCell, MinesweeperGame.Tile gameCell, int startX, int startY) throws Exception {
		if(gameCell.isRevealed()) {
			drawCellHelpers.drawNumberedCell(canvas, gameCell.getNumberSurroundingBombs(), startX, startY);
			return;
		}
		drawCellHelpers.drawBlankCell(canvas, startX, startY);

		GameActivity gameActivity = (GameActivity) getContext();

		if(gameCell.isFlagged()) {
			drawCellHelpers.drawFlag(canvas, startX, startY);
			if(minesweeperGame.getIsGameOver() && !gameCell.isBomb) {
				drawCellHelpers.drawRedX(canvas, startX, startY);
			}
		} else if(gameCell.isBomb && minesweeperGame.getIsGameOver()) {
			drawCellHelpers.drawBomb(canvas, startX, startY);
		} else if(gameCell.isBomb && gameActivity.getToggleBombsOn()) {
			drawCellHelpers.drawBomb(canvas, startX, startY);
		}

		if(gameActivity.getToggleHintsOn()) {
			if(solverCell.isLogicalBomb) {
				drawCellHelpers.drawLogicalBomb(canvas, startX, startY);
			} else if(solverCell.isLogicalFree) {
				drawCellHelpers.drawLogicalFree(canvas, startX, startY);
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Matrix canvasM = canvas.getMatrix();
		canvasM.getValues(matrixValues);
		scaleListener.setTopNavBarHeight(matrixValues[5]);
		canvasTransitionScale.set(scaleListener.getMatrix());
		canvasTransitionScale.preConcat(canvasM);
		canvas.setMatrix(canvasTransitionScale);

		final int numberOfRows = minesweeperGame.getNumberOfRows();
		final int numberOfCols = minesweeperGame.getNumberOfCols();
		for(int i = 0; i < numberOfRows; ++i) {
			for(int j = 0; j < numberOfCols; ++j) {
				try {
					drawCell(canvas, board.get(i).get(j), minesweeperGame.getCell(i,j), j * cellPixelLength, i * cellPixelLength);
				} catch (Exception e) {
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
			GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
			popupWindow.showAtLocation(gameCanvas, Gravity.CENTER,0,0);
		}
	}
}
