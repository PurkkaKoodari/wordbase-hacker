package net.pietu1998.wordbasehacker;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.pietu1998.wordbasehacker.solver.Game;
import net.pietu1998.wordbasehacker.solver.LegacySolver;
import net.pietu1998.wordbasehacker.solver.Move;
import net.pietu1998.wordbasehacker.solver.NativeSolver;
import net.pietu1998.wordbasehacker.solver.Possibility;
import net.pietu1998.wordbasehacker.solver.Scoring;
import net.pietu1998.wordbasehacker.solver.Tile;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BoardActivity extends AppCompatActivity {

	private Scoring scoring = Scoring.DEFAULT;
	private NativeSolver.Params nativeParams = NativeSolver.Params.DEFAULT;
	private Game game;
	private Possibility bestPossibility = null;
	private char[] tileLetters = null;
	private boolean loaded = false, playing = false;
	private boolean useNative;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_board);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BoardActivity.this);
        useNative = prefs.getBoolean(getString(R.string.pref_key_native), false);

        loadScoring();
		Button scoring = findViewById(R.id.scoring_btn);
		scoring.setOnClickListener(v -> scoringDialog());
		Button params = findViewById(R.id.params_btn);
		params.setOnClickListener(v -> nativeParamsDialog());
		params.setVisibility(useNative ? View.VISIBLE : View.GONE);
		Button play = findViewById(R.id.play_btn);
		play.setOnClickListener(view -> playWord());
		Parcelable extra = getIntent().getParcelableExtra("game");
		if (!(extra instanceof Game)) {
			Toast.makeText(this, R.string.internal_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		game = (Game) extra;
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(getString(R.string.title_activity_board, game.getOpponent()));
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		if (savedInstanceState != null) {
            loaded = savedInstanceState.getBoolean("loaded");
            tileLetters = savedInstanceState.getCharArray("tileLetters");
            String bestWord = savedInstanceState.getString("bestWord");
            byte[] bestPositions = savedInstanceState.getByteArray("bestPositions");
            int[] bestResult = savedInstanceState.getIntArray("bestResult");
            int bestScore = savedInstanceState.getInt("bestScore");
            if (bestWord != null && bestPositions != null && bestResult != null) {
                Possibility pos = new Possibility(bestPositions, bestWord);
                pos.setScore(bestScore);
                pos.setResult(bestResult);
                bestPossibility = pos;
                updateView();
            }
        }
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (playing) {
			((HackerApplication) getApplication()).hudOperationDone();
			playing = false;
		}
		if (!loaded) {
			loaded = true;
            if (useNative && NativeSolver.isBusy()) {
                new AlertDialog.Builder(BoardActivity.this).setMessage(R.string.solver_busy)
                        .setNeutralButton(R.string.ok, (dialog, which) -> finish()).show();
            } else {
                LoadTask task = new LoadTask();
                task.execute();
            }
		}
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
	    outState.putBoolean("loaded", loaded);
	    if (bestPossibility != null) {
	        outState.putString("bestWord", bestPossibility.getWord());
	        outState.putByteArray("bestPositions", bestPossibility.getCoordinates());
	        outState.putIntArray("bestResult", bestPossibility.getResult());
	        outState.putInt("bestScore", bestPossibility.getScore());
	        outState.putCharArray("tileLetters", tileLetters);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.boardmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
            case R.id.action_wordbase:
            	playWord();
                return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void playWord() {
		if (HudUtils.isHudEnabled(this)) {
			Toast.makeText(BoardActivity.this, R.string.play_no_hud_info, Toast.LENGTH_LONG).show();
			playWordWithoutInfo();
		} else {
			HudUtils.showHudInfo(this, R.string.pref_key_hide_play_info, R.string.play_hud_info, this::playWordWithoutInfo);
		}
	}

	private void playWordWithoutInfo() {
		playing = true;
		Intent intent = new Intent();
		intent.setComponent(new ComponentName("com.wordbaseapp", "com.wordbaseapp.BoardActivity"));
		intent.putExtra("game_id", (long) game.getId());
		startActivity(intent);
		if (bestPossibility != null)
			((HackerApplication) getApplication()).showSuggestedPath(bestPossibility.getCoordinates());
	}

	private class LoadTask extends AsyncTask<Void, Integer, Integer> {

	    private List<Tile> parseLayout(String json) throws ParseException {
            JSONArray jsonLayout = (JSONArray) new JSONParser().parse(json);
            List<Tile> layout = new ArrayList<>();
            for (Object o : jsonLayout) {
                JSONArray tile = (JSONArray) o;
                int flags = 0;
                Object type = tile.get(0);
                if ("Base".equals(type))
                    flags |= Tile.BASE;
                else if ("Mine".equals(type))
                    flags |= Tile.MINE;
                else if ("SuperMine".equals(type))
                    flags |= Tile.SUPER_MINE;
                Object owner = tile.get(3);
                if ("player".equals(owner))
                    flags |= Tile.PLAYER;
                else if ("opponent".equals(owner))
                    flags |= Tile.OPPONENT;
                int x = Integer.parseInt((String) tile.get(1));
                int y = Integer.parseInt((String) tile.get(2));
                layout.add(new Tile(x, y, flags));
            }
            return layout;
        }

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				publishProgress(R.string.loading_board);

				File cacheDb = new File(getCacheDir(), "wordbase.db");
				SQLiteDatabase db = SQLiteDatabase.openDatabase(cacheDb.getPath(), null, SQLiteDatabase.OPEN_READONLY);

				Cursor boardCursor = db.query("boards", new String[] { "rows", "words" }, "_id=" + game.getBoardId(),
						null, null, null, null);
				if (!boardCursor.moveToFirst()) {
					boardCursor.close();
					db.close();
					return R.string.no_board_found;
				}
				String[] rows = boardCursor.getString(0).replaceAll("[\\[\\]\\s]", "").split(",");
				String[] words = boardCursor.getString(1).replaceAll("[\\[\\]\\s]", "").split(",");
				boardCursor.close();

                char board[] = new char[130];
                for (int i = 0, j = 0; j < rows.length; j++) {
                    for (int k = 0; k < rows[j].length(); i++, k++) {
                        board[i] = rows[j].charAt(k);
                    }
                }

                List<Tile> layout = parseLayout(game.getLayout());

				publishProgress(R.string.loading_moves);
                List<Move> moves = new ArrayList<>();
				Cursor movesCursor = db.query("moves", new String[] { "fields", "word" }, "game_id=" + game.getId(),
						null, null, null, "created_at ASC");
				while (movesCursor.moveToNext())
                    moves.add(new Move(parseLayout(movesCursor.getString(0)), movesCursor.getString(1)));
				movesCursor.close();

				db.close();

				tileLetters = board;
				List<Possibility> possibilities;
				if (useNative) {
                    possibilities = NativeSolver.solve(
                            this::publishProgress, scoring, nativeParams, game, board, words, layout, moves);
                } else {
				    possibilities = LegacySolver.solve(
				            this::publishProgress, scoring, game, board, words, layout, moves);
                }
                int bestScore = Integer.MIN_VALUE;
                Possibility best = null;
                for (Possibility pos : possibilities) {
                    int score = pos.getScore();
                    if (score > bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
                bestPossibility = best;
				return 0;
			} catch (NumberFormatException | ParseException | SQLiteException | IndexOutOfBoundsException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			} catch (Exception e) {
			    Log.e("WordbaseHacker", "Error in solver.", e);
			    return R.string.internal_error;
            }
		}

        @Override
        protected void onPreExecute() {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            findViewById(R.id.result).setVisibility(View.GONE);
        }

        @Override
		protected void onProgressUpdate(Integer... values) {
            ((TextView) findViewById(R.id.status)).setText(values[0]);
		}

		@Override
		protected void onPostExecute(final Integer result) {
			findViewById(R.id.progress).setVisibility(View.GONE);
			findViewById(R.id.result).setVisibility(View.VISIBLE);
			if (result != 0) {
				new AlertDialog.Builder(BoardActivity.this).setMessage(result)
						.setNeutralButton(R.string.ok, (dialog, which) -> {
                            finish();
                            if (result == R.string.no_board_found) {
                                playWordWithoutInfo();
}
                        }).show();
			} else {
				updateView();
			}
		}

	}

	private void updateView() {
	    TextView result = findViewById(R.id.result);
	    ImageView boardArea = findViewById(R.id.move_picture);
		if (bestPossibility == null) {
			result.setText(R.string.no_moves);
			boardArea.setImageDrawable(null);
		} else {
            boardArea.setImageDrawable(new BoardDrawable(bestPossibility, tileLetters, !useNative && game.isFlipped()));
            result.setText(getString(R.string.best_move, bestPossibility.getWord(), bestPossibility.getScore()));
        }
	}

	private void loadScoring() {
		try {
			DataInputStream is = new DataInputStream(openFileInput("scoring.dat"));
			int letter = is.readInt();
			int mine = is.readInt();
			int tileGain = is.readInt();
			int tileKill = is.readInt();
			int progressGain = is.readInt();
			int progressKill = is.readInt();
			int winBonus = is.readInt();
			int loseMinus = is.readInt();
			is.close();
			scoring = new Scoring(letter, mine, tileGain, tileKill, progressGain, progressKill, winBonus, loseMinus);
		} catch (FileNotFoundException e) {
			scoring = Scoring.DEFAULT;
			Log.i("WordbaseHacker", "No previous scoring found, defaulting");
			saveScoring();
		} catch (IOException e) {
			scoring = Scoring.DEFAULT;
			Log.e("WordbaseHacker", "Failed to load scoring, defaulting", e);
			saveScoring();
		}
	}

	private void saveScoring() {
		try {
			DataOutputStream os = new DataOutputStream(openFileOutput("scoring.dat", 0));
			os.writeInt(scoring.letter);
			os.writeInt(scoring.mine);
			os.writeInt(scoring.tileGain);
			os.writeInt(scoring.tileKill);
			os.writeInt(scoring.progressGain);
			os.writeInt(scoring.progressKill);
			os.writeInt(scoring.winBonus);
			os.writeInt(scoring.loseMinus);
			os.close();
		} catch (IOException e) {
			Log.e("WordbaseHacker", "Failed to save scoring", e);
		}
	}

	private void restartSolver() {
        loaded = true;
        bestPossibility = null;
        updateView();
        LoadTask task = new LoadTask();
        task.execute();
    }

	private void scoringDialog() {
		ScoringDialog dialog = new ScoringDialog(this, scoring, useNative);
		dialog.setNewScoringListener(newScoring -> {
		    scoring = newScoring;
		    saveScoring();
		    restartSolver();
        });
		dialog.show();
	}

	private void nativeParamsDialog() {
        NativeParamsDialog dialog = new NativeParamsDialog(this);
        dialog.setNewParamsListener(newParams -> {
            nativeParams = newParams;
            restartSolver();
        });
        dialog.show();
    }

}
