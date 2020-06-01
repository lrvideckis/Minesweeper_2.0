package com.example.minesweeper20.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.example.minesweeper20.R;
import com.example.minesweeper20.helpers.FractionThenDouble;
import com.example.minesweeper20.helpers.MyMath;

class DrawCellHelpers {
	private final Integer cellPixelLength = 150;
	private final Paint
			backgroundGreyForVisibleCells = new Paint(),
			middleSquare = new Paint(),
			redFlag = new Paint(),
			black = new Paint(),
			redX = new Paint();
	private final Paint[] numberColors;
	private final String
			flagEmoji = new String(Character.toChars(0x1F6A9)),
			bombEmoji = new String(Character.toChars(0x1F4A3));
	private Rect[][] middleSquareRectangles, backgroundRectangles, lowerTriangleRectangles;

	DrawCellHelpers(Context context, int numberOfRows, int numberOfCols) {
		black.setColor(Color.BLACK);
		black.setTextSize(cellPixelLength / 3f);

		backgroundGreyForVisibleCells.setStyle(Paint.Style.FILL);
		backgroundGreyForVisibleCells.setColor(ContextCompat.getColor(context, R.color.backGroundGreyBlankVisibleCell));
		backgroundGreyForVisibleCells.setStyle(Paint.Style.FILL);

		middleSquare.setColor(ContextCompat.getColor(context, R.color.middleSquareColor));
		middleSquare.setStyle(Paint.Style.FILL_AND_STROKE);

		redFlag.setTextSize(cellPixelLength / 2f);
		redFlag.setTextAlign(Paint.Align.CENTER);

		redX.setColor(Color.BLACK);
		redX.setTextSize(cellPixelLength * 2 / 3f);
		redX.setTextAlign(Paint.Align.CENTER);

		numberColors = new Paint[9];
		for (int i = 1; i <= 8; ++i) {
			numberColors[i] = new Paint();
			numberColors[i].setStyle(Paint.Style.FILL);
			numberColors[i].setTextSize(cellPixelLength * 5 / 6f);
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
				final int startX = j * cellPixelLength;
				final int startY = i * cellPixelLength;

				Rect middleSquare = new Rect();
				middleSquare.set(startX + cellPixelLength * 89 / 100,
						startY + cellPixelLength * 89 / 100,
						startX + cellPixelLength * 11 / 100,
						startY + cellPixelLength * 11 / 100);

				Rect currBackground = new Rect(startX, startY, startX + cellPixelLength, startY + cellPixelLength);

				Rect lowerTriangleBounds = new Rect();
				lowerTriangleBounds.set(startX, startY, startX + cellPixelLength, startY + cellPixelLength);

				middleSquareRectangles[i][j] = middleSquare;
				backgroundRectangles[i][j] = currBackground;
				lowerTriangleRectangles[i][j] = lowerTriangleBounds;
			}
		}
	}

	void drawBlankCell(Canvas canvas, int i, int j, Resources resources) {
		final Drawable d = resources.getDrawable(R.drawable.lower_triangle, null);
		d.setBounds(lowerTriangleRectangles[i][j]);
		d.draw(canvas);
		canvas.drawRect(middleSquareRectangles[i][j], middleSquare);
	}

	void drawNumberedCell(Canvas canvas, Integer numberSurroundingBombs, int i, int j, int startX, int startY) {
		canvas.drawRect(backgroundRectangles[i][j], backgroundGreyForVisibleCells);
		if (numberSurroundingBombs > 0) {
			final int xPos = startX + cellPixelLength / 2;
			final int yPos = (int) (startY + cellPixelLength / 2 - ((numberColors[numberSurroundingBombs].descent() + numberColors[numberSurroundingBombs].ascent()) / 2));
			canvas.drawText(numberSurroundingBombs.toString(), xPos, yPos, numberColors[numberSurroundingBombs]);
		}
	}

	void drawFlag(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2));
		canvas.drawText(flagEmoji, xPos, yPos, redFlag);
	}

	void drawBomb(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2));
		canvas.drawText(bombEmoji, xPos, yPos, redFlag);
	}

	//TODO: make this look better
	void drawLogicalBomb(Canvas canvas, int startX, int startY) {
		canvas.drawText("B", startX, startY + cellPixelLength / 3f, black);
	}

	void drawLogicalFree(Canvas canvas, int startX, int startY) {
		canvas.drawText("F", startX, startY + cellPixelLength / 3f, black);
	}

	void drawRedX(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redX.descent() + redX.ascent()) / 2));
		canvas.drawText("X", xPos, yPos, redX);
	}

	void drawBombProbability(Canvas canvas, int startX, int startY, FractionThenDouble probability, Resources resources) throws Exception {
		//fraction has too many digits, displaying double format
		boolean displayFloat = probability.getHasOverflowed();
		if (!displayFloat) {
			final int digitsNumerator = MyMath.MyLog10MinWith4(probability.getNumerator());
			final int digitsDenominator = MyMath.MyLog10MinWith4(probability.getDenominator());
			if (digitsNumerator + digitsDenominator >= 5) {
				displayFloat = true;
			}
		}
		if (displayFloat) {
			canvas.drawText(
					String.format(resources.getString(R.string.two_decimal_places), probability.getValue()),
					startX,
					startY + cellPixelLength / 3f,
					black
			);
		} else {
			canvas.drawText(probability.getNumerator() + "/" + probability.getDenominator(), startX, startY + cellPixelLength / 3f, black);
		}
	}
}
