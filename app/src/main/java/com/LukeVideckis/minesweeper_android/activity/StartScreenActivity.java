package com.LukeVideckis.minesweeper_android.activity;

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

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.miscHelpers.PopupHelper;
import com.LukeVideckis.minesweeper_android.miscHelpers.Test;

//TODO: change game board scale to be pivoted around focus point instead of the middle of the screen
//TODO: add settings page were you can choose whether or not to have a zero-start, also choose iteration limit of backtracking solver, also choose defaults for Flag Mode, Game mode, etc
//TODO: Make minesweeper endless: always force >= 1 visible tile on the screen
//TODO: Recommend the guess which will reveal the greatest amount of further stuff
//TODO: save personal high scores (the time) for beginner, intermediate, expert
//TODO: figure out what to make button to the left of the yellow smiley face button

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

	public static final int rowsColsMin = 10, rowsColsMax = 30;
	public static final String
			MY_PREFERENCES = "MyPrefs",
			NUMBER_OF_ROWS = "numRows",
			NUMBER_OF_COLS = "numCols",
			NUMBER_OF_MINES = "numMines",
			GAME_MODE = "gameMode";
	private static final float maxMinePercentage = 0.23f, maxMinePercentageWith8 = 0.22f;
	private SharedPreferences sharedPreferences;
	private PopupWindow normalModeInfoPopup, noGuessingModeInfoPopup, noGuessingModeWith8InfoPopup;
	private int minesMin = 0, minesMax = 10 * 10 - 10, gameMode;

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
			//Test.BestSolverOnly(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/


		try {
			Test.performTestsForMineProbability(20);
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

		final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 10);
		final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 10);
		final int previousMines = sharedPreferences.getInt(NUMBER_OF_MINES, 10);
		gameMode = sharedPreferences.getInt(GAME_MODE, R.id.normal_mode);

		rowsInput.setProgress(previousRows);
		colsInput.setProgress(previousCols);
		minesInput.setProgress(previousMines);
		if (gameMode == R.id.no_guessing_mode) {
			noGuessingMode.setChecked(true);
		} else if (gameMode == R.id.noGuessingModeWithAn8) {
			noGuessingModeWith8.setChecked(true);
		} else {//default is normal mode
			normalMode.setChecked(true);
		}
		setMinesMinMaxAndText(previousRows, previousCols);

		Button startNewGameButton = findViewById(R.id.startNewGameButton);
		startNewGameButton.setOnClickListener(new startNewGameButtonListener(rowsInput, colsInput, minesInput));

		Button beginner = findViewById(R.id.beginner);
		beginner.setOnClickListener(view -> {
			/*
			 * beginner is 10 x 10 because solvable boards with an 8 aren't possible on 9x9 boards
			 * with start click in the center. It was easier to just make 10x10 the default for
			 * beginner boards
			 */
			rowsInput.setProgress(10);
			colsInput.setProgress(10);
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
		final int rows = ((SeekBar) findViewById(R.id.rowsInput)).getProgress();
		final int cols = ((SeekBar) findViewById(R.id.colsInput)).getProgress();

		switch (seekBar.getId()) {
			case R.id.rowsInput:
				setRowsText(seekBar.getProgress());
				setMinesMinMaxAndText(seekBar.getProgress(), cols);
				break;
			case R.id.colsInput:
				setColsText(seekBar.getProgress());
				setMinesMinMaxAndText(rows, seekBar.getProgress());
				break;
			case R.id.mineInput:
				setMinesMinMaxAndText(rows, cols);
				break;
		}
	}

	private void setRowsText(int val) {
		TextView rowsText = findViewById(R.id.rowsText);
		String text = "Height: " + val;
		rowsText.setText(text);
	}

	private void setColsText(int val) {
		TextView colsText = findViewById(R.id.colsText);
		String text = "Width: " + val;
		colsText.setText(text);
	}

	private void setMinesMinMaxAndText(int rows, int cols) {
		if (gameMode == R.id.no_guessing_mode) {
			minesMin = 0;
			minesMax = (int) (rows * cols * maxMinePercentage);
			minesMax = Math.min(minesMax, 100);
		} else if (gameMode == R.id.noGuessingModeWithAn8) {
			minesMin = 8;
			minesMax = (int) (rows * cols * maxMinePercentageWith8);
			minesMax = Math.min(minesMax, 100);
		} else {
			minesMin = 0;
			minesMax = rows * cols - 10;
		}
		SeekBar minesInput = findViewById(R.id.mineInput);
		minesInput.setMin(minesMin);
		minesInput.setMax(minesMax);

		final int mines = minesInput.getProgress();

		//update text
		TextView minesText = findViewById(R.id.mineText);
		String text = "Mines: " + mines + '\n';
		double minePercentage = 100 * mines / (double) (rows * cols);
		text += String.format(getResources().getString(R.string.two_decimal_places), minePercentage);
		text += '%';
		minesText.setText(text);
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
				gameMode = R.id.normal_mode;
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.no_guessing_mode:
				RadioButton normalMode = findViewById(R.id.normal_mode);
				normalMode.setChecked(false);
				noGuessingModeWith8 = findViewById(R.id.noGuessingModeWithAn8);
				noGuessingModeWith8.setChecked(false);
				gameMode = R.id.no_guessing_mode;
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.noGuessingModeWithAn8:
				normalMode = findViewById(R.id.normal_mode);
				normalMode.setChecked(false);
				noGuessingMode = findViewById(R.id.no_guessing_mode);
				noGuessingMode.setChecked(false);
				gameMode = R.id.noGuessingModeWithAn8;
				setMinesMinMaxAndText(rows, cols);
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
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.rowsIncrement:
				rows = Math.min(rowsColsMax, rows + 1);
				rowsInput.setProgress(rows);
				setRowsText(rows);
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.colsDecrement:
				cols = Math.max(rowsColsMin, cols - 1);
				colsInput.setProgress(cols);
				setColsText(cols);
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.colsIncrement:
				cols = Math.min(rowsColsMax, cols + 1);
				colsInput.setProgress(cols);
				setColsText(cols);
				setMinesMinMaxAndText(rows, cols);
				break;
			case R.id.minesDecrement:
				mines = Math.max(minesMin, mines - 1);
				minesInput.setProgress(mines);
				break;
			case R.id.minesIncrement:
				mines = Math.min(minesMax, mines + 1);
				minesInput.setProgress(mines);
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
