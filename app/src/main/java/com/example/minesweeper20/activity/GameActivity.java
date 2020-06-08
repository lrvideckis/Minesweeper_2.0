package com.example.minesweeper20.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.customExceptions.HitIterationLimitException;
import com.example.minesweeper20.minesweeperStuff.BacktrackingSolver;
import com.example.minesweeper20.minesweeperStuff.GaussianEliminationSolver;
import com.example.minesweeper20.minesweeperStuff.MinesweeperGame;
import com.example.minesweeper20.minesweeperStuff.MinesweeperSolver;
import com.example.minesweeper20.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.example.minesweeper20.miscHelpers.PopupHelper;
import com.example.minesweeper20.view.GameCanvas;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Locale;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
	public final static String
			flagEmoji = new String(Character.toChars(0x1F6A9)),
			mineEmoji = new String(Character.toChars(0x1F4A3));
	public static final int cellPixelLength = 150;

	private boolean
			toggleFlagModeOn = false,
			toggleBacktrackingHintsOn = false,
			toggleGaussHintsOn = false,
			toggleMinesOn = false,
			toggleMineProbabilityOn = false;
	private int numberOfRows, numberOfCols, numberOfMines;
	private String gameMode;
	private PopupWindow solverHitLimitPopup, stackStacePopup;

	private MinesweeperGame minesweeperGame;
	private MinesweeperSolver backtrackingSolver, gaussSolver;
	private MinesweeperSolver.VisibleTile[][] board;
	private int lastTapRow, lastTapCol;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 1);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 1);
		numberOfMines = getIntent().getIntExtra("numberOfMines", 1);
		gameMode = getIntent().getStringExtra("gameMode");
		setContentView(R.layout.game);

		try {
			minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
		} catch (Exception e) {
			e.printStackTrace();
		}
		backtrackingSolver = new BacktrackingSolver(numberOfRows, numberOfCols);
		gaussSolver = new GaussianEliminationSolver(numberOfRows, numberOfCols);
		board = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

		Button isThereAnyLogicalStuff = findViewById(R.id.isThereAnyLogicalStuffButton);
		isThereAnyLogicalStuff.setOnClickListener(this);
		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setOnClickListener(this);
		Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
		toggleFlagMode.setOnClickListener(this);
		toggleFlagMode.setText(mineEmoji);

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
		switch (v.getId()) {
			case R.id.isThereAnyLogicalStuffButton:
				System.out.println("is there any logical stuff button");
				break;
			case R.id.newGameButton:
				ImageButton newGameButton = findViewById(R.id.newGameButton);
				newGameButton.setBackgroundResource(R.drawable.smiley_face);
				startNewGame();
				GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
				gameCanvas.invalidate();
				break;
			case R.id.toggleFlagMode:
				toggleFlagModeOn = !toggleFlagModeOn;
				Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
				if (toggleFlagModeOn) {
					toggleFlagMode.setText(flagEmoji);
				} else {
					toggleFlagMode.setText(mineEmoji);
				}
				break;
		}
	}

	public void handleTap(float tapX, float tapY) {
		//TODO: have grid always fill screen
		//eventually I won't need this check, as the grid always fills the screen
		if (tapX < 0f ||
				tapY < 0f ||
				tapX > numberOfCols * cellPixelLength ||
				tapY > numberOfRows * cellPixelLength) {
			return;
		}
		final int row = (int) (tapY / cellPixelLength);
		final int col = (int) (tapX / cellPixelLength);

		if (!minesweeperGame.getIsGameLost()) {
			lastTapRow = row;
			lastTapCol = col;
		}

		String[] gameChoices = getResources().getStringArray(R.array.game_type);

		try {
			//TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
			if (!getGameMode().equals(gameChoices[0])) {
				ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
				boolean wantMine = gameMode.equals(gameChoices[1]);
				//TODO: bug: when toggle flags is on + hard mode: this will always put a mine under the cell you just flagged
				boolean[][] newMineLocations = backtrackingSolver.getMineConfiguration(board, 0, row, col, wantMine);
				if (newMineLocations != null) {
					minesweeperGame.changeMineLocations(newMineLocations);
				}
			}

			minesweeperGame.clickCell(row, col, toggleFlagModeOn);
			if (!minesweeperGame.getIsGameLost() && !(toggleFlagModeOn && !minesweeperGame.getCell(row, col).getIsVisible())) {
				if (toggleBacktrackingHintsOn || toggleMineProbabilityOn) {
					updateSolvedBoardWithBacktrackingSolver();
				} else if (toggleGaussHintsOn) {
					updateSolvedBoardWithGaussSolver();
				}
			}
		} catch (Exception e) {
			displayStackTracePopup(e);
			e.printStackTrace();
		}

		updateNumberOfMines(minesweeperGame.getNumberOfMines() - minesweeperGame.getNumberOfFlags());
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		gameCanvas.invalidate();
	}

	public void updateSolvedBoardWithGaussSolver() throws Exception {
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		try {
			gaussSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
		} catch (Exception e) {
			displayStackTracePopup(e);
			e.printStackTrace();
		}
	}

	private void startNewGame() {
		try {
			minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
		} catch (Exception e) {
			e.printStackTrace();
		}
		enableSwitchesAndSetToFalse();
		try {
			ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (toggleFlagModeOn) {
			toggleFlagModeOn = false;
			Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
			toggleFlagMode.setText(mineEmoji);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
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
				updateSolvedBoardWithBacktrackingSolver();
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
			updateNumberOfSolverIterations(0);
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
				updateSolvedBoardWithGaussSolver();
			} catch (Exception e) {
				displayStackTracePopup(e);
				e.printStackTrace();
			}
		}
		gameCanvas.invalidate();
	}

	public void updateSolvedBoardWithBacktrackingSolver() throws Exception {
		//TODO: only run solver if board has changed since last time
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board);
		try {
			backtrackingSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
			updateNumberOfSolverIterations(backtrackingSolver.getNumberOfIterations());
		} catch (HitIterationLimitException e) {
			solverHitIterationLimit();
		}
	}

	private void handleHintToggle(boolean isChecked) {
		toggleBacktrackingHintsOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		if (isChecked) {
			try {
				updateSolvedBoardWithBacktrackingSolver();
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
					updateSolvedBoardWithGaussSolver();
				} catch (Exception e) {
					displayStackTracePopup(e);
					e.printStackTrace();
				}
			}
			updateNumberOfSolverIterations(0);
		}
		gameCanvas.invalidate();
	}

	//TODO: make all thrown places show this
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
		text += " iterations. Hints and mine probability features are currently not available.";
		textView.setText(text);
	}

	private void setUpStackTracePopup() {
		stackStacePopup = PopupHelper.initializePopup(this, R.layout.stack_trace_popup);
	}

	public void solverHitIterationLimit() {
		String[] gameChoices = getResources().getStringArray(R.array.game_type);
		//TODO: think about changing this behavior to just (temporarily) switching modes to back to normal mode
		if (!gameMode.equals(gameChoices[0])) {
			Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
			startActivity(intent);
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
		String minesLeft;
		if (numberOfMinesLeft < 10) {
			minesLeft = "00" + numberOfMinesLeft;
		} else if (numberOfMinesLeft < 100) {
			minesLeft = "0" + numberOfMinesLeft;
		} else {
			minesLeft = String.valueOf(numberOfMinesLeft);
		}
		TextView numberOfMines = findViewById(R.id.showNumberOfMines);
		numberOfMines.setText(minesLeft);
	}

	public void updateNumberOfSolverIterations(int numberOfIterations) {
		TextView iterationTextView = findViewById(R.id.numberOfIterationsTextView);
		final String iterationsText = "iterations: " + numberOfIterations;
		iterationTextView.setText(iterationsText);
	}

	public void disableSwitches() {
		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setClickable(false);

		Switch toggleMines = findViewById(R.id.toggleMines);
		toggleMines.setClickable(false);

		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setClickable(false);

		Switch toggleGaussHints = findViewById(R.id.toggleGaussHints);
		toggleGaussHints.setClickable(false);
	}

	public void enableSwitchesAndSetToFalse() {
		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setClickable(true);
		toggleHints.setChecked(false);

		Switch toggleMines = findViewById(R.id.toggleMines);
		toggleMines.setClickable(true);
		toggleMines.setChecked(false);

		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setClickable(true);
		toggleProbability.setChecked(false);

		Switch toggleGaussHints = findViewById(R.id.toggleGaussHints);
		toggleGaussHints.setClickable(true);
		toggleGaussHints.setChecked(false);
	}

	public void setNewGameButtonDeadFace() {
		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setBackgroundResource(R.drawable.dead_face);
	}

	public void setNewGameButtonWinFace() {
		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setBackgroundResource(R.drawable.win_face);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public MinesweeperGame getMinesweeperGame() {
		return minesweeperGame;
	}

	public int getNumberOfRows() {
		return numberOfRows;
	}

	public int getNumberOfCols() {
		return numberOfCols;
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

	public MinesweeperSolver.VisibleTile[][] getBoard() {
		return board;
	}

	public int getLastTapRow() {
		return lastTapRow;
	}

	public int getLastTapCol() {
		return lastTapCol;
	}
}
