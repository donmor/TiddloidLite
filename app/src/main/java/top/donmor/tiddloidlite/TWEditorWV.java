/*
 * top.donmor.tiddloidlite.TWEditorWV <= [P|Tiddloid Lite]
 * Last modified: 18:33:05 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class TWEditorWV extends AppCompatActivity {

	private JSONObject db;
	private JSONObject wApp;
	private WebChromeClient wcc;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	private int mOriginalOrientation, nextWikiSerial = -1;
	private float scale;
	private Intent nextWikiIntent;
	private ValueCallback<Uri[]> uploadMessage;
	private WebView wv;
	private Toolbar toolbar;
	private ProgressBar wvProgress;
	private String id;
	private Uri uri = null;
	private static byte[] exData = null;

	// CONSTANT
	private static final String
			CHARSET_NAME_UTF_8 = "UTF-8",
			JSI = "twi",
			MIME_ANY = "*/*",
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
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.tweditor);
		try {
			db = MainActivity.readJson(this);
			if (db == null) throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
			finish();
		}
		toolbar = findViewById(R.id.wv_toolbar);
		setSupportActionBar(toolbar);
		this.setTitle(R.string.app_name);
		onConfigurationChanged(getResources().getConfiguration());
		wv = findViewById(R.id.twWebView);
		wvProgress = findViewById(R.id.progressBar);
		wvProgress.setMax(100);
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
		wvs.setAllowFileAccessFromFileURLs(true);
		wvs.setAllowUniversalAccessFromFileURLs(true);
		scale = getResources().getDisplayMetrics().density;
		wcc = new WebChromeClient() {
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

			@Override
			public void onShowCustomView(View view,
			                             WebChromeClient.CustomViewCallback callback) {
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

			@Override
			public void onHideCustomView() {
				FrameLayout decor = (FrameLayout) getWindow().getDecorView();
				decor.removeView(mCustomView);
				mCustomView = null;
				setRequestedOrientation(mOriginalOrientation);
				if (mCustomViewCallback != null) mCustomViewCallback.onCustomViewHidden();
				mCustomViewCallback = null;
			}
		};
		wv.setWebChromeClient(wcc);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		Bundle bu = getIntent().getExtras();
		CharSequence wvTitle = null, wvSubTitle = null;
		id = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
		boolean shortcut = bu != null && bu.getBoolean(MainActivity.KEY_SHORTCUT);
		try {
			if (id != null && id.length() > 0)
				for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
					wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
					if (wApp.getString(MainActivity.KEY_ID).equals(id)) {
						uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
						final int p = i;
						if (shortcut && !new MainActivity.TWInfo(this, uri).isWiki) {
							uri = null;
							new AlertDialog.Builder(this)
									.setTitle(android.R.string.dialog_alert_title)
									.setMessage(R.string.confirm_to_auto_remove_wiki)
									.setNegativeButton(android.R.string.no, null)
									.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											try {
												db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
												MainActivity.writeJson(TWEditorWV.this, db);
												if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
													revokeUriPermission(getPackageName(), uri, MainActivity.TAKE_FLAGS);
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}).setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface dialogInterface) {
									finish();
								}
							}).show();
						}
						wvTitle = wApp.getString(MainActivity.KEY_NAME);
						try {
							wvSubTitle = wApp.getString(MainActivity.DB_KEY_SUBTITLE);
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					} else if (i == db.getJSONArray(MainActivity.DB_KEY_WIKI).length() - 1)
						throw new Exception();
				}
			else throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			finish();
		}

		try {
			if (wvTitle != null && wvTitle.length() > 0) this.setTitle(wvTitle);
			if (wvSubTitle != null && wvSubTitle.length() > 0) toolbar.setSubtitle(wvSubTitle);
			InputStream is = null;
			try {
				is = new FileInputStream(new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id));
				Bitmap icon = BitmapFactory.decodeStream(is);
				if (icon != null) toolbar.setLogo(cIcon(icon));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		final class JavaScriptCallback {

			@SuppressWarnings("unused")
			@JavascriptInterface
			public void saveFile(String pathname, String data) {
				saveWiki(data);
			}

			@SuppressWarnings("unused")
			@JavascriptInterface
			public void saveDownload(String data) {
				TWEditorWV.exData = data.getBytes(Charset.forName(CHARSET_NAME_UTF_8));
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType(MIME_ANY);
				startActivityForResult(intent, 906);
			}

			@SuppressWarnings({"unused", "WeakerAccess"})
			@JavascriptInterface
			public void saveWiki(String data) {
				ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName(CHARSET_NAME_UTF_8)));
				OutputStream os = null;
				try {
					os = getContentResolver().openOutputStream(uri);
					if (os != null) {
						int len = is.available();
						int length, lengthTotal = 0;
						byte[] b = new byte[4096];
						while ((length = is.read(b)) != -1) {

							os.write(b, 0, length);
							lengthTotal += length;
						}
						os.flush();
						os.close();
						os = null;
						if (lengthTotal != len) throw new Exception();
						final MainActivity.TWInfo info = new MainActivity.TWInfo(TWEditorWV.this, uri);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (info.favicon != null) {
									toolbar.setLogo(cIcon(info.favicon));
								} else {
									toolbar.setLogo(null);
								}
								MainActivity.updateIcon(TWEditorWV.this, info.favicon, id);
								TWEditorWV.this.setTitle(info.title);
								toolbar.setSubtitle(info.subtitle);
							}
						});
						wApp.put(MainActivity.KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getString(R.string.tiddlywiki));
						wApp.put(MainActivity.DB_KEY_SUBTITLE, (info.subtitle != null && info.subtitle.length() > 0) ? info.subtitle : MainActivity.STR_EMPTY);
						MainActivity.writeJson(TWEditorWV.this, db);
					} else throw new Exception();
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
		}

		wv.addJavascriptInterface(new JavaScriptCallback(), JSI);
		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, WebResourceRequest request) {
				Uri u = request.getUrl();
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

			public void onPageFinished(WebView view, String url) {
				view.clearHistory();
			}
		});
		wv.loadUrl(uri != null ? uri.toString() : null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (requestCode == 906 && resultCode == RESULT_OK) {
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
					if (lengthTotal != len) throw new Exception();
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
		uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, resultData));
		uploadMessage = null;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Bundle bu = intent.getExtras();
		String fid = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
		if (fid != null) {
			int ser = -1;
			JSONObject w = null;
			try {
				for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
					w = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
					if (w.getString(MainActivity.KEY_ID).equals(fid)) {
						ser = i;
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (ser == -1) Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			else if (!fid.equals(id)) {
				try {
					final Uri u = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
					getContentResolver().takePersistableUriPermission(u, MainActivity.TAKE_FLAGS);
					if (new MainActivity.TWInfo(this, u).isWiki) {
						nextWikiIntent = intent;
						nextWikiSerial = ser;
						wv.evaluateJavascript(getString(R.string.js_exit), new ValueCallback<String>() {
							@Override
							public void onReceiveValue(String value) {
								confirmAndExit(Boolean.parseBoolean(value));
							}
						});
					} else {
						final int p = ser;
						new AlertDialog.Builder(this)
								.setTitle(android.R.string.dialog_alert_title)
								.setMessage(R.string.confirm_to_auto_remove_wiki)
								.setNegativeButton(android.R.string.no, null)
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										try {
											db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
											MainActivity.writeJson(TWEditorWV.this, db);
											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
												revokeUriPermission(getPackageName(), u, MainActivity.TAKE_FLAGS);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void confirmAndExit(boolean dirty) {
		if (dirty) {
			AlertDialog isExit = new AlertDialog.Builder(TWEditorWV.this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(R.string.confirm_to_exit_wiki)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									if (nextWikiIntent == null)
										TWEditorWV.super.onBackPressed();
									else
										nextWiki();
									dialog.dismiss();
								}
							}
					)
					.setNegativeButton(android.R.string.no, null)
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							nextWikiIntent = null;
							nextWikiSerial = -1;
						}
					})
					.show();
			isExit.setCanceledOnTouchOutside(false);
		} else {
			if (nextWikiIntent == null)
				TWEditorWV.super.onBackPressed();
			else
				nextWiki();
		}
	}

	private void nextWiki() {
		toolbar.setLogo(null);
		wApp = null;
		uri = null;
		wv.getSettings().setJavaScriptEnabled(false);
		wv.loadUrl(URL_BLANK);
		setIntent(nextWikiIntent);
		String wvTitle = null, wvSubTitle = null;
		try {
			wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(nextWikiSerial);
			if (wApp == null) throw new Exception();
			id = wApp.getString(MainActivity.KEY_ID);
			uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
			wvTitle = wApp.getString(MainActivity.KEY_NAME);
			try {
				wvSubTitle = wApp.getString(MainActivity.DB_KEY_SUBTITLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
			finish();
		}
		try {
			if (wvTitle != null && wvTitle.length() > 0) this.setTitle(wvTitle);
			if (wvSubTitle != null && wvSubTitle.length() > 0) toolbar.setSubtitle(wvTitle);
			InputStream is = null;
			try {
				is = new FileInputStream(new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id));
				Bitmap icon = BitmapFactory.decodeStream(is);
				if (icon != null) toolbar.setLogo(cIcon(icon));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		wv.getSettings().setJavaScriptEnabled(true);
		wv.loadUrl(uri.toString());
		nextWikiIntent = null;
		nextWikiSerial = -1;
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
					confirmAndExit(Boolean.parseBoolean(value));
				}
			});
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		try {
			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				findViewById(R.id.wv_toolbar).setVisibility(View.GONE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					getWindow().setStatusBarColor(getColor(R.color.design_default_color_primary));
					getWindow().getDecorView().setSystemUiVisibility(((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : View.SYSTEM_UI_FLAG_VISIBLE) | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
				} else
					getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				findViewById(R.id.wv_toolbar).setVisibility(View.VISIBLE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					getWindow().setStatusBarColor(getColor(R.color.design_default_color_primary));
					getWindow().getDecorView().setSystemUiVisibility((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : View.SYSTEM_UI_FLAG_VISIBLE);
				} else
					getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
			toolbar.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary));
			toolbar.setTitleTextAppearance(this, R.style.Toolbar_TitleText);
			toolbar.setSubtitleTextAppearance(this, R.style.TextAppearance_AppCompat_Small);
			toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
			wvProgress.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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