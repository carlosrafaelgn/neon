//
// MIT License
//
// Copyright (c) 2019 Carlos Rafael Gimenes das Neves
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// https://github.com/carlosrafaelgn/neon
//

package br.com.carlosrafaelgn.neon;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity implements Handler.Callback, SensorEventListener {

	// https://developer.android.com/guide/webapps/webview

	private final class LibWebViewJavaScriptInterface {
		public boolean supported;
		public float x, y;
		public String browserLanguage;

		@JavascriptInterface
		public boolean isSupported() {
			return supported;
		}

		@JavascriptInterface
		public float getX() {
			return x;
		}

		@JavascriptInterface
		public float getY() {
			return y;
		}

		@JavascriptInterface
		public String getBrowserLanguage() {
			return browserLanguage;
		}
	}

	private final class LibWebViewClient extends WebViewClient {
		private boolean shouldOverrideUrlLoading(Uri uri) {
			try {
				if (uri.getScheme().toLowerCase(Locale.US).startsWith("http")) {
					startActivity(new Intent(Intent.ACTION_VIEW, uri));
					return true;
				}
			} catch (Throwable th) {
				// Just ignore...
			}
			return false;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return shouldOverrideUrlLoading(Uri.parse(url));
		}

		@Override
		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			return shouldOverrideUrlLoading(request.getUrl());
		}
	}

	private final class LibWebChromeClient extends WebChromeClient {

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
			return false;
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
			if (message == null || message.length() == 0)
				return false;
			final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
				.setMessage(message)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				.setTitle(R.string.app_name_short)
				.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					result.confirm();
				}
			});
			alertDialog.show();
			return true;
		}

		@Override
		public boolean onJsBeforeUnload(WebView view, String url, String message, final JsResult result) {
			final boolean[] ok = new boolean[1];
			final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
				.setMessage(R.string.confirm_quit)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ok[0] = true;
						result.confirm();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.setTitle(R.string.app_name_short)
				.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (!ok[0])
						result.cancel();
				}
			});
			alertDialog.show();
			return true;
		}

		@Override
		public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
			if (message == null || message.length() == 0)
				return false;
			final boolean[] ok = new boolean[1];
			final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
				.setMessage(message)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ok[0] = true;
						result.confirm();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.setTitle(R.string.app_name_short)
				.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (!ok[0])
						result.cancel();
				}
			});
			alertDialog.show();
			return true;
		}

		@Override
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
			LinearLayout linearLayout = new LinearLayout(MainActivity.this);
			linearLayout.setOrientation(LinearLayout.VERTICAL);

			final int padding = dpToPxI(16);
			linearLayout.setPadding(padding, padding, padding, padding);

			if (message != null && message.length() > 0) {
				final TextView lbl = new TextView(MainActivity.this);
				lbl.setText(message);
				final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				layoutParams.bottomMargin = padding >> 1;
				linearLayout.addView(lbl, layoutParams);
			}

			final EditText txt = new EditText(MainActivity.this);
			txt.setMaxLines(1);
			txt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			if (defaultValue != null && defaultValue.length() > 0)
				txt.setText(defaultValue);
			linearLayout.addView(txt, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			final boolean[] ok = new boolean[1];
			final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
				.setView(linearLayout)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ok[0] = true;
						result.confirm(txt.getText().toString());
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.setTitle(R.string.app_name_short)
				.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (!ok[0])
						result.cancel();
				}
			});
			alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					txt.requestFocus();
					txt.selectAll();
				}
			});
			alertDialog.show();
			return true;
		}

		@Override
		public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
			filePathCallback.onReceiveValue(null);
			return true;
		}

		@Override
		public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
			System.out.println(consoleMessage.sourceId() + ": " + consoleMessage.lineNumber() + " - " + consoleMessage.message());
			return true;
		}

		@Override
		public void onGeolocationPermissionsHidePrompt() {
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
			callback.invoke(origin, true, true);
		}

		@Override
		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		public void onPermissionRequest(PermissionRequest request) {
			try {
				request.grant(request.getResources());
			} catch (Throwable th) {
				// Just ignore...
			}
		}

		@Override
		public void onPermissionRequestCanceled(PermissionRequest request) {
		}
	}

	private static final String PREF_NAME = "prefs";
	private static final String PREF_KEY_VERSION = "version";
	private static final String PREF_KEY_LASTTIME = "lastTime";

	private float density;

	private Handler handler;
	private SensorManager sensorManager;
	private Sensor sensor;
	private SystemUIObserver systemUIObserver;
	private int systemUIObserverVersion;
	private String indexUrl;
	private int version;
	private long lastTime;
	private final Object updateThreadSync = new Object();
	private volatile boolean updateThreadValid;
	private Thread updateThread;

	private WebView webView;
	private LibWebViewJavaScriptInterface webViewJavaScriptInterface;
	private boolean isWindowFocused;

	private int dpToPxI(float dp) {
		return (int)((dp * density) + 0.5f);
	}

	private void hideAllUIDelayed() {
		systemUIObserverVersion++;
		handler.removeMessages(SystemUIObserver.MSG_HIDE);
		handler.sendMessageDelayed(Message.obtain(handler, SystemUIObserver.MSG_HIDE, systemUIObserverVersion, 0), 4000);
	}

	private void prepareSystemUIObserver() {
		if (systemUIObserver == null)
			systemUIObserver = new SystemUIObserver(handler, getWindow().getDecorView());
		systemUIObserver.prepare();
	}

	private void hideSystemUI() {
		if (systemUIObserver != null)
			systemUIObserver.hide();
	}

	private void cleanupSystemUIObserver() {
		if (systemUIObserver != null) {
			systemUIObserver.cleanup();
			systemUIObserver = null;
		}
	}

	private void prepareSensor() {
		webViewJavaScriptInterface.supported = false;
		try {
			sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, sensor, 50000);
			webViewJavaScriptInterface.supported = true;
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

	private void cleanupSensor() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
			sensorManager = null;
		}

		sensor = null;
	}

	private void loadPreferences() {
		try {
			final SharedPreferences preferences = this.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
			version = preferences.getInt(PREF_KEY_VERSION, 0);
			lastTime = preferences.getLong(PREF_KEY_LASTTIME, 0);
		} catch (Throwable th) {
			// Just ignore...
		}
	}

	private void savePreferences() {
		try {
			final SharedPreferences preferences = this.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
			final SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(PREF_KEY_VERSION, version);
			editor.putLong(PREF_KEY_LASTTIME, lastTime);
			editor.apply();
		} catch (Throwable th) {
			// Just ignore...
		}
	}

	private void abortUpdateThread() {
		updateThreadValid = false;

		synchronized (updateThreadSync) {
			if (updateThread != null)
				updateThread.interrupt();
			updateThread = null;
		}
	}

	private void checkForUpdates() {
		abortUpdateThread();

		updateThreadValid = true;
		updateThread = new Thread("Update Thread") {
			@Override
			public void run() {
				int version = MainActivity.this.version;

				final File newIndex = new File(getFilesDir(), "index.updt");
				HttpURLConnection connection = null;
				InputStream inputStream = null;
				OutputStream outputStream = null;
				boolean success = false;

				try {
					connection = (HttpURLConnection)(new URL("https://carlosrafaelgn.github.io/neon/version.json")).openConnection();
					connection.setAllowUserInteraction(false);
					connection.setConnectTimeout(30000);
					connection.setInstanceFollowRedirects(true);
					connection.setReadTimeout(30000);
					connection.setUseCaches(false);
					inputStream = connection.getInputStream();

					if (!updateThreadValid)
						return;

					byte[] tmp = new byte[8];
					int len;
					if ((len = inputStream.read(tmp, 0, 8)) > 0)
						version = Integer.parseInt((new String(tmp, 0, len)).trim());

					inputStream.close();
					inputStream = null;
					connection.disconnect();
					connection = null;

					if (updateThreadValid && version != MainActivity.this.version) {
						if (!newIndex.exists() || newIndex.delete()) {
							outputStream = new FileOutputStream(newIndex, false);
							connection = (HttpURLConnection)(new URL("https://carlosrafaelgn.github.io/neon/index.html")).openConnection();
							connection.setAllowUserInteraction(false);
							connection.setConnectTimeout(30000);
							connection.setInstanceFollowRedirects(true);
							connection.setReadTimeout(30000);
							connection.setUseCaches(false);
							inputStream = connection.getInputStream();

							tmp = new byte[32768];
							while (updateThreadValid && (len = inputStream.read(tmp, 0, tmp.length)) > 0)
								outputStream.write(tmp, 0, len);

							if (updateThreadValid)
								success = true;
						}
					} else {
						success = true;
					}
				} catch (Throwable th) {
					th.printStackTrace();
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch (Throwable th) {
							// Just ignore...
						}
					}
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (Throwable th) {
							// Just ignore...
						}
					}
					if (connection != null) {
						try {
							connection.disconnect();
						} catch (Throwable th) {
							// Just ignore...
						}
					}
				}

				synchronized (updateThreadSync) {
					updateThread = null;

					if (updateThreadValid && success) {
						MainActivity.this.version = version;
						lastTime = System.currentTimeMillis();
						savePreferences();
					} else if (newIndex.exists()) {
						//noinspection ResultOfMethodCallIgnored
						newIndex.delete();
					}
				}
			}
		};
		updateThread.start();
	}

	private boolean copyFileFromAssets(String source, File destination) {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		boolean success = false;
		try {
			inputStream = getAssets().open(source, AssetManager.ACCESS_STREAMING);
			outputStream = new FileOutputStream(destination, false);
			final byte[] tmp = new byte[32768];
			int len;
			while ((len = inputStream.read(tmp, 0, tmp.length)) > 0)
				outputStream.write(tmp, 0, len);
			success = true;
		} catch (Throwable th) {
			// Just ignore...
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Throwable th) {
					// Just ignore...
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (Throwable th) {
					// Just ignore...
				}
			}
			if (!success) {
				try {
					//noinspection ResultOfMethodCallIgnored
					destination.delete();
				} catch (Throwable th) {
					// Just ignore...
				}
			}
		}
		return success;
	}

	private boolean isFileValid(File dir, String asset) {
		final File file = new File(dir, asset);

		if (file.exists() && file.length() > 10240)
			return true;

		if (copyFileFromAssets(asset, file)) {
			System.gc();
			return true;
		}

		return false;
	}

	private void prepareFiles() {
		indexUrl = null;

		try {
			final File dir = getFilesDir();
			final File assetsDir = new File(dir, "assets");
			if (assetsDir.exists() || assetsDir.mkdir()) {
				final File newIndex = new File(dir, "index.updt");
				final File index = new File(dir, "index.html");
				if (newIndex.exists()) {
					if (!index.exists() || index.delete()) {
						//noinspection ResultOfMethodCallIgnored
						newIndex.renameTo(index);
					}
				}
				if (isFileValid(dir, "index.html") &&
					isFileValid(dir, "phaser-3.18.1.min.js") &&
					isFileValid(dir, "assets/atlas.json") &&
					isFileValid(dir, "assets/atlas.png"))
					indexUrl = "file://" + index.getAbsolutePath();
			}
		} catch (Throwable th) {
			// Just ignore...
		}

		if (indexUrl == null)
			indexUrl = "file:///android_asset/index.html";
	}

	@Override
	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loadPreferences();

		prepareFiles();

		final Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);
		density = displayMetrics.density;

		isWindowFocused = true;

		handler = new Handler(this);

		webView = new WebView(this);
		webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		webView.setWebViewClient(new LibWebViewClient());
		webView.setWebChromeClient(new LibWebChromeClient());
		webView.addJavascriptInterface(webViewJavaScriptInterface = new LibWebViewJavaScriptInterface(), "neon");
		webView.setHorizontalScrollBarEnabled(false);
		final WebSettings settings = webView.getSettings();
		settings.setAllowContentAccess(true);
		settings.setAllowFileAccess(true);
		settings.setAllowFileAccessFromFileURLs(true);
		settings.setAllowUniversalAccessFromFileURLs(true);
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.setSupportMultipleWindows(false);
		settings.setSupportZoom(false);
		settings.setUseWideViewPort(true);
		settings.setLoadWithOverviewMode(true);
		settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		settings.setLoadWithOverviewMode(true);
		settings.setDisplayZoomControls(false);
		settings.setBuiltInZoomControls(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			settings.setMediaPlaybackRequiresUserGesture(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(false);
		settings.setLoadsImagesAutomatically(true);
		settings.setDomStorageEnabled(true);
		settings.setDatabaseEnabled(true);
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			settings.setDatabasePath(getFilesDir().getPath());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		webView.setBackgroundColor(0xff000000);

		setContentView(webView);

		prepareSystemUIObserver();

		prepareSensor();

		webViewJavaScriptInterface.browserLanguage = getString(R.string.browser_language);

		webView.loadUrl(indexUrl);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Check for updates every 72 hours
		if ((System.currentTimeMillis() - lastTime) > (72 * 60 * 60 * 1000))
			checkForUpdates();
	}

	@Override
	protected void onStart() {
		super.onStart();

		prepareSensor();
	}

	@Override
	protected void onStop() {
		super.onStop();

		cleanupSensor();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		cleanupSystemUIObserver();

		abortUpdateThread();
	}

	@Override
	public void onBackPressed() {
		if (webView.canGoBack())
			webView.goBack();
		else
			super.onBackPressed();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if ((isWindowFocused = hasFocus))
			hideAllUIDelayed();

		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (webViewJavaScriptInterface != null) {
			webViewJavaScriptInterface.x = event.values[0];
			webViewJavaScriptInterface.y = event.values[1];
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case SystemUIObserver.MSG_HIDE:
			if (msg.arg1 != systemUIObserverVersion || !isWindowFocused)
				break;
			hideSystemUI();
			break;
		case SystemUIObserver.MSG_SYSTEM_UI_CHANGED:
			hideAllUIDelayed();
			break;
		}
		return true;
	}
}
