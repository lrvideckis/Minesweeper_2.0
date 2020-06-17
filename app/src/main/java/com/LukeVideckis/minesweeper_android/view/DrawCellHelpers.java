package com.LukeVideckis.minesweeper_android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.activity.GameActivity;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.BigFraction;

public class DrawCellHelpers {
	private final Paint
			backgroundGreyForVisibleCells = new Paint();
	private final Paint backgroundRedForVisibleCells = new Paint();
	private final Paint middleSquare = new Paint();
	private final Paint middleRedSquare = new Paint();
	private final Paint middleGreenSquare = new Paint();
	private final Paint redFlag = new Paint();
	private final Paint black = new Paint();
	private final Paint blackX = new Paint();
	private final Paint[] numberColors;
	private Rect[][] middleSquareRectangles, backgroundRectangles, lowerTriangleRectangles;

	DrawCellHelpers(Context context, int numberOfRows, int numberOfCols) {
		black.setColor(Color.BLACK);
		black.setTextSize(GameActivity.cellPixelLength / 3f);

		backgroundGreyForVisibleCells.setColor(ContextCompat.getColor(context, R.color.backgroundGreyBlankVisibleCell));
		backgroundGreyForVisibleCells.setStyle(Paint.Style.FILL);

		backgroundRedForVisibleCells.setColor(ContextCompat.getColor(context, R.color.backgroundRedBlankVisibleCell));
		backgroundRedForVisibleCells.setStyle(Paint.Style.FILL);

		middleSquare.setColor(ContextCompat.getColor(context, R.color.middleSquareColor));
		middleSquare.setStyle(Paint.Style.FILL_AND_STROKE);

		middleRedSquare.setColor(ContextCompat.getColor(context, R.color.middleRedSquare));
		middleRedSquare.setStyle(Paint.Style.FILL_AND_STROKE);

		middleGreenSquare.setColor(ContextCompat.getColor(context, R.color.middleGreenSquare));
		middleGreenSquare.setStyle(Paint.Style.FILL_AND_STROKE);

		redFlag.setTextSize(GameActivity.cellPixelLength / 2f);
		redFlag.setTextAlign(Paint.Align.CENTER);

		blackX.setColor(Color.BLACK);
		blackX.setTextSize(GameActivity.cellPixelLength * 2 / 3f);
		blackX.setTextAlign(Paint.Align.CENTER);

		numberColors = new Paint[9];
		for (int i = 1; i <= 8; ++i) {
			numberColors[i] = new Paint();
			numberColors[i].setStyle(Paint.Style.FILL);
			numberColors[i].setTextSize(GameActivity.cellPixelLength * 5 / 6f);
			numberColors[i].setTextAlign(Paint.Align.CENTER);
			numberColors[i].setTypeface(Typeface.create("Arial", Typeface.BOLD));
		}

		numberColors[1].setColor(ContextCompat.getColor(context, R.color.one));
		numberColors[2].setColor(ContextCompat.getColor(context, R.color.two));
		numberColors[3].setColor(ContextCompat.getColor(context, R.color.three));
		numberColors[4].setColor(ContextCompat.getColor(context, R.color.four));
		numberColors[5].setColor(ContextCompat.getColor(context, R.color.five));
		numberColors[6].setColor(ContextCompat.getColor(context, R.color.six));
		numberColors[7].setColor(ContextCompat.getColor(context, R.color.seven));
		numberColors[8].setColor(ContextCompat.getColor(context, R.color.eight));

		initializeTrianglesAndRectangles(numberOfRows, numberOfCols);
	}

