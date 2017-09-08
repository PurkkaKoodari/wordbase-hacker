package net.pietu1998.wordbasehacker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BoardActivity extends Activity {

	private Scoring currentScoring, scoring = Scoring.DEFAULT;
	private boolean changingValues = false;
	private Game game;
	private List<Possibility> possibilities = new ArrayList<>();
	private boolean loaded = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_board);
		loadScoring();
		Button scoring = (Button) findViewById(R.id.scoringBtn);
		scoring.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scoringDialog();
			}
		});
		Parcelable extra = getIntent().getParcelableExtra("game");
		if (extra == null) {
			Toast.makeText(this, R.string.internal_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		game = (Game) extra;
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

	private class LoadTask extends AsyncTask<Void, Integer, Integer> {

		private ProgressDialog dialog;
		private List<Possibility> results;

		public LoadTask(ProgressDialog dialog) {
			this.dialog = dialog;
		}

		private class TrieNode {
			private TrieNode[] nodes = new TrieNode[1024];
			private boolean hasChildren = false;
			private String content = null;
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
				int[][] layout = new int[10][13];
				for (Move move : moves) {
					JSONArray jsonLayout = (JSONArray) new JSONParser().parse(move.getLayout());
					game.addWord(move.getWord());
					for (Object o : jsonLayout) {
						JSONArray tile = (JSONArray) o;
						int flags = 0;
						if ("Base".equals(tile.get(0)))
							flags |= Tile.BASE;
						if ("Mine".equals(tile.get(0)))
							flags |= Tile.MINE;
						if ("SuperMine".equals(tile.get(0)))
							flags |= Tile.SUPER_MINE;
						if ("player".equals(tile.get(3)))
							flags |= Tile.PLAYER;
						if ("opponent".equals(tile.get(3)))
							flags |= Tile.OPPONENT;
						layout[Integer.parseInt((String) tile.get(1))][Integer.parseInt((String) tile.get(2))] = flags;
					}
				}

				short charMap[] = new short[65536];
				short charIndex = 0;

				Tile[][] tiles = new Tile[10][13];
				short[] tileMap = new short[130];
				for (int x = 0; x < 10; x++) {
					for (int y = 0; y < 13; y++) {
						char letter = rows[y].charAt(x);
						int mapping = charMap[letter];
						if (mapping == 0)
							mapping = charMap[letter] = (short) ++charIndex;
						tiles[x][y] = new Tile(layout[x][y], letter);
						tileMap[x + 10 * y] = (short) (mapping - 1);
					}
				}
				game.setBoard(new Board(tiles, words));

				publishProgress(R.string.analyzing_words);
				words = game.getBoard().getWords();
				int longest = 0;
				TrieNode root = new TrieNode();
				for (String word : words) {
					if (game.isPlayed(word))
						continue;
					if (word.length() > longest)
						longest = word.length();
					TrieNode node = root;
					for (int i = 0; i < word.length(); i++) {
						int mapping = charMap[word.charAt(i)] - 1;
						node.hasChildren = true;
						TrieNode next = node.nodes[mapping];
						if (next == null)
							node = node.nodes[mapping] = new TrieNode();
						else
							node = next;
					}
					node.content = word;
				}

				publishProgress(R.string.finding_words);
				results = new ArrayList<>();
				long start = System.currentTimeMillis();
				for (int iteration = 0; iteration < 100; iteration++) {
					boolean positions[] = new boolean[130];
					byte coords[] = new byte[2 * longest];
					short letters[] = new short[longest];
					for (int x = 0; x < 10; x++) {
						for (int y = 0; y < 13; y++) {
							if (game.getBoard().getTiles()[x][y].isSet(Tile.PLAYER)) {
								findWords(tileMap, 0, longest, x, y, coords, positions, letters, root);
							}
						}
					}
					if (iteration < 99) results.clear();
				}
				long end = System.currentTimeMillis();
				Log.d("WordbaseHacker", "Finding took " + (end - start) + "ms");

				publishProgress(R.string.scoring_words);
				start = System.currentTimeMillis();
				for (int iteration = 0; iteration < 20; iteration++)
					for (Possibility pos : results)
						game.getBoard().score(pos, game.isFlipped());
				end = System.currentTimeMillis();
				Log.d("WordbaseHacker", "Scoring took " + (end - start) + "ms");
				return 0;
			} catch (NumberFormatException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			} catch (ParseException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			} catch (SQLiteCantOpenDatabaseException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			} catch (SQLiteException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			}
		}

		private void findWords(short[] tiles, int length, int max, int x, int y, byte[] coords, boolean[] positions, short[] letters, TrieNode node) {
			int index = y * 10 + x;
			if (length >= max || x < 0 || x > 9 || y < 0 || y > 12 || positions[index])
				return;
			short letter = tiles[index];
			node = node.nodes[letter];
			if (node == null)
				return;
			coords[length * 2] = (byte) x;
			coords[length * 2 + 1] = (byte) y;
			letters[length] = letter;
			if (node.content != null)
				results.add(new Possibility(Arrays.copyOf(coords, length * 2 + 2), node.content));
			if (!node.hasChildren)
				return;
			positions[index] = true;
			findWords(tiles, length + 1, max, x, y + 1, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x - 1, y + 1, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x + 1, y + 1, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x - 1, y, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x + 1, y, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x, y - 1, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x - 1, y - 1, coords, positions, letters, node);
			findWords(tiles, length + 1, max, x + 1, y - 1, coords, positions, letters, node);
			positions[index] = false;
		}

		private int findPos(String[] items, String item) {
			int left = 0, right = items.length - 1;
			int mid = (left + right) / 2;
			int cmp;
			while (right >= left) {
				mid = (left + right) / 2;
				cmp = items[mid].compareTo(item);
				if (cmp == 0)
					return mid;
				else if (cmp < 0)
					left = mid + 1;
				else
					right = mid - 1;
			}
			return mid + (items[mid].compareTo(item) < 0 ? 1 : 0);
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
				possibilities = results;
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
		((ImageView) findViewById(R.id.movePicture)).setImageDrawable(new BoardDrawable(best, game.isFlipped()));
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
				if (scoring.equals(new Scoring(7, 0, 3, 3, 5, 6, false)))
					enableDev();
				if (scoring.equals(new Scoring(4, 0, 6, 7, 7, 8, false)))
					toggleHorst();
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

		Button defaults = (Button) layout.findViewById(R.id.defaults);
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

	private void enableDev() {
		getSharedPreferences("WordbaseHacker", 0).edit().putBoolean("dev", true).commit();
		Toast.makeText(this, R.string.dev_on, Toast.LENGTH_SHORT).show();
	}

	private void toggleHorst() {
		SharedPreferences pref = getSharedPreferences("WordbaseHacker", 0);
		boolean horst = !pref.getBoolean("horst", false);
		pref.edit().putBoolean("horst", horst).commit();
		getPackageManager()
				.setComponentEnabledSetting(
						new ComponentName(this, "net.pietu1998.wordbasehacker.GameListActivityHorst"),
						horst ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
		getPackageManager()
				.setComponentEnabledSetting(
						new ComponentName(this, "net.pietu1998.wordbasehacker.GameListActivityNormal"),
						horst ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP);
		Toast.makeText(this, R.string.horst_toggle, Toast.LENGTH_SHORT).show();
	}

}
