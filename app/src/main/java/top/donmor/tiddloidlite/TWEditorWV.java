/*
 * top.donmor.tiddloidlite.TWEditorWV <= [P|Tiddloid Lite]
 * Last modified: 14:59:19 2021/06/08
 * Copyright (c) 2022 donmor
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
import android.os.Bundle;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

public class TWEditorWV extends AppCompatActivity {

	// 定义属性
	private JSONObject db, wApp;
	private WebChromeClient wcc;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	private int mOriginalOrientation;
	private Integer themeColor = null;
	private boolean hideAppbar = false, ready = false, firstRun;
	private float scale;
	private ValueCallback<Uri[]> uploadMessage;
	private WebView wv;
	private Toolbar toolbar;
	private ProgressBar wvProgress;
	private Uri uri = null;
	private byte[] exData = null;
	private String id;
	private ActivityResultLauncher<Intent> getChooserDL, getChooserImport;

	// 常量
	private static final String
			JSI = "twi",
			MIME_ANY = "*/*",
			KEY_ENC = "enc",
			KEY_YES = "yes",
			SCH_ABOUT = "about",
			SCH_FILE = "file",
			SCH_HTTP = "http",
			SCH_HTTPS = "https",
			SCH_TEL = "tel",
			SCH_MAILTO = "mailto",
			URL_BLANK = "about:blank";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 全局设定
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.tweditor);
		// 初始化顶栏
		toolbar = findViewById(R.id.wv_toolbar);
		setSupportActionBar(toolbar);
		toolbar.setNavigationOnClickListener(v -> onBackPressed());
		this.setTitle(R.string.app_name);
		onConfigurationChanged(getResources().getConfiguration());
		wvProgress = findViewById(R.id.progressBar);
		wvProgress.setMax(100);
		// 初始化WebView
		LinearLayout wrapper = findViewById(R.id.wv_wrapper);
		wv = new WebView(this);
		wv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
		wrapper.addView(wv);
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
		getChooserDL = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			if (exData == null) return;
			if (result.getData() != null) {
				uri = result.getData().getData();
				if (uri == null) return;
				try (ParcelFileDescriptor ofd = Objects.requireNonNull(getContentResolver().openFileDescriptor(uri, MainActivity.KEY_FD_W));
						FileOutputStream os = new FileOutputStream(ofd.getFileDescriptor());
						FileChannel oc = os.getChannel()) {
					MainActivity.ba2fc(exData, oc);
				} catch (NullPointerException | IOException | NonWritableChannelException e) {
					e.printStackTrace();
					try {
						DocumentsContract.deleteDocument(getContentResolver(), uri);
					} catch (FileNotFoundException e1) {
						e.printStackTrace();
					}
					Toast.makeText(this, R.string.failed_creating_file, Toast.LENGTH_SHORT).show();
				}
			}
			exData = null;
		});
		getChooserImport = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			if (uploadMessage != null)
				uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData()));
			uploadMessage = null;
		});
		wcc = new WebChromeClient() {
			// 进度条
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				ready = newProgress == 100;
				toolbar.setVisibility(hideAppbar && ready ? View.GONE : View.VISIBLE);
				wvProgress.setVisibility(ready ? View.GONE : View.VISIBLE);
				wvProgress.setProgress(newProgress);
				super.onProgressChanged(view, newProgress);
			}

			// 5.0+ 导入文件
			@Override
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
				uploadMessage = filePathCallback;
				getChooserImport.launch(fileChooserParams.createIntent());
				return true;
			}

			// 全屏
			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
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
						.setOnDismissListener(dialog1 -> {
							nwv.loadDataWithBaseURL(null, MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
							nwv.clearHistory();
							((ViewGroup) nwv.getParent()).removeView(nwv);
							nwv.removeAllViews();
							nwv.destroy();
						})
						.create();
				if (themeColor != null && dialog.getWindow() != null)
					dialog.getWindow().getDecorView().setBackgroundColor(themeColor);
				nwv.setWebViewClient(new WebViewClient() {
					@Override
					public void onPageFinished(WebView view, String url) {
						Uri u1;
						String p;
						if ((u1 = Uri.parse(url)).getSchemeSpecificPart().equals(Uri.parse(wv.getUrl()).getSchemeSpecificPart()) && (p = u1.getFragment()) != null) {
							wv.evaluateJavascript(getString(R.string.js_pop, Uri.decode(p)), null);
							dialog.dismiss();
						}
						super.onPageFinished(view, url);
					}

					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						dialog.dismiss();
						return TWEditorWV.this.overrideUrlLoading(view, Uri.parse(url));
					}

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
		wv.addJavascriptInterface(new Object() {
			@JavascriptInterface
			public void onDecrypted() {
				runOnUiThread(() -> getInfo(wv));
			}

			// 打印
			@JavascriptInterface
			public void print() {
				runOnUiThread(() -> {
					PrintManager printManager = (PrintManager) TWEditorWV.this.getSystemService(Context.PRINT_SERVICE);
					PrintDocumentAdapter printDocumentAdapter = wv.createPrintDocumentAdapter(getTitle().toString());
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
				exData = data.getBytes(StandardCharsets.UTF_8);
				getChooserDL.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT)
						.addCategory(Intent.CATEGORY_OPENABLE)
						.setType(MIME_ANY)
						.putExtra(Intent.EXTRA_TITLE, filename));
			}

			// 保存
			@JavascriptInterface
			public void saveWiki(String data) {
				try (ParcelFileDescriptor ofd = Objects.requireNonNull(getContentResolver().openFileDescriptor(uri, MainActivity.KEY_FD_W));
						FileOutputStream os = new FileOutputStream(ofd.getFileDescriptor());
						FileChannel oc = os.getChannel()) {
					MainActivity.ba2fc(data.getBytes(StandardCharsets.UTF_8), oc);
					runOnUiThread(() -> getInfo(wv));
				} catch (NullPointerException | IOException | NonWritableChannelException e) {
					e.printStackTrace();
					Toast.makeText(TWEditorWV.this, R.string.failed, Toast.LENGTH_SHORT).show();
				}
			}

			@JavascriptInterface
			public void exportDB() {
				MainActivity.exportJson(TWEditorWV.this, db);
			}
		}, JSI);
		wv.setWebViewClient(new WebViewClient() {
			// 跳转处理
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				return TWEditorWV.this.overrideUrlLoading(view, request.getUrl());
			}

			// 加载完成回调
			public void onPageFinished(WebView view, String url) {
				view.evaluateJavascript(getString(R.string.js_print), null);
				view.evaluateJavascript(getString(R.string.js_is_wiki), value -> {
					if (Boolean.parseBoolean(value)) getInfo(view);
					else if (!URL_BLANK.equals(url)) {
						if (wApp == null) notWikiConfirm();
						else {
							try {
								autoRemoveConfirm(db.getJSONObject(MainActivity.DB_KEY_WIKI), id, Uri.parse(wApp.optString(MainActivity.DB_KEY_URI)));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
				view.clearHistory();
			}
		});
		// 初始化db
		try {
			db = MainActivity.readJson(this);
			if (!db.has(MainActivity.DB_KEY_WIKI)) throw new JSONException(MainActivity.EXCEPTION_JSON_DATA_ERROR);
		} catch (JSONException e) {
			e.printStackTrace();
			try {
				db = MainActivity.initJson(TWEditorWV.this);    // 初始化JSON数据，如果加载失败
				MainActivity.writeJson(TWEditorWV.this, db);
				firstRun = true;
			} catch (JSONException e1) {
				e1.printStackTrace();
				Toast.makeText(TWEditorWV.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}
		MainActivity.trimDB120(this, db);
		nextWiki(getIntent());
		if (firstRun)
			MainActivity.firstRunReq(this);
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
					try {
						view.getContext().startActivity(intent);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
					break;
				case SCH_MAILTO:
					intent = new Intent(Intent.ACTION_SENDTO, u);
					try {
						view.getContext().startActivity(intent);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
					break;
				case SCH_ABOUT:
				case SCH_FILE:
				case SCH_HTTP:
				case SCH_HTTPS:
					intent = new Intent(Intent.ACTION_VIEW, u);
					try {
						view.getContext().startActivity(intent);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
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
				toolbar.setVisibility(hideAppbar && ready ? View.GONE : View.VISIBLE);
				// 解取主题色
				String color = array.getString(3);
				float[] l = new float[3];
				if (color.length() == 7) {
					themeColor = Color.parseColor(color);
					Color.colorToHSV(themeColor, l);
				} else themeColor = null;
				getDelegate().setLocalNightMode(themeColor == null ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : l[2] > 0.75 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);   // 系统栏模式 根据主题色灰度/日夜模式
				// 解取favicon
				String fib64 = array.getString(4);
				byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
				Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
				toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
				// 写Json
				wApp.put(MainActivity.KEY_NAME, title).put(MainActivity.DB_KEY_SUBTITLE, subtitle).put(MainActivity.DB_KEY_COLOR, themeColor).put(MainActivity.KEY_FAVICON, fib64.length() > 0 ? fib64 : null);
				MainActivity.writeJson(TWEditorWV.this, db);
				onConfigurationChanged(newConfig);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		});
	}

	// 保存提醒
	private void confirmAndExit(boolean dirty, final Intent nextWikiIntent) {
		if (dirty) {
			AlertDialog isExit = new AlertDialog.Builder(TWEditorWV.this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_exit_wiki)
					.setPositiveButton(android.R.string.yes, (dialog, which) -> {
								dialog.dismiss();
								if (nextWikiIntent == null)
									TWEditorWV.super.finish();
								else
									nextWiki(nextWikiIntent);
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

	private void notWikiConfirm() {
		new AlertDialog.Builder(TWEditorWV.this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.not_a_wiki_page)
				.setPositiveButton(android.R.string.ok, null)
				.setOnDismissListener(dialog -> TWEditorWV.this.finish())
				.show();
	}

	private void autoRemoveConfirm(JSONObject wl, String id, Uri u) {
		AlertDialog confirmAutoRemove = new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.confirm_to_auto_remove_wiki)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.yes, (dialog, which) -> {
					try {
						wl.remove(id);
						MainActivity.writeJson(TWEditorWV.this, db);
						if (MainActivity.APIOver26 && u != null) revokeUriPermission(getPackageName(), u, MainActivity.TAKE_FLAGS);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}).setOnDismissListener(dialogInterface -> TWEditorWV.this.finish())
				.create();
		confirmAutoRemove.setOnShowListener(dialog1 -> {
			Window w;
			if ((w = confirmAutoRemove.getWindow()) != null)
				w.getDecorView().setLayoutDirection(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()));
		});
		confirmAutoRemove.show();
	}

	// 加载内容
	private void nextWiki(Intent nextWikiIntent) {
		// 读取数据
		final JSONObject wl;
		try {
			wl = db.getJSONObject(MainActivity.DB_KEY_WIKI);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		final JSONObject wa;
		Bundle bu;
		final String nextWikiId;
		final Uri u;
		if ((bu = nextWikiIntent.getExtras()) == null
				|| (nextWikiId = bu.getString(MainActivity.KEY_ID)) == null
				|| nextWikiId.length() == 0
				|| (wa = wl.optJSONObject(nextWikiId)) == null) {
			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if ((u = Uri.parse(wa.optString(MainActivity.DB_KEY_URI))) == null) {
			autoRemoveConfirm(wl, nextWikiId, null);
			return;
		}
		// 重置
		if (wv.getUrl() != null) {
			toolbar.setLogo(null);
			setTitle(R.string.app_name);
			toolbar.setSubtitle(null);
			wv.getSettings().setJavaScriptEnabled(false);
			wv.loadUrl(URL_BLANK);
			themeColor = null;
			hideAppbar = false;
			onConfigurationChanged(getResources().getConfiguration());
		}
		// 解取Title/Subtitle/favicon
		wApp = wa;
		uri = u;
		id = nextWikiId;
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
		float[] l = new float[3];
		try {
			themeColor = wApp.getInt(MainActivity.DB_KEY_COLOR);
			Color.colorToHSV(themeColor, l);
		} catch (JSONException e) {
			themeColor = null;
		}
		getDelegate().setLocalNightMode(themeColor == null ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : l[2] > 0.75 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);   // 系统栏模式 根据主题色灰度/日夜模式
		onConfigurationChanged(getResources().getConfiguration());
		wv.getSettings().setJavaScriptEnabled(true);
		try {
			getContentResolver().takePersistableUriPermission(uri, MainActivity.TAKE_FLAGS);  //保持读写权限
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
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
		else
			wv.evaluateJavascript(getString(R.string.js_exit), value -> confirmAndExit(Boolean.parseBoolean(value), null));
	}

	// 应用主题
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int primColor = themeColor != null ? themeColor : getResources().getColor(R.color.design_default_color_primary);    // 优先主题色 >> 自动色
		float[] l = new float[3];
		if (themeColor != null) Color.colorToHSV(primColor, l);
		boolean lightBar = themeColor != null ? (l[2] > 0.75) : (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;    // 系统栏模式 根据主题色灰度/日夜模式
		toolbar.setVisibility(wApp != null && hideAppbar && ready ? View.GONE : View.VISIBLE);
		Window window = getWindow();
		WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(window, window.getDecorView());
		if (MainActivity.APIOver23)
			window.setStatusBarColor(primColor);
		if (MainActivity.APIOver26)
			window.setNavigationBarColor(primColor);
		wic.setAppearanceLightNavigationBars(lightBar);
		wic.setAppearanceLightStatusBars(lightBar);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			wic.hide(WindowInsetsCompat.Type.systemBars());
			wic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
			WindowCompat.setDecorFitsSystemWindows(window, false);
		} else {
			wic.show(WindowInsetsCompat.Type.systemBars());
			WindowCompat.setDecorFitsSystemWindows(window, true);
		}
		findViewById(R.id.wv_appbar).setBackgroundColor(primColor);
		toolbar.setTitleTextAppearance(this, R.style.Toolbar_TitleText);
		toolbar.setSubtitleTextAppearance(this, R.style.TextAppearance_AppCompat_Small);
		toolbar.setNavigationIcon(MainActivity.APIOver24 || lightBar ? R.drawable.ic_arrow_back : R.drawable.ic_arrow_back_d);
		if (MainActivity.APIOver29 && wv != null) wv.getSettings().setForceDark(lightBar ? WebSettings.FORCE_DARK_OFF : WebSettings.FORCE_DARK_ON);
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
			wv.loadDataWithBaseURL(null, MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
			wv.clearHistory();
			wv.removeAllViews();
			wv.destroyDrawingCache();
			wv.destroy();
			wv = null;
		}
		super.onDestroy();
	}
}