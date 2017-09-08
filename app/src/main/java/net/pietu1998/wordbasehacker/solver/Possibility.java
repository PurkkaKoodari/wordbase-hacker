package net.pietu1998.wordbasehacker.solver;

public class Possibility {

	private Coordinate[] coordinates;
	private Score score;
	private Board result;
	private char[] word;

	public Possibility(Coordinate[] coordinates, char[] word) {
		this.coordinates = coordinates;
		this.word = word;
	}

	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public Board getResult() {
		return result;
	}

	public void setResult(Board result) {
		this.result = result;
	}

	public Coordinate[] getCoordinates() {
		return coordinates;
	}

	public char[] getWord() {
		return word;
	}

}
