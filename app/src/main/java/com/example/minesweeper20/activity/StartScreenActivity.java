package com.example.minesweeper20.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.helpers.PopupHelper;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: make translate + scale less laggy
//TODO: undo button - only when you lose
//TODO: for bomb percentage: handle before the first click
//TODO: add settings page were you can choose whether or not to have a zero-start, also choose iteration limit of backtracking solver, also choose defaults for Flag Mode, Game mode, etc
//TODO: gauss elimination solver
//TODO: make rows/cols/bombs be the same as the last played game
//TODO: make sure back button on bottom nav bar works
//TODO: when you lose, you can't click the back button, you have to click the okay popup button - fix this

public class StartScreenActivity extends AppCompatActivity implements View.OnClickListener {

	private PopupWindow newGamePopup;
	private PopupWindow gridTooSmallPopup;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_menu);

		Button startGameButton = findViewById(R.id.startNewGame);
		startGameButton.setOnClickListener(this);

		setUpNewGamePopup();
		setUpGridTooSmallPopup();
	}

	@Override
	public void onClick(View v) {
		LinearLayout startScreen = findViewById(R.id.start_screen);
		PopupHelper.displayPopup(newGamePopup, startScreen, getResources());
	}

	private void setUpNewGamePopup() {
		newGamePopup = PopupHelper.initializePopup(getApplicationContext(), R.layout.new_game_popup);
		Button okButton = newGamePopup.getContentView().findViewById(R.id.startNewGameButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("InflateParams")
			@Override
			public void onClick(View view) {
				EditText rowInput = newGamePopup.getContentView().findViewById(R.id.rowsInput);
				EditText colInput = newGamePopup.getContentView().findViewById(R.id.colsInput);
				EditText bombInput = newGamePopup.getContentView().findViewById(R.id.bombsInput);
				final int numberOfRows = getNumberInput(rowInput.getText().toString());
				final int numberOfCols = getNumberInput(colInput.getText().toString());
				final int numberOfBombs = getNumberInput(bombInput.getText().toString());

				if(MinesweeperGame.tooManyBombsForZeroStart(numberOfRows, numberOfCols, numberOfBombs)) {
					LinearLayout startScreen = findViewById(R.id.start_screen);
					PopupHelper.displayPopup(gridTooSmallPopup, startScreen, getResources());
					return;
				}

				Spinner gameType = newGamePopup.getContentView().findViewById(R.id.game_type);
				String selectedGameType = gameType.getSelectedItem().toString();

				newGamePopup.dismiss();

				Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
				intent.putExtra("numberOfRows", numberOfRows);
				intent.putExtra("numberOfCols", numberOfCols);
				intent.putExtra("numberOfBombs", numberOfBombs);
				intent.putExtra("gameMode", selectedGameType);
				startActivity(intent);
			}
		});
		final EditText rowsInput = newGamePopup.getContentView().findViewById(R.id.rowsInput);
		final EditText colsInput = newGamePopup.getContentView().findViewById(R.id.colsInput);
		final EditText bombsInput = newGamePopup.getContentView().findViewById(R.id.bombsInput);

		Button beginner = newGamePopup.getContentView().findViewById(R.id.beginner);
		beginner.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._9);
				colsInput.setText(R.string._9);
				bombsInput.setText(R.string._10);
			}
		});

		Button intermediate = newGamePopup.getContentView().findViewById(R.id.intermediate);
		intermediate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._14);
				colsInput.setText(R.string._16);
				bombsInput.setText(R.string._40);
			}
		});

		Button expert = newGamePopup.getContentView().findViewById(R.id.expert);
		expert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._16);
				colsInput.setText(R.string._30);
				bombsInput.setText(R.string._99);
			}
		});
	}

	private void setUpGridTooSmallPopup() {
		gridTooSmallPopup = PopupHelper.initializePopup(getApplicationContext(), R.layout.grid_too_small_for_zero_start_popup);
		Button okButton = gridTooSmallPopup.getContentView().findViewById(R.id.gridTooSmallOkButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				gridTooSmallPopup.dismiss();
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
