/*
 * top.donmor.tiddloidlite.TWEditorWV <= [P|Tiddloid Lite]
 * Last modified: 18:33:05 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.content.DialogInterface;
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

import static top.donmor.tiddloidlite.MainActivity.KEY_SHORTCUT;
import static top.donmor.tiddloidlite.MainActivity.TAKE_FLAGS;

//import android.util.Log;

public class TWEditorWV extends AppCompatActivity {

	// 定义属性
	private JSONObject db;
	private JSONObject wApp;
	private WebChromeClient wcc;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	private int mOriginalOrientation;
	private Integer themeColor = null;
	//	private Integer themeColor = Color.argb(255, 243, 243, 183);
	private float scale;
	private ValueCallback<Uri[]> uploadMessage;
	private WebView wv;
	private Toolbar toolbar;
	private ProgressBar wvProgress;
	//	private String id;
	private Uri uri = null;
	private static byte[] exData = null;

	// 常量
	private static final String
			JSI = "twi",
			MIME_ANY = "*/*",
			KEY_HASH_1 = "(function(){new $tw.Story().navigateTiddler(\"",
			KEY_HASH_2 = "\");})();",
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
		// 硬件加速
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.tweditor);
		// 读数据
		try {
			db = MainActivity.readJson(this);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
//		MainActivity.trimDB115(this, db);
		// 初始化顶栏
		toolbar = findViewById(R.id.wv_toolbar);
		setSupportActionBar(toolbar);
		this.setTitle(R.string.app_name);
		wvProgress = findViewById(R.id.progressBar);
		wvProgress.setMax(100);
//		onConfigurationChanged(getResources().getConfiguration());
		// 初始化WebView
		wv = findViewById(R.id.twWebView);
		WebSettings wvs = wv.getSettings();
		wvs.setJavaScriptEnabled(true);
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
		scale = getResources().getDisplayMetrics().density;
		wcc = new WebChromeClient() {
			// 进度条
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (newProgress == 100) {
					wvProgress.setVisibility(View.GONE);
				} else {
					wvProgress.setVisibility(View.VISIBLE);
					wvProgress.setProgress(newProgress);
				}
				super.onProgressChanged(view, newProgress);
			}

			// 5.0+ 导入文件
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
				uploadMessage = filePathCallback;
				Intent intent = fileChooserParams.createIntent();
				try {
					startActivityForResult(intent, 40);
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
				if (data != null) return overrideUrlLoading(view, Uri.parse(data));
				final WebView nwv = new WebView(TWEditorWV.this);
				final AlertDialog dialog = new AlertDialog.Builder(TWEditorWV.this).setView(nwv).setPositiveButton(android.R.string.ok, null).setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						nwv.destroy();
					}
				}).create();
				nwv.setWebViewClient(new WebViewClient() {

					//					@Override
//					public void onLoadResource(WebView view, String url) {
//						System.out.println("------------------------");
//						System.out.println(url);
////						if (URL_BLANK.equals(url))new AlertDialog.Builder(TWEditorWV.this).setView(view).setPositiveButton(android.R.string.ok,null).show();
////						dialog.dismiss();
//						super.onLoadResource(view, url);
//					}
//
//					@Override
//					public void onPageStarted(WebView view, String url, Bitmap favicon) {
//						System.out.println(1);
//						System.out.println(url);
////						if (URL_BLANK.equals(url))new AlertDialog.Builder(TWEditorWV.this).setView(view).setPositiveButton(android.R.string.ok,null).show();
//////						dialog.dismiss();
//						super.onPageStarted(view, url, favicon);
//					}
////
////
					@Override
					public void onPageFinished(WebView view, String url) {
						if (url.startsWith(TWEditorWV.this.uri.toString())) {
							String p = url.substring(url.indexOf('#') + 1);
							wv.evaluateJavascript(KEY_HASH_1 + Uri.decode(p) + KEY_HASH_2, null);
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
//				nwv.setWebChromeClient(this);
				WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
				transport.setWebView(nwv);
				resultMsg.sendToTarget();
				dialog.show();
				return true;
//				return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
			}
		};
		wv.setWebChromeClient(wcc);
		// JS请求处理
		final class JavaScriptCallback {
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
				ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
				OutputStream os = null;
				try {
					os = getContentResolver().openOutputStream(uri);
					if (os == null)
						throw new FileNotFoundException();
					int len = is.available();
					int length, lengthTotal = 0;
					byte[] b = new byte[BUF_SIZE];
//					ByteBuffer b = ByteBuffer.allocate(BUF_SIZE);
					while ((length = is.read(b)) != -1) {

						os.write(b, 0, length);
						lengthTotal += length;
					}
					os.flush();
					os.close();
					os = null;
					if (lengthTotal != len) throw new IOException();
//					MainActivity.TWInfo info = new MainActivity.TWInfo(TWEditorWV.this, uri);//TODO: Refactor
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							getInfo(wv);
						}
					});
//					runOnUiThread(new Runnable() {
//						@Override
//						public void run() {
//							if (info.favicon != null) {
//								toolbar.setLogo(cIcon(info.favicon));
//							} else {
//								toolbar.setLogo(null);
//							}
//							MainActivity.updateIcon(TWEditorWV.this, info.favicon, id);
//							TWEditorWV.this.setTitle(info.title);
//							toolbar.setSubtitle(info.subtitle);
//						}
//					});
//					wApp.put(MainActivity.KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getString(R.string.tiddlywiki));
//					wApp.put(MainActivity.DB_KEY_SUBTITLE, (info.subtitle != null && info.subtitle.length() > 0) ? info.subtitle : MainActivity.STR_EMPTY);
//					MainActivity.writeJson(TWEditorWV.this, db);

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(TWEditorWV.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
				} finally {
					try {
						is.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (os != null)
						try {
							os.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
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
				Uri u = Uri.parse(url);
				return TWEditorWV.this.overrideUrlLoading(view, u);
			}

			// Lollipop+ entry
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				Uri u = request.getUrl();
				return TWEditorWV.this.overrideUrlLoading(view, u);
//				String sch = u.getScheme();
//				if (sch == null || sch.length() == 0)
//					return false;
//				try {
//					final Intent intent;
//					switch (sch) {
//						case SCH_TEL:
//							intent = new Intent(Intent.ACTION_DIAL, u);
//							view.getContext().startActivity(intent);
//							break;
//						case SCH_MAILTO:
//							intent = new Intent(Intent.ACTION_SENDTO, u);
//							view.getContext().startActivity(intent);
//							break;
//						case SCH_ABOUT:
//						case SCH_FILE:
//						case SCH_HTTP:
//						case SCH_HTTPS:
//							intent = new Intent(Intent.ACTION_VIEW, u);
//							view.getContext().startActivity(intent);
//							break;
//						default:
//							intent = new Intent(Intent.ACTION_VIEW, u);
//							new AlertDialog.Builder(TWEditorWV.this)
//									.setTitle(android.R.string.dialog_alert_title)
//									.setMessage(R.string.third_part_rising)
//									.setNegativeButton(android.R.string.no, null)
//									.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											try {
//												view.getContext().startActivity(intent);
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										}
//									}).show();
//							break;
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				return true;
			}

			public void onPageFinished(WebView view, String url) {
				view.clearHistory();
				if (!URL_BLANK.equals(url)) getInfo(view);
			}
		});
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		nextWiki(getIntent());
//		// 读请求数据
//		Bundle bu = getIntent().getExtras();
//		id = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
//		boolean shortcut = bu != null && bu.getBoolean(MainActivity.KEY_SHORTCUT);
//		final JSONObject wl;
//		if (id == null || id.length() == 0 || (wl = db.optJSONObject("DKW2")) == null || (wApp = wl.optJSONObject(id)) == null) {
//			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
//			finish();
//			return;
//		}
//		uri = Uri.parse(wApp.optString(MainActivity.DB_KEY_URI));
//		if (shortcut && !new MainActivity.TWInfo(this, uri).isWiki) {//TODO: 自动移除机制重构
//			uri = null;
//			new AlertDialog.Builder(this)
//					.setTitle(android.R.string.dialog_alert_title)
//					.setMessage(R.string.confirm_to_auto_remove_wiki)
//					.setNegativeButton(android.R.string.no, null)
//					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							try {
//								wl.remove(id);
//								MainActivity.writeJson(TWEditorWV.this, db);
//								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//									revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//					}).setOnDismissListener(new DialogInterface.OnDismissListener() {
//				@Override
//				public void onDismiss(DialogInterface dialogInterface) {
//					finish();
//				}
//			}).show();
//		}
//		String wvTitle = wApp.optString(MainActivity.KEY_NAME);
//		String wvSubTitle = wApp.optString(MainActivity.DB_KEY_SUBTITLE);
//		String fib64 = wApp.optString(MainActivity.KEY_FAVICON);
//		// 解取Title/Subtitle/favicon
//		if (wvTitle.length() > 0) this.setTitle(wvTitle);
//		if (wvSubTitle.length() > 0) toolbar.setSubtitle(wvSubTitle);
//		if (wvSubTitle.length() > 0) {
//			byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
//			Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
//			toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
//		}
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//			wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
//		else {
//			// KitKat fallback
//			if (uri == null) {
//				wv.loadUrl(MainActivity.STR_EMPTY);
//				return;
//			}
//			BufferedInputStream is = null;
//			ByteArrayOutputStream os = null;
//			String data = null;
//			try {
//				is = new BufferedInputStream(Objects.requireNonNull(getContentResolver().openInputStream(uri)));
//				os = new ByteArrayOutputStream(BUF_SIZE);   //读全部数据
//				int len = is.available();
//				int length, lenTotal = 0;
//				byte[] b = new byte[BUF_SIZE];
//				while ((length = is.read(b)) != -1) {
//					os.write(b, 0, length);
//					lenTotal += length;
//				}
//				os.flush();
//				if (lenTotal != len) throw new IOException();
//				is.close();
//				os.close();
//				data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(os.toByteArray())).toString();
//				try {  //保持读写权限
//					getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (is != null) try {
//					is.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (os != null) try {
//					os.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			wv.loadDataWithBaseURL(uri.toString(), data != null ? data : MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
//		}
//		// 读请求数据
//		Bundle bu = getIntent().getExtras();
//		String wvTitle = null, wvSubTitle = null, fib64 = null;
//		id = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
//		boolean shortcut = bu != null && bu.getBoolean(MainActivity.KEY_SHORTCUT);
//		try {
//			if (id != null && id.length() > 0) {
//				final JSONObject wl = db.getJSONObject("DKW2");
//				wApp = wl.getJSONObject(id);
////				for (int i = 0; i < wl.length(); i++) {
////					wApp = wl.getJSONObject(i);
////					if (id.equals(wApp.getString(MainActivity.KEY_ID))) {
//				uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
////						final int p = i;
//				if (shortcut && !new MainActivity.TWInfo(this, uri).isWiki) {//TODO: 自动移除机制重构
//					uri = null;
//					new AlertDialog.Builder(this)
//							.setTitle(android.R.string.dialog_alert_title)
//							.setMessage(R.string.confirm_to_auto_remove_wiki)
//							.setNegativeButton(android.R.string.no, null)
//							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialog, int which) {
//									try {
//										wl.remove(id);
//										MainActivity.writeJson(TWEditorWV.this, db);
//										if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//											revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
//									} catch (Exception e) {
//										e.printStackTrace();
//									}
//								}
//							}).setOnDismissListener(new DialogInterface.OnDismissListener() {
//						@Override
//						public void onDismiss(DialogInterface dialogInterface) {
//							finish();
//						}
//					}).show();
//				}
////						try {
//				wvTitle = wApp.optString(MainActivity.KEY_NAME, null);
//				wvSubTitle = wApp.optString(MainActivity.DB_KEY_SUBTITLE, null);
//				fib64 = wApp.optString(MainActivity.KEY_FAVICON, null);
////						} catch (Exception e) {
////							e.printStackTrace();
////						}
////						break;
////					} else if (i == wl.length() - 1)
////						throw new IOException();
////				}
////				final JSONArray wl = db.getJSONArray(MainActivity.DB_KEY_WIKI);
////				for (int i = 0; i < wl.length(); i++) {
////					wApp = wl.getJSONObject(i);
////					if (id.equals(wApp.getString(MainActivity.KEY_ID))) {
////						uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
////						final int p = i;
////						if (shortcut && !new MainActivity.TWInfo(this, uri).isWiki) {//TODO: 自动移除机制重构
////							uri = null;
////							new AlertDialog.Builder(this)
////									.setTitle(android.R.string.dialog_alert_title)
////									.setMessage(R.string.confirm_to_auto_remove_wiki)
////									.setNegativeButton(android.R.string.no, null)
////									.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
////										@Override
////										public void onClick(DialogInterface dialog, int which) {
////											try {
////												wl.remove(p);
////												MainActivity.writeJson(TWEditorWV.this, db);
////												if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
////													revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
////											} catch (Exception e) {
////												e.printStackTrace();
////											}
////										}
////									}).setOnDismissListener(new DialogInterface.OnDismissListener() {
////								@Override
////								public void onDismiss(DialogInterface dialogInterface) {
////									finish();
////								}
////							}).show();
////						}
////						try {
////							wvTitle = wApp.getString(MainActivity.KEY_NAME);
////							wvSubTitle = wApp.getString(MainActivity.DB_KEY_SUBTITLE);
////							fib64 = wApp.getString(MainActivity.KEY_FAVICON);
////						} catch (Exception e) {
////							e.printStackTrace();
////						}
////						break;
////					} else if (i == wl.length() - 1)
////						throw new IOException();
////				}
//			} else throw new IOException();
//		} catch (Exception e) {
//			e.printStackTrace();
//			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
//			finish();
//		}
//		try {
//			if (wvTitle != null && wvTitle.length() > 0) this.setTitle(wvTitle);
//			if (wvSubTitle != null && wvSubTitle.length() > 0) toolbar.setSubtitle(wvSubTitle);
//			// 解取favicon
//			if (fib64 != null) {
//				byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
//				Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
//				toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
//			}
////			InputStream is = null;
////			File fi = new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id);
////			if (fi.canRead()) try {
////				is = new FileInputStream(fi);
////				Bitmap icon = BitmapFactory.decodeStream(is);
////				if (icon != null) toolbar.setLogo(cIcon(icon));
////			} catch (Exception e) {
////				e.printStackTrace();
////			} finally {
////				if (is != null) try {
////					is.close();
////				} catch (Exception e) {
////					e.printStackTrace();
////				}
////			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//			wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
//		else {
//			// KitKat fallback
//			if (uri == null) {
//				wv.loadUrl(MainActivity.STR_EMPTY);
//				return;
//			}
//			BufferedInputStream is = null;
//			ByteArrayOutputStream os = null;
//			String data = null;
//			try {
//				is = new BufferedInputStream(Objects.requireNonNull(getContentResolver().openInputStream(uri)));
//				os = new ByteArrayOutputStream(BUF_SIZE);   //读全部数据
//				int len = is.available();
//				int length, lenTotal = 0;
//				byte[] b = new byte[BUF_SIZE];
//				while ((length = is.read(b)) != -1) {
//					os.write(b, 0, length);
//					lenTotal += length;
//				}
//				os.flush();
//				if (lenTotal != len) throw new IOException();
//				is.close();
//				os.close();
//				data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(os.toByteArray())).toString();
//				try {  //保持读写权限
//					getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (is != null) try {
//					is.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (os != null) try {
//					os.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			wv.loadDataWithBaseURL(uri.toString(), data != null ? data : MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
//		}
	}

	// 热启动
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
//		Bundle bu = intent.getExtras();
//		String fid;
//		fid = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
//		if (bu == null || (fid = bu.getString(MainActivity.KEY_ID)) == null) return;
//		JSONObject w = null;

