package net.pietu1998.wordbasehacker.solver;

import android.support.annotation.NonNull;

public class Possibility {

	@NonNull
	private byte[] coordinates;
	@NonNull
	private String word;
	private Score score;
	private int[] result = new int[130];
	private char[] tileLetters;

	public Possibility(@NonNull byte[] coordinates, @NonNull String word) {
		this.coordinates = coordinates;
		this.word = word;
	}

	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public int[] getResult() {
		return result;
	}

	public void setResult(int[] result) {
		System.arraycopy(result, 0, this.result, 0, 130);
	}

	public char[] getTileLetters() {
		return tileLetters;
	}

	public void setTileLetters(char[] tileLetters) {
		this.tileLetters = tileLetters;
	}

	@NonNull
	public byte[] getCoordinates() {
		return coordinates;
	}

	@NonNull
	public String getWord() {
		return word;
	}

}
