package com.LukeVideckis.minesweeper_android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.customExceptions.HitIterationLimitException;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.HolyGrailSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperGame;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MinesweeperSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.MyBacktrackingSolver;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.ConvertGameBoardFormat;
import com.LukeVideckis.minesweeper_android.minesweeperStuff.minesweeperHelpers.CreateSolvableBoard;
import com.LukeVideckis.minesweeper_android.miscHelpers.PopupHelper;
import com.LukeVideckis.minesweeper_android.view.GameCanvas;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
	public final static String
			flagEmoji = new String(Character.toChars(0x1F6A9)),
			mineEmoji = new String(Character.toChars(0x1F4A3));
	public static final int cellPixelLength = 150;
	private static final long millisecondsBeforeDisplayingLoadingScreen = 100;

	private boolean
			toggleFlagModeOn = false,
			toggleBacktrackingHintsOn = false,
			toggleMineProbabilityOn = false;
	private int numberOfRows, numberOfCols, numberOfMines, gameMode;
	private PopupWindow solverHitLimitPopup, couldNotFindNoGuessBoardPopup;

	private MinesweeperGame minesweeperGame;
	private MinesweeperSolver holyGrailSolver;
	private MinesweeperSolver.VisibleTile[][] board;
	private int lastTapRow, lastTapCol;
	private Thread updateTimeThread;
	private AlertDialog loadingScreenForSolvableBoardGeneration;
	private CreateSolvableBoard createSolvableBoard;
	private Thread createSolvableBoardThread;

	public void stopTimerThread() {
		updateTimeThread.interrupt();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_ROWS, 1);
		numberOfCols = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_COLS, 1);
		numberOfMines = getIntent().getIntExtra(StartScreenActivity.NUMBER_OF_MINES, 1);
		//default game mode is normal mode
		gameMode = getIntent().getIntExtra(StartScreenActivity.GAME_MODE, R.id.normal_mode);
		setContentView(R.layout.game);

		createSolvableBoard = new CreateSolvableBoard(numberOfRows, numberOfCols, numberOfMines);

		try {
			minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
		} catch (Exception e) {
			e.printStackTrace();
		}
		holyGrailSolver = new HolyGrailSolver(numberOfRows, numberOfCols);
		board = ConvertGameBoardFormat.convertToNewBoard(minesweeperGame);

		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setOnClickListener(this);
		Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
		toggleFlagMode.setOnClickListener(this);
		toggleFlagMode.setText(mineEmoji);

		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setOnCheckedChangeListener(this);
		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setOnCheckedChangeListener(this);

		updateNumberOfMines(numberOfMines);
		setUpIterationLimitPopup();
		setUpNoGuessBoardPopup();

		updateTimeThread = new TimeUpdateThread();


		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setOnKeyListener((dialog, keyCode, event) -> {
					if (keyCode == KeyEvent.KEYCODE_BACK &&
							event.getAction() == KeyEvent.ACTION_UP &&
							!event.isCanceled()) {
						dialog.cancel();
						createSolvableBoardThread.interrupt();
						onBackPressed();
						return true;
					}
					return false;
				});

		builder.setCancelable(false);
		builder.setView(R.layout.layout_loading_dialog);
		loadingScreenForSolvableBoardGeneration = builder.create();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.newGameButton:
				ImageButton newGameButton = findViewById(R.id.newGameButton);
				newGameButton.setImageResource(R.drawable.smiley_face);
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
		if (tapX < 0f ||
				tapY < 0f ||
				tapX > numberOfCols * cellPixelLength ||
				tapY > numberOfRows * cellPixelLength) {
			return;
		}
		final int row = (int) (tapY / cellPixelLength);
		final int col = (int) (tapX / cellPixelLength);

		if (minesweeperGame.isBeforeFirstClick() && !toggleFlagModeOn) {
			//TODO: start timer after board generation is complete
			if (gameMode == R.id.no_guessing_mode || gameMode == R.id.noGuessingModeWithAn8) {
				//TODO: either break out of board gen (after like 2 seconds), or improve board gen to not take forever sometimes
				AtomicBoolean finishedBoardGen = new AtomicBoolean(false);
				new Thread() {
					@Override
					public void run() {
						try {
							sleep(millisecondsBeforeDisplayingLoadingScreen);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (!finishedBoardGen.get()) {
							runOnUiThread(() -> loadingScreenForSolvableBoardGeneration.show());
						} else {
							findViewById(R.id.gridCanvas).invalidate();
						}
					}
				}.start();
				createSolvableBoardThread = new Thread() {
					private AtomicBoolean isInterrupted = new AtomicBoolean(false);

					@Override
					public void run() {
						try {
							minesweeperGame = createSolvableBoard.getSolvableBoard(row, col, gameMode == R.id.noGuessingModeWithAn8, isInterrupted);
							if (isInterrupted.get()) {
								return;
							}
							finishedBoardGen.set(true);
							updateTimeThread.start();
							runOnUiThread(() -> loadingScreenForSolvableBoardGeneration.dismiss());
						} catch (Exception ignored) {
							if (isInterrupted.get()) {
								return;
							}
							finishedBoardGen.set(true);
							updateTimeThread.start();
							try {
								minesweeperGame.clickCell(row, col, false);
							} catch (Exception e) {
								e.printStackTrace();
							}
							runOnUiThread(() -> {
								displayNoGuessBoardPopup();
								loadingScreenForSolvableBoardGeneration.dismiss();
							});
						}
					}

					@Override
					public void interrupt() {
						super.interrupt();
						isInterrupted.set(true);
					}
				};
				createSolvableBoardThread.start();
				return;
			}
			updateTimeThread.start();
		}

		if (!minesweeperGame.getIsGameLost()) {
			lastTapRow = row;
			lastTapCol = col;
		}

		try {
			//TODO: bug here: when you click a visible cell which results in revealing extra cells in easy/hard mode - make sure you win/lose
			//TODO: don't change mine configuration when the current config matches what you want
			minesweeperGame.clickCell(row, col, toggleFlagModeOn);
			if (!minesweeperGame.getIsGameLost() && !(toggleFlagModeOn && !minesweeperGame.getCell(row, col).getIsVisible())) {
				if (toggleBacktrackingHintsOn || toggleMineProbabilityOn) {
					updateSolvedBoardWithBacktrackingSolver();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		updateNumberOfMines(minesweeperGame.getNumberOfMines() - minesweeperGame.getNumberOfFlags());
		findViewById(R.id.gridCanvas).invalidate();
	}

	private void startNewGame() {
		try {
			minesweeperGame = new MinesweeperGame(numberOfRows, numberOfCols, numberOfMines);
		} catch (Exception e) {
			e.printStackTrace();
		}
		enableButtonsAndSwitchesAndSetToFalse();

		if (toggleFlagModeOn) {
			toggleFlagModeOn = false;
			Button toggleFlagMode = findViewById(R.id.toggleFlagMode);
			toggleFlagMode.setText(mineEmoji);
		}

		stopTimerThread();
		updateTimeThread = new TimeUpdateThread();
		updateTime(0);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.toggleBacktrackingHints:
				handleHintToggle(isChecked);
				break;
			case R.id.toggleMineProbability:
				handleToggleMineProbability(isChecked);
				break;
		}
	}

	private void handleToggleMineProbability(boolean isChecked) {
		toggleMineProbabilityOn = isChecked;
		if (isChecked) {
			//TODO: don't update if hints is already enabled, it will do nothing
			try {
				updateSolvedBoardWithBacktrackingSolver();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		findViewById(R.id.gridCanvas).invalidate();
	}

	public void updateSolvedBoardWithBacktrackingSolver() throws Exception {
		//TODO: only run solver if board has changed since last time
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, false);
		try {
			holyGrailSolver.solvePosition(board, minesweeperGame.getNumberOfMines());
		} catch (HitIterationLimitException e) {
			solverHitIterationLimit();
		}
		minesweeperGame.updateLogicalStuff(board);
	}

	private void handleHintToggle(boolean isChecked) {
		toggleBacktrackingHintsOn = isChecked;
		GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
		if (isChecked) {
			try {
				updateSolvedBoardWithBacktrackingSolver();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		gameCanvas.invalidate();
	}

	private void setUpIterationLimitPopup() {
		solverHitLimitPopup = PopupHelper.initializePopup(this, R.layout.solver_hit_limit_popup);
		Button okButton = solverHitLimitPopup.getContentView().findViewById(R.id.solverHitLimitOkButton);
		okButton.setOnClickListener(view -> solverHitLimitPopup.dismiss());
		TextView textView = solverHitLimitPopup.getContentView().findViewById(R.id.iterationLimitText);
		String text = "Solver took more than ";
		text += NumberFormat.getNumberInstance(Locale.US).format(MyBacktrackingSolver.iterationLimit);
		text += " iterations. Hints and mine probability are currently not available.";
		textView.setText(text);
	}

	private void setUpNoGuessBoardPopup() {
		couldNotFindNoGuessBoardPopup = PopupHelper.initializePopup(this, R.layout.couldnt_find_no_guess_board_popup);
		Button okButton = couldNotFindNoGuessBoardPopup.getContentView().findViewById(R.id.noGuessBoardOkButton);
		okButton.setOnClickListener(view -> couldNotFindNoGuessBoardPopup.dismiss());
	}

	private void displayNoGuessBoardPopup() {
		PopupHelper.displayPopup(couldNotFindNoGuessBoardPopup, findViewById(R.id.gameLayout), getResources());
	}

	public void solverHitIterationLimit() {
		//TODO: think about changing this behavior to just (temporarily) switching modes to back to normal mode
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

	private void updateTime(int newTime) {
		String currTime;
		if (newTime < 10) {
			currTime = "00" + newTime;
		} else if (newTime < 100) {
			currTime = "0" + newTime;
		} else {
			currTime = String.valueOf(newTime);
		}
		TextView timeText = findViewById(R.id.timeTextView);
		timeText.setText(currTime);
	}

	public void disableSwitchesAndButtons() {
		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setClickable(false);

		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setClickable(false);

		Button flagModeButton = findViewById(R.id.toggleFlagMode);
		flagModeButton.setClickable(false);
	}

	public void enableButtonsAndSwitchesAndSetToFalse() {
		Switch toggleHints = findViewById(R.id.toggleBacktrackingHints);
		toggleHints.setClickable(true);
		toggleHints.setChecked(false);

		Switch toggleProbability = findViewById(R.id.toggleMineProbability);
		toggleProbability.setClickable(true);
		toggleProbability.setChecked(false);

		Button flagModeButton = findViewById(R.id.toggleFlagMode);
		flagModeButton.setClickable(true);
		flagModeButton.setText(mineEmoji);
		toggleFlagModeOn = false;
	}

	public void setNewGameButtonDeadFace() {
		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setImageResource(R.drawable.dead_face);
	}

	public void setNewGameButtonWinFace() {
		ImageButton newGameButton = findViewById(R.id.newGameButton);
		newGameButton.setImageResource(R.drawable.win_face);
	}

	@Override
	public void onBackPressed() {
		stopTimerThread();
		Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
		startActivity(intent);
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

	public boolean getToggleBacktrackingHintsOn() {
		return toggleBacktrackingHintsOn;
	}

	public boolean getToggleMineProbabilityOn() {
		return toggleMineProbabilityOn;
	}

	public MinesweeperSolver.VisibleTile[][] getBoard() throws Exception {
		ConvertGameBoardFormat.convertToExistingBoard(minesweeperGame, board, (toggleBacktrackingHintsOn || toggleMineProbabilityOn));
		return board;
	}

	public int getLastTapRow() {
		return lastTapRow;
	}

	public int getLastTapCol() {
		return lastTapCol;
	}

	private class TimeUpdateThread extends Thread {
		@Override
		public void run() {
			try {
				synchronized (this) {
					AtomicInteger time = new AtomicInteger(-1);
					while (time.incrementAndGet() <= 999) {
						runOnUiThread(() -> updateTime(time.get()));
						wait(1000);
					}
				}
			} catch (InterruptedException ignored) {
			}
		}
	}
}