//		try {
//			JSONArray wl = db.getJSONArray(MainActivity.DB_KEY_WIKI);
//			for (int i = 0; i < wl.length(); i++) {
//				w = wl.getJSONObject(i);
//				if (w.getString(MainActivity.KEY_ID).equals(fid)) {
//					break;
//				}
//			}
//			if (fid.equals(id)) return;
//			Uri u = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
//			getContentResolver().takePersistableUriPermission(u, TAKE_FLAGS);
//			if (new MainActivity.TWInfo(this, u).isWiki) {
		wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
			@Override
			public void onReceiveValue(String value) {
				confirmAndExit(Boolean.parseBoolean(value), intent);
			}
		});
//			} else {
//				new AlertDialog.Builder(this)
//						.setTitle(android.R.string.dialog_alert_title)
//						.setMessage(R.string.confirm_to_auto_remove_wiki)
//						.setNegativeButton(android.R.string.no, null)
//						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								try {
//									db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
//									MainActivity.writeJson(TWEditorWV.this, db);
//									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//										revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							}
//						}).show();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
//		}
//			else if (!fid.equals(id)) {
//				try {
//					final Uri u = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
//					getContentResolver().takePersistableUriPermission(u, TAKE_FLAGS);
//					if (new MainActivity.TWInfo(this, u).isWiki) {
//						final int vs = ser;
//						wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
//							@Override
//							public void onReceiveValue(String value) {
//								confirmAndExit(Boolean.parseBoolean(value), intent, vs);
//							}
//						});
//					} else {
//						final int p = ser;
//						new AlertDialog.Builder(this)
//								.setTitle(android.R.string.dialog_alert_title)
//								.setMessage(R.string.confirm_to_auto_remove_wiki)
//								.setNegativeButton(android.R.string.no, null)
//								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//									@Override
//									public void onClick(DialogInterface dialog, int which) {
//										try {
//											db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
//											MainActivity.writeJson(TWEditorWV.this, db);
//											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//												revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
//										} catch (Exception e) {
//											e.printStackTrace();
//										}
//									}
//								}).show();
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
	}
