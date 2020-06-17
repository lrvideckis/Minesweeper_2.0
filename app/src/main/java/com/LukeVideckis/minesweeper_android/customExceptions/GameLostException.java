package com.LukeVideckis.minesweeper_android.customExceptions;

public class GameLostException extends Exception {
	public GameLostException(String message) {
		super(message);
	}
}
