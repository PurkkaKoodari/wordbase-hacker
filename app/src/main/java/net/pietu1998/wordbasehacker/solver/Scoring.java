package net.pietu1998.wordbasehacker.solver;

public class Scoring {

	public final int letter, mine, tileGain, tileKill, progressGain, progressKill;
	public final boolean winBonus;

	public Scoring(int letter, int mine, int tileGain, int tileKill, int progressGain, int progressKill, boolean winBonus) {
		this.letter = letter;
		this.mine = mine;
		this.tileGain = tileGain;
		this.tileKill = tileKill;
		this.progressGain = progressGain;
		this.progressKill = progressKill;
		this.winBonus = winBonus;
	}

	public static final Scoring DEFAULT = new Scoring(1, 0, 10, 40, 100, 300, true);

	public int calculateScore(Score score) {
		int result = 0;
		result += score.wordLength * letter;
		result += score.minesExploded * mine;
		result += score.tilesGained * tileGain;
		result += score.tilesKilled * tileKill;
		result += score.progressGained * progressGain;
		result += score.progressKilled * progressKill;
		if (winBonus && score.gameWon)
			result += 1000000000;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Scoring))
			return false;
		Scoring other = (Scoring) o;
		return letter == other.letter && mine == other.mine && tileGain == other.tileGain && tileKill == other.tileKill
				&& progressGain == other.progressGain && progressKill == other.progressKill
				&& winBonus == other.winBonus;
	}

}
