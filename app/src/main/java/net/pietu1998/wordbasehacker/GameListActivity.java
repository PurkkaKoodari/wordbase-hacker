package net.pietu1998.wordbasehacker;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.pietu1998.wordbasehacker.solver.Game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GameListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<GameListActivity.LoadResult> {

	private ArrayAdapter<Game> adapter;
	private final List<Game> games = new ArrayList<>();
	private SwipeRefreshLayout swipe = null;
	private ListView list;
	private TextView listEmpty;
	private long lastModified = -1;

	@SuppressLint("SdCardPath")
	private static final String WORDBASE_DB_PATH = "/data/data/com.wordbaseapp/databases/wordbase.db";

	private static final int REQUEST_SETTINGS = 1;

	public static final int SHOULD_NOT_RELOAD = 0;
	public static final int SHOULD_RELOAD = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_list);

		swipe = (SwipeRefreshLayout) findViewById(R.id.list_swipe);
		swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				reloadData(true);
			}
		});

		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setTitle(R.string.title_activity_game_list);

		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, games);
		listEmpty = (TextView) findViewById(R.id.list_empty);
		list = (ListView) findViewById(R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(GameListActivity.this, BoardActivity.class);
				intent.putExtra("game", games.get(position));
				startActivity(intent);
			}
		});

		getLoaderManager().initLoader(0, Bundle.EMPTY, this);
	}

	private void reloadData(boolean explicit) {
		Bundle bundle = new Bundle();
		bundle.putLong("lastModified", lastModified);
		bundle.putBoolean("explicit", explicit);
		getLoaderManager().restartLoader(0, bundle, this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (HudUtils.isHudEnabled(this) && !HudUtils.canShowHud(this))
			HudUtils.requestHudPermission(this, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, REQUEST_SETTINGS);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SETTINGS && resultCode == SHOULD_RELOAD) {
			lastModified = -1;
			reloadData(false);
		}
	}

	@Override
	public Loader<LoadResult> onCreateLoader(int i, Bundle bundle) {
		return new GameListLoader(this, bundle);
	}

	@Override
	public void onLoadFinished(Loader<LoadResult> loader, LoadResult result) {
		swipe.setRefreshing(false);
		if (result.message != 0) {
			adapter.clear();
			listEmpty.setText(result.message);
			list.setVisibility(View.GONE);
			listEmpty.setVisibility(View.VISIBLE);
		} else {
			if (result.games != null) {
				lastModified = result.lastModified;
				adapter.clear();
				adapter.addAll(result.games);
			} else if (result.explicit) {
				final Snackbar snackbar = Snackbar.make(swipe, R.string.data_not_changed, Snackbar.LENGTH_LONG);
				snackbar.setAction(R.string.open, new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						snackbar.dismiss();
						Intent intent = new Intent();
						intent.setComponent(new ComponentName("com.wordbaseapp", "com.wordbaseapp.GameMenuActivity"));
						startActivity(intent);
					}
				});
				snackbar.show();
			}
			listEmpty.setText(R.string.no_games);
			list.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
			listEmpty.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<LoadResult> loader) {}

	public static class LoadResult {
		public final List<Game> games;
		public final long lastModified;
		public final int message;
		public final boolean explicit;

		public LoadResult(List<Game> games, long lastModified, boolean explicit) {
			this.games = games;
			this.lastModified = lastModified;
			this.explicit = explicit;
			this.message = 0;
		}

		public LoadResult(int error, boolean explicit) {
			this.games = null;
			this.lastModified = -1;
			this.explicit = explicit;
			this.message = error;
		}
	}

	private static class GameListLoader extends AsyncTaskLoader<LoadResult> {
		private final String inputDb;
		private final File cacheDb;
		private final String pendingOpponent, unknownOpponent;
		private final WeakReference<GameListActivity> activityReference;
		private long lastModified;
		private boolean explicit;

		GameListLoader(GameListActivity activity, Bundle bundle) {
			super(activity);
			activityReference = new WeakReference<>(activity);
			pendingOpponent = activity.getResources().getString(R.string.pending_opponent);
			unknownOpponent = activity.getResources().getString(R.string.unknown_opponent);
			String db = PreferenceManager.getDefaultSharedPreferences(activity).getString(activity.getString(R.string.pref_key_dbpath), "");
			inputDb = db.isEmpty() ? WORDBASE_DB_PATH : db;
			cacheDb = new File(activity.getCacheDir(), "wordbase.db");
			this.lastModified = bundle.getLong("lastModified", -1);
			this.explicit = bundle.getBoolean("explicit", false);
		}

		@Override
		@NonNull
		public LoadResult loadInBackground() {
			boolean wasExplicit = explicit;
			explicit = false;
			try {
				Process whoami = new ProcessBuilder("whoami").start();
				String username;
				try (BufferedReader buf = new BufferedReader(new InputStreamReader(whoami.getInputStream()))) {
					username = buf.readLine();
					if (username == null) {
						Log.e("WordbaseHacker", "No output from whoami");
						return new LoadResult(R.string.internal_error, wasExplicit);
					}
				}
				long currentModified = -1;
				try (RootShell root = new RootShell()) {
					root.sendCommand("stat -c %Y \"" + inputDb + "\"");
					String out = root.readResultLine();
					try {
						currentModified = Long.parseLong(out);
						if (lastModified == currentModified)
							return new LoadResult(null, lastModified, wasExplicit);
					} catch (NumberFormatException ignore) {}
					root.sendCommand("cp \"" + inputDb + "\" \"" + cacheDb.getPath() + "\" && " +
							"chown " + username + ":" + username + " \"" + cacheDb.getPath() + "\" && " +
							"restorecon -F " + cacheDb.getPath() + " && " +
							"echo hacked");
					out = root.readResultLine();
					if (out == null) {
						Log.e("WordbaseHacker", "Root command failed with no output");
						return new LoadResult(R.string.cant_get_root, wasExplicit);
					}
					if (out.toLowerCase(Locale.getDefault()).contains("no such")) {
						Log.e("WordbaseHacker", "Root command failed: " + out);
						return new LoadResult(R.string.cant_find_db, wasExplicit);
					}
				}

				List<Game> results = new ArrayList<>();
				try (SQLiteDatabase db = SQLiteDatabase.openDatabase(cacheDb.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
					try (Cursor gameCursor = db.query("games", new String[]{"_id", "board_id", "opponent_id", "layout", "owner"},
							"turn='player' AND winner is null", null, null, null, "updated_at DESC")) {
						while (gameCursor.moveToNext()) {
							int id = gameCursor.getInt(0);
							int boardId = gameCursor.getInt(1);
							int opponentId = gameCursor.getInt(2);
							String layout = gameCursor.getString(3);
							String owner = gameCursor.getString(4);
							Game game = new Game(id, boardId, opponentId, layout, owner.equals("opponent"));
							if (opponentId == 0)
								game.setOpponent(pendingOpponent);
							else {
								try (Cursor opponentCursor = db.query("players", new String[]{"first_name"}, "_id=" + opponentId,
										null, null, null, null)) {
									if (opponentCursor.moveToNext())
										game.setOpponent(opponentCursor.getString(0));
									else
										game.setOpponent(unknownOpponent);
								}
							}
							results.add(game);
						}
					}
				}

				return new LoadResult(results, currentModified, wasExplicit);
			} catch (FileNotFoundException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return new LoadResult(R.string.cant_find_db, wasExplicit);
			} catch (SQLiteException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return new LoadResult(R.string.internal_error, wasExplicit);
			} catch (IOException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return new LoadResult(R.string.cant_get_root, wasExplicit);
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
			try {
				GameListActivity activity = activityReference.get();
				activity.swipe.setRefreshing(true);
				activity.listEmpty.setVisibility(View.GONE);
			} catch (NullPointerException ignore) {}
		}

		@Override
		protected void onReset() {
			super.onReset();
			lastModified = -1;
		}
	}

}
