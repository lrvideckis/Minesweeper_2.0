package com.example.minesweeper20.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: make translate + scale less laggy
//TODO: organize colors + styles in values folder
//TODO: undo button
//TODO: for the solver, also implement row reduce solver
//TODO: show bomb percentage on start screen new game popup (bombs/(rows*cols))
//TODO: add settings page were you can choose whether or not to have a zero-start
//TODO: only draw part of canvas which screen can see
//TODO: when game is over, put an 'X' over the incorrect flags

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
		final EditText rowsInput = newGamePopup.findViewById(R.id.rowsInput);
		final EditText colsInput = newGamePopup.findViewById(R.id.colsInput);
		final EditText bombsInput = newGamePopup.findViewById(R.id.bombsInput);

		Button beginner = newGamePopup.findViewById(R.id.beginner);
		beginner.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText("9");
				colsInput.setText("9");
				bombsInput.setText("10");
			}
		});

		Button intermediate = newGamePopup.findViewById(R.id.intermediate);
		intermediate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText("14");
				colsInput.setText("16");
				bombsInput.setText("40");
			}
		});

		Button expert = newGamePopup.findViewById(R.id.expert);
		expert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText("16");
				colsInput.setText("30");
				bombsInput.setText("99");
			}
		});
	}

	private int getNumberInput(String input) {
		if(input.equals("")) {
			return 1;
		}
		return Integer.parseInt(input);
	}
}
