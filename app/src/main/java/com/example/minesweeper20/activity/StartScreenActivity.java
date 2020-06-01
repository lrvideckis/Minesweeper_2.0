package com.example.minesweeper20.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.miscHelpers.NumberInputFilterMinMax;
import com.example.minesweeper20.miscHelpers.Test;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: undo button - only when you lose
//TODO: add settings page were you can choose whether or not to have a zero-start, also choose iteration limit of backtracking solver, also choose defaults for Flag Mode, Game mode, etc
//TODO: make sure back button on bottom nav bar works
//TODO: when you lose, you can't click the back button, you have to click the okay popup button - fix this
//TODO: Backtracking popup shows twice when clicking bomb probability
//TODO: Win screen
//TODO: Hard mode: always lose if there exists a possible bomb combination s.t. the move loses
//TODO: Easy mode: always keep playing if there is a combination of bombs St the move doesn't lose
//TODO: Make minesweeper endless: always force >= 1 visible tile on the screen
//TODO: Recommend the guess which will reveal the greatest amount of further stuff

public class StartScreenActivity extends AppCompatActivity {

	public static final String
			MY_PREFERENCES = "MyPrefs",
			NUMBER_OF_ROWS = "numRows",
			NUMBER_OF_COLS = "numCols",
			NUMBER_OF_BOMBS = "numBombs";
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_screen);

		sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);


		Button okButton = findViewById(R.id.startNewGameButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("InflateParams")
			@Override
			public void onClick(View view) {
				final EditText rowInput = findViewById(R.id.rowsInput);
				final EditText colInput = findViewById(R.id.colsInput);
				final EditText bombInput = findViewById(R.id.bombsInput);

				final int numberOfRows = getNumberInput(rowInput.getText().toString());
				final int numberOfCols = getNumberInput(colInput.getText().toString());
				final int numberOfBombs = getNumberInput(bombInput.getText().toString());

				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putInt(NUMBER_OF_ROWS, numberOfRows);
				editor.putInt(NUMBER_OF_COLS, numberOfCols);
				editor.putInt(NUMBER_OF_BOMBS, numberOfBombs);
				editor.apply();

				if (MinesweeperGame.tooManyBombsForZeroStart(numberOfRows, numberOfCols, numberOfBombs)) {
					System.out.println("too many bombs for zero start, UI doesn't allow for this to happen");
					return;
				}

				Spinner gameType = findViewById(R.id.game_type);
				String selectedGameType = gameType.getSelectedItem().toString();

				Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
				intent.putExtra("numberOfRows", numberOfRows);
				intent.putExtra("numberOfCols", numberOfCols);
				intent.putExtra("numberOfBombs", numberOfBombs);
				intent.putExtra("gameMode", selectedGameType);
				startActivity(intent);
			}
		});
		final EditText rowsInput = findViewById(R.id.rowsInput);
		final EditText colsInput = findViewById(R.id.colsInput);
		final EditText bombsInput = findViewById(R.id.bombsInput);

		final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 9);
		final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 9);
		final int previousBombs = sharedPreferences.getInt(NUMBER_OF_BOMBS, 10);

		rowsInput.setText(String.valueOf(previousRows));
		colsInput.setText(String.valueOf(previousCols));
		bombsInput.setText(String.valueOf(previousBombs));


		NumberInputFilterMinMax rowsAndColsFilter = new NumberInputFilterMinMax(1, 50);
		rowsInput.setFilters(new InputFilter[]{rowsAndColsFilter});
		colsInput.setFilters(new InputFilter[]{rowsAndColsFilter});
		bombsInput.setFilters(new InputFilter[]{new NumberInputFilterMinMax(1, 1000)});

		Button beginner = findViewById(R.id.beginner);
		beginner.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._9);
				colsInput.setText(R.string._9);
				bombsInput.setText(R.string._10);
			}
		});

		Button intermediate = findViewById(R.id.intermediate);
		intermediate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._14);
				colsInput.setText(R.string._16);
				bombsInput.setText(R.string._40);
			}
		});

		Button expert = findViewById(R.id.expert);
		expert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setText(R.string._16);
				colsInput.setText(R.string._30);
				bombsInput.setText(R.string._99);
			}
		});

		Test.testPreviouslyFailedBoards();
		Test.performTestsForBombProbability(20);
		Test.performTestsForFractionOverflow(20);
		Test.performTestsForGaussSolver(20);
	}

	private int getNumberInput(String input) {
		if (input.equals("")) {
			return 1;
		}
		return Integer.parseInt(input);
	}
}
