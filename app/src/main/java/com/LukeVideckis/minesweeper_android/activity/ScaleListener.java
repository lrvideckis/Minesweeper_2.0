package com.LukeVideckis.minesweeper_android.activity;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.LukeVideckis.minesweeper_android.view.GameCanvas;

public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener implements View.OnTouchListener {

	//variables to hand a swipe (translate) and pinch (scale)
	private final ScaleGestureDetector SGD;
	private final GameCanvas gameCanvas;
	private final Matrix matrix = new Matrix();
	private final Context context;
	private final int rows, cols;
	private int halfScreenWidth = 0, halfScreenHeight = 0;
	private float scale = 1f, absoluteX = 0f, absoluteY = 0f, prevFocusX, prevFocusY;
	private int prevPointerCount = 0;
	//variables to handle a tap
	private boolean seenMoreThanOnePointer = false, hasBeenTooFar = false;
	private float startOfTapX, startOfTapY, startAbsoluteX, startAbsoluteY;
	private float minScaleVal;

	public ScaleListener(Context context, GameCanvas gameCanvas, int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		this.context = context;
		SGD = new ScaleGestureDetector(context, this);
		this.gameCanvas = gameCanvas;
	}

	public void setScreenWidthAndHeight(float screenWidth, float screenHeight) {
		halfScreenWidth = (int) (screenWidth / 2f);
		halfScreenHeight = (int) (screenHeight / 2f);

		final float minScaleX = screenWidth / (float) (GameActivity.cellPixelLength * cols);
		final float minScaleY = screenHeight / (float) (GameActivity.cellPixelLength * rows);

		scale = minScaleVal = Math.min(minScaleX, minScaleY);

		absoluteX = (getMinAbsoluteX() + getMaxAbsoluteX(getBoundX())) / 2f;
		absoluteY = (getMinAbsoluteY() + getMaxAbsoluteY(getBoundY())) / 2f;

		makeSureGridIsOnScreen();
		matrix.setTranslate(absoluteX, absoluteY);
		matrix.postScale(scale, scale, halfScreenWidth, halfScreenHeight);
		gameCanvas.invalidate();
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		prevFocusX = detector.getFocusX();
		prevFocusY = detector.getFocusY();
		return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		scale = scale * detector.getScaleFactor();
		scale = Math.max(0.1f, Math.min(scale, 3.3f));
		absoluteX += (detector.getFocusX() - prevFocusX) / scale;
		absoluteY += (detector.getFocusY() - prevFocusY) / scale;
		prevFocusX = detector.getFocusX();
		prevFocusY = detector.getFocusY();
		makeSureGridIsOnScreen();
		return true;
	}

	private boolean checkIfTap(MotionEvent event) {
		boolean returnVal = (!seenMoreThanOnePointer &&
				!hasBeenTooFar &&
				Math.abs(event.getX() - startOfTapX) <= 50f &&
				Math.abs(event.getY() - startOfTapY) <= 50f);
		if (!returnVal) {
			hasBeenTooFar = true;
		}
		return returnVal;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		if (prevPointerCount > 1 && event.getPointerCount() == 1) {
			prevFocusX = event.getX();
			prevFocusY = event.getY();
		}
		prevPointerCount = event.getPointerCount();

		if (event.getPointerCount() == 1) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startOfTapX = prevFocusX = event.getX();
					startOfTapY = prevFocusY = event.getY();
					hasBeenTooFar = seenMoreThanOnePointer = false;
					startAbsoluteX = absoluteX;
					startAbsoluteY = absoluteY;
					break;
				case MotionEvent.ACTION_MOVE:
					absoluteX += (event.getX() - prevFocusX) / scale;
					absoluteY += (event.getY() - prevFocusY) / scale;
					prevFocusX = event.getX();
					prevFocusY = event.getY();
					makeSureGridIsOnScreen();
					break;
				case MotionEvent.ACTION_UP:
					absoluteX += (event.getX() - prevFocusX) / scale;
					absoluteY += (event.getY() - prevFocusY) / scale;
					makeSureGridIsOnScreen();
					if (checkIfTap(event)) {
						absoluteX = startAbsoluteX;
						absoluteY = startAbsoluteY;

						((GameActivity) context).handleTap(
								convertScreenToGridX(startOfTapX),
								convertScreenToGridY(startOfTapY)
						);
					}
					break;
			}
		} else {
			seenMoreThanOnePointer = true;
			SGD.onTouchEvent(event);
		}
		if (!checkIfTap(event)) {
			matrix.setTranslate(absoluteX, absoluteY);
			matrix.postScale(scale, scale, halfScreenWidth, halfScreenHeight);
			gameCanvas.invalidate();
		}
		return true;
	}

	private float convertScreenToGridX(float startOfTapX) {
		return (startOfTapX - halfScreenWidth) / scale + halfScreenWidth - absoluteX;
	}

	private float convertScreenToGridY(float startOfTapY) {
		return (startOfTapY - halfScreenHeight) / scale + halfScreenHeight - absoluteY;
	}

	private float getBoundX() {
		return GameActivity.cellPixelLength * scale * cols - (2 * halfScreenWidth);
	}

	private float getBoundY() {
		return GameActivity.cellPixelLength * scale * rows - (2 * halfScreenHeight);
	}

	private float getMinAbsoluteX() {
		return (-halfScreenWidth) / scale + halfScreenWidth;
	}

	private float getMaxAbsoluteX(float boundX) {
		return (-boundX - halfScreenWidth) / scale + halfScreenWidth;
	}

	private float getMinAbsoluteY() {
		return (-halfScreenHeight) / scale + halfScreenHeight;
	}

	private float getMaxAbsoluteY(float boundY) {
		return (-boundY - halfScreenHeight) / scale + halfScreenHeight;
	}

	private void makeSureGridIsOnScreen() {
		scale = Math.max(scale, minScaleVal);

		final float newX = (absoluteX - halfScreenWidth) * scale + halfScreenWidth;
		final float newY = (absoluteY - halfScreenHeight) * scale + halfScreenHeight;

		final boolean boardLessThanWidth = (2 * halfScreenWidth > GameActivity.cellPixelLength * scale * cols);
		final boolean boardLessThanHeight = (2 * halfScreenHeight > GameActivity.cellPixelLength * scale * rows);

		if ((newX > 0) ^ boardLessThanWidth) {
			absoluteX = getMinAbsoluteX();
		} else {
			final float boundX = getBoundX();
			if ((newX < -boundX) ^ boardLessThanWidth) {
				absoluteX = getMaxAbsoluteX(boundX);
			}
		}

		if ((newY > 0) ^ boardLessThanHeight) {
			absoluteY = getMinAbsoluteY();
		} else {
			final float boundY = getBoundY();
			if ((newY < -boundY) ^ boardLessThanHeight) {
				absoluteY = getMaxAbsoluteY(boundY);
			}
		}
	}

	public Matrix getMatrix() {
		return matrix;
	}
}
