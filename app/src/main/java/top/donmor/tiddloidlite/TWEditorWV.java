/*
 * top.donmor.tiddloidlite.TWEditorWV <= [P|Tiddloid Lite]
 * Last modified: 18:33:05 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
//import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//import com.github.donmor.filedialog.lib.FileDialog;

//import org.joda.time.DateTime;
//import org.joda.time.DateTimeZone;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

public class TWEditorWV extends AppCompatActivity {

	private JSONObject db;
	private JSONObject wApp;
	private WebChromeClient wcc;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	private int mOriginalOrientation, nextWikiSerial = -1;
	private Intent nextWikiIntent;
	protected FrameLayout mFullscreenContainer;
	private ValueCallback<Uri[]> uploadMessage;
	private WebView wv;
	private WebSettings wvs;
	private Toolbar toolbar;
	private ProgressBar wvProgress;
	private Bitmap favicon;
	private boolean isWiki, isClassic;
	private String id;
	private Uri uri = null;

	// CONSTANT
	private static final String F02D = "%02d",
			F03D = "%03d",
			F04D = "%04d",
			SCH_ABOUT = "about",
			SCH_BLOB = "blob",
			SCH_FILE = "file",
			SCH_HTTP = "http",
			SCH_HTTPS = "https",
			SCH_TEL = "tel",
			SCH_MAILTO = "mailto",
			SCH_JS = "javascript",
	//			SCH_EX_FILE = "file://",
	PREF_BLOB = "$blob$",
			PREF_DEST = "$dest$",
			URL_BLANK = "about:blank";

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		setContentView(R.layout.tweditor);
		try {
			db = MainActivity.readJson(openFileInput(MainActivity.DB_FILE_NAME));
			if (db == null) throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
			finish();
		}
		toolbar = findViewById(R.id.wv_toolbar);
		setSupportActionBar(toolbar);
		this.setTitle(getResources().getString(R.string.app_name));
		configurationChanged(getResources().getConfiguration());
		wv = findViewById(R.id.twWebView);
		wvProgress = findViewById(R.id.progressBar);
		wvProgress.setMax(100);
		wvs = wv.getSettings();
		wvs.setJavaScriptEnabled(true);
		wvs.setDatabaseEnabled(true);
//		wvs.setDatabasePath(getCacheDir().getPath());
		wvs.setDomStorageEnabled(true);
		wvs.setBuiltInZoomControls(false);
		wvs.setDisplayZoomControls(false);
		wvs.setUseWideViewPort(true);
		wvs.setLoadWithOverviewMode(true);
		wvs.setAllowFileAccess(true);
		wvs.setAllowContentAccess(true);
		wvs.setAllowFileAccessFromFileURLs(true);
		wvs.setAllowUniversalAccessFromFileURLs(true);
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		wvs.setAllowFileAccessFromFileURLs(true);
		wvs.setAllowUniversalAccessFromFileURLs(true);
//		}
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
			public void onReceivedTitle(WebView view, String title) {
				TWEditorWV.this.setTitle(title);
			}

			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
//				if (uri != null) {
				if (icon != null) {
					FileOutputStream os = null;
					try {
						os = new FileOutputStream(new File(getDir(MainActivity.KEY_FAVICON, MODE_PRIVATE), id));
						icon.compress(Bitmap.CompressFormat.PNG, 100, os);
						os.flush();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (os != null)
							try {
								os.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
					}
					int width = icon.getWidth(), height = icon.getHeight();
					float scale = getResources().getDisplayMetrics().density * 16f;
					Matrix matrix = new Matrix();
					matrix.postScale(scale / width, scale / height);
					favicon = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
					toolbar.setLogo(new BitmapDrawable(getResources(), favicon));
				}

//				}
				super.onReceivedIcon(view, icon);
			}

			//			@TargetApi(Build.VERSION_CODES.LOLLIPOP)
			@Override
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
				uploadMessage = filePathCallback;
				Intent intent = fileChooserParams.createIntent();
				try {
					startActivityForResult(intent, 40);
				} catch (Exception e) {
					e.printStackTrace();
				}
//				switch (fileChooserParams.getMode()) {
//					case FileChooserParams.MODE_OPEN:
//						intent = new Intent(Intent.ACTION_GET_CONTENT);
//						break;
////					case FileChooserParams.MODE_OPEN_MULTIPLE:
////						intent = new Intent(Intent.ACTION_);
////						break;
//					case FileChooserParams.MODE_SAVE:
//						intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//						break;
//					default:
//						intent = new Intent(Intent.ACTION_GET_CONTENT);
//				}
//				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				intent.setType(fileChooserParams.getAcceptTypes()[0]);

//				File lastDir = Environment.getExternalStorageDirectory();
//				boolean showHidden = false;
//				try {
//					lastDir = new File(db.getString(MainActivity.DB_KEY_LAST_DIR));
//					showHidden = db.getBoolean(MainActivity.DB_KEY_SHOW_HIDDEN);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				final int mode = fileChooserParams.getMode();
//				FileDialog.fileDialog(TWEditorWV.this, lastDir, null, mode, 0, fileChooserParams.getAcceptTypes(), 0, showHidden, false, new FileDialog.OnFileTouchedListener() {
//					@Override
//					public void onFileTouched(File[] files) {
//						if (uploadMessage == null) return;
//						Uri[] results = null;
//						try {
//							if (files != null && files.length > 0) {
//								switch (mode) {
//									case 0:
//										File file = files[0];
//										if (file != null && file.exists()) {
//											try {
//												results = new Uri[]{Uri.fromFile(file)};
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										} else throw new Exception();
//										break;
//									case 1:
//										for (File file1 : files) {
//											try {
//												results = new Uri[]{Uri.parse(file1.toURI().toString())};
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//
//										}
//										break;
//									case 3:
//										File file3 = files[0];
//										if (file3 != null && file3.exists()) {
//											try {
//												results = new Uri[]{Uri.parse(file3.toURI().toString())};
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										} else throw new Exception();
//										break;
//								}
//								db.put(MainActivity.DB_KEY_LAST_DIR, files[0].getParentFile().getAbsolutePath());
//								MainActivity.writeJson(openFileOutput(MainActivity.DB_FILE_NAME, MODE_PRIVATE), db);
//							} else throw new Exception();
//
//						} catch (Exception e) {
//							e.printStackTrace();
//							Toast.makeText(TWEditorWV.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
//						}
//						uploadMessage.onReceiveValue(results);
//						uploadMessage = null;
//					}
//
//					@Override
//					public void onCanceled() {
//						if (uploadMessage == null) return;
//						uploadMessage.onReceiveValue(null);
//						uploadMessage = null;
//					}
//				});
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
//		String ueu = URL_BLANK;
		String wvTitle = null;
		id = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
		try {
			for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
				wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
				if (wApp.getString(MainActivity.KEY_ID).equals(id)) {
					uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
					wvTitle = wApp.getString(MainActivity.KEY_NAME);
					break;
				} else if (i == db.getJSONArray(MainActivity.DB_KEY_WIKI).length() - 1)
					throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
			finish();
		}
//		if (uri != null) {
		try {
//				ueu = SCH_EX_FILE + wApp.getString(MainActivity.DB_KEY_PATH);
			if (wvTitle != null && !wvTitle.equals("")) this.setTitle(wvTitle);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		} else {
//			String url = bu != null ? bu.getString(MainActivity.KEY_URL) : null;
//			if (url != null) ueu = url;
//			else {
//			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//			finish();
//			}
//		}

		final class JavaScriptCallback {

			private static final String CHARSET_NAME_UTF_8 = "UTF-8";

			@SuppressWarnings("unused")
			@JavascriptInterface
			public void getVersion(String title, boolean classic) {
				if (title.equals(getResources().getString(R.string.tiddlywiki))) {
					isWiki = true;
//					if (wApp == null) {
//						if (!classic)
//							runOnUiThread(new Runnable() {
//								@Override
//								public void run() {
//									Toast.makeText(TWEditorWV.this, R.string.ready_to_fork, Toast.LENGTH_SHORT).show();
//									toolbar.setLogo(R.drawable.ic_fork);
//								}
//							});
//					}
					wvs.setBuiltInZoomControls(classic);
					wvs.setDisplayZoomControls(classic);
					isClassic = classic;
				} else isWiki = false;
			}

//			@SuppressWarnings("unused")
//			@JavascriptInterface
//			public void getB64(String data, String dest) {
//				MainActivity.wGet(TWEditorWV.this, Uri.parse(MainActivity.SCHEME_BLOB_B64 + ':' + data), new File(dest));
//			}

			@SuppressWarnings("unused")
			@JavascriptInterface
			public void isDirtyOnQuit(boolean d) {
				if (d) {
					final AlertDialog.Builder isExit = new AlertDialog.Builder(TWEditorWV.this);
					isExit.setTitle(android.R.string.dialog_alert_title);
					isExit.setMessage(R.string.confirm_to_exit_wiki);
					isExit.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									quitting();
									dialog.dismiss();
								}
							}
					);
					isExit.setNegativeButton(android.R.string.no, null);
					isExit.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							nextWikiIntent = null;
							nextWikiSerial = -1;
						}
					});
					AlertDialog dialog = isExit.create();
					dialog.setCanceledOnTouchOutside(false);
					dialog.show();
				} else {
					quitting();
				}
			}

			private void quitting() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (nextWikiIntent == null)
							TWEditorWV.super.onBackPressed();
						else
							nextWiki();
					}
				});
			}

			@SuppressWarnings("unused")
			@JavascriptInterface
			public void saveWiki(String data) {
				final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName(CHARSET_NAME_UTF_8)));
//				if (wApp != null) {
				OutputStream os = null;
				try {
//						String fp = wApp.getString(MainActivity.DB_KEY_PATH);
//						if (fp.equals(filepath)) {
//							File file = new File(fp);
//							if (wApp.getBoolean(MainActivity.DB_KEY_BACKUP)) {
//								FileInputStream isb = null;
//								FileOutputStream osb = null;
//								try {
//									String mfn = file.getName();
//									File mfd = new File(file.getParentFile().getAbsolutePath() + '/' + mfn + MainActivity.BACKUP_DIRECTORY_PATH_PREFIX);
//									if (!mfd.exists()) mfd.mkdir();
//									else if (!mfd.isDirectory()) throw new Exception();
//									DateTime dateTime = new DateTime(file.lastModified(), DateTimeZone.UTC);
//									String prefix = '.'
//											+ String.format(Locale.US, F04D, dateTime.year().get())
//											+ String.format(Locale.US, F02D, dateTime.monthOfYear().get())
//											+ String.format(Locale.US, F02D, dateTime.dayOfMonth().get())
//											+ String.format(Locale.US, F02D, dateTime.hourOfDay().get())
//											+ String.format(Locale.US, F02D, dateTime.minuteOfHour().get())
//											+ String.format(Locale.US, F02D, dateTime.secondOfMinute().get())
//											+ String.format(Locale.US, F03D, dateTime.millisOfSecond().get());
//									isb = new FileInputStream(file);
//									osb = new FileOutputStream(new File(mfd.getAbsolutePath() + '/' + new StringBuilder(mfn).insert(mfn.lastIndexOf('.'), prefix).toString()));
//									int len = isb.available();
//									int length, lengthTotal = 0;
//									byte[] b = new byte[512];
//									while ((length = isb.read(b)) != -1) {
//										osb.write(b, 0, length);
//										lengthTotal += length;
//									}
//									osb.flush();
//									if (lengthTotal != len) throw new Exception();
//								} catch (Exception e) {
//									e.printStackTrace();
//									Toast.makeText(TWEditorWV.this, R.string.backup_failed, Toast.LENGTH_SHORT).show();
//								} finally {
//									if (isb != null)
//										try {
//											isb.close();
//										} catch (Exception e) {
//											e.printStackTrace();
//										}
//									if (osb != null)
//										try {
//											osb.close();
//										} catch (Exception e) {
//											e.printStackTrace();
//										}
//								}
//							}
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
						if (lengthTotal != len) throw new Exception();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Bitmap icon = wv.getFavicon();
								if (icon == null) try {
									toolbar.setLogo(null);
									if (!new File(getDir(MainActivity.KEY_FAVICON, MODE_PRIVATE), id).delete())
										throw new Exception();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
//						for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
//							JSONObject w = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
//							if (w.getString(MainActivity.KEY_ID).equals(id)) {
//								w.put(MainActivity.KEY_NAME, TWEditorWV.this.getTitle().toString());
//								break;
//							} else if (i == db.getJSONArray(MainActivity.DB_KEY_WIKI).length() - 1) throw new Exception();
//						}
						wApp.put(MainActivity.KEY_NAME, TWEditorWV.this.getTitle().toString());
						MainActivity.writeJson(openFileOutput(MainActivity.DB_FILE_NAME, MODE_PRIVATE), db);
					} else throw new Exception();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(TWEditorWV.this, R.string.failed, Toast.LENGTH_SHORT).show();
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
//				} else {
//					File lastDir = Environment.getExternalStorageDirectory();
//					boolean showHidden = false;
//					try {
//						lastDir = new File(db.getString(MainActivity.DB_KEY_LAST_DIR));
//						showHidden = db.getBoolean(MainActivity.DB_KEY_SHOW_HIDDEN);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					FileDialog.fileSave(TWEditorWV.this, lastDir, MainActivity.HTML_FILTERS, showHidden, new FileDialog.OnFileTouchedListener() {
//						@Override
//						public void onFileTouched(File[] files) {
//							FileOutputStream os = null;
//							try {
//								if (files != null && files.length > 0 && files[0] != null) {
//									File file = files[0];
//									os = new FileOutputStream(file);
//									int len = is.available();
//									int length, lengthTotal = 0;
//									byte[] b = new byte[512];
//									while ((length = is.read(b)) != -1) {
//										os.write(b, 0, length);
//										lengthTotal += length;
//									}
//									os.flush();
//									if (lengthTotal != len) throw new Exception();
//									try {
//										boolean exist = false;
//										JSONObject w = new JSONObject();
//										for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
//											if (db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i).getString(MainActivity.DB_KEY_PATH).equals(file.getAbsolutePath())) {
//												exist = true;
//												w = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
//												break;
//											}
//										}
//										if (exist) {
//											Toast.makeText(TWEditorWV.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
//
//										} else {
//											w.put(MainActivity.KEY_NAME, TWEditorWV.this.getTitle());
//											w.put(MainActivity.KEY_ID, MainActivity.genId());
//											w.put(MainActivity.DB_KEY_PATH, file.getAbsolutePath());
//											w.put(MainActivity.DB_KEY_BACKUP, false);
//											db.getJSONArray(MainActivity.DB_KEY_WIKI).put(db.getJSONArray(MainActivity.DB_KEY_WIKI).length(), w);
//											if (!MainActivity.writeJson(openFileOutput(MainActivity.DB_FILE_NAME, Context.MODE_PRIVATE), db))
//												throw new Exception();
//										}
//										wApp = w;
//										db.put(MainActivity.DB_KEY_LAST_DIR, file.getParentFile().getAbsolutePath());
//										if (!MainActivity.writeJson(openFileOutput(MainActivity.DB_FILE_NAME, Context.MODE_PRIVATE), db))
//											throw new Exception();
//									} catch (Exception e) {
//										e.printStackTrace();
//										Toast.makeText(TWEditorWV.this, R.string.data_error, Toast.LENGTH_SHORT).show();
//									}
//									if (wApp != null) {
//										runOnUiThread(new Runnable() {
//											@Override
//											public void run() {
//												Bitmap icon = wv.getFavicon();
//												if (wApp != null) {
//													if (icon != null) {
//														FileOutputStream os = null;
//														try {
//															os = new FileOutputStream(new File(getDir(MainActivity.KEY_FAVICON, MODE_PRIVATE), wApp.getString(MainActivity.KEY_ID)));
//															icon.compress(Bitmap.CompressFormat.PNG, 100, os);
//															os.flush();
//														} catch (Exception e) {
//															e.printStackTrace();
//														} finally {
//															if (os != null)
//																try {
//																	os.close();
//																} catch (Exception e) {
//																	e.printStackTrace();
//																}
//														}
//														int width = icon.getWidth(), height = icon.getHeight();
//														float scale = getResources().getDisplayMetrics().density * 16f;
//														Matrix matrix = new Matrix();
//														matrix.postScale(scale / width, scale / height);
//														favicon = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
//														toolbar.setLogo(new BitmapDrawable(getResources(), favicon));
//													} else toolbar.setLogo(null);
//												}
//												if (icon != null) icon.recycle();
//												wv.clearHistory();
//											}
//										});
//									}
//
//								} else throw new Exception();
//							} catch (Exception e) {
//								e.printStackTrace();
//							} finally {
//								try {
//									is.close();
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//								if (os != null)
//									try {
//										os.close();
//									} catch (Exception e) {
//										e.printStackTrace();
//									}
//							}
//						}
//
//						@Override
//						public void onCanceled() {
//							Toast.makeText(TWEditorWV.this, R.string.cancelled, Toast.LENGTH_SHORT).show();
//
//						}
//					});
//				}
//			}
			}
		}

		wv.addJavascriptInterface(new JavaScriptCallback(), "client");
		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
				Uri u = Uri.parse(url);
				String sch = u.getScheme();
//				boolean browse = sch != null && (sch.equals(SCH_ABOUT) || sch.equals(SCH_HTTP) || sch.equals(SCH_HTTPS));
				if (sch == null || sch.length() == 0)
					return false;
				try {
					Intent intent;
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
							final Intent intent1 = intent;
							new AlertDialog.Builder(TWEditorWV.this)
									.setTitle(android.R.string.dialog_alert_title)
									.setMessage(R.string.third_part_rising)
									.setNegativeButton(android.R.string.no, null)
									.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											try {
												view.getContext().startActivity(intent1);
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

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				FileInputStream is = null;
				try {
					is = new FileInputStream(new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id));
					Bitmap icon = BitmapFactory.decodeStream(is);
					if (icon != null) {
						int width = icon.getWidth(), height = icon.getHeight();
						float scale = getResources().getDisplayMetrics().density * 16f;
						Matrix matrix = new Matrix();
						matrix.postScale(scale / width, scale / height);
						TWEditorWV.this.favicon = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
						toolbar.setLogo(new BitmapDrawable(getResources(), TWEditorWV.this.favicon));
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (is != null) try {
						is.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			public void onPageFinished(WebView view, String url) {
				view.loadUrl(SCH_JS + ':' + getResources().getString(R.string.js_save));
				view.clearHistory();
			}
		});
//		wv.setDownloadListener(new DownloadListener() {
//			@Override
//			public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
//				final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//				DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
////				.setMimeType(mimeType)
//						.setDescription(contentDisposition)
//						.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
//						.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//				request.allowScanningByMediaScanner();
//				if (downloadManager != null) downloadManager.enqueue(request);
////				File lastDir = Environment.getExternalStorageDirectory();
////				boolean showHidden = false;
////				try {
////					lastDir = new File(db.getString(MainActivity.DB_KEY_LAST_DIR));
////					showHidden = db.getBoolean(MainActivity.DB_KEY_SHOW_HIDDEN);
////				} catch (Exception e) {
////					e.printStackTrace();
////				}
////				String filenameProbable = URLUtil.guessFileName(url, contentDisposition, mimeType);
////				FileDialog.fileDialog(TWEditorWV.this, lastDir, filenameProbable, 3, 0, new String[]{mimeType, FileDialog.MIME_ALL}, 0, showHidden, false, new FileDialog.OnFileTouchedListener() {
////					@Override
////					public void onFileTouched(File[] files) {
////						if (files != null && files.length > 0) {
////							String scheme = Uri.parse(url) != null ? Uri.parse(url).getScheme() : null;
////							if (scheme != null && scheme.equals(SCH_BLOB)) {
////								wv.loadUrl(SCH_JS + ':' + getResources().getString(R.string.js_blob).replace(PREF_BLOB, url).replace(PREF_DEST, files[0].getAbsolutePath()));
////							} else
////								MainActivity.wGet(TWEditorWV.this, Uri.parse(url), files[0]);
////						}
////					}
////
////					@Override
////					public void onCanceled() {
////						Toast.makeText(TWEditorWV.this, R.string.cancelled, Toast.LENGTH_SHORT).show();
////					}
////				});
//			}
//		});
		wv.loadUrl(uri.toString());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (uploadMessage == null) return;
//		if (resultData!=null)grantUriPermission(getPackageName(),resultData.getData(),Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, resultData));
		uploadMessage = null;
//		if (resultCode == Activity.RESULT_OK && resultData != null) {
//			final Uri uri = resultData.getData();
//			if (uri != null && requestCode==40){
//
//			}
//		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Bundle bu = intent.getExtras();
		final String fid = bu != null ? bu.getString(MainActivity.KEY_ID) : null;
		if (fid != null) {
			int ser = -1;
			try {
				for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
					if (db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i).getString(MainActivity.KEY_ID).equals(fid)) {
						ser = i;
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (ser == -1)
				Toast.makeText(this, R.string.wiki_not_exist, Toast.LENGTH_SHORT).show();
			else if (!fid.equals(id)) {
				if (new MainActivity.TWInfo(this, uri).isWiki) {
					nextWikiIntent = intent;
					nextWikiSerial = ser;
					if (isWiki)
						wv.loadUrl(SCH_JS + ':' + getResources().getString(isClassic ? R.string.js_quit_c : R.string.js_quit));
					else nextWiki();
				} else {
					final int p = ser;
					final Uri r = uri;
					new android.app.AlertDialog.Builder(this)
							.setTitle(android.R.string.dialog_alert_title)
							.setMessage(R.string.confirm_to_auto_remove_wiki)
							.setNegativeButton(android.R.string.no, null)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									try {
										db.getJSONArray(MainActivity.DB_KEY_WIKI).remove(p);
										MainActivity.writeJson(openFileOutput(MainActivity.DB_FILE_NAME, MODE_PRIVATE), db);
										if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
											revokeUriPermission(getPackageName(), r, MainActivity.takeFlags);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}).show();
				}
			}
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void nextWiki() {
//		String ueu = URL_BLANK;
		toolbar.setLogo(null);
		wvs.setBuiltInZoomControls(false);
		wvs.setDisplayZoomControls(false);
		wApp = null;
		uri = null;
		wvs.setJavaScriptEnabled(false);
		wv.loadUrl(URL_BLANK);
		setIntent(nextWikiIntent);
		String wvTitle = null;
//		try {
//			for (int i = 0; i < db.getJSONArray(MainActivity.DB_KEY_WIKI).length(); i++) {
//				wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(i);
//				if (wApp.getString(MainActivity.KEY_ID).equals(id)) {
//					uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
//					wvTitle = wApp.getString(MainActivity.KEY_NAME);
//					break;
//				} else if (i == db.getJSONArray(MainActivity.DB_KEY_WIKI).length() - 1)
//					throw new Exception();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//			finish();
//		}
		try {
			wApp = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(nextWikiSerial);
			if (wApp == null) throw new Exception();
			id = wApp.getString(MainActivity.KEY_ID);
			uri = Uri.parse(wApp.getString(MainActivity.DB_KEY_URI));
			wvTitle = wApp.getString(MainActivity.KEY_NAME);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
			finish();
		}
//		if (wApp != null) {
		try {
//			ueu = SCH_EX_FILE + wApp.getString(MainActivity.DB_KEY_PATH);
//				String wvTitle = wApp.getString(MainActivity.KEY_NAME);
			if (!wvTitle.equals("")) this.setTitle(wvTitle);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		} else {
//			Bundle bu = getIntent().getExtras();
//			String url = bu != null ? bu.getString(MainActivity.KEY_URL) : null;
//			if (url != null) ueu = url;
//			if (bu != null) ueu = bu.getString(MainActivity.KEY_URL);
//		}
		wvs.setJavaScriptEnabled(true);
		wv.loadUrl(uri.toString());
		nextWikiIntent = null;
		nextWikiSerial = -1;
	}

	@Override
	public void onBackPressed() {
		if (mCustomView != null)
			wcc.onHideCustomView();
		else if (wv.canGoBack())
			wv.goBack();
		else if (isWiki) {
			wv.loadUrl(SCH_JS + ':' + getResources().getString(isClassic ? R.string.js_quit_c : R.string.js_quit));
		} else {
			TWEditorWV.super.onBackPressed();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		configurationChanged(newConfig);
	}

	private void configurationChanged(Configuration config) {
		try {
			if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				findViewById(R.id.wv_toolbar).setVisibility(View.GONE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					TWEditorWV.this.getWindow().setStatusBarColor(Color.WHITE);
					TWEditorWV.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
				} else
					TWEditorWV.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			} else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				findViewById(R.id.wv_toolbar).setVisibility(View.VISIBLE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					TWEditorWV.this.getWindow().setStatusBarColor(Color.WHITE);
					TWEditorWV.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				} else
					TWEditorWV.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		if (wv != null) {
			wvs.setJavaScriptEnabled(false);
			wv.loadUrl(URL_BLANK);
			((ViewGroup) wv.getParent()).removeView(wv);
			wv.destroy();
			wv = null;
		}
		super.onDestroy();
	}
}