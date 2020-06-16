package com.LukeVideckis.minesweeper20.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.LukeVideckis.minesweeper20.R;
import com.LukeVideckis.minesweeper20.miscHelpers.PopupHelper;
import com.LukeVideckis.minesweeper20.miscHelpers.Test;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: add settings page were you can choose whether or not to have a zero-start, also choose iteration limit of backtracking solver, also choose defaults for Flag Mode, Game mode, etc
//TODO: Make minesweeper endless: always force >= 1 visible tile on the screen
//TODO: Recommend the guess which will reveal the greatest amount of further stuff
//TODO: save personal high scores (the time) for beginner, intermediate, expert
//TODO: figure out what to make button to the left of the yellow smiley face button

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

	public static final int rowsColsMax = 30, rowsColsMin = 8;

	public static final String
			MY_PREFERENCES = "MyPrefs",
			NUMBER_OF_ROWS = "numRows",
			NUMBER_OF_COLS = "numCols",
			NUMBER_OF_MINES = "numMines",
			GAME_MODE = "gameMode";
	private SharedPreferences sharedPreferences;
	private PopupWindow normalModeInfoPopup, noGuessingModeInfoPopup, noGuessingModeWith8InfoPopup;
	private int minesMin = 0;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		/*
		try {
			Test.testPreviouslyFailedBoards();
			Test.performTestsForMineProbability(20);
			Test.performTestsWithBigIntSolverForLargerGrids(20);
			Test.performTestsForGaussSolver(20);
			Test.performTestsMultipleRunsOfSameBoard(10);
			Test.TestThatSolvableBoardsAreSolvable(20);
			Test.TestThatSolvableBoardsWith8AreSolvable(10);
		} catch (Exception e) {
			e.printStackTrace();
		}
		 */

		try {
			Test.CompareTimeOfSolvers(10);
		} catch (Exception e) {
			e.printStackTrace();
		}


		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_screen);

		sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);

		final Button rowsDecrement = findViewById(R.id.rowsDecrement);
		final Button rowsIncrement = findViewById(R.id.rowsIncrement);
		final Button colsDecrement = findViewById(R.id.colsDecrement);
		final Button colsIncrement = findViewById(R.id.colsIncrement);
		final Button minesDecrement = findViewById(R.id.minesDecrement);
		final Button minesIncrement = findViewById(R.id.minesIncrement);
		rowsDecrement.setOnClickListener(this);
		rowsIncrement.setOnClickListener(this);
		colsDecrement.setOnClickListener(this);
		colsIncrement.setOnClickListener(this);
		minesDecrement.setOnClickListener(this);
		minesIncrement.setOnClickListener(this);

		final SeekBar rowsInput = findViewById(R.id.rowsInput);
		final SeekBar colsInput = findViewById(R.id.colsInput);
		final SeekBar minesInput = findViewById(R.id.mineInput);

		rowsInput.setOnSeekBarChangeListener(this);
		colsInput.setOnSeekBarChangeListener(this);
		minesInput.setOnSeekBarChangeListener(this);

		rowsInput.setMin(rowsColsMin);
		colsInput.setMin(rowsColsMin);
		rowsInput.setMax(rowsColsMax);
		colsInput.setMax(rowsColsMax);

		RadioButton normalMode = findViewById(R.id.normal_mode);
		normalMode.setOnClickListener(this);

		RadioButton noGuessingMode = findViewById(R.id.no_guessing_mode);
		noGuessingMode.setOnClickListener(this);

		RadioButton noGuessingModeWith8 = findViewById(R.id.noGuessingModeWithAn8);
		noGuessingModeWith8.setOnClickListener(this);

		final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 9);
		final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 9);
		final int previousMines = sharedPreferences.getInt(NUMBER_OF_MINES, 10);
		final int gameMode = sharedPreferences.getInt(GAME_MODE, R.id.normal_mode);

		rowsInput.setProgress(previousRows);
		colsInput.setProgress(previousCols);
		minesInput.setProgress(previousMines);
		if (gameMode == R.id.normal_mode) {
			normalMode.setChecked(true);
		} else if (gameMode == R.id.no_guessing_mode) {
			noGuessingMode.setChecked(true);
		} else if (gameMode == R.id.noGuessingModeWithAn8) {
			noGuessingModeWith8.setChecked(true);
			minesInput.setMin(8);
			minesMin = 8;
		} else {
			normalMode.setChecked(true);
		}

		Button startNewGameButton = findViewById(R.id.startNewGameButton);
		startNewGameButton.setOnClickListener(new startNewGameButtonListener(rowsInput, colsInput, minesInput));

		Button beginner = findViewById(R.id.beginner);
		beginner.setOnClickListener(view -> {
			rowsInput.setProgress(9);
			colsInput.setProgress(9);
			minesInput.setProgress(10);
		});

		Button intermediate = findViewById(R.id.intermediate);
		intermediate.setOnClickListener(view -> {
			rowsInput.setProgress(14);
			colsInput.setProgress(16);
			minesInput.setProgress(40);
		});

		Button expert = findViewById(R.id.expert);
		expert.setOnClickListener(view -> {
			rowsInput.setProgress(16);
			colsInput.setProgress(30);
			minesInput.setProgress(99);
		});

		TextView normalModeInfo = findViewById(R.id.normal_mode_info);
		normalModeInfo.setOnClickListener(this);

		TextView noGuessingModeInfo = findViewById(R.id.no_guessing_mode_info);
		noGuessingModeInfo.setOnClickListener(this);

		TextView noGuessingModeWith8Info = findViewById(R.id.no_guessing_mode_with_8_info);
		noGuessingModeWith8Info.setOnClickListener(this);

		setUpNormalModeInfoPopup();
		setUpNoGuessingModeInfoPopup();
		setUpNoGuessingWith8ModeInfoPopup();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch (seekBar.getId()) {
			case R.id.rowsInput:
				setRowsText(seekBar.getProgress());
				break;
			case R.id.colsInput:
				setColsText(seekBar.getProgress());
				break;
			case R.id.mineInput:
				final int rows = ((SeekBar) findViewById(R.id.rowsInput)).getProgress();
				final int cols = ((SeekBar) findViewById(R.id.colsInput)).getProgress();
				setMinesText(rows, cols, seekBar.getProgress());
				break;
		}
	}

	private void setRowsText(int val) {
		TextView rowsText = findViewById(R.id.rowsText);
		String text = "Height: " + val;
		rowsText.setText(text);
		setMaxMineSeekBar();
	}

	private void setColsText(int val) {
		TextView colsText = findViewById(R.id.colsText);
		String text = "Width: " + val;
		colsText.setText(text);
		setMaxMineSeekBar();
	}

	private void setMinesText(int rows, int cols, int mines) {
		TextView minesText = findViewById(R.id.mineText);
		String text = "Mines: " + mines + '\n';
		double minePercentage = 100 * mines / (double) (rows * cols);
		text += String.format(getResources().getString(R.string.two_decimal_places), minePercentage);
		text += '%';
		minesText.setText(text);
	}

	private void setMaxMineSeekBar() {
		final int rows = ((SeekBar) findViewById(R.id.rowsInput)).getProgress();
		final int cols = ((SeekBar) findViewById(R.id.colsInput)).getProgress();
		SeekBar minesInput = findViewById(R.id.mineInput);
		minesInput.setMax(rows * cols - 10);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onClick(View v) {
		final SeekBar rowsInput = findViewById(R.id.rowsInput);
		final SeekBar colsInput = findViewById(R.id.colsInput);
		final SeekBar minesInput = findViewById(R.id.mineInput);

		int rows = rowsInput.getProgress();
		int cols = colsInput.getProgress();
		int mines = minesInput.getProgress();

		switch (v.getId()) {
			case R.id.normal_mode:
				RadioButton noGuessingMode = findViewById(R.id.no_guessing_mode);
				noGuessingMode.setChecked(false);
				RadioButton noGuessingModeWith8 = findViewById(R.id.noGuessingModeWithAn8);
				noGuessingModeWith8.setChecked(false);
				minesInput.setMin(0);
				minesMin = 0;
				break;
			case R.id.no_guessing_mode:
				RadioButton normalMode = findViewById(R.id.normal_mode);
				normalMode.setChecked(false);
				noGuessingModeWith8 = findViewById(R.id.noGuessingModeWithAn8);
				noGuessingModeWith8.setChecked(false);
				minesInput.setMin(0);
				minesMin = 0;
				break;
			case R.id.noGuessingModeWithAn8:
				normalMode = findViewById(R.id.normal_mode);
				normalMode.setChecked(false);
				noGuessingMode = findViewById(R.id.no_guessing_mode);
				noGuessingMode.setChecked(false);
				minesInput.setMin(8);
				minesMin = 8;
				break;
			case R.id.normal_mode_info:
				displayNormalModeInfoPopup();
				break;
			case R.id.no_guessing_mode_info:
				displayNoGuessingModeInfoPopup();
				break;
			case R.id.no_guessing_mode_with_8_info:
				displayNoGuessingWith8ModeInfoPopup();
				break;


			case R.id.rowsDecrement:
				rows = Math.max(rowsColsMin, rows - 1);
				rowsInput.setProgress(rows);
				setRowsText(rows);
				setMinesText(rows, cols, mines);
				break;
			case R.id.rowsIncrement:
				rows = Math.min(rowsColsMax, rows + 1);
				rowsInput.setProgress(rows);
				setRowsText(rows);
				setMinesText(rows, cols, mines);
				break;
			case R.id.colsDecrement:
				cols = Math.max(rowsColsMin, cols - 1);
				colsInput.setProgress(cols);
				setColsText(cols);
				setMinesText(rows, cols, mines);
				break;
			case R.id.colsIncrement:
				cols = Math.min(rowsColsMax, cols + 1);
				colsInput.setProgress(cols);
				setColsText(cols);
				setMinesText(rows, cols, mines);
				break;
			case R.id.minesDecrement:
				mines = Math.max(minesMin, mines - 1);
				minesInput.setProgress(mines);
				setMinesText(rows, cols, mines);
				break;
			case R.id.minesIncrement:
				mines = Math.min(rows * cols - 10, mines + 1);
				minesInput.setProgress(mines);
				setMinesText(rows, cols, mines);
				break;
		}
	}

	public void displayNormalModeInfoPopup() {
		PopupHelper.displayPopup(normalModeInfoPopup, findViewById(R.id.startScreenLayout), getResources());
	}

	private void setUpNormalModeInfoPopup() {
		normalModeInfoPopup = PopupHelper.initializePopup(this, R.layout.normal_mode_info);
		Button okButton = normalModeInfoPopup.getContentView().findViewById(R.id.normalModeInfoOkButton);
		okButton.setOnClickListener(view -> normalModeInfoPopup.dismiss());
	}

	public void displayNoGuessingModeInfoPopup() {
		PopupHelper.displayPopup(noGuessingModeInfoPopup, findViewById(R.id.startScreenLayout), getResources());
	}

	public void displayNoGuessingWith8ModeInfoPopup() {
		PopupHelper.displayPopup(noGuessingModeWith8InfoPopup, findViewById(R.id.startScreenLayout), getResources());
	}

	private void setUpNoGuessingModeInfoPopup() {
		noGuessingModeInfoPopup = PopupHelper.initializePopup(this, R.layout.no_guessing_mode_info);
		Button okButton = noGuessingModeInfoPopup.getContentView().findViewById(R.id.noGuessingModeInfoOkButton);
		okButton.setOnClickListener(view -> noGuessingModeInfoPopup.dismiss());
	}

	private void setUpNoGuessingWith8ModeInfoPopup() {
		noGuessingModeWith8InfoPopup = PopupHelper.initializePopup(this, R.layout.no_guessing_mode_with_8_info);
		Button okButton = noGuessingModeWith8InfoPopup.getContentView().findViewById(R.id.noGuessingModeWith8InfoOkButton);
		okButton.setOnClickListener(view -> noGuessingModeWith8InfoPopup.dismiss());
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	private class startNewGameButtonListener implements View.OnClickListener {

		private final SeekBar rowInput;
		private final SeekBar colInput;
		private final SeekBar mineInput;

		public startNewGameButtonListener(SeekBar rowInput, SeekBar colInput, SeekBar mineInput) {
			this.rowInput = rowInput;
			this.colInput = colInput;
			this.mineInput = mineInput;
		}

		@Override
		public void onClick(View v) {
			final int numberOfRows = this.rowInput.getProgress();
			final int numberOfCols = this.colInput.getProgress();
			final int numberOfMines = this.mineInput.getProgress();

			final RadioButton normalMode = findViewById(R.id.normal_mode);
			final RadioButton noGuessMode = findViewById(R.id.no_guessing_mode);
			final RadioButton noGuessModeWith8 = findViewById(R.id.noGuessingModeWithAn8);

			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt(NUMBER_OF_ROWS, numberOfRows);
			editor.putInt(NUMBER_OF_COLS, numberOfCols);
			editor.putInt(NUMBER_OF_MINES, numberOfMines);
			if (normalMode.isChecked()) {
				editor.putInt(GAME_MODE, R.id.normal_mode);
			} else if (noGuessMode.isChecked()) {
				editor.putInt(GAME_MODE, R.id.no_guessing_mode);
			} else if (noGuessModeWith8.isChecked()) {
				editor.putInt(GAME_MODE, R.id.noGuessingModeWithAn8);
			}
			editor.apply();

			Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
			intent.putExtra(NUMBER_OF_ROWS, numberOfRows);
			intent.putExtra(NUMBER_OF_COLS, numberOfCols);
			intent.putExtra(NUMBER_OF_MINES, numberOfMines);
			if (normalMode.isChecked()) {
				intent.putExtra(GAME_MODE, R.id.normal_mode);
			} else if (noGuessMode.isChecked()) {
				intent.putExtra(GAME_MODE, R.id.no_guessing_mode);
			} else if (noGuessModeWith8.isChecked()) {
				intent.putExtra(GAME_MODE, R.id.noGuessingModeWithAn8);
			}
			startActivity(intent);
		}
	}
}
