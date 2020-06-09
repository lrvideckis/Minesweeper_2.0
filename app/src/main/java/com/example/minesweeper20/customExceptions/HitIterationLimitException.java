package com.example.minesweeper20.customExceptions;

public class HitIterationLimitException extends Exception {
	public HitIterationLimitException(String message) {
		super(message);
	}
}
