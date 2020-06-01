package com.example.minesweeper20.customExceptions;

public class NoSolutionFoundException extends Exception {
	public NoSolutionFoundException(String message) {
		super(message);
	}
}
