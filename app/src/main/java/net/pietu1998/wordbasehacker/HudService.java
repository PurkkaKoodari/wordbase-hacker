package net.pietu1998.wordbasehacker;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.WindowManager;

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

    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point screenSize = new Point();
        manager.getDefaultDisplay().getSize(screenSize);
        view = new HudView(this, screenSize.x);
    }

    @Override
    public void onDestroy() {
        hideHud();
        super.onDestroy();
    }

    private HudView view;
    private boolean windowShown = false;
    private boolean settingMode = false;

    private int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : settingMode ? WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
    }

    private void showHudIfPossible() {
        if (!windowShown && HudUtils.canShowHud(this)) {
            windowShown = true;
            int untouchable = settingMode ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    getOverlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | untouchable,
                    PixelFormat.TRANSLUCENT);
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            manager.addView(view, params);
        }
    }

    private void hideHud() {
        if (windowShown) {
            windowShown = false;
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            manager.removeView(view);
        }
    }

    public void hudOperationDone() {
        hideHud();
        settingMode = false;
        view.setEditMode(false);
        view.setCoordinates(null);
    }

    public void editHudSettings() {
        settingMode = true;
        view.setEditMode(true);
        view.invalidate();
        showHudIfPossible();
    }

    public void showSuggestedPath(@NonNull byte[] path) {
        settingMode = false;
        view.setEditMode(false);
        view.setCoordinates(path);
        view.invalidate();
        showHudIfPossible();
    }

}
