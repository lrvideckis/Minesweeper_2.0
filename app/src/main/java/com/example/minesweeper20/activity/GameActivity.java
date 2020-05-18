package com.example.minesweeper20.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.view.GameCanvas;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private Boolean toggleFlagModeOn, toggleHintsOn, toggleBombsOn, solverHasFailed;
	private Integer numberOfRows, numberOfCols, numberOfBombs;
	private String gameMode;
	private PopupWindow popupWindow;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 1);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 1);
		numberOfBombs = getIntent().getIntExtra("numberOfBombs", 1);
		solverHasFailed = false;
		gameMode = getIntent().getStringExtra("gameMode");
		toggleBombsOn = toggleFlagModeOn = toggleHintsOn = false;
		setContentView(R.layout.game);
		Button backToStartScreen = findViewById(R.id.backToStartScreen);
		backToStartScreen.setOnClickListener(this);

		Switch toggleFlagMode = findViewById(R.id.toggleFlagMode);
		toggleFlagMode.setOnCheckedChangeListener(this);

		Switch toggleHints = findViewById(R.id.toggleHints);
		toggleHints.setOnCheckedChangeListener(this);

		Switch toggleBombs = findViewById(R.id.toggleBombs);
		toggleBombs.setOnCheckedChangeListener(this);

		updateNumberOfBombs(numberOfBombs);
		setUpIterationLimitPopup();
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
			case R.id.toggleHints:
				if(solverHasFailed) {
					buttonView.setChecked(false);
					popupWindow.showAtLocation(findViewById(R.id.gameLayout), Gravity.CENTER,0,0);
					break;
				}
				toggleHintsOn = isChecked;
				if(isChecked) {
					GameCanvas gameCanvas = findViewById(R.id.gridCanvas);
					try {
						gameCanvas.updateSolvedBoard();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			case R.id.toggleBombs:
				toggleBombsOn = isChecked;
				break;
		}
	}

	private void setUpIterationLimitPopup() {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		@SuppressLint("InflateParams") final View solverHitLimitPopup = inflater.inflate(R.layout.solver_hit_limit_popup,null);
		popupWindow = new PopupWindow(
				solverHitLimitPopup,
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		popupWindow.setFocusable(true);
		popupWindow.setElevation(5.0f);
		Button okButton = solverHitLimitPopup.findViewById(R.id.solverHitLimitOkButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				popupWindow.dismiss();
			}
		});
	}

	public void solverHasJustFailed() {
		String[] gameChoices = getResources().getStringArray(R.array.game_type);
		if(!gameMode.equals(gameChoices[0])) {
			onClick(findViewById(R.id.gameLayout));
			return;
		}
		solverHasFailed = true;
		Switch toggleHints = findViewById(R.id.toggleHints);
		toggleHints.setChecked(false);
		toggleHintsOn = false;
		popupWindow.showAtLocation(findViewById(R.id.gameLayout), Gravity.CENTER,0,0);
	}

	public void updateNumberOfBombs(int numberOfBombsLeft) {
		TextView numberOfBombs = findViewById(R.id.showNumberOfBombs);
		final String bombsLeft = "bombs: " + numberOfBombsLeft;
		numberOfBombs.setText(bombsLeft);
	}

	public Integer getNumberOfRows() {
		return numberOfRows;
	}

	public Integer getNumberOfCols() {
		return numberOfCols;
	}

	public Integer getNumberOfBombs() {
		return numberOfBombs;
	}

	public Boolean getToggleFlagModeOn() {
		return toggleFlagModeOn;
	}

	public Boolean getToggleBombsOn() {
		return toggleBombsOn;
	}

	public Boolean getToggleHintsOn() {
		return toggleHintsOn;
	}

	public String getGameMode() {
		return gameMode;
	}
}
