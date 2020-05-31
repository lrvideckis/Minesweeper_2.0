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
import com.example.minesweeper20.helpers.PopupHelper;
import com.example.minesweeper20.view.GameCanvas;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private boolean
			toggleFlagModeOn = false,
			toggleBacktrackingHintsOn = false,
			toggleGaussHintsOn = false,
			toggleBombsOn = false,
			toggleBombProbabilityOn = false,
			solverHitIterationLimit = false;
	private int numberOfRows, numberOfCols, numberOfBombs;
	private String gameMode;
	private PopupWindow solverHitLimitPopup, stackStacePopup;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 1);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 1);
		numberOfBombs = getIntent().getIntExtra("numberOfBombs", 1);
		gameMode = getIntent().getStringExtra("gameMode");
		setContentView(R.layout.game);
		Button backToStartScreen = findViewById(R.id.backToStartScreen);
		backToStartScreen.setOnClickListener(this);

		Switch toggleFlagMode = findViewById(R.id.toggleFlagMode);
		toggleFlagMode.setOnCheckedChangeListener(this);

		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setOnCheckedChangeListener(this);

		Switch toggleBombs = findViewById(R.id.toggleBombs);
		toggleBombs.setOnCheckedChangeListener(this);

		Switch toggleProbability = findViewById(R.id.toggleBombProbability);
		toggleProbability.setOnCheckedChangeListener(this);

		Switch toggleGaussHints = findViewById(R.id.toggleGaussHints);
		toggleGaussHints.setOnCheckedChangeListener(this);

		updateNumberOfBombs(numberOfBombs);
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
				handleHintToggle(buttonView, isChecked);
				break;
			case R.id.toggleBombs:
				handleToggleShowBombs(isChecked);
				break;
			case R.id.toggleBombProbability:
				handleToggleBombProbability(isChecked);
				break;
			case R.id.toggleGaussHints:
				handleGaussHintToggle(isChecked);
				break;
		}
	}

	private void handleToggleShowBombs(boolean isChecked) {
		toggleBombsOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		gameCanvas.invalidate();
	}

	private void handleToggleBombProbability(boolean isChecked) {
		toggleBombProbabilityOn = isChecked;
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

			if (toggleBombProbabilityOn) {
				toggleBombProbabilityOn = false;
				Switch bombProbability = findViewById(R.id.toggleBombProbability);
				bombProbability.setChecked(false);
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

	private void handleHintToggle(CompoundButton buttonView, boolean isChecked) {
		if (solverHitIterationLimit) {
			buttonView.setChecked(false);
			Switch bombProbability = findViewById(R.id.toggleBombProbability);
			bombProbability.setChecked(false);
			toggleBombProbabilityOn = false;
			PopupHelper.displayPopup(solverHitLimitPopup, findViewById(R.id.gameLayout), getResources());
			return;
		}
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
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				solverHitLimitPopup.dismiss();
			}
		});
	}

	private void setUpStackTracePopup() {
		stackStacePopup = PopupHelper.initializePopup(this, R.layout.stack_trace_popup);
	}

	public void solverHasJustHitIterationLimit() {
		String[] gameChoices = getResources().getStringArray(R.array.game_type);
		if (!gameMode.equals(gameChoices[0])) {
			onClick(findViewById(R.id.gameLayout));
			return;
		}
		solverHitIterationLimit = true;
		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setChecked(false);
		toggleBacktrackingHintsOn = false;
		PopupHelper.displayPopup(solverHitLimitPopup, findViewById(R.id.gameLayout), getResources());
	}

	public void updateNumberOfBombs(int numberOfBombsLeft) {
		TextView numberOfBombs = findViewById(R.id.showNumberOfBombs);
		final String bombsLeft = "bombs: " + numberOfBombsLeft;
		numberOfBombs.setText(bombsLeft);
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

	public int getNumberOfBombs() {
		return numberOfBombs;
	}

	public boolean getToggleFlagModeOn() {
		return toggleFlagModeOn;
	}

	public boolean getToggleBombsOn() {
		return toggleBombsOn;
	}

	public boolean getToggleBacktrackingHintsOn() {
		return toggleBacktrackingHintsOn;
	}

	public boolean getToggleBombProbabilityOn() {
		return toggleBombProbabilityOn;
	}

	public String getGameMode() {
		return gameMode;
	}

	public boolean getToggleGaussHintsOn() {
		return toggleGaussHintsOn;
	}
}
