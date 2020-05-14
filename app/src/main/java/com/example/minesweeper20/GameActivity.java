package com.example.minesweeper20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private Boolean toggleBombsOn;
	private Integer numberOfRows, numberOfCols, numberOfBombs;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		numberOfRows = getIntent().getIntExtra("numberOfRows", 5);
		numberOfCols = getIntent().getIntExtra("numberOfCols", 5);
		numberOfBombs = getIntent().getIntExtra("numberOfBombs", 5);
		toggleBombsOn = false;
		setContentView(R.layout.game);
		Button backToStartScreen = findViewById(R.id.backToStartScreen);
		backToStartScreen.setOnClickListener(this);

		Switch toggleBombs = findViewById(R.id.toggleBombs);
		toggleBombs.setOnCheckedChangeListener(this);
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

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(GameActivity.this, StartScreenActivity.class);
		startActivity(intent);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		toggleBombsOn = isChecked;
	}

	public Boolean getToggleBombsOn() {
		return toggleBombsOn;
	}

}
