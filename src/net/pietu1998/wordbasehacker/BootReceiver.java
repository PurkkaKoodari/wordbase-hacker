package net.pietu1998.wordbasehacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, HudService.class);
		// TODO Remove condition when HUD working
		if (BuildConfig.DEBUG)
			context.startService(service);
	}

}
