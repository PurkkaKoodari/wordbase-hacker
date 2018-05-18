package net.pietu1998.wordbasehacker.solver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.pietu1998.wordbasehacker.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("JniMissingFunction")
public class NativeSolver {

    static {
        System.loadLibrary("solver");
    }

    private NativeSolver() {
    }

    private static volatile boolean busy = false;

    public static boolean isBusy() {
        return busy;
    }

    private static native void setScoring0(
            int wordLength,
            int gainedTiles, int killedTiles,
            int gainedDist, int killedDist,
            int winBonus, int loseMinus);

    private static native void setParams0(
            int maxDepth, int maxBreadth, int maxSteps,
            int speedupFactor);

    private static native boolean initBoard0(char[] board);

    private static native boolean addWord0(String word);

    private static native boolean removeWord0(String word);

    private static native int solve0(
            boolean playerOrange, boolean orangeTurn, byte[] cells,
            byte[] positionsOut, char[] wordOut, int[] scoreOut);

    private static final byte EMPTY = 1;
    private static final byte ORANGE = 2;
    private static final byte BLUE = 3;
    private static final byte MINE = 4;
    private static final byte SUPER_MINE = 5;

    private static void applyMove(boolean playerOrange, byte cells[], List<Tile> layout) {
        for (Tile tile : layout) {
            byte cell;
            if ((tile.type & Tile.SUPER_MINE) != 0)
                cell = SUPER_MINE;
            else if ((tile.type & Tile.MINE) != 0)
                cell = MINE;
            else if ((tile.type & Tile.PLAYER) != 0)
                cell = playerOrange ? ORANGE : BLUE;
            else if ((tile.type & Tile.OPPONENT) != 0)
                cell = playerOrange ? BLUE : ORANGE;
            else
                cell = EMPTY;
            cells[tile.x + 10 * tile.y] = cell;
        }
    }

    private static void applyMoveToOutput(int outCells[], List<Tile> layout) {
        for (Tile tile : layout) {
            outCells[tile.x + 10 * tile.y] = tile.type;
        }
    }

    public static synchronized List<Possibility> solve(
            StatusCallback status, Scoring scoring, Params params,
            Game game, char[] board, String[] words, List<Tile> layout, List<Move> played) {
        busy = true;

        status.update(R.string.setting_params);
        setScoring0(scoring.letter, scoring.tileGain, scoring.tileKill,
                scoring.progressGain, scoring.progressKill, scoring.winBonus, scoring.loseMinus);
        setParams0(params.maxDepth, params.maxBreadth, params.maxSteps, params.speedupFactor);

        status.update(R.string.analyzing_words);
        if (!initBoard0(board))
            throw new RuntimeException("failed to init board");
        for (String word : words) {
            if (!addWord0(word))
                throw new RuntimeException("failed to add word");
        }

        boolean playerOrange = !game.isFlipped();

        status.update(R.string.applying_moves);
        byte[] cells = new byte[130];
        int[] outCells = new int[130];
        applyMove(playerOrange, cells, layout);
        applyMoveToOutput(outCells, layout);
        for (Move move : played) {
            applyMove(playerOrange, cells, move.getLayout());
            applyMoveToOutput(outCells, move.getLayout());
            if (!removeWord0(move.getWord()))
                throw new RuntimeException("failed to remove word");
        }

        char[] solutionChars = new char[12];
        byte[] solutionPos = new byte[24];
        int[] solutionScore = new int[1];

        status.update(R.string.finding_words);
        int solutionLength = solve0(playerOrange, playerOrange, cells,
                solutionPos, solutionChars, solutionScore);

        List<Tile> solutionLayout = new ArrayList<>();
        for (int i = 0; i < solutionLength * 2; i += 2)
            solutionLayout.add(new Tile(solutionPos[i], solutionPos[i + 1], Tile.PLAYER));
        applyMoveToOutput(outCells, solutionLayout);

        busy = false;

        String word = new String(solutionChars, 0, solutionLength);
        Possibility result = new Possibility(Arrays.copyOf(solutionPos, solutionLength * 2), word);
        result.setResult(outCells);
        result.setScore(solutionScore[0]);
        return Collections.singletonList(result);
    }

    public static class Params {
        public final int maxDepth;
        public final int maxBreadth;
        public final int maxSteps;
        public final int speedupFactor;

        public static final Params DEFAULT = new Params(6, 10, 15000, 245);

        public Params(int maxDepth, int maxBreadth, int maxSteps, int speedupFactor) {
            this.maxDepth = maxDepth;
            this.maxBreadth = maxBreadth;
            this.maxSteps = maxSteps;
            this.speedupFactor = speedupFactor;
        }

        public static Params fromPreferences(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int maxDepth = prefs.getInt(context.getString(R.string.pref_key_native_max_depth), DEFAULT.maxDepth);
            int maxBreadth = prefs.getInt(context.getString(R.string.pref_key_native_max_breadth), DEFAULT.maxBreadth);
            int maxSteps = prefs.getInt(context.getString(R.string.pref_key_native_max_steps), DEFAULT.maxSteps);
            int speedupFactor = prefs.getInt(context.getString(R.string.pref_key_native_speedup_factor), DEFAULT.speedupFactor);
            return new Params(maxDepth, maxBreadth, maxSteps, speedupFactor);
        }

        public void saveToPreferences(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit()
                    .putInt(context.getString(R.string.pref_key_native_max_depth), maxDepth)
                    .putInt(context.getString(R.string.pref_key_native_max_breadth), maxBreadth)
                    .putInt(context.getString(R.string.pref_key_native_max_steps), maxSteps)
                    .putInt(context.getString(R.string.pref_key_native_speedup_factor), speedupFactor)
                    .apply();
        }
    }

}