//	// 热启动
//	@Override
//	protected void onNewIntent(final Intent intent) {
//		super.onNewIntent(intent);
//		Bundle bu = intent.getExtras();
//		String fid = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
//		if (fid == null) return;
//		int ser = -1;
//		JSONObject w = null;
//		try {
//			JSONArray wl = db.getJSONArray(MainActivity.DB_KEY_WIKI);
//			for (int i = 0; i < wl.length(); i++) {
//				w = wl.getJSONObject(i);
//				if (w.getString(MainActivity.KEY_ID).equals(fid)) {
//					ser = i;
//					break;
//				}
//			}
//			if (ser == -1) throw new IOException();
//			if (fid.equals(id)) return;
//			final Uri u = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
//			getContentResolver().takePersistableUriPermission(u, TAKE_FLAGS);
//			if (new MainActivity.TWInfo(this, u).isWiki) {
//				final int vs = ser;
//				wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
//					@Override
//					public void onReceiveValue(String value) {
//						confirmAndExit(Boolean.parseBoolean(value), intent, vs);
//					}
//				});
//			} else {
//				final int p = ser;
//				new AlertDialog.Builder(this)
//						.setTitle(android.R.string.dialog_alert_title)
//						.setMessage(R.string.confirm_to_auto_remove_wiki)
//						.setNegativeButton(android.R.string.no, null)
//						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								try {
//									db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
//									MainActivity.writeJson(TWEditorWV.this, db);
//									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//										revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							}
//						}).show();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
//		}
////			else if (!fid.equals(id)) {
////				try {
////					final Uri u = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
////					getContentResolver().takePersistableUriPermission(u, TAKE_FLAGS);
////					if (new MainActivity.TWInfo(this, u).isWiki) {
////						final int vs = ser;
////						wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
////							@Override
////							public void onReceiveValue(String value) {
////								confirmAndExit(Boolean.parseBoolean(value), intent, vs);
////							}
////						});
////					} else {
////						final int p = ser;
////						new AlertDialog.Builder(this)
////								.setTitle(android.R.string.dialog_alert_title)
////								.setMessage(R.string.confirm_to_auto_remove_wiki)
////								.setNegativeButton(android.R.string.no, null)
////								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
////									@Override
////									public void onClick(DialogInterface dialog, int which) {
////										try {
////											db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
////											MainActivity.writeJson(TWEditorWV.this, db);
////											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
////												revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
////										} catch (Exception e) {
////											e.printStackTrace();
////										}
////									}
////								}).show();
////					}
////				} catch (Exception e) {
////					e.printStackTrace();
////				}
////			}
//	}

