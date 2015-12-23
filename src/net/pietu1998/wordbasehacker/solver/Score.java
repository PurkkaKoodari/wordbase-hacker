package net.pietu1998.wordbasehacker.solver;

public class Score {

	public final int wordLength, tilesGained, tilesKilled, progressGained, progressKilled;
	public final boolean gameWon;

	public Score(int wordLength, int tilesGained, int tilesKilled, int progressGained, int progressKilled, boolean gameWon) {
		this.wordLength = wordLength;
		this.tilesGained = tilesGained;
		this.tilesKilled = tilesKilled;
		this.progressGained = progressGained;
		this.progressKilled = progressKilled;
		this.gameWon = gameWon;
	}

}
