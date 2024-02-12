/*
 * top.donmor.tiddloidlite.MainActivity <= [P|Tiddloid Lite]
 * Last modified: 21:29:11 2021/06/18
 * Copyright (c) 2022 donmor
 */

package top.donmor.tiddloidlite;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
	private TextView noWiki;
	private WikiListAdapter wikiListAdapter = null;
	private JSONObject db;
	private ActivityResultLauncher<Intent> getChooserCreate, getChooserImport;
	private int dialogPadding;
	private static boolean firstRun;

	// CONSTANT
	static final int TAKE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION, BUF_SIZE = 4096;
	static final String
			KEY_TW = "TiddlyWiki",
			KEY_NAME = "name",
			KEY_LBL = " — ",
			KEY_FAVICON = "favicon",
			KEY_ID = "id",
			DB_KEY_WIKI = "wiki",
			DB_KEY_COLOR = "color",
			DB_KEY_URI = "uri",
			KEY_SHORTCUT = "shortcut",
			DB_KEY_SUBTITLE = "subtitle",
			KEY_FD_R = "r",
			KEY_FD_W = "w",
			STR_EMPTY = "",
			TYPE_HTML = "text/html";
	private static final String
			DB_FILE_NAME = "data.json",
			KEY_URI_RATE = "market://details?id=",
			LICENSE_FILE_NAME = "LICENSE",
			TEMPLATE_FILE_NAME = "template.html";
	static final String
			EXCEPTION_JSON_DATA_ERROR = "JSON data file corrupted";
	private static final String
			EXCEPTION_NO_INTERNET = "No Internet connection",
			EXCEPTION_INTERRUPTED = "Interrupted by user";
	static final boolean APIOver23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
			APIOver24 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
			APIOver26 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
			APIOver29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
	private static final String[] TYPE_FILTERS = {TYPE_HTML};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		long time0 = System.nanoTime();
		super.onCreate(savedInstanceState);
		Window w = getWindow();
		w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		w.setFormat(PixelFormat.RGBA_8888);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.activity_main);
		onConfigurationChanged(getResources().getConfiguration());
		dialogPadding = (int) (getResources().getDisplayMetrics().density * 30);
		// 加载UI
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		noWiki = findViewById(R.id.t_noWiki);
		RecyclerView rvWikiList = findViewById(R.id.rvWikiList);
		rvWikiList.setLayoutManager(new LinearLayoutManager(this));
		rvWikiList.setItemAnimator(new DefaultItemAnimator());
		wikiListAdapter = new WikiListAdapter(this);
		wikiListAdapter.setReloadListener(count -> runOnUiThread(() -> noWiki.setVisibility(count > 0 ? View.GONE : View.VISIBLE)));
		wikiListAdapter.setOnItemClickListener(new WikiListAdapter.ItemClickListener() {
			// 点击打开
			@Override
			public void onItemClick(final int pos, final String id) {
				if (pos == -1) return;
				try {
					JSONObject wa = db.getJSONObject(DB_KEY_WIKI).getJSONObject(id);
					Uri uri = Uri.parse(wa.getString(DB_KEY_URI));
					try {
						getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
					if (!loadPage(id))
						Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				}
			}

			// 长按属性
			@Override
			public void onItemLongClick(final int pos, final String id) {
				if (pos == -1) return;
				try {
					JSONObject wl = db.getJSONObject(DB_KEY_WIKI), wa = wl.getJSONObject(id);
					Uri uri = Uri.parse(wa.getString(DB_KEY_URI));
					final String name = wa.optString(KEY_NAME, KEY_TW), sub = wa.optString(DB_KEY_SUBTITLE);
					byte[] b = Base64.decode(wa.optString(KEY_FAVICON), Base64.NO_PADDING);
					final Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
					Drawable icon = AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_description);
					if (favicon != null) try {
						icon = new BitmapDrawable(getResources(), favicon);
					} catch (Exception e) {
						e.printStackTrace();
					}
					TextView view = new TextView(MainActivity.this);
					DocumentFile file = DocumentFile.fromSingleUri(MainActivity.this, uri);
					String fn = file != null ? file.getName() : null, provider = getString(R.string.unknown);
					// 获取来源名
					try {
						PackageManager pm = getPackageManager();
						for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
							if (Objects.requireNonNull(uri.getAuthority()).startsWith(info.packageName)) {
								provider = pm.getApplicationLabel(info).toString();
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					// 显示属性
					CharSequence s = getString(R.string.provider)
							+ provider
							+ '\n'
							+ getString(R.string.pathDir)
							+ Uri.decode(uri.getLastPathSegment())
							+ '\n'
							+ getString(R.string.filename)
							+ (fn != null && fn.length() > 0 ? fn : getString(R.string.unknown));
					view.setText(s);
					if (APIOver23)
						view.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
					AlertDialog wikiConfigDialog = new AlertDialog.Builder(MainActivity.this)
							.setTitle(name)
							.setIcon(icon)
							.setPositiveButton(android.R.string.ok, null)
							.setNegativeButton(R.string.remove_wiki, (dialogInterface, i) -> {
								dialogInterface.dismiss();
								// 移除确认
								AlertDialog removeWikiConfirmationDialog = new AlertDialog.Builder(MainActivity.this)
										.setTitle(android.R.string.dialog_alert_title)
										.setMessage(R.string.confirm_to_remove_wiki)
										.setNegativeButton(android.R.string.no, null)
										.setNeutralButton(R.string.delete_the_html_file, (dialogInterface1, i1) -> {
											removeWiki(id, true);
											wikiListAdapter.notifyItemRemoved(pos);
										})
										.setPositiveButton(android.R.string.yes, (dialog, which) -> {
											removeWiki(id);
											wikiListAdapter.notifyItemRemoved(pos);
										})
										.create();
								removeWikiConfirmationDialog.show();
							})
							.setNeutralButton(R.string.create_shortcut, (dialogInterface, i) -> {
								try {
									// 快捷方式
									Bundle bu = new Bundle();
									bu.putString(KEY_ID, id);
									bu.putBoolean(KEY_SHORTCUT, true);
									Intent in = new Intent(MainActivity.this, TWEditorWV.class).putExtras(bu).setAction(Intent.ACTION_MAIN);
									if (ShortcutManagerCompat.isRequestPinShortcutSupported(MainActivity.this)) {
										ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(MainActivity.this, id)
												.setShortLabel(name)
												.setLongLabel(name + (sub.length() > 0 ? KEY_LBL + sub : sub))
												.setIcon(favicon != null ? IconCompat.createWithBitmap(favicon) : IconCompat.createWithResource(MainActivity.this, R.drawable.ic_shortcut))
												.setIntent(in)
												.build();
										if (ShortcutManagerCompat.requestPinShortcut(MainActivity.this, shortcut, null))
											Toast.makeText(MainActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
										else throw new Exception();
									}
								} catch (Exception e) {
									e.printStackTrace();
									Toast.makeText(MainActivity.this, R.string.shortcut_failed, Toast.LENGTH_SHORT).show();
								}
							})
							.create();
					int m = Math.round(20 * getResources().getDisplayMetrics().density + 0.5f);
					wikiConfigDialog.setView(view, m, 0, m, 0);
					wikiConfigDialog.show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				}
			}
		});
		rvWikiList.setAdapter(wikiListAdapter);
		// SAF处理
		getChooserCreate = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			if (result.getData() != null) {
				createWiki(result.getData().getData());
			}
		});
		getChooserImport = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			if (result.getData() != null) {
				importWiki(result.getData().getData());
			}
		});
		new Thread(() -> {
			// 读取/初始化db，转换新格式
			try {
				db = readJson(this);
				if (!db.has(DB_KEY_WIKI)) throw new JSONException(EXCEPTION_JSON_DATA_ERROR);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					db = initJson(this);
					writeJson(this, db);
					firstRun = true;
				} catch (Exception e1) {
					e1.printStackTrace();
					Toast.makeText(this, R.string.data_error, Toast.LENGTH_SHORT).show();
					finish();
					return;
				}
			}
			trimDB120(this, db);
			try {
				wikiListAdapter.reload(db);
			} catch (JSONException e) {
				e.printStackTrace();
				runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show());
				return;
			}
			runOnUiThread(() -> {
				rvWikiList.setAdapter(wikiListAdapter);
				noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
			});
			while ((System.nanoTime() - time0) / 1000000 < 1000) try {
				//noinspection BusyWait
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runOnUiThread(() -> {
				View splash = findViewById(R.id.splash_layout);
				ViewParent parent;
				if (splash != null && (parent = splash.getParent()) instanceof ViewGroup)
					((ViewGroup) parent).removeView(splash);
				w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
				if (firstRun)
					firstRunReq(this);
			});
		}).start();
	}

	private interface OnGetSrc {
		void run(File file);
	}

	private static InputStream getAdaptiveUriInputStream(Uri uri, final long[] lastModified) throws NetworkErrorException, InterruptedIOException {
		try {
			HttpsURLConnection httpURLConnection;
			URL url = new URL(uri.normalizeScheme().toString());
			httpURLConnection = (HttpsURLConnection) url.openConnection();
			httpURLConnection.connect();
			lastModified[0] = httpURLConnection.getLastModified();
			return httpURLConnection.getInputStream();
		} catch (InterruptedIOException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			throw new NetworkErrorException(EXCEPTION_NO_INTERNET);
		}
	}

	private void fetchInThread(OnGetSrc cb, AlertDialog progressDialog) {
		boolean interrupted = false;
		File cache = new File(getCacheDir(), genId()), dest = new File(getCacheDir(), TEMPLATE_FILE_NAME);
		long pModified = dest.lastModified();
		final long[] lastModified = new long[]{0L};
		try (InputStream isw = getAdaptiveUriInputStream(Uri.parse(getString(R.string.template_repo)), lastModified);
				OutputStream osw = Objects.requireNonNull(getContentResolver().openOutputStream(Uri.fromFile(cache)));
				ParcelFileDescriptor ifd = Objects.requireNonNull(getContentResolver().openFileDescriptor(Uri.fromFile(cache), KEY_FD_R));
				ParcelFileDescriptor ofd = Objects.requireNonNull(getContentResolver().openFileDescriptor(Uri.fromFile(dest), KEY_FD_W));
				FileInputStream is = new FileInputStream(ifd.getFileDescriptor());
				FileOutputStream os = new FileOutputStream(ofd.getFileDescriptor());
				FileChannel ic = is.getChannel();
				FileChannel oc = os.getChannel()) {
			// 下载到缓存
			if (lastModified[0] != pModified) {
				int length;
				byte[] bytes = new byte[BUF_SIZE];
				while ((length = isw.read(bytes)) > -1) {
					osw.write(bytes, 0, length);
					if (Thread.currentThread().isInterrupted()) {
						interrupted = true;
						break;
					}
				}
				osw.flush();
				if (interrupted) throw new InterruptedException(EXCEPTION_INTERRUPTED);
				fc2fc(ic, oc);
			}
			dest.setLastModified(lastModified[0]);
			if (progressDialog != null) progressDialog.dismiss();
		} catch (InterruptedException | InterruptedIOException ignored) {
			interrupted = true;
			runOnUiThread(() -> Toast.makeText(this, R.string.cancelled, Toast.LENGTH_SHORT).show());
		} catch (NetworkErrorException e) {
			e.printStackTrace();
			runOnUiThread(() -> Toast.makeText(this, R.string.server_error, Toast.LENGTH_SHORT).show());
			if (progressDialog != null) progressDialog.dismiss();
		} catch (IOException | SecurityException | NullPointerException | NonReadableChannelException | NonWritableChannelException e) {
			e.printStackTrace();
			runOnUiThread(() -> Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show());
			if (progressDialog != null) progressDialog.dismiss();
		} finally {
			cache.delete();
			if (!interrupted) cb.run(dest);
		}
	}

	private void getSrcFromUri(OnGetSrc cb) {
		// 对话框等待
		LinearLayout layout = new LinearLayout(this);
		layout.setPaddingRelative(dialogPadding, dialogPadding, dialogPadding, 0);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		ProgressBar progressBar = new ProgressBar(this);
		progressBar.setIndeterminate(true);
		progressBar.setPaddingRelative(0, 0, dialogPadding, 0);
		layout.addView(progressBar);
		TextView lblWait = new TextView(this);
		lblWait.setText(R.string.please_wait);
		lblWait.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Small);
		layout.addView(lblWait);
		final AlertDialog progressDialog = new AlertDialog.Builder(this)
				.setView(layout)
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
				.create();
		progressDialog.setCanceledOnTouchOutside(false);
		final Thread thread = new Thread(() -> fetchInThread(file -> runOnUiThread(() -> cb.run(file)), progressDialog));
		progressDialog.setOnShowListener(dialog -> thread.start());
		progressDialog.setOnCancelListener(dialogInterface -> thread.interrupt());
		progressDialog.show();
	}

	private Boolean loadPage(String id) {
		try {
			if (!db.getJSONObject(DB_KEY_WIKI).has(id))
				throw new FileNotFoundException();
			Bundle bu = new Bundle();
			bu.putString(KEY_ID, id);
			Intent in = new Intent().putExtras(bu).setClass(MainActivity.this, TWEditorWV.class);
			startActivity(in);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void removeWiki(String id) {
		removeWiki(id, false);
	}

	private void removeWiki(String id, boolean del) {
		try {
			JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
			JSONObject xw = (JSONObject) wl.remove(id);
			writeJson(MainActivity.this, db);
			if (xw != null) {
				Uri uri = Uri.parse(xw.optString(DB_KEY_URI));
				if (del) {
					DocumentsContract.deleteDocument(getContentResolver(), uri);
					Toast.makeText(MainActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
				}
				if (APIOver26)
					revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
			}
			wikiListAdapter.reload(db);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	// 菜单栏
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		final int idNew = R.id.action_new,
				idImport = R.id.action_import,
				idAbout = R.id.action_about;
		if (id == idNew) {
			getChooserCreate.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML));
		} else if (id == idImport) {
			getChooserImport.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML).putExtra(Intent.EXTRA_MIME_TYPES, TYPE_FILTERS));
		} else if (id == idAbout) {
			SpannableString spannableString = new SpannableString(getString(R.string.about));
			Linkify.addLinks(spannableString, Linkify.ALL);
			AlertDialog aboutDialog = new AlertDialog.Builder(this)
					.setTitle(getString(R.string.about_title, getVersion(this)))
					.setMessage(spannableString)
					.setPositiveButton(android.R.string.ok, null)
					.setNeutralButton(R.string.market, (dialog, which) -> {
						try {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(KEY_URI_RATE + getPackageName())));
						} catch (Exception e) {
							e.printStackTrace();
						}
					})
					.show();
			((TextView) aboutDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			if (APIOver23)
				((TextView) aboutDialog.findViewById(android.R.id.message)).setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		}
		return super.onOptionsItemSelected(item);
	}

	private void createWiki(final Uri uri) {
		OnGetSrc cb = file -> {
			if (file.exists()) {
				try (ParcelFileDescriptor ifd = Objects.requireNonNull(getContentResolver().openFileDescriptor(Uri.fromFile(file), KEY_FD_R));
						ParcelFileDescriptor ofd = Objects.requireNonNull(getContentResolver().openFileDescriptor(uri, KEY_FD_W));
						FileInputStream is = new FileInputStream(ifd.getFileDescriptor());
						FileOutputStream os = new FileOutputStream(ofd.getFileDescriptor());
						FileChannel ic = is.getChannel();
						FileChannel oc = os.getChannel()) {
					// 查重
					String id = null;
					JSONObject wl = db.getJSONObject(DB_KEY_WIKI), wa = null;
					boolean exist = false;
					Iterator<String> iterator = wl.keys();
					while (iterator.hasNext()) {
						exist = uri.toString().equals((wa = wl.getJSONObject(id = iterator.next())).optString(DB_KEY_URI));
						if (exist) break;
					}
					if (exist) {
						Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
					} else {
						wa = new JSONObject()
								.put(DB_KEY_URI, uri.toString());
						id = genId();
						wl.put(id, wa);
					}
					wa.put(KEY_NAME, KEY_TW)
							.put(DB_KEY_SUBTITLE, STR_EMPTY);
					writeJson(MainActivity.this, db);
					// 从缓存写入文件
					fc2fc(ic, oc);
					try {
						getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
					if (!loadPage(id))
						Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
				} catch (IOException | NullPointerException | NonReadableChannelException | NonWritableChannelException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.failed_creating_file, Toast.LENGTH_SHORT).show();
				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				}
			} else Toast.makeText(MainActivity.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
		};
		getSrcFromUri(cb);
	}

	private void importWiki(Uri uri) {
		String id = genId();
		try {
			JSONObject wl = db.getJSONObject(DB_KEY_WIKI), wa;
			boolean exist = false;
			Iterator<String> iterator = wl.keys();
			while (iterator.hasNext()) {
				exist = uri.toString().equals(wl.getJSONObject(iterator.next()).getString(DB_KEY_URI));
				if (exist) break;
			}
			if (exist) {
				Toast.makeText(this, R.string.wiki_already_exists, Toast.LENGTH_SHORT).show();
			} else {
				id = genId();
				wa = new JSONObject()
						.put(KEY_NAME, KEY_TW)
						.put(DB_KEY_SUBTITLE, STR_EMPTY)
						.put(DB_KEY_URI, uri.toString());
				wl.put(id, wa);
			}
			writeJson(MainActivity.this, db);
			try {
				getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
		}
		if (!loadPage(id))
			Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@SuppressLint("NotifyDataSetChanged")
	@Override
	public void onResume() {
		super.onResume();
		if (db != null) try {
			db = readJson(this);
			wikiListAdapter.reload(db);
			wikiListAdapter.notifyDataSetChanged();
			noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		onConfigurationChanged(getResources().getConfiguration());    // 刷新界面主题色
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Window w = getWindow();
		int color = getResources().getColor(R.color.design_default_color_primary);
		WindowInsetsControllerCompat wic = WindowCompat.getInsetsController(w, w.getDecorView());
		if (APIOver23) w.setStatusBarColor(color);
		if (APIOver26) w.setNavigationBarColor(color);
		wic.show(WindowInsetsCompat.Type.systemBars());
		boolean lightBar = (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
		wic.setAppearanceLightNavigationBars(lightBar);
		wic.setAppearanceLightStatusBars(lightBar);
		WindowCompat.setDecorFitsSystemWindows(w, true);
	}

	static JSONObject initJson(Context context) throws JSONException {
		File ext = context.getExternalFilesDir(null), file = new File(ext, DB_FILE_NAME);
		if (ext != null && file.isFile())
			try (ParcelFileDescriptor ifd = Objects.requireNonNull(context.getContentResolver().openFileDescriptor(Uri.fromFile(file), KEY_FD_R));
					FileInputStream is = new FileInputStream(ifd.getFileDescriptor());
					FileChannel ic = is.getChannel()) {
				JSONObject jsonObject = new JSONObject(new String(fc2ba(ic)));
				if (!jsonObject.has(DB_KEY_WIKI)) jsonObject.put(DB_KEY_WIKI, new JSONObject());
				return jsonObject;
			} catch (IOException | JSONException | NonReadableChannelException e) {
				e.printStackTrace();
			} finally {
				file.delete();
			}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(DB_KEY_WIKI, new JSONObject());
		return jsonObject;
	}

	static JSONObject readJson(Context context) throws JSONException {
		try (FileInputStream is = context.openFileInput(DB_FILE_NAME);
				FileChannel ic = is.getChannel()) {
			return new JSONObject(new String(fc2ba(ic)));
		} catch (IOException | NonReadableChannelException e) {
			throw new JSONException(e.getMessage());
		}
	}

	static void writeJson(Context context, JSONObject vdb) throws JSONException {
		try (FileOutputStream os = context.openFileOutput(DB_FILE_NAME, MODE_PRIVATE);
				FileChannel oc = os.getChannel()) {
			ba2fc(vdb.toString(2).getBytes(), oc);
		} catch (IOException | NonWritableChannelException e) {
			throw new JSONException(e.getMessage());
		}
	}

	static void exportJson(Context context, JSONObject vdb) {
		File ext = context.getExternalFilesDir(null);
		if (ext == null) return;
		try (ParcelFileDescriptor ofd = Objects.requireNonNull(context.getContentResolver().openFileDescriptor(Uri.fromFile(new File(ext, DB_FILE_NAME)), KEY_FD_W));
				FileOutputStream os = new FileOutputStream(ofd.getFileDescriptor());
				FileChannel oc = os.getChannel()) {
			ba2fc(vdb.toString(2).getBytes(), oc);
		} catch (IOException | JSONException | NonWritableChannelException e) {
			e.printStackTrace();
		}
	}

	private static String genId() {
		return UUID.randomUUID().toString();
	}

	static byte[] fc2ba(@NonNull FileChannel ic) throws IOException, NonReadableChannelException {
		if (ic.size() > Integer.MAX_VALUE) throw new IOException();
		ByteBuffer buffer = ByteBuffer.allocate((int) ic.size());
		ic.read(buffer);
		return buffer.array();
	}

	static void ba2fc(byte[] bytes, @NonNull FileChannel oc) throws IOException, NonWritableChannelException {
		oc.write(ByteBuffer.wrap(bytes));
		try {
			oc.truncate(bytes.length);
			oc.force(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void fc2fc(@NonNull FileChannel ic, @NonNull FileChannel oc) throws IOException, NonReadableChannelException, NonWritableChannelException {
		long len = ic.size();
		ic.transferTo(0, len, oc);
		try {
			oc.truncate(len);
			ic.force(true);
			oc.force(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void trimDB120(Context context, JSONObject db) {
		try {
			JSONArray wl1 = db.optJSONArray(DB_KEY_WIKI);
			if (wl1 == null) return;
			JSONObject wl2 = new JSONObject();
			for (int i = 0; i < wl1.length(); i++) {
				JSONObject wiki = new JSONObject(), w0 = wl1.optJSONObject(i);
				if (w0 == null) continue;
				wiki.put(KEY_NAME, w0.optString(KEY_NAME, KEY_TW));
				wiki.put(DB_KEY_SUBTITLE, w0.optString(DB_KEY_SUBTITLE));
				wiki.put(DB_KEY_URI, w0.optString(DB_KEY_URI));
				wl2.put(w0.optString(KEY_ID, genId()), wiki);
			}
			db.remove(DB_KEY_WIKI);
			db.put(DB_KEY_WIKI, wl2);
			writeJson(context, db);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	static void firstRunReq(Activity context) {
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		int dialogPadding2 = (int) (context.getResources().getDisplayMetrics().density * 12),
				dialogPadding3 = (int) (context.getResources().getDisplayMetrics().density * 24);
		layout.setPaddingRelative(dialogPadding3, dialogPadding2, dialogPadding3, 0);
		TextView lbl1 = new TextView(context);
		lbl1.setText(R.string.agreements_desc1);
		lbl1.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		layout.addView(lbl1);
		LinearLayout.LayoutParams agl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		TextView ag1 = new TextView(context);
		ag1.setLayoutParams(agl);
		ag1.setPadding(4, 0, 4, 0);
		StringBuffer sb = new StringBuffer();
		try (InputStream is = context.getAssets().open(LICENSE_FILE_NAME);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			sb.append(br.readLine());
			String line;
			while ((line = br.readLine()) != null) {
				sb.append("\n").append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		ag1.setText(sb);
		ag1.setHorizontallyScrolling(true);
		ag1.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		ag1.setTypeface(Typeface.MONOSPACE);
		ag1.setTextSize(12);
		ag1.setBackgroundColor(context.getResources().getColor(R.color.content_back_dec));
		HorizontalScrollView agh1 = new HorizontalScrollView(context);
		agh1.setHorizontalScrollBarEnabled(false);
		agh1.addView(ag1);
		ScrollView agc1 = new ScrollView(context);
		LinearLayout.LayoutParams agl1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (context.getResources().getDisplayMetrics().density * 80));
		agc1.setLayoutParams(agl1);
		agc1.addView(agh1);
		layout.addView(agc1);
		TextView lbl2 = new TextView(context);
		lbl2.setText(R.string.agreements_desc2);
		lbl2.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		layout.addView(lbl2);
		TextView ag2 = new TextView(context);
		ag2.setLayoutParams(agl);
		ag2.setPadding(4, 0, 4, 0);
		ag2.setText(R.string.agreements_privacy);
		ag2.setHorizontallyScrolling(true);
		ag2.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		ag2.setTypeface(Typeface.MONOSPACE);
		ag2.setTextSize(12);
		ag2.setBackgroundColor(context.getResources().getColor(R.color.content_back_dec));
		HorizontalScrollView agh2 = new HorizontalScrollView(context);
		agh2.setHorizontalScrollBarEnabled(false);
		agh2.addView(ag2);
		ScrollView agc2 = new ScrollView(context);
		LinearLayout.LayoutParams agl2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (context.getResources().getDisplayMetrics().density * 40));
		agc2.setLayoutParams(agl2);
		agc2.addView(agh2);
		layout.addView(agc2);
		TextView lbl3 = new TextView(context);
		lbl3.setText(R.string.agreements_desc3);
		lbl3.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		layout.addView(lbl3);
		AlertDialog firstRunDialog = new AlertDialog.Builder(context)
				.setTitle(R.string.agreements_title)
				.setView(layout)
				.setPositiveButton(R.string.agreements_accept, null)
				.setNegativeButton(R.string.agreements_decline, (dialog, which) -> {
					File dir = context.getFilesDir(), file = new File(dir, DB_FILE_NAME);
					file.delete();
					context.finishAffinity();
					System.exit(0);
				})
				.create();
		firstRunDialog.setCanceledOnTouchOutside(false);
		firstRunDialog.show();
	}

	private static String getVersion(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			return info.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}