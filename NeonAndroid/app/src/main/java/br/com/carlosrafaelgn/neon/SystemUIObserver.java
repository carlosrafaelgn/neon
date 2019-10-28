package br.com.carlosrafaelgn.neon;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.view.View;

@SuppressLint("InlinedApi")
@SuppressWarnings("WeakerAccess")
public final class SystemUIObserver implements View.OnSystemUiVisibilityChangeListener {
	public static final int MSG_HIDE = 0x0400;
	public static final int MSG_SYSTEM_UI_CHANGED = 0x0401;

	private Handler handler;
	private View decor;

	public SystemUIObserver(Handler handler, View decor) {
		this.handler = handler;
		this.decor = decor;
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		if (handler != null)
			handler.sendMessage(Message.obtain(handler, MSG_SYSTEM_UI_CHANGED, visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION, 0));
	}

	public void hide() {
		if (decor == null)
			return;
		try {
			decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LOW_PROFILE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_IMMERSIVE);
		} catch (Throwable th) {
			// Just ignore...
		}
	}

	public void show() {
		if (decor == null)
			return;
		try {
			decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				//View.SYSTEM_UI_FLAG_LOW_PROFILE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				//View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_IMMERSIVE);
		} catch (Throwable th) {
			// Just ignore...
		}
	}

	public void prepare() {
		show();
		if (decor == null)
			return;
		try {
			decor.setOnSystemUiVisibilityChangeListener(this);
		} catch (Throwable th) {
			// Just ignore...
		}
	}

	public void cleanup() {
		handler = null;
		if (decor != null) {
			try {
				decor.setOnSystemUiVisibilityChangeListener(null);
			} catch (Throwable th) {
				// Just ignore...
			}
			decor = null;
		}
	}
}