//	// 加载内容
//	private void nextWiki(Intent nextWikiIntent, int nextWikiSerial) {
//		// 重置
//		toolbar.setLogo(null);
//		setTitle(R.string.app_name);
//		toolbar.setSubtitle(null);
//		wApp = null;
//		uri = null;
//		wv.getSettings().setJavaScriptEnabled(false);
//		wv.loadUrl(URL_BLANK);
//		// 读取数据
//		setIntent(nextWikiIntent);
//		String wvTitle = null, wvSubTitle = null, fib64 = null;
//		try {
//			wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(nextWikiSerial);
//			if (wApp == null) throw new IOException();
//			id = wApp.getString(MainActivity.KEY_ID);
//			uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
////			try {
//			wvTitle = wApp.optString(MainActivity.KEY_NAME);
//			wvSubTitle = wApp.optString(MainActivity.DB_KEY_SUBTITLE);
//			fib64 = wApp.optString(MainActivity.KEY_FAVICON);
////			} catch (JSONException e) {
////				e.printStackTrace();
////			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//			finish();
//		}
////		try {//TODO: Refactor
//		if (wvTitle != null && wvTitle.length() > 0) this.setTitle(wvTitle);
//		if (wvSubTitle != null && wvSubTitle.length() > 0) toolbar.setSubtitle(wvSubTitle);
//		// 解取favicon
//		if (fib64 != null && fib64.length() > 0) {
//			byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
//			Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
//			toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
//		}
////			InputStream is = null;
////			try {
////				is = new FileInputStream(new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id));
////				Bitmap icon = BitmapFactory.decodeStream(is);
////				if (icon != null) toolbar.setLogo(cIcon(icon));
////			} catch (Exception e) {
////				e.printStackTrace();
////			} finally {
////				if (is != null) try {
////					is.close();
////				} catch (Exception e) {
////					e.printStackTrace();
////				}
////			}
////		} catch (Exception e) {
////			e.printStackTrace();
////		}
//		wv.getSettings().setJavaScriptEnabled(true);
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//			wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
//		else {
//			// KitKat fallback
//			if (uri == null) {
//				wv.loadUrl(MainActivity.STR_EMPTY);
//				return;
//			}
//			BufferedInputStream is = null;
//			ByteArrayOutputStream os = null;
//			String data = null;
//			try {
//				is = new BufferedInputStream(Objects.requireNonNull(getContentResolver().openInputStream(uri)));
//				os = new ByteArrayOutputStream(BUF_SIZE);   //读全部数据
//				int len = is.available();
//				int length, lenTotal = 0;
//				byte[] b = new byte[BUF_SIZE];
//				while ((length = is.read(b)) != -1) {
//					os.write(b, 0, length);
//					lenTotal += length;
//				}
//				os.flush();
//				if (lenTotal != len) throw new IOException();
//				is.close();
//				os.close();
//				data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(os.toByteArray())).toString();
//				try {  //保持读写权限
//					getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (is != null) try {
//					is.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (os != null) try {
//					os.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			wv.loadDataWithBaseURL(uri.toString(), data != null ? data : MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
//		}
//	}

	// 加载内容
