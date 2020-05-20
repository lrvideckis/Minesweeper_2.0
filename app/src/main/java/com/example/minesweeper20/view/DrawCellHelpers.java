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

class DrawCellHelpers {
	private final Integer cellPixelLength = 150;
	private final Paint blankCell = new Paint(), backgroundGray = new Paint(), redFlag = new Paint(), black = new Paint(), redX = new Paint();
	private final Paint[] numberColors;
	private final Rect rect = new Rect();
	DrawCellHelpers(Context context) {
		black.setColor(Color.BLACK);
		black.setTextSize(cellPixelLength / 3f);

		backgroundGray.setStyle(Paint.Style.FILL);

		backgroundGray.setColor(Color.parseColor("#cccccc"));
		backgroundGray.setStyle(Paint.Style.FILL);

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
	}

	void drawBlankCell(Canvas canvas, int startX, int startY) {
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

	void drawNumberedCell(Canvas canvas, Integer numberSurroundingBombs, int startX, int startY) {
		Rect background = new Rect(startX, startY, startX + cellPixelLength, startY + cellPixelLength);
		canvas.drawRect(background, backgroundGray);
		if(numberSurroundingBombs > 0) {
			final int xPos = startX + cellPixelLength / 2;
			final int yPos = (int) (startY + cellPixelLength / 2 - ((numberColors[numberSurroundingBombs].descent() + numberColors[numberSurroundingBombs].ascent()) / 2)) ;
			canvas.drawText(numberSurroundingBombs.toString(), xPos, yPos, numberColors[numberSurroundingBombs]);
		}
	}

	void drawFlag(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(new String(Character.toChars(0x1F6A9)), xPos, yPos, redFlag);
	}

	void drawBomb(Canvas canvas, int startX, int startY) {
		final int xPos = startX + cellPixelLength / 2;
		final int yPos = (int) (startY + cellPixelLength / 2 - ((redFlag.descent() + redFlag.ascent()) / 2)) ;
		canvas.drawText(new String(Character.toChars(0x1F4A3)), xPos, yPos, redFlag);
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

	void drawBombProbability(Canvas canvas, int startX, int startY, int numerator, int denominator) {
		String probability = numerator + "/" + denominator;
		canvas.drawText(probability, startX, startY+cellPixelLength/3f, black);
	}
}
