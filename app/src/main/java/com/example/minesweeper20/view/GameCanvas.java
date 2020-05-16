package com.example.minesweeper20.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
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

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class GameCanvas extends View {

	private final ScaleListener scaleListener;
	private final MinesweeperGame minesweeperGame;
	private final Integer cellPixelLength = 150;
	private final Paint blankCell = new Paint(), black = new Paint(), backgroundGray = new Paint(), redFlag = new Paint();
	private final Rect rect = new Rect();
	private Paint[] numberColors;
	private PopupWindow popupWindow;
	private final float[] matrixValues = new float[9];
	private final Matrix canvasTransitionScale = new Matrix();

	public GameCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		final GameActivity gameActivity = (GameActivity) getContext();
		Integer screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		Integer screenHeight = context.getResources().getDisplayMetrics().heightPixels;
		scaleListener = new ScaleListener(context, this, screenWidth, screenHeight);
		setOnTouchListener(scaleListener);
		minesweeperGame = new MinesweeperGame(
				gameActivity.getNumberOfRows(),
				gameActivity.getNumberOfCols(),
				gameActivity.getNumberOfBombs());
		setUpColors();
		setUpEndGamePopup();
	}

	private void setUpColors() {
		black.setColor(Color.BLACK);
		black.setStrokeWidth(3);

		backgroundGray.setColor(Color.parseColor("#cccccc"));
		backgroundGray.setStyle(Paint.Style.FILL);

		redFlag.setTextSize(cellPixelLength / 2f);
		redFlag.setTextAlign(Paint.Align.CENTER);

		numberColors = new Paint[9];
		for(int i = 1; i <= 8; ++i) {
			numberColors[i] = new Paint();
			numberColors[i].setStyle(Paint.Style.FILL);
			numberColors[i].setTextSize(cellPixelLength * 5 / 6f);
			numberColors[i].setTextAlign(Paint.Align.CENTER);
			numberColors[i].setTypeface(Typeface.create("Arial", Typeface.BOLD));
		}
		//TODO: move these colors to colors.xml file
		numberColors[1].setColor(Color.BLUE);
		numberColors[2].setColor(Color.parseColor("#009933"));
		numberColors[3].setColor(Color.RED);
		numberColors[4].setColor(Color.parseColor("#000099"));
		numberColors[5].setColor(Color.parseColor("#800000"));
		numberColors[6].setColor(Color.parseColor("#009999"));
		numberColors[7].setColor(Color.BLACK);
		numberColors[8].setColor(Color.GRAY);
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

	private void drawBlankCell(Canvas canvas, int startX, int startY) {
		blankCell.setColor(Color.parseColor("#666666"));
		blankCell.setStyle(Paint.Style.FILL_AND_STROKE);
		blankCell.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));

		Path path = new Path();
		path.setFillType(Path.FillType.WINDING);
		path.moveTo(startX, startY + cellPixelLength);
		path.lineTo(startX + cellPixelLength, startY + cellPixelLength);
		path.lineTo(startX + cellPixelLength, startY);
		path.lineTo(startX, startY + cellPixelLength);
		path.close();

		canvas.drawPath(path, blankCell);


		blankCell.setColor(Color.parseColor("#f2f2f2"));
		Path path1 = new Path();
		path1.setFillType(Path.FillType.WINDING);
		path1.moveTo(startX, startY);
		path1.lineTo(startX, startY + cellPixelLength);
		path1.lineTo(startX + cellPixelLength, startY);
		path1.lineTo(startX, startY);
		path1.close();

		canvas.drawPath(path1, blankCell);


		rect.set(startX + cellPixelLength*88/100,
				 startY + cellPixelLength*88/100,
				startX + cellPixelLength*12/100,
				startY + cellPixelLength*12/100);
		blankCell.setColor(Color.parseColor("#b3b3b3"));
		canvas.drawRect(rect, blankCell);
	}

	public void handleTap(Float tapX, Float tapY) throws Exception {
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
		minesweeperGame.clickCell(row, col, gameActivity.getToggleBombsOn());
		invalidate();
	}

	private void drawNumberedCell(Canvas canvas, Integer numberSurroundingBombs, int startX, int startY) {
		Rect background = new Rect(startX, startY, startX + cellPixelLength, startY + cellPixelLength);
		canvas.drawRect(background, backgroundGray);
		if(numberSurroundingBombs > 0) {
			final int xPos = startX + cellPixelLength / 2;
			final int yPos = (int) (startY + cellPixelLength / 2 - ((numberColors[numberSurroundingBombs].descent() + numberColors[numberSurroundingBombs].ascent()) / 2)) ;
			canvas.drawText(numberSurroundingBombs.toString(), xPos, yPos, numberColors[numberSurroundingBombs]);
		}
	}

	private void drawFlag(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(new String(Character.toChars(0x1F6A9)), xPos, yPos, redFlag);
	}

	private void drawBomb(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(new String(Character.toChars(0x1F4A3)), xPos, yPos, redFlag);
	}

	private void drawCell(Canvas canvas, MinesweeperGame.Tile cell, int startX, int startY) throws Exception {
		if(cell.isRevealed()) {
			drawNumberedCell(canvas, cell.getNumberSurroundingBombs(), startX, startY);
		} else {
			drawBlankCell(canvas, startX, startY);
			if(cell.isFlagged()) {
				drawFlag(canvas, startX, startY);
			} else if(cell.isBomb && minesweeperGame.getIsGameOver()) {
				drawBomb(canvas, startX, startY);
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
					drawCell(canvas, minesweeperGame.getCell(i,j), j * cellPixelLength, i * cellPixelLength);
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
