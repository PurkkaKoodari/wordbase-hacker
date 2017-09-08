package net.pietu1998.wordbasehacker.solver;

import android.support.annotation.NonNull;

public class Possibility {

	@NonNull
	private byte[] coordinates;
	@NonNull
	private String word;
	private Score score;
	private Board result;

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

	@NonNull
	public Board getResult() {
		return result;
	}

	public void setResult(Board result) {
		this.result = result;
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