//	private void nextWiki(Intent nextWikiIntent, String nextWikiId) {
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
			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if ((u = Uri.parse(wa.optString(MainActivity.DB_KEY_URI))) == null || bu.getBoolean(KEY_SHORTCUT) && !new MainActivity.TWInfo(this, u).isWiki) {
			new AlertDialog.Builder(this)    //TODO: 自动移除机制重构
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_auto_remove_wiki)
					.setNegativeButton(android.R.string.no, null)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {
								wl.remove(nextWikiId);
								MainActivity.writeJson(TWEditorWV.this, db);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
									revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialogInterface) {
					TWEditorWV.this.finish();
				}
			}).show();
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
		String wvTitle = wApp.optString(MainActivity.KEY_NAME);
		String wvSubTitle = wApp.optString(MainActivity.DB_KEY_SUBTITLE);
		String fib64 = wApp.optString(MainActivity.KEY_FAVICON);
		if (wvTitle.length() > 0) this.setTitle(wvTitle);
		if (wvSubTitle.length() > 0) toolbar.setSubtitle(wvSubTitle);
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
		getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);  //保持读写权限
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			wv.loadUrl(uri != null ? uri.toString() : MainActivity.STR_EMPTY);
		else {
			// KitKat fallback
			if (uri == null) {
				wv.loadUrl(MainActivity.STR_EMPTY);
				return;
			}
			BufferedInputStream is = null;
			ByteArrayOutputStream os = null;
			String data = null;
			try {
				is = new BufferedInputStream(Objects.requireNonNull(getContentResolver().openInputStream(uri)));
				os = new ByteArrayOutputStream(BUF_SIZE);   //读全部数据
				int len = is.available();
				int length, lenTotal = 0;
				byte[] b = new byte[BUF_SIZE];
				while ((length = is.read(b)) != -1) {
					os.write(b, 0, length);
					lenTotal += length;
				}
				os.flush();
				if (lenTotal != len) throw new IOException();
				is.close();
				os.close();
				data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(os.toByteArray())).toString();
//				try {
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (os != null) try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			wv.loadDataWithBaseURL(uri.toString(), data != null ? data : MainActivity.STR_EMPTY, MainActivity.TYPE_HTML, StandardCharsets.UTF_8.name(), null);
		}
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
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									try {
										view.getContext().startActivity(intent);
									} catch (Exception e) {
										e.printStackTrace();
									}
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
		view.evaluateJavascript(getString(R.string.js_info), new ValueCallback<String>() {
			@Override
			public void onReceiveValue(String value) {
				System.out.println(value);
				try {
					JSONArray array = new JSONArray(value);
					// 解取标题
					String title = array.getString(0), subtitle = array.getString(1);
					TWEditorWV.this.setTitle(title);
					toolbar.setSubtitle(subtitle);
					// 解取主题色
					String color = array.getString(2);
					if (color.length() == 7) themeColor = Color.parseColor(color);
					else themeColor = null;
					TWEditorWV.this.onConfigurationChanged(getResources().getConfiguration());
					// 解取favicon
					String fib64 = array.getString(3);
					byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
					Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
					toolbar.setLogo(favicon != null ? cIcon(favicon) : null);
					// 写Json
					wApp.put(MainActivity.KEY_NAME, title).put(MainActivity.DB_KEY_SUBTITLE, subtitle).put(MainActivity.DB_KEY_COLOR, themeColor).put(MainActivity.KEY_FAVICON, fib64.length() > 0 ? fib64 : null);
					MainActivity.writeJson(TWEditorWV.this, db);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	// 接收导入导出文件
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (requestCode == KEY_REQ_DOWN && resultCode == RESULT_OK) {
			Uri uri = resultData.getData();
			InputStream is = null;
			OutputStream os = null;
			if (uri != null) try {
				os = getContentResolver().openOutputStream(uri);
				if (os != null && exData != null) {
					is = new ByteArrayInputStream(exData);
					int len = is.available();
					int length;
					int lengthTotal = 0;
					byte[] bytes = new byte[4096];
					while ((length = is.read(bytes)) > -1) {
						os.write(bytes, 0, length);
						lengthTotal += length;
					}
					os.flush();
					os.close();
					os = null;
					if (lengthTotal != len) throw new IOException();
				}

			} catch (Exception e) {
				e.printStackTrace();
				try {
					DocumentsContract.deleteDocument(getContentResolver(), uri);
				} catch (Exception e1) {
					e.printStackTrace();
				}
				Toast.makeText(this, R.string.failed_creating_file, Toast.LENGTH_SHORT).show();
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				if (os != null)
					try {
						os.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				exData = null;
			}
			return;
		} else if (uploadMessage == null) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, resultData));
		uploadMessage = null;
	}


	// 保存提醒
//	private void confirmAndExit(boolean dirty, final Intent nextWikiIntent, final int nextWikiSerial) {
//		if (dirty) {
//			AlertDialog isExit = new AlertDialog.Builder(TWEditorWV.this)
//					.setTitle(android.R.string.dialog_alert_title)
//					.setMessage(R.string.confirm_to_exit_wiki)
//					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int which) {
//									if (nextWikiIntent == null)
//										TWEditorWV.super.onBackPressed();
//									else
//										nextWiki(nextWikiIntent, nextWikiSerial);
//									dialog.dismiss();
//								}
//							}
//					)
//					.setNegativeButton(android.R.string.no, null)
//					.setOnCancelListener(new DialogInterface.OnCancelListener() {
//						@Override
//						public void onCancel(DialogInterface dialog) {
//						}
//					})
//					.show();
//			isExit.setCanceledOnTouchOutside(false);
//		} else {
//			if (nextWikiIntent == null)
//				TWEditorWV.super.onBackPressed();
//			else
//				nextWiki(nextWikiIntent, nextWikiSerial);
//		}
//	}
	// 保存提醒
	private void confirmAndExit(boolean dirty, final Intent nextWikiIntent) {
		if (dirty) {
			AlertDialog isExit = new AlertDialog.Builder(TWEditorWV.this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_exit_wiki)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									if (nextWikiIntent == null)
										TWEditorWV.super.onBackPressed();
									else
										nextWiki(nextWikiIntent);
									dialog.dismiss();
								}
							}
					)
					.setNegativeButton(android.R.string.no, null)
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
						}
					})
					.show();
			isExit.setCanceledOnTouchOutside(false);
		} else {
			if (nextWikiIntent == null)
				TWEditorWV.super.onBackPressed();
			else
				nextWiki(nextWikiIntent);
		}
	}

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
			wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
				@Override
				public void onReceiveValue(String value) {
					confirmAndExit(Boolean.parseBoolean(value), null);
//					confirmAndExit(Boolean.parseBoolean(value), null, -1);
				}
			});
	}

	// 应用主题
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int primColor = themeColor != null ? themeColor : getResources().getColor(R.color.design_default_color_primary);    // 优先主题色 >> 自动色
//		System.out.println(primColor);
		float[] l = new float[3];
		Color.colorToHSV(primColor, l);
		boolean lightBar = themeColor != null ? (l[2] > 0.75) : (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;    // 系统栏模式 根据主题色灰度/日夜模式
		try {
			int bar = 0;
			boolean landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
			findViewById(R.id.wv_toolbar).setVisibility(landscape ? View.GONE : View.VISIBLE);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				getWindow().setStatusBarColor(primColor);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					getWindow().setNavigationBarColor(primColor);
				bar = lightBar ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0) : View.SYSTEM_UI_FLAG_VISIBLE;
			}
			getWindow().getDecorView().setSystemUiVisibility(bar | (landscape ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : View.SYSTEM_UI_FLAG_VISIBLE));
