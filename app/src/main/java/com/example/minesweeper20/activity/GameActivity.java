package com.example.minesweeper20.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.miscHelpers.PopupHelper;
import com.example.minesweeper20.view.GameCanvas;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Locale;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private boolean
			toggleFlagModeOn = false,
			toggleBacktrackingHintsOn = false,
			toggleGaussHintsOn = false,
			toggleMinesOn = false,
			toggleMineProbabilityOn = false;
	private int numberOfRows, numberOfCols, numberOfMines;
	private String gameMode;
	private PopupWindow solverHitLimitPopup, stackStacePopup;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 1);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 1);
		numberOfMines = getIntent().getIntExtra("numberOfMines", 1);
		gameMode = getIntent().getStringExtra("gameMode");
		setContentView(R.layout.game);
		Button backToStartScreen = findViewById(R.id.backToStartScreen);
		backToStartScreen.setOnClickListener(this);

		Switch toggleFlagMode = findViewById(R.id.toggleFlagMode);
		toggleFlagMode.setOnCheckedChangeListener(this);

		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setOnCheckedChangeListener(this);

		Switch toggleMines = findViewById(R.id.toggleMines);
		toggleMines.setOnCheckedChangeListener(this);

		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setOnCheckedChangeListener(this);

		Switch toggleGaussHints = findViewById(R.id.toggleGaussHints);
		toggleGaussHints.setOnCheckedChangeListener(this);

		updateNumberOfMines(numberOfMines);
		setUpIterationLimitPopup();
		setUpStackTracePopup();
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
		startActivity(intent);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.toggleFlagMode:
				toggleFlagModeOn = isChecked;
				break;
			case R.id.toggleBacktrackingHints:
				handleHintToggle(isChecked);
				break;
			case R.id.toggleMines:
				handleToggleShowMines(isChecked);
				break;
			case R.id.toggleMineProbability:
				handleToggleMineProbability(isChecked);
				break;
			case R.id.toggleGaussHints:
				handleGaussHintToggle(isChecked);
				break;
		}
	}

	private void handleToggleShowMines(boolean isChecked) {
		toggleMinesOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		gameCanvas.invalidate();
	}

	private void handleToggleMineProbability(boolean isChecked) {
		toggleMineProbabilityOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		if (isChecked) {
			try {
				gameCanvas.updateSolvedBoardWithBacktrackingSolver();
			} catch (Exception e) {
				displayStackTracePopup(e);
				e.printStackTrace();
			}
			if (toggleGaussHintsOn) {
				toggleGaussHintsOn = false;
				Switch gaussHints = findViewById(R.id.toggleGaussHints);
				gaussHints.setChecked(false);
			}
		}
		gameCanvas.invalidate();
	}

	private void handleGaussHintToggle(boolean isChecked) {
		toggleGaussHintsOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		if (isChecked) {

			if (toggleBacktrackingHintsOn) {
				toggleBacktrackingHintsOn = false;
				Switch backtrackingHints = findViewById(R.id.toggleBacktrackingHints);
				backtrackingHints.setChecked(false);
			}

			if (toggleMineProbabilityOn) {
				toggleMineProbabilityOn = false;
				Switch mineProbability = findViewById(R.id.toggleMineProbability);
				mineProbability.setChecked(false);
			}

			try {
				gameCanvas.updateSolvedBoardWithGaussSolver();
			} catch (Exception e) {
				displayStackTracePopup(e);
				e.printStackTrace();
			}
		}
		gameCanvas.invalidate();
	}

	private void handleHintToggle(boolean isChecked) {
		toggleBacktrackingHintsOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		if (isChecked) {
			try {
				gameCanvas.updateSolvedBoardWithBacktrackingSolver();
			} catch (Exception e) {
				displayStackTracePopup(e);
				e.printStackTrace();
			}
			if (toggleGaussHintsOn) {
				toggleGaussHintsOn = false;
				Switch gaussHints = findViewById(R.id.toggleGaussHints);
				gaussHints.setChecked(false);
			}
		} else {
			if (toggleGaussHintsOn) {
				try {
					gameCanvas.updateSolvedBoardWithGaussSolver();
				} catch (Exception e) {
					displayStackTracePopup(e);
					e.printStackTrace();
				}
			}
			updateNumberOfSolverIterations(0);
		}
		gameCanvas.invalidate();
	}

	public void displayStackTracePopup(Exception e) {
		TextView textView = stackStacePopup.getContentView().findViewById(R.id.stackTrace);
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		textView.setText(sw.toString());
		textView.setMovementMethod(new ScrollingMovementMethod());
		PopupHelper.displayPopup(stackStacePopup, findViewById(R.id.gameLayout), getResources());
	}

	private void setUpIterationLimitPopup() {
		solverHitLimitPopup = PopupHelper.initializePopup(this, R.layout.solver_hit_limit_popup);
		Button okButton = solverHitLimitPopup.getContentView().findViewById(R.id.solverHitLimitOkButton);
		okButton.setOnClickListener(view -> solverHitLimitPopup.dismiss());
		TextView textView = solverHitLimitPopup.getContentView().findViewById(R.id.iterationLimitText);
		String text = "Backtracking solver took more than ";
		text += NumberFormat.getNumberInstance(Locale.US).format(BacktrackingSolver.iterationLimit);
		text += " iterations. Hints and mine probability features are no longer available.";
		textView.setText(text);
	}

	private void setUpStackTracePopup() {
		stackStacePopup = PopupHelper.initializePopup(this, R.layout.stack_trace_popup);
	}

	public void solverHitIterationLimit() {
		String[] gameChoices = getResources().getStringArray(R.array.game_type);
		//TODO: think about changing this behavior to just switching modes to back to normal mode
		if (!gameMode.equals(gameChoices[0])) {
			onClick(findViewById(R.id.gameLayout));
			return;
		}
		if (toggleBacktrackingHintsOn) {
			Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
			toggleHints.setChecked(false);
			toggleBacktrackingHintsOn = false;
		}
		if (toggleMineProbabilityOn) {
			Switch toggleProb = findViewById(R.id.toggleMineProbability);
			toggleProb.setChecked(false);
			toggleMineProbabilityOn = false;
		}
		updateNumberOfSolverIterations(0);
		PopupHelper.displayPopup(solverHitLimitPopup, findViewById(R.id.gameLayout), getResources());
	}

	public void updateNumberOfMines(int numberOfMinesLeft) {
		TextView numberOfMines = findViewById(R.id.showNumberOfMines);
		final String minesLeft = "mines: " + numberOfMinesLeft;
		numberOfMines.setText(minesLeft);
	}

	public void updateNumberOfSolverIterations(int numberOfIterations) {
		TextView iterationTextView = findViewById(R.id.numberOfIterationsTextView);
		final String iterationsText = "iterations: " + numberOfIterations;
		iterationTextView.setText(iterationsText);
	}

	public int getNumberOfRows() {
		return numberOfRows;
	}

	public int getNumberOfCols() {
		return numberOfCols;
	}

	public int getNumberOfMines() {
		return numberOfMines;
	}

	public boolean getToggleFlagModeOn() {
		return toggleFlagModeOn;
	}

	public boolean getToggleMinesOn() {
		return toggleMinesOn;
	}

	public boolean getToggleBacktrackingHintsOn() {
		return toggleBacktrackingHintsOn;
	}

	public boolean getToggleMineProbabilityOn() {
		return toggleMineProbabilityOn;
	}

	public String getGameMode() {
		return gameMode;
	}

	public boolean getToggleGaussHintsOn() {
		return toggleGaussHintsOn;
	}
}
