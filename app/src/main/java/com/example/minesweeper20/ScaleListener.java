package com.example.minesweeper20;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener implements View.OnTouchListener {

	//variables to hand a swipe (translate) and pinch (scale)
	private final Matrix matrix = new Matrix();
	private final ScaleGestureDetector SGD;
	private Float scale = 1f, absoluteX = 0f, absoluteY = 0f, prevFocusX, prevFocusY;
	private final GameCanvas gameCanvas;
	private Integer prevPointerCount = 0;

	//variables to handle a tap
	private Boolean seenMoreThanOnePointer = false, hasBeenTooFar = false;
	private Float startOfTapX, startOfTapY, startAbsoluteX, startAbsoluteY;

	ScaleListener(Context context, GameCanvas _gameCanvas) {
		SGD = new ScaleGestureDetector(context, this);
		gameCanvas = _gameCanvas;
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
		scale = Math.max(0.1f, Math.min(scale, 5f));
		absoluteX += (detector.getFocusX() - prevFocusX) / scale;
		absoluteY += (detector.getFocusY() - prevFocusY) / scale;
		prevFocusX = detector.getFocusX();
		prevFocusY = detector.getFocusY();
		return true;
	}

	private boolean checkIfTap(MotionEvent event) {
		boolean returnVal = (!seenMoreThanOnePointer &&
				!hasBeenTooFar &&
				Math.abs(event.getX() - startOfTapX) <= 50f &&
				Math.abs(event.getY() - startOfTapY) <= 50f);
		if(!returnVal) {
			hasBeenTooFar = true;
		}
		return returnVal;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		if (prevPointerCount == 2 && event.getPointerCount() == 1) {
			prevFocusX = event.getX();
			prevFocusY = event.getY();
		}
		prevPointerCount = event.getPointerCount();

		final Integer halfScreenWidth = 720;
		final Integer halfScreenHeight = 1200;
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
					break;
				case MotionEvent.ACTION_UP:
					absoluteX += (event.getX() - prevFocusX) / scale;
					absoluteY += (event.getY() - prevFocusY) / scale;
					if(checkIfTap(event)) {
						absoluteX = startAbsoluteX;
						absoluteY = startAbsoluteY;

						Float newX = startOfTapX;
						newX -= halfScreenWidth;
						newX /= scale;
						newX += halfScreenWidth;
						newX -= absoluteX;

						Float newY = startOfTapY;
						newY -= halfScreenHeight;
						newY /= scale;
						newY += halfScreenHeight;
						newY -= absoluteY;

						try {
							gameCanvas.handleTap(newX, newY);
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
		if(!checkIfTap(event)) {
			matrix.setTranslate(absoluteX, absoluteY);
			matrix.postScale(scale, scale, halfScreenWidth, halfScreenHeight);
			gameCanvas.invalidate();
		}
		return true;
	}

	Matrix getMatrix() {
		return matrix;
	}
}

