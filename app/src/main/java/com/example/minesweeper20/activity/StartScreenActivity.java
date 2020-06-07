package com.example.minesweeper20.activity;

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
//TODO: Backtracking popup shows twice when clicking mine probability
//TODO: Win screen - make this look good and improve functionality
//TODO: Hard mode: always lose if there exists a possible mine combination s.t. the move loses
//TODO: Easy mode: always keep playing if there is a combination of mines St the move doesn't lose
//TODO: Make minesweeper endless: always force >= 1 visible tile on the screen
//TODO: Recommend the guess which will reveal the greatest amount of further stuff

public class StartScreenActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

	public static final int rowsColsMax = 50;

	public static final String
			MY_PREFERENCES = "MyPrefs",
			NUMBER_OF_ROWS = "numRows",
			NUMBER_OF_COLS = "numCols",
			NUMBER_OF_MINES = "numMines";
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Test.testPreviouslyFailedBoards();
		Test.performTestsForMineProbability(20);
		Test.performTestsWithBigIntSolverForLargerGrids(20);
		Test.performTestsForGaussSolver(20);
		Test.performTestsMultipleRunsOfSameBoard(10);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_screen);

		sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);

		Button rowsDecrement = findViewById(R.id.rowsDecrement);
		Button rowsIncrement = findViewById(R.id.rowsIncrement);
		Button colsDecrement = findViewById(R.id.colsDecrement);
		Button colsIncrement = findViewById(R.id.colsIncrement);
		Button minesDecrement = findViewById(R.id.minesDecrement);
		Button minesIncrement = findViewById(R.id.minesIncrement);
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

		rowsInput.setMax(rowsColsMax);
		colsInput.setMax(rowsColsMax);

		final int previousRows = sharedPreferences.getInt(NUMBER_OF_ROWS, 9);
		final int previousCols = sharedPreferences.getInt(NUMBER_OF_COLS, 9);
		final int previousMines = sharedPreferences.getInt(NUMBER_OF_MINES, 10);

		rowsInput.setProgress(previousRows);
		colsInput.setProgress(previousCols);
		minesInput.setProgress(previousMines);

		Button okButton = findViewById(R.id.startNewGameButton);
		okButton.setOnClickListener(new okButtonListener(rowsInput, colsInput, minesInput));

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
		minesInput.setMax(rows * cols - 9);
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
			case R.id.rowsDecrement:
				rows = Math.max(4, rows - 1);
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
				cols = Math.max(4, cols - 1);
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
				mines = Math.max(0, mines - 1);
				minesInput.setProgress(mines);
				setMinesText(rows, cols, mines);
				break;
			case R.id.minesIncrement:
				mines = Math.min(rows * cols - 9, mines + 1);
				minesInput.setProgress(mines);
				setMinesText(rows, cols, mines);
				break;
		}
	}

	private class okButtonListener implements View.OnClickListener {

		private final SeekBar rowInput;
		private final SeekBar colInput;
		private final SeekBar mineInput;

		public okButtonListener(SeekBar rowInput, SeekBar colInput, SeekBar mineInput) {
			this.rowInput = rowInput;
			this.colInput = colInput;
			this.mineInput = mineInput;
		}

		@Override
		public void onClick(View v) {
			final int numberOfRows = this.rowInput.getProgress();
			final int numberOfCols = this.colInput.getProgress();
			final int numberOfMines = this.mineInput.getProgress();

			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt(NUMBER_OF_ROWS, numberOfRows);
			editor.putInt(NUMBER_OF_COLS, numberOfCols);
			editor.putInt(NUMBER_OF_MINES, numberOfMines);
			editor.apply();

			if (MinesweeperGame.tooManyMinesForZeroStart(numberOfRows, numberOfCols, numberOfMines)) {
				System.out.println("too many mines for zero start, UI doesn't allow for this to happen");
				return;
			}

			Spinner gameType = findViewById(R.id.game_type);
			String selectedGameType = gameType.getSelectedItem().toString();

			Intent intent = new Intent(StartScreenActivity.this, GameActivity.class);
			intent.putExtra("numberOfRows", numberOfRows);
			intent.putExtra("numberOfCols", numberOfCols);
			intent.putExtra("numberOfMines", numberOfMines);
			intent.putExtra("gameMode", selectedGameType);
			startActivity(intent);
		}

	}
}