//			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//				findViewById(R.id.wv_toolbar).setVisibility(View.GONE);
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////					getWindow().setStatusBarColor(primColor);
////					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
////						getWindow().setNavigationBarColor(primColor);
//					getWindow().getDecorView().setSystemUiVisibility(bar | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//				} else
////					getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
//					;
//			} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//				findViewById(R.id.wv_toolbar).setVisibility(View.VISIBLE);
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////					getWindow().setStatusBarColor(primColor);
////					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
////						getWindow().setNavigationBarColor(primColor);
////					getWindow().getDecorView().setSystemUiVisibility(bar);
//				} else
////					getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE)
//					;
//			}
			findViewById(R.id.wv_appbar).setBackgroundColor(primColor);
//			toolbar.setBackgroundColor(primColor);
			toolbar.setTitleTextAppearance(this, R.style.Toolbar_TitleText);
			toolbar.setSubtitleTextAppearance(this, R.style.TextAppearance_AppCompat_Small);
			if (themeColor != null) {    // 有主题色则根据灰度切换字色
				toolbar.setTitleTextColor(getResources().getColor(lightBar ? R.color.content_tint_l : R.color.content_tint_d));
				toolbar.setSubtitleTextColor(getResources().getColor(lightBar ? R.color.content_sub_l : R.color.content_sub_d));
			}
			toolbar.setNavigationIcon(themeColor != null ? (lightBar ? R.drawable.ic_arrow_back_l : R.drawable.ic_arrow_back_d) : R.drawable.ic_arrow_back);
//			wvProgress.setBackgroundColor(primColor);
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