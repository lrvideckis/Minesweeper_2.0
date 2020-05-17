package com.example.minesweeper20.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minesweeper20.R;
import com.example.minesweeper20.view.GameCanvas;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private Boolean toggleBombsOn, toggleHintsOn;
	private Integer numberOfRows, numberOfCols, numberOfBombs;
	private String gameMode;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 1);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 1);
		numberOfBombs = getIntent().getIntExtra("numberOfBombs", 1);
		gameMode = getIntent().getStringExtra("gameMode");
		toggleBombsOn = toggleHintsOn = false;
		setContentView(R.layout.game);
		Button backToStartScreen = findViewById(R.id.backToStartScreen);
		backToStartScreen.setOnClickListener(this);

		Switch toggleBombs = findViewById(R.id.toggleBombs);
		toggleBombs.setOnCheckedChangeListener(this);

		Switch toggleHints = findViewById(R.id.toggleHints);
		toggleHints.setOnCheckedChangeListener(this);

		updateNumberOfBombs(numberOfBombs);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
		startActivity(intent);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.toggleBombs:
				toggleBombsOn = isChecked;
				break;
			case R.id.toggleHints:
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
		}
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
