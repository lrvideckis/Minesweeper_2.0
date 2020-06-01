package com.example.minesweeper20.customExceptions;

public class HitIterationLimitException extends Exception {
	public HitIterationLimitException() {
		super("too many iterations");
	}
}
