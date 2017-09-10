package net.pietu1998.wordbasehacker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class HudService extends Service {

    private final HudBinder binder = new HudBinder();

    public class HudBinder extends Binder {
        HudService getService() {
            return HudService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Timer timer;
    private boolean timerStarted = false;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        createHud();
        showHudIfPossible();
        if (HudUtils.isHudAutoEnabled(this))
            startTimer();
    }

    public void startTimer() {
        if (!timerStarted) {
            timerStarted = true;
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            HudService.this.checkWordbaseRunning();
                        }
                    });
                }
            }, 0, 1000);
        }
    }

    public void stopTimer() {
        if (timerStarted) {
            timerStarted = false;
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onDestroy() {
        stopTimer();
        hideHud();
        super.onDestroy();
    }

    private TextView view;
    private boolean windowShown = false;

    private boolean wasRunning = false;

    private boolean moving = false;
    private boolean resizing = false;
    private int origX, origY;
    private float startX, startY;

    private int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }

    private void showHudIfPossible() {
        if (!windowShown && HudUtils.canShowHud(this)) {
            windowShown = true;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, getOverlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = origX;
            params.y = origY;
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            manager.addView(view, params);
        }
    }

    private void createHud() {
        view = new TextView(this);
        view.setText("HUD placeholder");
        view.setTextColor(0xFF00FF00);
        view.setBackgroundColor(0xFFFFFF00);
        view.setPadding(5, 5, 5, 5);
        origX = 100;
        origY = 100;

        view.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                if (params == null)
                    return false;
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        moving = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        moving = false;
                        params.x = origX = (int) (origX + event.getRawX() - startX);
                        params.y = origY = (int) (origY + event.getRawY() - startY);
                        ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(view, params);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (moving) {
                            params.x = (int) (origX + event.getRawX() - startX);
                            params.y = (int) (origY + event.getRawY() - startY);
                            ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(view, params);
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void hideHud() {
        if (windowShown) {
            windowShown = false;
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            manager.removeView(view);
        }
    }

    private boolean wordbaseRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.RunningAppProcessInfo processInfo = manager.getRunningAppProcesses().get(0);
        return processInfo.processName.startsWith("com.wordbaseapp");
    }

    private void checkWordbaseRunning() {
        boolean runningNow = wordbaseRunning();
        if (runningNow != wasRunning) {
            wasRunning = runningNow;
            if (runningNow)
                showHudIfPossible();
            else
                hideHud();
        }
    }

}
