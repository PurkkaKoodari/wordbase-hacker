package net.pietu1998.wordbasehacker.solver;

import net.pietu1998.wordbasehacker.R;

import java.util.List;

public class LegacySolver {

    private LegacySolver() {
    }

    private static void applyMove(int[] tileStates, List<Tile> layout) {
        for (Tile tile : layout) {
            tileStates[tile.x + 10 * tile.y] = tile.type;
        }
    }

    public static List<Possibility> solve(
            StatusCallback status, Scoring scoring,
            Game game, char[] chars, String[] words, List<Tile> layout, List<Move> moves) {
        status.update(R.string.applying_moves);
        int[] tileStates = new int[130];
        applyMove(tileStates, layout);
        for (Move move : moves) {
            game.addWord(move.getWord());
            applyMove(tileStates, move.getLayout());
        }

        status.update(R.string.analyzing_words);
        Board board = new Board(chars, tileStates, words, game);

        status.update(R.string.finding_words);
        board.findWords();

        status.update(R.string.scoring_words);
        board.scoreWords(scoring, game.isFlipped());

        return board.getResults();
    }

}