	private void initializeTrianglesAndRectangles(int rows, int cols) {
		middleSquareRectangles = new Rect[rows][cols];
		backgroundRectangles = new Rect[rows][cols];
		lowerTriangleRectangles = new Rect[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				final int startX = j * GameActivity.cellPixelLength;
				final int startY = i * GameActivity.cellPixelLength;

				Rect middleSquare = new Rect();
				middleSquare.set(startX + GameActivity.cellPixelLength * 89 / 100,
						startY + GameActivity.cellPixelLength * 89 / 100,
						startX + GameActivity.cellPixelLength * 11 / 100,
						startY + GameActivity.cellPixelLength * 11 / 100);

				Rect currBackground = new Rect(startX, startY, startX + GameActivity.cellPixelLength, startY + GameActivity.cellPixelLength);

				Rect lowerTriangleBounds = new Rect();
				lowerTriangleBounds.set(startX, startY, startX + GameActivity.cellPixelLength, startY + GameActivity.cellPixelLength);

				middleSquareRectangles[i][j] = middleSquare;
				backgroundRectangles[i][j] = currBackground;
				lowerTriangleRectangles[i][j] = lowerTriangleBounds;
			}
		}
	}

	void drawBlankCell(Canvas canvas, int i, int j, Resources resources) {
		final Drawable lowerTriangleDrawable = resources.getDrawable(R.drawable.lower_triangle_grey, null);
		lowerTriangleDrawable.setBounds(lowerTriangleRectangles[i][j]);
		lowerTriangleDrawable.draw(canvas);
		canvas.drawRect(middleSquareRectangles[i][j], middleSquare);
	}

	void drawNumberedCell(Canvas canvas, Integer numberSurroundingMines, int i, int j, int startX, int startY) {
		canvas.drawRect(backgroundRectangles[i][j], backgroundGreyForVisibleCells);
		if (numberSurroundingMines > 0) {
			final int xPos = startX + GameActivity.cellPixelLength / 2;
			final int yPos = (int) (startY + GameActivity.cellPixelLength / 2 - ((numberColors[numberSurroundingMines].descent() + numberColors[numberSurroundingMines].ascent()) / 2));
			canvas.drawText(numberSurroundingMines.toString(), xPos, yPos, numberColors[numberSurroundingMines]);
		}
	}

	void drawFlag(Canvas canvas, int startX, int startY) {
		final int xPos = startX + GameActivity.cellPixelLength / 2;
		final int yPos = (int) (startY + GameActivity.cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2));
		canvas.drawText(GameActivity.flagEmoji, xPos, yPos, redFlag);
	}

	void drawMine(Canvas canvas, int startX, int startY) {
		final int xPos = startX + GameActivity.cellPixelLength / 2;
		final int yPos = (int) (startY + GameActivity.cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2));
		canvas.drawText(GameActivity.mineEmoji, xPos, yPos, redFlag);
	}

	void drawLogicalMine(Canvas canvas, int i, int j, Resources resources) {
		final Drawable upperTriangleDrawable = resources.getDrawable(R.drawable.upper_triangle_red, null);
		upperTriangleDrawable.setBounds(lowerTriangleRectangles[i][j]);
		upperTriangleDrawable.draw(canvas);
		final Drawable lowerTriangleDrawable = resources.getDrawable(R.drawable.lower_triangle_red, null);
		lowerTriangleDrawable.setBounds(lowerTriangleRectangles[i][j]);
		lowerTriangleDrawable.draw(canvas);
		canvas.drawRect(middleSquareRectangles[i][j], middleRedSquare);
	}

	void drawLogicalFree(Canvas canvas, int i, int j, Resources resources) {
		final Drawable upperTriangleDrawable = resources.getDrawable(R.drawable.upper_triangle_green, null);
		upperTriangleDrawable.setBounds(lowerTriangleRectangles[i][j]);
		upperTriangleDrawable.draw(canvas);
		final Drawable lowerTriangleDrawable = resources.getDrawable(R.drawable.lower_triangle_green, null);
		lowerTriangleDrawable.setBounds(lowerTriangleRectangles[i][j]);
		lowerTriangleDrawable.draw(canvas);
		canvas.drawRect(middleSquareRectangles[i][j], middleGreenSquare);
	}

	void drawEndGameTap(Canvas canvas, int i, int j) {
		canvas.drawRect(backgroundRectangles[i][j], backgroundRedForVisibleCells);
	}

	void drawBlackX(Canvas canvas, int startX, int startY) {
		final int xPos = startX + GameActivity.cellPixelLength / 2;
		final int yPos = (int) (startY + GameActivity.cellPixelLength / 2 - ((blackX.descent() + blackX.ascent()) / 2));
		canvas.drawText("X", xPos, yPos, blackX);
	}

	void drawMineProbability(Canvas canvas, int startX, int startY, BigFraction probability, Resources resources) throws Exception {
		//fraction has too many digits, displaying double format
		final int digitsNumerator = probability.getNumerator().toString().length();
		final int digitsDenominator = probability.getDenominator().toString().length();
		if (digitsNumerator + digitsDenominator >= 5) {
			canvas.drawText(
					String.format(resources.getString(R.string.two_decimal_places), probability.getDoubleValue()),
					startX,
					startY + GameActivity.cellPixelLength / 3f,
					black
			);
		} else if (probability.equals(1)) {
			canvas.drawText("1", startX, startY + GameActivity.cellPixelLength / 3f, black);
		} else if (probability.equals(0)) {
			canvas.drawText("0", startX, startY + GameActivity.cellPixelLength / 3f, black);
		} else {
			canvas.drawText(probability.getNumerator() + "/" + probability.getDenominator(), startX, startY + GameActivity.cellPixelLength / 3f, black);
		}
	}
}
