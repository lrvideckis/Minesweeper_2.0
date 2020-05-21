package com.example.minesweeper20;

public class HitIterationLimitException extends Exception {
	public HitIterationLimitException() {
		super("too many iterations");
	}
}
