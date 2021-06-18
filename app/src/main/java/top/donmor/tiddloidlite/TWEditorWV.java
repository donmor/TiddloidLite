/*
 * top.donmor.tiddloidlite.TWEditorWV <= [P|Tiddloid Lite]
 * Last modified: 18:33:05 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TWEditorWV extends AppCompatActivity {

	// 定义属性
	private JSONObject db, wApp;
	private WebChromeClient wcc;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	private int mOriginalOrientation;
	private Integer themeColor = null;
	private boolean hideAppbar = false, ready = false;
	private float scale;
	private ValueCallback<Uri[]> uploadMessage;
	private WebView wv;
	private Toolbar toolbar;
	private ProgressBar wvProgress;
	private Uri uri = null;
	private static byte[] exData = null;

	// 常量
	private static final String
			JSI = "twi",
			MIME_ANY = "*/*",
			STR_JS_POP_PRE = "(function(){new $tw.Story().navigateTiddler(\"",
			STR_JS_POP_POST = "\");})();",
			STR_JS_PRINT = "(function(){window.print=function(){window.twi.print();}})();",
			KEY_ENC = "enc",
			KEY_YES = "yes",
			SCH_ABOUT = "about",
			SCH_FILE = "file",
			SCH_HTTP = "http",
			SCH_HTTPS = "https",
			SCH_TEL = "tel",
			SCH_MAILTO = "mailto",
			URL_BLANK = "about:blank";
	private static final int
			KEY_REQ_DOWN = 906,
			BUF_SIZE = 4096;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 全局设定
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.tweditor);
		// 初始化db
		try {
			db = MainActivity.readJson(this);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		// 初始化顶栏
		toolbar = findViewById(R.id.wv_toolbar);
		setSupportActionBar(toolbar);
		this.setTitle(R.string.app_name);
		wvProgress = findViewById(R.id.progressBar);
		wvProgress.setMax(100);
		// 初始化WebView
		wv = findViewById(R.id.twWebView);
		WebSettings wvs = wv.getSettings();
		wvs.setDatabaseEnabled(true);
		wvs.setDomStorageEnabled(true);
		wvs.setBuiltInZoomControls(false);
		wvs.setDisplayZoomControls(false);
		wvs.setUseWideViewPort(true);
		wvs.setLoadWithOverviewMode(true);
		wvs.setAllowFileAccess(true);
		wvs.setAllowContentAccess(true);
		wvs.setAllowFileAccessFromFileURLs(true);
		wvs.setAllowUniversalAccessFromFileURLs(true);
		wvs.setSupportMultipleWindows(true);
		wvs.setMediaPlaybackRequiresUserGesture(false);
		scale = getResources().getDisplayMetrics().density;
		wcc = new WebChromeClient() {
			// 进度条
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				ready = newProgress == 100;
				toolbar.setVisibility(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || hideAppbar && ready ? View.GONE : View.VISIBLE);
				wvProgress.setVisibility(ready ? View.GONE : View.VISIBLE);
				wvProgress.setProgress(newProgress);
				super.onProgressChanged(view, newProgress);
			}

			// 5.0+ 导入文件
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
				uploadMessage = filePathCallback;
				Intent intent = fileChooserParams.createIntent();
				try {
					startActivityForResult(intent, MainActivity.REQUEST_OPEN);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}

			// 全屏
			@Override
			public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
				if (mCustomView != null) {
					onHideCustomView();
					return;
				}
				mCustomView = view;
				mOriginalOrientation = getRequestedOrientation();
				mCustomViewCallback = callback;
				FrameLayout decor = (FrameLayout) getWindow().getDecorView();
				decor.addView(mCustomView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			}

			// 退出全屏
			@Override
			public void onHideCustomView() {
				FrameLayout decor = (FrameLayout) getWindow().getDecorView();
				decor.removeView(mCustomView);
				mCustomView = null;
				setRequestedOrientation(mOriginalOrientation);
				if (mCustomViewCallback != null) mCustomViewCallback.onCustomViewHidden();
				mCustomViewCallback = null;
			}

			// 小窗打开
			@Override
			public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
				WebView.HitTestResult result = view.getHitTestResult();
				String data = result.getExtra();
				if (data != null && !isDialog) return overrideUrlLoading(view, Uri.parse(data));
				final WebView nwv = new WebView(TWEditorWV.this);
				final AlertDialog dialog = new AlertDialog.Builder(TWEditorWV.this)
						.setView(nwv)
						.setPositiveButton(android.R.string.ok, null)
						.setOnDismissListener(dialog1 -> nwv.destroy())
						.create();
				if (themeColor != null && dialog.getWindow() != null)
					dialog.getWindow().getDecorView().setBackgroundColor(themeColor);
				nwv.setWebViewClient(new WebViewClient() {
					@Override
					public void onPageFinished(WebView view, String url) {
						if (url.startsWith(TWEditorWV.this.uri.toString())) {
							String p = url.substring(url.indexOf('#') + 1);
							wv.evaluateJavascript(STR_JS_POP_PRE + Uri.decode(p) + STR_JS_POP_POST, null);
							dialog.dismiss();
						}
						super.onPageFinished(view, url);
					}

					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						dialog.dismiss();
						return TWEditorWV.this.overrideUrlLoading(view, Uri.parse(url));
					}

					@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
						dialog.dismiss();
						return TWEditorWV.this.overrideUrlLoading(view, request.getUrl());
					}
				});
				WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
				transport.setWebView(nwv);
				resultMsg.sendToTarget();
				dialog.show();
				return true;
			}
		};
		wv.setWebChromeClient(wcc);
		// JS请求处理
		final class JavaScriptCallback {
			@JavascriptInterface
			public void onDecrypted() {
				runOnUiThread(() -> getInfo(wv));
			}

			// 打印
			@JavascriptInterface
			public void print() {
				runOnUiThread(() -> {
					PrintManager printManager = (PrintManager) TWEditorWV.this.getSystemService(Context.PRINT_SERVICE);
					PrintDocumentAdapter printDocumentAdapter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? wv.createPrintDocumentAdapter(getTitle().toString()) : wv.createPrintDocumentAdapter();
					printManager.print(getTitle().toString(), printDocumentAdapter, new PrintAttributes.Builder().build());
				});
			}

			// AndTidWiki fallback
			@JavascriptInterface
			public void saveFile(String pathname, String data) {
				saveWiki(data);
			}

			// 保存文件
			@JavascriptInterface
			public void saveDownload(String data) {
				saveDownload(data, null);
			}

			// 保存文件（指名）
			@JavascriptInterface
			public void saveDownload(String data, String filename) {
				TWEditorWV.exData = data.getBytes(StandardCharsets.UTF_8);
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType(MIME_ANY);
				if (filename != null) intent.putExtra(Intent.EXTRA_TITLE, filename);
				startActivityForResult(intent, KEY_REQ_DOWN);
			}

			// 保存
			@JavascriptInterface
			public void saveWiki(String data) {
				try (ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
						OutputStream os = getContentResolver().openOutputStream(uri)) {
					if (os == null) throw new FileNotFoundException();
					int len = is.available();
					int length, lengthTotal = 0;
					byte[] b = new byte[BUF_SIZE];
					while ((length = is.read(b)) != -1) {

						os.write(b, 0, length);
						lengthTotal += length;
					}
					os.flush();
					if (lengthTotal != len) throw new IOException();
					runOnUiThread(() -> getInfo(wv));
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(TWEditorWV.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
				}
			}

			@JavascriptInterface
			public void exportDB() {
				MainActivity.exportJson(TWEditorWV.this, db);
			}
		}
		wv.addJavascriptInterface(new JavaScriptCallback(), JSI);
		wv.setWebViewClient(new WebViewClient() {
			// KitKat fallback
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url == null) return false;
				return TWEditorWV.this.overrideUrlLoading(view, Uri.parse(url));
			}

			// 跳转处理
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				return TWEditorWV.this.overrideUrlLoading(view, request.getUrl());
			}

			// 加载完成回调
			public void onPageFinished(WebView view, String url) {
				view.evaluateJavascript(STR_JS_PRINT, null);
				view.evaluateJavascript(getString(R.string.js_decrypt), null);
				view.clearHistory();
				if (!URL_BLANK.equals(url)) getInfo(view);
			}
		});
		toolbar.setNavigationOnClickListener(v -> onBackPressed());
		nextWiki(getIntent());
	}

	// 热启动
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Bundle bu;
		String fid;
		JSONObject wl, wa;
		if ((bu = intent.getExtras()) == null || (fid = bu.getString(MainActivity.KEY_ID)) == null)
			return;
		if ((wl = db.optJSONObject(MainActivity.DB_KEY_WIKI)) == null || (wa = wl.optJSONObject(fid)) == null) {
			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			return;
		}
		if (wa == wApp) return;
		wv.evaluateJavascript(getString(R.string.js_exit), value -> confirmAndExit(Boolean.parseBoolean(value), intent));
	}

	// 处理跳转App
	private boolean overrideUrlLoading(final WebView view, Uri u) {
		String sch = u.getScheme();
		if (sch == null || sch.length() == 0)
			return false;
		try {
			final Intent intent;
			switch (sch) {
				case SCH_TEL:
					intent = new Intent(Intent.ACTION_DIAL, u);
					view.getContext().startActivity(intent);
					break;
				case SCH_MAILTO:
					intent = new Intent(Intent.ACTION_SENDTO, u);
					view.getContext().startActivity(intent);
					break;
				case SCH_ABOUT:
				case SCH_FILE:
				case SCH_HTTP:
				case SCH_HTTPS:
					intent = new Intent(Intent.ACTION_VIEW, u);
					view.getContext().startActivity(intent);
					break;
				default:
					intent = new Intent(Intent.ACTION_VIEW, u);
					new AlertDialog.Builder(TWEditorWV.this)
							.setTitle(android.R.string.dialog_alert_title)
							.setMessage(R.string.third_part_rising)
							.setNegativeButton(android.R.string.no, null)
							.setPositiveButton(android.R.string.yes, (dialog, which) -> {
								try {
									view.getContext().startActivity(intent);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}).show();
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	// 读配置 favicon 主题等
	private void getInfo(WebView view) {
		view.evaluateJavascript(getString(R.string.js_info), value -> {
			try {
				JSONArray array = new JSONArray(value);
				if (KEY_ENC.equals(array.getString(2)) || KEY_ENC.equals(array.getString(3)) || KEY_ENC.equals(array.getString(4)))
					return;
				// 解取标题
				String title = array.getString(0), subtitle = array.getString(1);
				TWEditorWV.this.setTitle(title);
				toolbar.setSubtitle(subtitle);
				// appbar隐藏
				hideAppbar = KEY_YES.equals(array.getString(2));
				Configuration newConfig = getResources().getConfiguration();
				toolbar.setVisibility(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || hideAppbar && ready ? View.GONE : View.VISIBLE);
				// 解取主题色
				String color = array.getString(3);
				if (color.length() == 7) themeColor = Color.parseColor(color);
				else themeColor = null;
				TWEditorWV.this.onConfigurationChanged(newConfig);
				// 解取favicon
				String fib64 = array.getString(4);
				byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
				Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
				toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
				// 写Json
				wApp.put(MainActivity.KEY_NAME, title).put(MainActivity.DB_KEY_SUBTITLE, subtitle).put(MainActivity.DB_KEY_COLOR, themeColor).put(MainActivity.KEY_FAVICON, fib64.length() > 0 ? fib64 : null);
				MainActivity.writeJson(TWEditorWV.this, db);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		});
	}

	// 接收导入导出文件
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (resultCode != RESULT_OK) return;
		if (requestCode == KEY_REQ_DOWN) {
			if (exData == null) return;
			Uri uri = resultData.getData();
			if (uri != null)
				try (OutputStream os = getContentResolver().openOutputStream(uri); InputStream is = new ByteArrayInputStream(exData)) {
					if (os == null || exData == null) throw new FileNotFoundException();
					int len = is.available();
					int length;
					int lengthTotal = 0;
					byte[] bytes = new byte[4096];
					while ((length = is.read(bytes)) > -1) {
						os.write(bytes, 0, length);
						lengthTotal += length;
					}
					os.flush();
					if (lengthTotal != len) throw new IOException();


				} catch (Exception e) {
					e.printStackTrace();
					try {
						DocumentsContract.deleteDocument(getContentResolver(), uri);
					} catch (Exception e1) {
						e.printStackTrace();
					}
					Toast.makeText(this, R.string.failed_creating_file, Toast.LENGTH_SHORT).show();
				}
		}
		if (requestCode == MainActivity.REQUEST_OPEN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && uploadMessage != null)
			uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, resultData));
		exData = null;
		uploadMessage = null;
	}

	// 保存提醒
	private void confirmAndExit(boolean dirty, final Intent nextWikiIntent) {
		if (dirty) {
			AlertDialog isExit = new AlertDialog.Builder(TWEditorWV.this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_exit_wiki)
					.setPositiveButton(android.R.string.yes, (dialog, which) -> {
								if (nextWikiIntent == null)
									TWEditorWV.super.onBackPressed();
								else
									nextWiki(nextWikiIntent);
								dialog.dismiss();
							}
					)
					.setNegativeButton(android.R.string.no, null)
					.show();
			isExit.setCanceledOnTouchOutside(false);
		} else {
			if (nextWikiIntent == null)
				TWEditorWV.super.onBackPressed();
			else
				nextWiki(nextWikiIntent);
		}
	}

	// 加载内容
	private void nextWiki(Intent nextWikiIntent) {
		// 读取数据
		final JSONObject wl, wa;
		Bundle bu;
		final String nextWikiId;
		Uri u;
		if ((bu = nextWikiIntent.getExtras()) == null
				|| (nextWikiId = bu.getString(MainActivity.KEY_ID)) == null
				|| nextWikiId.length() == 0
				|| (wl = db.optJSONObject(MainActivity.DB_KEY_WIKI)) == null
				|| (wa = wl.optJSONObject(nextWikiId)) == null) {
			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if ((u = Uri.parse(wa.optString(MainActivity.DB_KEY_URI))) == null || bu.getBoolean(MainActivity.KEY_SHORTCUT) && !MainActivity.isWiki(this, u)) {
			new AlertDialog.Builder(this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_auto_remove_wiki)
					.setNegativeButton(android.R.string.no, null)
					.setPositiveButton(android.R.string.yes, (dialog, which) -> {
						try {
							wl.remove(nextWikiId);
							MainActivity.writeJson(TWEditorWV.this, db);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
								revokeUriPermission(getPackageName(), u, MainActivity.TAKE_FLAGS);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}).setOnDismissListener(dialogInterface -> TWEditorWV.this.finish()).show();
			return;
		}
		// 重置
		if (wv.getUrl() != null) {
			toolbar.setLogo(null);
			setTitle(R.string.app_name);
			toolbar.setSubtitle(null);
			wv.getSettings().setJavaScriptEnabled(false);
			wv.loadUrl(URL_BLANK);
		}
		// 解取Title/Subtitle/favicon
		wApp = wa;
		uri = u;
		if (nextWikiIntent != getIntent()) setIntent(nextWikiIntent);
		String wvTitle = wApp.optString(MainActivity.KEY_NAME, MainActivity.KEY_TW);
		String wvSubTitle = wApp.optString(MainActivity.DB_KEY_SUBTITLE);
		String fib64 = wApp.optString(MainActivity.KEY_FAVICON);
		this.setTitle(wvTitle);
		toolbar.setSubtitle(wvSubTitle.length() > 0 ? wvSubTitle : null);
		if (fib64.length() > 0) {
			byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
			Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
			toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
		}
		try {
			themeColor = wApp.getInt(MainActivity.DB_KEY_COLOR);
		} catch (JSONException e) {
			themeColor = null;
		}
		onConfigurationChanged(getResources().getConfiguration());
		wv.getSettings().setJavaScriptEnabled(true);
		getContentResolver().takePersistableUriPermission(uri, MainActivity.TAKE_FLAGS);  //保持读写权限
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
		else {
			// KitKat fallback
			if (uri == null) {
				wv.loadUrl(MainActivity.STR_EMPTY);
				return;
			}
			String data = null;
			try (BufferedInputStream is = new BufferedInputStream(Objects.requireNonNull(getContentResolver().openInputStream(uri)));
					ByteArrayOutputStream os = new ByteArrayOutputStream(BUF_SIZE)) {   //读全部数据
				int len = is.available();
				int length, lenTotal = 0;
				byte[] b = new byte[BUF_SIZE];
				while ((length = is.read(b)) != -1) {
					os.write(b, 0, length);
					lenTotal += length;
				}
				os.flush();
				if (lenTotal != len) throw new IOException();
				data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(os.toByteArray())).toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			wv.loadDataWithBaseURL(uri.toString(), data != null ? data : MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
		}
	}

	//生成icon
	private BitmapDrawable cIcon(Bitmap icon) {
		Matrix matrix = new Matrix();
		matrix.postScale(scale * 32f / icon.getWidth(), scale * 32f / icon.getHeight());
		Bitmap icons = Bitmap.createBitmap(Math.round(scale * 40f), Math.round(scale * 32f), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(icons);
		c.drawBitmap(icon, matrix, null);
		c.save();
		c.restore();
		return new BitmapDrawable(getResources(), icons);
	}

	// 关闭
	@Override
	public void onBackPressed() {
		if (mCustomView != null)
			wcc.onHideCustomView();
		else if (wv.canGoBack())
			wv.goBack();
		else
			wv.evaluateJavascript(getString(R.string.js_exit), value -> confirmAndExit(Boolean.parseBoolean(value), null));
	}

	// 应用主题
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int primColor = themeColor != null ? themeColor : getResources().getColor(R.color.design_default_color_primary);    // 优先主题色 >> 自动色
		float[] l = new float[3];
		Color.colorToHSV(primColor, l);
		boolean lightBar = themeColor != null ? (l[2] > 0.75) : (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;    // 系统栏模式 根据主题色灰度/日夜模式
		try {
			int bar = 0;
			boolean landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
			toolbar.setVisibility(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || hideAppbar && ready ? View.GONE : View.VISIBLE);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				getWindow().setStatusBarColor(primColor);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					getWindow().setNavigationBarColor(primColor);
				bar = lightBar ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0) : View.SYSTEM_UI_FLAG_VISIBLE;
			}
			getWindow().getDecorView().setSystemUiVisibility(bar | (landscape ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : View.SYSTEM_UI_FLAG_VISIBLE));
			findViewById(R.id.wv_appbar).setBackgroundColor(primColor);
			toolbar.setTitleTextAppearance(this, R.style.Toolbar_TitleText);
			toolbar.setSubtitleTextAppearance(this, R.style.TextAppearance_AppCompat_Small);
			if (themeColor != null) {    // 有主题色则根据灰度切换字色
				toolbar.setTitleTextColor(getResources().getColor(lightBar ? R.color.content_tint_l : R.color.content_tint_d));
				toolbar.setSubtitleTextColor(getResources().getColor(lightBar ? R.color.content_sub_l : R.color.content_sub_d));
			}
			toolbar.setNavigationIcon(themeColor != null ? (lightBar ? R.drawable.ic_arrow_back_l : R.drawable.ic_arrow_back_d) : R.drawable.ic_arrow_back);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// WebView清理
	@Override
	protected void onDestroy() {
		if (wv != null) {
			ViewParent parent = wv.getParent();
			if (parent != null) ((ViewGroup) parent).removeView(wv);
			wv.stopLoading();
			wv.getSettings().setJavaScriptEnabled(false);
			wv.removeJavascriptInterface(JSI);
			wv.clearHistory();
			wv.loadUrl(URL_BLANK);
			wv.removeAllViews();
			wv.destroyDrawingCache();
			wv.destroy();
			wv = null;
		}
		super.onDestroy();
	}
}