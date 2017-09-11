package net.pietu1998.wordbasehacker;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class HackerApplication extends Application {

    private HudService service;
    private boolean serviceRunning = false;
    private final Object syncer = new Object();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            HudService.HudBinder binder = (HudService.HudBinder) iBinder;
            service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            service = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (HudUtils.isHudEnabled(this))
            startHudService();
    }

    public void startHudService() {
        synchronized (syncer) {
            if (!serviceRunning) {
                serviceRunning = true;
                Intent start = new Intent(this, HudService.class);
                bindService(start, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    public void stopHudService() {
        synchronized (syncer) {
            if (serviceRunning) {
                serviceRunning = false;
                unbindService(serviceConnection);
                Intent stop = new Intent(this, HudService.class);
                stopService(stop);
            }
        }
    }

    public void editHudSettings() {
        if (service != null)
            service.editHudSettings();
    }

    public void showSuggestedPath(byte[] coords) {
        if (service != null)
            service.showSuggestedPath(coords);
    }

    public void hudOperationDone() {
        if (service != null)
            service.hudOperationDone();
    }

}
