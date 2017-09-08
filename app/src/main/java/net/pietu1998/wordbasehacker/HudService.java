package net.pietu1998.wordbasehacker;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class HudService extends Service {

	private final HudBinder binder = new HudBinder();

	private class HudBinder extends Binder {
		@SuppressWarnings("unused")
		HudService getService() {
			return HudService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private Timer timer;
	private HudHandler handler = new HudHandler(this);

	public HudHandler getHandler() {
		return handler;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		showHud();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				handler.obtainMessage(HudHandler.SHOW_HUD).sendToTarget();
			}
		};
		timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, 1000);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		hideHud();
	}

	private TextView view;

	private boolean drag = false;
	private int origX, origY;
	private float startX, startY;

	public void showHud() {
		view = new TextView(this);
		view.setText("horst");
		view.setTextAppearance(this, android.R.style.TextAppearance_Holo_Large);
		view.setTextColor(0xFF00FF00);
		view.setBackgroundColor(0xFFFFFF00);
		view.setPadding(5, 5, 5, 5);
		origX = 100;
		origY = 100;

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = origX;
		params.y = origY;

		WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
		manager.addView(view, params);

		view.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					startX = event.getRawX();
					startY = event.getRawY();
					drag = true;
					break;
				case MotionEvent.ACTION_UP:
					drag = false;
					params.x = origX = (int) (origX + event.getRawX() - startX);
					params.y = origY = (int) (origY + event.getRawY() - startY);
					((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(v, params);
					break;
				case MotionEvent.ACTION_MOVE:
					if (drag) {
						params.x = (int) (origX + event.getRawX() - startX);
						params.y = (int) (origY + event.getRawY() - startY);
						((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(v, params);
					}
					break;
				}
				return true;
			}
		});
	}

	public void hideHud() {
		if (view != null) {
			WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
			manager.removeView(view);
		}
	}

	private boolean wordbaseRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		ActivityManager.RunningAppProcessInfo processInfo = manager.getRunningAppProcesses().get(0);
		return processInfo.processName.startsWith("com.wordbaseapp");
	}

	public void updateWbr() {
		view.setText(wordbaseRunning() ? "Wordbase running" : "Wordbase not running");
	}

}
