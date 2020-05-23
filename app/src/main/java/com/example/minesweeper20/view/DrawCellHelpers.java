package com.example.minesweeper20.view;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.example.minesweeper20.R;

import java.util.ArrayList;

class DrawCellHelpers {
	private final Integer cellPixelLength = 150;
	private final Paint
			backgroundGreyForVisibleCells = new Paint(),
			lowerTriangle = new Paint(),
			upperTriangle = new Paint(),
			middleSquare = new Paint(),
			redFlag = new Paint(),
			black = new Paint(),
			redX = new Paint();
	private ArrayList<ArrayList<ArrayList<Path>>> trianglePaths;
	private ArrayList<ArrayList<Rect>> middleSquareRectangles, backgroundRectangles;
	private final Paint[] numberColors;
	private final String
			flagSymbol = new String(Character.toChars(0x1F6A9)),
			bombSymbol = new String(Character.toChars(0x1F4A3));

	DrawCellHelpers(Context context, int numberOfRows, int numberOfCols) {
		black.setColor(Color.BLACK);
		black.setTextSize(cellPixelLength / 3f);

		backgroundGreyForVisibleCells.setStyle(Paint.Style.FILL);
		backgroundGreyForVisibleCells.setColor(ContextCompat.getColor(context, R.color.backGroundGreyBlankVisibleCell));
		backgroundGreyForVisibleCells.setStyle(Paint.Style.FILL);

		lowerTriangle.setColor(ContextCompat.getColor(context, R.color.lowerTriangleColor));
		lowerTriangle.setStyle(Paint.Style.FILL_AND_STROKE);
		lowerTriangle.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));

		upperTriangle.setColor(ContextCompat.getColor(context, R.color.upperTriangleColor));
		upperTriangle.setStyle(Paint.Style.FILL_AND_STROKE);
		upperTriangle.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));

		middleSquare.setColor(ContextCompat.getColor(context, R.color.middleSquareColor));
		middleSquare.setStyle(Paint.Style.FILL_AND_STROKE);
		middleSquare.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));

		redFlag.setTextSize(cellPixelLength / 2f);
		redFlag.setTextAlign(Paint.Align.CENTER);

		redX.setColor(Color.BLACK);
		redX.setTextSize(cellPixelLength * 2 / 3f);
		redX.setTextAlign(Paint.Align.CENTER);

		numberColors = new Paint[9];
		for(int i = 1; i <= 8; ++i) {
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
		trianglePaths = new ArrayList<>(rows);
		middleSquareRectangles = new ArrayList<>(rows);
		backgroundRectangles = new ArrayList<>(rows);
		for(int i = 0; i < rows; ++i) {
			ArrayList<ArrayList<Path>> currTriangleRow = new ArrayList<>(cols);
			ArrayList<Rect> currRectRow = new ArrayList<>(cols), currBackgroundRow = new ArrayList<>(cols);
			for(int j = 0; j < cols; ++j) {
				final int startX = j * cellPixelLength;
				final int startY = i * cellPixelLength;

				ArrayList<Path> currSpot = new ArrayList<>(2);

				Path lowerTrianglePath = new Path();
				lowerTrianglePath.setFillType(Path.FillType.WINDING);
				lowerTrianglePath.moveTo(startX, startY + cellPixelLength);
				lowerTrianglePath.lineTo(startX + cellPixelLength, startY + cellPixelLength);
				lowerTrianglePath.lineTo(startX + cellPixelLength, startY);
				lowerTrianglePath.lineTo(startX, startY + cellPixelLength);
				lowerTrianglePath.close();

				Path upperTrianglePath = new Path();
				upperTrianglePath.setFillType(Path.FillType.WINDING);
				upperTrianglePath.moveTo(startX, startY);
				upperTrianglePath.lineTo(startX, startY + cellPixelLength);
				upperTrianglePath.lineTo(startX + cellPixelLength, startY);
				upperTrianglePath.lineTo(startX, startY);
				upperTrianglePath.close();

				Rect middleSquare = new Rect();
				middleSquare.set(startX + cellPixelLength*88/100,
						startY + cellPixelLength*88/100,
						startX + cellPixelLength*12/100,
						startY + cellPixelLength*12/100);

				Rect currBackground = new Rect(startX, startY, startX + cellPixelLength, startY + cellPixelLength);

				currSpot.add(lowerTrianglePath);
				currSpot.add(upperTrianglePath);

				currTriangleRow.add(currSpot);
				currRectRow.add(middleSquare);
				currBackgroundRow.add(currBackground);
			}
			trianglePaths.add(currTriangleRow);
			middleSquareRectangles.add(currRectRow);
			backgroundRectangles.add(currBackgroundRow);
		}
	}

	void drawBlankCell(Canvas canvas, int i, int j) {
		canvas.drawPath(trianglePaths.get(i).get(j).get(0), lowerTriangle);
		canvas.drawPath(trianglePaths.get(i).get(j).get(1), upperTriangle);
		canvas.drawRect(middleSquareRectangles.get(i).get(j), middleSquare);
	}

	void drawNumberedCell(Canvas canvas, Integer numberSurroundingBombs, int i, int j) {
		final int startX = j * cellPixelLength;
		final int startY = i * cellPixelLength;
		canvas.drawRect(backgroundRectangles.get(i).get(j), backgroundGreyForVisibleCells);
		if(numberSurroundingBombs > 0) {
			final int xPos = startX + cellPixelLength / 2;
			final int yPos = (int) (startY + cellPixelLength / 2 - ((numberColors[numberSurroundingBombs].descent() + numberColors[numberSurroundingBombs].ascent()) / 2)) ;
			canvas.drawText(numberSurroundingBombs.toString(), xPos, yPos, numberColors[numberSurroundingBombs]);
		}
	}

	void drawFlag(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(flagSymbol, xPos, yPos, redFlag);
	}

	void drawBomb(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(bombSymbol, xPos, yPos, redFlag);
	}

	//TODO: make this look better
	void drawLogicalBomb(Canvas canvas, int startX, int startY) {
		canvas.drawText("B", startX, startY+cellPixelLength/3f, black);
	}

	void drawLogicalFree(Canvas canvas, int startX, int startY) {
		canvas.drawText("F", startX, startY+cellPixelLength/3f, black);
	}

	void drawRedX(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redX.descent() + redX.ascent()) / 2)) ;
		canvas.drawText("X", xPos, yPos, redX);
	}

	//TODO: if text is too long, this will draw over into the next cell
	void drawBombProbability(Canvas canvas, int startX, int startY, int numerator, int denominator) {
		canvas.drawText(numerator + "/" + denominator, startX, startY+cellPixelLength/3f, black);
	}
}
