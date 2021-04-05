/*
 * top.donmor.tiddloidliteF.SplashActivity <= [P|Tiddloid Lite]
 * Last modified: 03:43:55 2019/05/07
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

public class SplashActivity extends Activity {
	private static final int LOAD_DISPLAY_TIME = 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
		setContentView(R.layout.splash);
		TextView ver = findViewById(R.id.textVersionSplash);
		ver.setText(BuildConfig.VERSION_NAME);
		new Handler().postDelayed(new Runnable() {
			public void run() {
				Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
				SplashActivity.this.startActivity(mainIntent);
				SplashActivity.this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				SplashActivity.this.finish();
			}
		}, LOAD_DISPLAY_TIME);
	}
}
