package net.pietu1998.wordbasehacker;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class HudHandler extends Handler {

	private HudService service;

	public static final int SHOW_HUD = 1;
	public static final int HIDE_HUD = 2;

	public HudHandler() {}

	public HudHandler(HudService service) {
		super(Looper.getMainLooper());
		this.service = service;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case SHOW_HUD:
			service.updateWbr();
			break;
		case HIDE_HUD:
			service.updateWbr();
			break;
		}
	}

}
