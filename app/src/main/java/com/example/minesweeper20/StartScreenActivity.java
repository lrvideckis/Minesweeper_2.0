package com.example.minesweeper20;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

//TODO: implement # bombs input
//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: make translate + scale less laggy (maybe look into parallelizing it)
//TODO: organize colors + styles in values folder
//TODO: undo button
//TODO: redo start menu screen
//TODO: for the solver, also implement row reduce solver
//TODO: show bomb percentage on start screen new game popup (bombs/(rows*cols))
//TODO: add settings page were you can choose whether or not to have a zero-start

public class StartScreenActivity extends AppCompatActivity implements View.OnClickListener {

	private PopupWindow popupWindow;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_menu);

		Button startGameButton = findViewById(R.id.startNewGame);
		startGameButton.setOnClickListener(this);

		setUpNewGamePopup();
	}

	@Override
	public void onClick(View v) {
		LinearLayout startScreen = findViewById(R.id.start_screen);
		popupWindow.showAtLocation(startScreen, Gravity.CENTER,0,0);
	}

	private void setUpNewGamePopup() {
		Context context = getApplicationContext();
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		@SuppressLint("InflateParams") final View newGamePopup = inflater.inflate(R.layout.new_game_popup,null);
		popupWindow = new PopupWindow(
				newGamePopup,
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		popupWindow.setFocusable(true);
		popupWindow.setElevation(5.0f);
		Button okButton = newGamePopup.findViewById(R.id.startNewGameButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("InflateParams")
			@Override
			public void onClick(View view) {
				popupWindow.setContentView(inflater.inflate(R.layout.new_game_popup, null, false));
				EditText rowInput = newGamePopup.findViewById(R.id.rowsInput);
				EditText colInput = newGamePopup.findViewById(R.id.colsInput);
				EditText bombInput = newGamePopup.findViewById(R.id.bombsInput);
				final int numberOfRows = getNumberInput(rowInput.getText().toString());
				final int numberOfCols = getNumberInput(colInput.getText().toString());
				final int numberOfBombs = getNumberInput(bombInput.getText().toString());

				popupWindow.dismiss();

				Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
				intent.putExtra("numberOfRows", numberOfRows);
				intent.putExtra("numberOfCols", numberOfCols);
				intent.putExtra("numberOfBombs", numberOfBombs);
				startActivity(intent);
			}
		});
	}

	private int getNumberInput(String input) {
		if(input.equals("")) {
			return 5;
		}
		return Integer.parseInt(input);
	}
}
