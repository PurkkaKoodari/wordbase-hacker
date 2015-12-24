package net.pietu1998.wordbasehacker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.pietu1998.wordbasehacker.solver.Game;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GameListActivity extends ListActivity {

	private ArrayAdapter<Game> adapter;
	private List<Game> games = new ArrayList<>();
	private boolean loading = false;
	private SwipeRefreshLayout swipe = null;

	@SuppressLint("SdCardPath")
	public static final String WORDBASE_DB_PATH = "/data/data/com.wordbaseapp/databases/wordbase.db";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_list);

		swipe = (SwipeRefreshLayout) findViewById(R.id.list_swipe);
		swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				loadData(true);
			}
		});

		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, games);
		setListAdapter(adapter);
		getActionBar().setTitle(R.string.choose_game);
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(GameListActivity.this, BoardActivity.class);
				intent.putExtra("game", games.get(position));
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		invalidateOptionsMenu();
		loadData(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.miDev).setVisible(getSharedPreferences("WordbaseHacker", 0).getBoolean("dev", false));
		return true;
	}

	@Override
	@SuppressLint("InflateParams")
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.miInfo:
			try {
				String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				if (BuildConfig.DEBUG) {
					version += " (debug build)";
				}
				String aboutText = getResources().getString(R.string.about_text, version);
				SpannableString spannable = new SpannableString(aboutText);
				Linkify.addLinks(spannable, Pattern.compile("https?://[\u0021-\u007e]+"), "http://");
				TextView view = new TextView(this);
				view.setTextAppearance(this, android.R.style.TextAppearance_Holo_Small);
				view.setPadding(5, 5, 5, 5);
				view.setText(spannable);
				view.setMovementMethod(LinkMovementMethod.getInstance());
				new AlertDialog.Builder(this).setView(view).setNeutralButton(R.string.ok, null)
						.setTitle(R.string.about_title).show();
			} catch (NameNotFoundException e) {
				Log.e("WordbaseHacker", "Couldn't get version.", e);
				Toast.makeText(this, R.string.internal_error, Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.miOffdev:
			getSharedPreferences("WordbaseHacker", 0).edit().putBoolean("dev", false).commit();
			invalidateOptionsMenu();
			Toast.makeText(this, R.string.dev_off, Toast.LENGTH_SHORT).show();
			return true;
		case R.id.miExtfile:
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.dbfile);
			final EditText editText = new EditText(this);
			editText.setInputType(EditorInfo.TYPE_CLASS_TEXT);
			editText.setRawInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			editText.setText(getSharedPreferences("WordbaseHacker", 0).getString("db", WORDBASE_DB_PATH));
			dialog.setView(editText);
			dialog.setNegativeButton(R.string.cancel, null);
			dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (editText.length() != 0) {
						getSharedPreferences("WordbaseHacker", 0).edit().putString("db", editText.getText().toString())
								.commit();
						loadData(true);
					}
				}
			});
			dialog.setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getSharedPreferences("WordbaseHacker", 0).edit().putString("db", WORDBASE_DB_PATH).commit();
					loadData(true);
				}
			});
			dialog.show();
			return true;
		case R.id.miHudsvc:
			Intent start = new Intent(this, HudService.class);
			startService(start);
			return true;
		case R.id.miHudsvcoff:
			Intent stop = new Intent(this, HudService.class);
			stopService(stop);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void loadData(boolean background) {
		if (loading)
			return;
		loading = true;
		ProgressDialog dialog = null;
		if (background) {
			swipe.setRefreshing(true);
		} else {
			dialog = new ProgressDialog(this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setCancelable(false);
			dialog.show();
		}
		AcquireDatabaseTask task = new AcquireDatabaseTask(getSharedPreferences("WordbaseHacker", 0).getString("db",
				WORDBASE_DB_PATH), dialog);
		task.execute();
	}

	private class AcquireDatabaseTask extends AsyncTask<Void, Integer, Integer> {

		private String inputDb;
		private ProgressDialog dialog;
		private List<Game> results;

		public AcquireDatabaseTask(String inputDb, ProgressDialog dialog) {
			this.inputDb = inputDb;
			this.dialog = dialog;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				publishProgress(R.string.loading_games_db);
				ProcessBuilder pb = new ProcessBuilder("su");
				pb.redirectErrorStream(true);
				Process root = pb.start();
				Writer cmd = new OutputStreamWriter(root.getOutputStream());
				BufferedReader res = new BufferedReader(new InputStreamReader(root.getInputStream()));
				File cacheDb = new File(getCacheDir(), "wordbase.db");
				cmd.write("cp \"" + inputDb + "\" \"" + cacheDb.getPath() + "\" && chmod 0777 \"" + cacheDb.getPath()
						+ "\" && echo hacked\n");
				cmd.flush();
				String out = res.readLine();
				if (out == null) {
					cmd.close();
					res.close();
					return R.string.cant_get_root;
				}
				if (out != null && out.toLowerCase(Locale.getDefault()).contains("no such")) {
					cmd.write("exit\n");
					cmd.flush();
					cmd.close();
					res.close();
					return R.string.cant_find_db;
				}

				publishProgress(R.string.loading_games_tbl);

				results = new ArrayList<>();
				SQLiteDatabase db = SQLiteDatabase.openDatabase(cacheDb.getPath(), null, SQLiteDatabase.OPEN_READONLY);

				Cursor gameCursor = db.query("games",
						new String[] { "_id", "board_id", "opponent_id", "layout", "owner" },
						"turn='player' AND winner is null", null, null, null, "updated_at DESC");
				while (gameCursor.moveToNext()) {
					int id = gameCursor.getInt(0);
					int boardId = gameCursor.getInt(1);
					int opponentId = gameCursor.getInt(2);
					String layout = gameCursor.getString(3);
					String owner = gameCursor.getString(4);
					Game game = new Game(id, boardId, opponentId, layout, owner.equals("opponent"));
					if (opponentId == 0)
						game.setOpponent(getResources().getString(R.string.pending_opponent));
					else {
						Cursor opponentCursor = db.query("players", new String[] { "first_name" }, "_id=" + opponentId,
								null, null, null, null);
						if (opponentCursor.moveToNext())
							game.setOpponent(opponentCursor.getString(0));
						else
							game.setOpponent(getResources().getString(R.string.unknown_opponent));
					}
					results.add(game);
				}

				db.close();

				cmd.write("exit\n");
				cmd.flush();
				cmd.close();
				res.close();
				root.waitFor();
				return 0;
			} catch (IOException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.cant_get_root;
			} catch (IllegalThreadStateException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.cant_get_root;
			} catch (InterruptedException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.cant_get_root;
			} catch (SQLiteCantOpenDatabaseException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.cant_find_db;
			} catch (SQLiteException e) {
				Log.e("WordbaseHacker", "Failed to read data.", e);
				return R.string.internal_error;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (dialog != null) {
				dialog.setMessage(getResources().getString(values[0]));
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (dialog != null) {
				dialog.dismiss();
			}
			swipe.setRefreshing(false);
			if (result != 0) {
				new AlertDialog.Builder(GameListActivity.this).setMessage(result)
						.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						}).show();
			} else {
				adapter.clear();
				adapter.addAll(results);
				loading = false;
			}
		}

	}

}
