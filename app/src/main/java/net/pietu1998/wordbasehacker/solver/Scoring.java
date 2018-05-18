package net.pietu1998.wordbasehacker.solver;

public class Scoring {

    public final int letter, mine, tileGain, tileKill, progressGain, progressKill, winBonus, loseMinus;

    public Scoring(int letter, int mine,
                   int tileGain, int tileKill,
                   int progressGain, int progressKill,
                   int winBonus, int loseMinus) {
        this.letter = letter;
        this.mine = mine;
        this.tileGain = tileGain;
        this.tileKill = tileKill;
        this.progressGain = progressGain;
        this.progressKill = progressKill;
        this.winBonus = winBonus;
        this.loseMinus = loseMinus;
    }

    public static final Scoring DEFAULT = new Scoring(
            1,
            0,
            10,
            40,
            100,
            400,
            1000000,
            -1000000);

    public int calculateScore(Score score) {
        int result = 0;
        result += score.wordLength * letter;
        result += score.minesExploded * mine;
        result += score.tilesGained * tileGain;
        result += score.tilesKilled * tileKill;
        result += score.progressGained * progressGain;
        result += score.progressKilled * progressKill;
        if (score.gameWon)
            result += winBonus;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Scoring))
            return false;
        Scoring other = (Scoring) o;
        return letter == other.letter && mine == other.mine
                && tileGain == other.tileGain && tileKill == other.tileKill
                && progressGain == other.progressGain && progressKill == other.progressKill
                && winBonus == other.winBonus && loseMinus == other.loseMinus;
    }

}
