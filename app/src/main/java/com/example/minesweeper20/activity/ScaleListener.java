package com.example.minesweeper20.activity;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.minesweeper20.view.GameCanvas;

public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener implements View.OnTouchListener {

	//variables to hand a swipe (translate) and pinch (scale)
	private final ScaleGestureDetector SGD;
	private final GameCanvas gameCanvas;
	private final Matrix matrix = new Matrix();
	private int halfScreenWidth = 0, halfScreenHeight = 0, rows, cols;
	private final Context context;
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

		minScaleVal = screenWidth / (float) (GameActivity.cellPixelLength * cols);
		minScaleVal = Math.max(minScaleVal, screenHeight / (float) (GameActivity.cellPixelLength * rows));
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

						float newX = startOfTapX;
						newX -= halfScreenWidth;
						newX /= scale;
						newX += halfScreenWidth;
						newX -= absoluteX;

						float newY = startOfTapY;
						newY -= halfScreenHeight;
						newY /= scale;
						newY += halfScreenHeight;
						newY -= absoluteY;

						try {
							((GameActivity) context).handleTap(newX, newY);
						} catch (Exception e) {
							e.printStackTrace();
						}
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

	private void makeSureGridIsOnScreen() {
		scale = Math.max(scale, minScaleVal);

		final float newX = (absoluteX - halfScreenWidth) * scale + halfScreenWidth;
		final float newY = (absoluteY - halfScreenHeight) * scale + halfScreenHeight;

		if (newX > 0) {
			absoluteX = (-halfScreenWidth) / scale + halfScreenWidth;
		} else {
			final float boundX = GameActivity.cellPixelLength * scale * cols - (2 * halfScreenWidth);
			if (newX < -boundX) {
				absoluteX = (-boundX - halfScreenWidth) / scale + halfScreenWidth;
			}
		}

		if (newY > 0) {
			absoluteY = (-halfScreenHeight) / scale + halfScreenHeight;
		} else {
			final float boundY = GameActivity.cellPixelLength * scale * rows - (2 * halfScreenHeight);
			if (newY < -boundY) {
				absoluteY = (-boundY - halfScreenHeight) / scale + halfScreenHeight;
			}
		}
	}

	public Matrix getMatrix() {
		return matrix;
	}
}
