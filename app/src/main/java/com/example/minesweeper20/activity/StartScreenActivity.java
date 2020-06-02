package com.example.minesweeper20.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
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

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

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
				final SeekBar rowInput = findViewById(R.id.rowsInput);
				final SeekBar colInput = findViewById(R.id.colsInput);
				final SeekBar bombInput = findViewById(R.id.bombsInput);

				final int numberOfRows = rowInput.getProgress();
				final int numberOfCols = colInput.getProgress();
				final int numberOfBombs = bombInput.getProgress();

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
		final SeekBar rowsInput = findViewById(R.id.rowsInput);
		final SeekBar colsInput = findViewById(R.id.colsInput);
		final SeekBar bombsInput = findViewById(R.id.bombsInput);

		rowsInput.setOnSeekBarChangeListener(this);
		colsInput.setOnSeekBarChangeListener(this);
		bombsInput.setOnSeekBarChangeListener(this);

		final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 9);
		final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 9);
		final int previousBombs = sharedPreferences.getInt(NUMBER_OF_BOMBS, 10);

		rowsInput.setProgress(previousRows);
		colsInput.setProgress(previousCols);
		bombsInput.setProgress(previousBombs);


		Button beginner = findViewById(R.id.beginner);
		beginner.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setProgress(9);
				colsInput.setProgress(9);
				bombsInput.setProgress(10);
			}
		});

		Button intermediate = findViewById(R.id.intermediate);
		intermediate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setProgress(14);
				colsInput.setProgress(16);
				bombsInput.setProgress(40);
			}
		});

		Button expert = findViewById(R.id.expert);
		expert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				rowsInput.setProgress(16);
				colsInput.setProgress(30);
				bombsInput.setProgress(99);
			}
		});

		Test.testPreviouslyFailedBoards();
		Test.performTestsForBombProbability(20);
		Test.performTestsForFractionOverflow(20);
		Test.performTestsForGaussSolver(20);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		String text;
		switch (seekBar.getId()) {
			case R.id.rowsInput:
				TextView rowsText = findViewById(R.id.rowsText);
				text = "Height: " + seekBar.getProgress();
				rowsText.setText(text);
				setMaxBombSeekBar();
				break;
			case R.id.colsInput:
				TextView colsText = findViewById(R.id.colsText);
				text = "Width: " + seekBar.getProgress();
				colsText.setText(text);
				setMaxBombSeekBar();
				break;
			case R.id.bombsInput:
				TextView bombsText = findViewById(R.id.bombsText);
				text = "Mines: " + seekBar.getProgress();
				bombsText.setText(text);
				break;
		}
	}

	private void setMaxBombSeekBar() {
		final int rows = ((SeekBar) findViewById(R.id.rowsInput)).getProgress();
		final int cols = ((SeekBar) findViewById(R.id.colsInput)).getProgress();
		SeekBar bombsInput = findViewById(R.id.bombsInput);
		bombsInput.setMax(rows * cols - 9);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}
