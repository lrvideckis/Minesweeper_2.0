package com.LukeVideckis.minesweeper_android.activity;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.LukeVideckis.minesweeper_android.view.GameCanvas;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener implements View.OnTouchListener {

	private static final long longTapDurationMilliseconds = 500;

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
	private AtomicBoolean
			hasBeenTooFar = new AtomicBoolean(false),
			singleTapHasEnded = new AtomicBoolean(false),
			isLongTap = new AtomicBoolean(false);
	private boolean seenMoreThanOnePointer = false;
	private float startOfTapX, startOfTapY, startAbsoluteX, startAbsoluteY;
	private float minScaleVal;
	private LongTapRunnable longTapRunnable = new LongTapRunnable();

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
		minScaleVal = Math.min(minScaleVal, screenHeight / (float) (GameActivity.cellPixelLength * rows));

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
				!isLongTap.get() &&
				!hasBeenTooFar.get() &&
				Math.abs(event.getX() - startOfTapX) <= 50f &&
				Math.abs(event.getY() - startOfTapY) <= 50f);
		if (!returnVal) {
			hasBeenTooFar.set(true);
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
					seenMoreThanOnePointer = false;
					hasBeenTooFar.set(false);
					singleTapHasEnded.set(false);
					startAbsoluteX = absoluteX;
					startAbsoluteY = absoluteY;
					isLongTap.set(false);
					new Thread(longTapRunnable).start();
					break;
				case MotionEvent.ACTION_MOVE:
					absoluteX += (event.getX() - prevFocusX) / scale;
					absoluteY += (event.getY() - prevFocusY) / scale;
					prevFocusX = event.getX();
					prevFocusY = event.getY();
					makeSureGridIsOnScreen();
					break;
				case MotionEvent.ACTION_UP:
					singleTapHasEnded.set(true);
					absoluteX += (event.getX() - prevFocusX) / scale;
					absoluteY += (event.getY() - prevFocusY) / scale;
					makeSureGridIsOnScreen();
					if (checkIfTap(event)) {
						absoluteX = startAbsoluteX;
						absoluteY = startAbsoluteY;
						((GameActivity) context).handleTap(
								convertScreenToGridX(startOfTapX),
								convertScreenToGridY(startOfTapY),
								false
						);
					}
					break;
			}
		} else {
			singleTapHasEnded.set(true);
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

	public float convertScreenToGridX(float startOfTapX) {
		return (startOfTapX - halfScreenWidth) / scale + halfScreenWidth - absoluteX;
	}

	public float convertScreenToGridY(float startOfTapY) {
		return (startOfTapY - halfScreenHeight) / scale + halfScreenHeight - absoluteY;
	}

	private void makeSureGridIsOnScreen() {
		scale = Math.max(scale, minScaleVal);

		final float newX = (absoluteX - halfScreenWidth) * scale + halfScreenWidth;
		final float newY = (absoluteY - halfScreenHeight) * scale + halfScreenHeight;

		final boolean boardLessThanWidth = (2 * halfScreenWidth > GameActivity.cellPixelLength * scale * cols);
		final boolean boardLessThanHeight = (2 * halfScreenHeight > GameActivity.cellPixelLength * scale * rows);

		if ((newX > 0) ^ boardLessThanWidth) {
			absoluteX = (-halfScreenWidth) / scale + halfScreenWidth;
		} else {
			final float boundX = GameActivity.cellPixelLength * scale * cols - (2 * halfScreenWidth);
			if ((newX < -boundX) ^ boardLessThanWidth) {
				absoluteX = (-boundX - halfScreenWidth) / scale + halfScreenWidth;
			}
		}

		if ((newY > 0) ^ boardLessThanHeight) {
			absoluteY = (-halfScreenHeight) / scale + halfScreenHeight;
		} else {
			final float boundY = GameActivity.cellPixelLength * scale * rows - (2 * halfScreenHeight);
			if ((newY < -boundY) ^ boardLessThanHeight) {
				absoluteY = (-boundY - halfScreenHeight) / scale + halfScreenHeight;
			}
		}
	}

	public Matrix getMatrix() {
		return matrix;
	}

	public class LongTapRunnable implements Runnable {
		public void run() {
			try {
				sleep(longTapDurationMilliseconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!hasBeenTooFar.get() && !singleTapHasEnded.get()) {
				isLongTap.set(true);
				((GameActivity) context).runHandleTapOnUIThread(
						convertScreenToGridX(startOfTapX),
						convertScreenToGridY(startOfTapY),
						true
				);
			}
		}
	}
}
