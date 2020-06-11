package com.LukeVideckis.minesweeper20.miscHelpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.LukeVideckis.minesweeper20.R;
import com.LukeVideckis.minesweeper20.view.GameCanvas;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class PopupHelper {

	public static PopupWindow initializePopup(Context context, int layoutId) {
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		@SuppressLint("InflateParams") final View view = inflater.inflate(layoutId, null);
		final PopupWindow popup = new PopupWindow(
				view,
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		popup.setFocusable(true);
		popup.setElevation(5.0f);
		return popup;
	}

	public static void displayPopup(PopupWindow popup, View parentView, Resources resources) {
		if (parentView.getTag().equals(resources.getString(R.string.is_linear_layout))) {
			LinearLayout linearLayout = (LinearLayout) parentView;
			popup.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);
		} else if (parentView.getTag().equals(resources.getString(R.string.is_game_canvas_layout))) {
			GameCanvas gameCanvasLayout = (GameCanvas) parentView;
			popup.showAtLocation(gameCanvasLayout, Gravity.CENTER, 0, 0);
		} else if (parentView.getTag().equals(resources.getString(R.string.is_relative_layout))) {
			RelativeLayout relativeLayout = (RelativeLayout) parentView;
			popup.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);
		}
	}
}
