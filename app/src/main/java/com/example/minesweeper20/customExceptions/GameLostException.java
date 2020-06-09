package com.example.minesweeper20.customExceptions;

public class GameLostException extends Exception {
	public GameLostException(String message) {
		super(message);
	}
}
