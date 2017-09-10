package net.pietu1998.wordbasehacker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.pietu1998.wordbasehacker.solver.Board;
import net.pietu1998.wordbasehacker.solver.Game;
import net.pietu1998.wordbasehacker.solver.Move;
import net.pietu1998.wordbasehacker.solver.Possibility;
import net.pietu1998.wordbasehacker.solver.Scoring;
import net.pietu1998.wordbasehacker.solver.Tile;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BoardActivity extends AppCompatActivity {

	private Scoring currentScoring, scoring = Scoring.DEFAULT;
	private boolean changingValues = false;
	private Game game;
	private List<Possibility> possibilities = new ArrayList<>();
	private char[] tileLetters = null;
	private boolean loaded = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_board);
		loadScoring();
		Button scoring = (Button) findViewById(R.id.scoring_btn);
		scoring.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scoringDialog();
			}
		});
		Button play = (Button) findViewById(R.id.play_btn);
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("com.wordbaseapp", "com.wordbaseapp.BoardActivity"));
				intent.putExtra("game_id", (long) game.getId());
				startActivity(intent);
			}
		});
		Parcelable extra = getIntent().getParcelableExtra("game");
		if (!(extra instanceof Game)) {
			Toast.makeText(this, R.string.internal_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		game = (Game) extra;
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(getResources().getString(R.string.title_activity_board, game.getOpponent()));
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		loaded = false;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (loaded)
			return;
		loaded = true;
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.show();
		LoadTask task = new LoadTask(dialog);
		task.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class LoadTask extends AsyncTask<Void, Integer, Integer> {

		private final ProgressDialog dialog;

		public LoadTask(ProgressDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				publishProgress(R.string.loading_board);

				File cacheDb = new File(getCacheDir(), "wordbase.db");
				SQLiteDatabase db = SQLiteDatabase.openDatabase(cacheDb.getPath(), null, SQLiteDatabase.OPEN_READONLY);

				List<Move> moves = new ArrayList<>();
				moves.add(new Move(game.getLayout(), ""));

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

				publishProgress(R.string.loading_moves);
				Cursor movesCursor = db.query("moves", new String[] { "fields", "word" }, "game_id=" + game.getId(),
						null, null, null, "created_at ASC");
				while (movesCursor.moveToNext())
					moves.add(new Move(movesCursor.getString(0), movesCursor.getString(1)));
				movesCursor.close();

				db.close();

				publishProgress(R.string.applying_moves);
				int[] tileStates = new int[130];
				for (Move move : moves) {
					JSONArray jsonLayout = (JSONArray) new JSONParser().parse(move.getLayout());
					game.addWord(move.getWord());
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
						tileStates[x + 10 * y] = flags;
					}
				}

				publishProgress(R.string.analyzing_words);
				Board board = new Board(rows, tileStates, words, game);

				publishProgress(R.string.finding_words);
				board.findWords();

				publishProgress(R.string.scoring_words);
				board.scoreWords(game.isFlipped());

				tileLetters = board.getTileLetters();
				possibilities = board.getResults();
				return 0;
			} catch (NumberFormatException | ParseException  | SQLiteException | IndexOutOfBoundsException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			dialog.setMessage(getResources().getString(values[0]));
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			if (result != 0) {
				new AlertDialog.Builder(BoardActivity.this).setMessage(result)
						.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						}).show();
			} else {
				updateView();
			}
		}

	}

	private void updateView() {
		int max = Integer.MIN_VALUE;
		Possibility best = null;
		for (Possibility pos : possibilities) {
			int score = scoring.calculateScore(pos.getScore());
			if (score > max) {
				max = score;
				best = pos;
			}
		}
		if (best == null) {
			((TextView) findViewById(R.id.status)).setText(R.string.no_moves);
			return;
		}
		((ImageView) findViewById(R.id.move_picture)).setImageDrawable(new BoardDrawable(best, tileLetters, game.isFlipped()));
		((TextView) findViewById(R.id.status)).setText(getResources().getString(R.string.best_move, best.getWord(), max));
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
			boolean winBonus = is.readBoolean();
			is.close();
			scoring = new Scoring(letter, mine, tileGain, tileKill, progressGain, progressKill, winBonus);
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
			os.writeBoolean(scoring.winBonus);
			os.close();
		} catch (IOException e) {
			Log.e("WordbaseHacker", "Failed to save scoring", e);
		}
	}

	private void scoringDialog() {
		currentScoring = scoring;
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		final View layout = View.inflate(this, R.layout.scoring_dialog, null);
		dialog.setView(layout);
		dialog.setTitle(R.string.scoring);
		dialog.setNegativeButton(R.string.cancel, null);
		dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				scoring = currentScoring;
				saveScoring();
				updateView();
			}
		});
		final AlertDialog shown = dialog.create();

		updateFields(layout);

		TextWatcher watcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateScoring(layout, shown);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		};
		((EditText) layout.findViewById(R.id.lettersBox)).addTextChangedListener(watcher);
		((EditText) layout.findViewById(R.id.minesBox)).addTextChangedListener(watcher);
		((EditText) layout.findViewById(R.id.tilesPlrBox)).addTextChangedListener(watcher);
		((EditText) layout.findViewById(R.id.tilesOppBox)).addTextChangedListener(watcher);
		((EditText) layout.findViewById(R.id.progressPlrBox)).addTextChangedListener(watcher);
		((EditText) layout.findViewById(R.id.progressOppBox)).addTextChangedListener(watcher);
		((CheckBox) layout.findViewById(R.id.winBox))
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						updateScoring(layout, shown);
					}
				});

		Button defaults = layout.findViewById(R.id.defaults);
		defaults.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changingValues = true;
				currentScoring = Scoring.DEFAULT;
				updateFields(layout);
				changingValues = false;
			}
		});

		shown.show();
	}

	private void updateFields(View layout) {
		((EditText) layout.findViewById(R.id.lettersBox)).setText(Integer.toString(currentScoring.letter));
		((EditText) layout.findViewById(R.id.minesBox)).setText(Integer.toString(currentScoring.mine));
		((EditText) layout.findViewById(R.id.tilesPlrBox)).setText(Integer.toString(currentScoring.tileGain));
		((EditText) layout.findViewById(R.id.tilesOppBox)).setText(Integer.toString(currentScoring.tileKill));
		((EditText) layout.findViewById(R.id.progressPlrBox)).setText(Integer.toString(currentScoring.progressGain));
		((EditText) layout.findViewById(R.id.progressOppBox)).setText(Integer.toString(currentScoring.progressKill));
		((CheckBox) layout.findViewById(R.id.winBox)).setChecked(currentScoring.winBonus);
	}

	private void updateScoring(View layout, AlertDialog shown) {
		if (changingValues)
			return;
		try {
			int letter = Integer.parseInt(((EditText) layout.findViewById(R.id.lettersBox)).getText().toString());
			int mine = Integer.parseInt(((EditText) layout.findViewById(R.id.minesBox)).getText().toString());
			int tileGain = Integer.parseInt(((EditText) layout.findViewById(R.id.tilesPlrBox)).getText().toString());
			int tileKill = Integer.parseInt(((EditText) layout.findViewById(R.id.tilesOppBox)).getText().toString());
			int progressGain = Integer.parseInt(((EditText) layout.findViewById(R.id.progressPlrBox)).getText()
					.toString());
			int progressKill = Integer.parseInt(((EditText) layout.findViewById(R.id.progressOppBox)).getText()
					.toString());
			boolean winBonus = ((CheckBox) layout.findViewById(R.id.winBox)).isChecked();
			currentScoring = new Scoring(letter, mine, tileGain, tileKill, progressGain, progressKill, winBonus);
			shown.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
		} catch (NumberFormatException e) {
			shown.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
		}
	}

}
