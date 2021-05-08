/*
 * top.donmor.tiddloidlite.MainActivity <= [P|Tiddloid Lite]
 * Last modified: 18:18:25 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
	private TextView noWiki;
	private WikiListAdapter wikiListAdapter = null;
	private JSONObject db;

	// CONSTANT
	static final int TAKE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
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
			STR_EMPTY = "",
			TYPE_HTML = "text/html";
	private static final String
			DB_FILE_NAME = "data.json",
			KEY_APPLICATION_NAME = "application-name",
			KEY_CONTENT = "content",
			KEY_URI_RATE = "market://details?id=";
	static final int REQUEST_OPEN = 42, REQUEST_CREATE = 43;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.activity_main);
		// 读取/初始化db，转换新格式
		try {
			db = readJson(this);
			db.getJSONObject(DB_KEY_WIKI);
		} catch (Exception e) {
			e.printStackTrace();
			db = initJson(this);
			try {
				writeJson(this, db);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		trimDB120(this, db);
		// 加载UI
		onConfigurationChanged(getResources().getConfiguration());
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		noWiki = findViewById(R.id.t_noWiki);
		RecyclerView rvWikiList = findViewById(R.id.rvWikiList);
		rvWikiList.setLayoutManager(new LinearLayoutManager(this));
		rvWikiList.setItemAnimator(new DefaultItemAnimator());
		try {
			wikiListAdapter = new WikiListAdapter(this, db);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		rvWikiList.setAdapter(wikiListAdapter);
		wikiListAdapter.setReloadListener(new WikiListAdapter.ReloadListener() {
			@Override
			public void onReloaded(int count) {
				noWiki.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
			}
		});
		wikiListAdapter.setOnItemClickListener(new WikiListAdapter.ItemClickListener() {
			// 点击打开
			@Override
			public void onItemClick(final int pos, final String id) {
				if (pos == -1) return;
				try {
					JSONObject wa = db.getJSONObject(DB_KEY_WIKI).getJSONObject(id);
					Uri uri = Uri.parse(wa.getString(DB_KEY_URI));
					if (isWiki(MainActivity.this, uri)) {
						getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
						if (!loadPage(id))
							Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
					} else {
						new AlertDialog.Builder(MainActivity.this)
								.setTitle(android.R.string.dialog_alert_title)
								.setMessage(R.string.confirm_to_auto_remove_wiki)
								.setNegativeButton(android.R.string.no, null)
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										removeWiki(id);
										if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
											wikiListAdapter.notifyDataSetChanged();
										else wikiListAdapter.notifyItemRemoved(pos);
									}
								}).show();
					}
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
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
						view.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
					AlertDialog wikiConfigDialog = new AlertDialog.Builder(MainActivity.this)
							.setTitle(name)
							.setIcon(icon)
							.setPositiveButton(android.R.string.ok, null)
							.setNegativeButton(R.string.remove_wiki, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									dialogInterface.dismiss();
									// 移除确认
									AlertDialog removeWikiConfirmationDialog = new AlertDialog.Builder(MainActivity.this)
											.setTitle(android.R.string.dialog_alert_title)
											.setMessage(R.string.confirm_to_remove_wiki)
											.setNegativeButton(android.R.string.no, null)
											.setNeutralButton(R.string.delete_the_html_file, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialogInterface, int i) {
													removeWiki(id, true);
													if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
														wikiListAdapter.notifyDataSetChanged();
													else wikiListAdapter.notifyItemRemoved(pos);
												}
											})
											.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													removeWiki(id);
													if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
														wikiListAdapter.notifyDataSetChanged();
													else wikiListAdapter.notifyItemRemoved(pos);
												}
											})
											.create();
									removeWikiConfirmationDialog.show();
								}
							})
							.setNeutralButton(R.string.create_shortcut, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
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
													.setIcon(favicon != null ? IconCompat.createWithBitmap(favicon) : IconCompat.createWithResource(MainActivity.this, Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT ? R.drawable.ic_shortcut : R.mipmap.ic_shortcut))
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
		noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
	}

	private Boolean loadPage(String id) {
		try {
			if (db.getJSONObject(DB_KEY_WIKI).has(id)) {
				Bundle bu = new Bundle();
				bu.putString(KEY_ID, id);
				Intent in = new Intent().putExtras(bu).setClass(MainActivity.this, TWEditorWV.class);
				startActivity(in);
			} else throw new FileNotFoundException();
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
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
		switch (id) {
			case idNew:
				startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), REQUEST_CREATE);
				break;
			case idImport:
				startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), REQUEST_OPEN);
				break;
			case idAbout:
				SpannableString spannableString = new SpannableString(getString(R.string.about));
				Linkify.addLinks(spannableString, Linkify.ALL);
				AlertDialog aboutDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.action_about)
						.setMessage(spannableString)
						.setPositiveButton(android.R.string.ok, null)
						.setNeutralButton(R.string.market, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(KEY_URI_RATE + getPackageName())));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						})
						.show();
				((TextView) aboutDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
					((TextView) aboutDialog.findViewById(android.R.id.message)).setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	// SAF处理
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (resultCode == Activity.RESULT_OK && resultData != null) {
			Uri uri = resultData.getData();
			if (uri != null)
				switch (requestCode) {
					case REQUEST_CREATE:    // 新建
						createWiki(uri);
						break;
					case REQUEST_OPEN:    // 导入
						importWiki(uri);
						break;
				}
		}
	}

	private void createWiki(final Uri uri) {
		// 对话框等待
		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.please_wait));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCanceledOnTouchOutside(false);
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean interrupted = false;
				final boolean[] iNet = new boolean[3];
				class AdaptiveUriInputStream {
					private final InputStream is;

					AdaptiveUriInputStream(Uri uri) throws NoSuchAlgorithmException, KeyManagementException, IOException {
						HttpURLConnection httpURLConnection;
						URL url = new URL(uri.normalizeScheme().toString());
						if (uri.getScheme() != null && uri.getScheme().equals("https")) {
							httpURLConnection = (HttpsURLConnection) url.openConnection();
							if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
								((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(new TLSSocketFactory());
						} else httpURLConnection = (HttpURLConnection) url.openConnection();
						httpURLConnection.connect();
						is = httpURLConnection.getInputStream();
						iNet[0] = true;
					}

					InputStream get() {
						return is;
					}
				}
				File cache = new File(getCacheDir(), genId());
				try (InputStream isw = new AdaptiveUriInputStream(Uri.parse(getString(R.string.template_repo))).get();
					 OutputStream osw = new FileOutputStream(cache);
					 InputStream is = new FileInputStream(cache);
					 OutputStream os = getContentResolver().openOutputStream(uri)) {
					// 下载到缓存
					int length;
					byte[] bytes = new byte[4096];
					while ((length = isw.read(bytes)) > -1) {
						osw.write(bytes, 0, length);
						if (Thread.currentThread().isInterrupted()) {
							interrupted = true;
							break;
						}
					}
					osw.flush();
					if (interrupted) throw new InterruptedException();
					if (!isWiki(cache)) throw new IOException();
					progressDialog.dismiss();
					iNet[1] = true;
					String id = null;
					// 查重
					JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
					boolean exist = false;
					Iterator<String> iterator = wl.keys();
					JSONObject w = null;
					while (iterator.hasNext()) {
						exist = uri.toString().equals((w = wl.getJSONObject(id = iterator.next())).getString(DB_KEY_URI));
						if (exist) break;
					}
					if (exist) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
							}
						});
					} else {
						id = genId();
						w = new JSONObject();
						wl.put(id, w);
					}
					// 初始化键对
					w.put(KEY_NAME, KEY_TW);
					w.put(DB_KEY_SUBTITLE, STR_EMPTY);
					w.put(DB_KEY_URI, uri.toString());
					if (!MainActivity.writeJson(MainActivity.this, db))
						throw new IOException();
					iNet[2] = true;
					// 从缓存写入文件
					if (os == null) throw new FileNotFoundException();
					while ((length = is.read(bytes)) > -1) os.write(bytes, 0, length);
					os.flush();
					getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
					if (!loadPage(id))
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
							}
						});
				} catch (JSONException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
					e.printStackTrace();
					progressDialog.dismiss();
					if (iNet[1]) try {
						DocumentsContract.deleteDocument(getContentResolver(), uri);
					} catch (Exception e1) {
						e.printStackTrace();
					}
					final int fid = iNet[2] ? R.string.failed_creating_file : iNet[1] ? R.string.data_error : iNet[0] ? R.string.download_failed : R.string.no_internet;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, fid, Toast.LENGTH_SHORT).show();
						}
					});
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					cache.delete();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, R.string.cancelled, Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		});

		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				progressDialog.cancel();
			}
		});
		progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				thread.start();
			}
		});
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				thread.interrupt();
			}
		});
		progressDialog.show();
	}

	private void importWiki(Uri uri) {
		String id = genId();
		if (isWiki(this, uri)) {
			try {
				JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
				boolean exist = false;
				Iterator<String> iterator = wl.keys();
				JSONObject w;
				while (iterator.hasNext()) {
					id = iterator.next();
					w = wl.getJSONObject(id);
					exist = uri.toString().equals(w.getString(DB_KEY_URI));
					if (exist) break;
				}
				if (exist) {
					Toast.makeText(MainActivity.this, R.string.wiki_already_exists, Toast.LENGTH_SHORT).show();
				} else {
					id = genId();
					w = new JSONObject();
					w.put(KEY_NAME, KEY_TW);
					w.put(DB_KEY_SUBTITLE, STR_EMPTY);
					w.put(DB_KEY_URI, uri.toString());
					wl.put(id, w);
				}
				if (!MainActivity.writeJson(MainActivity.this, db))
					throw new Exception();
				getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
			}
			if (!loadPage(id))
				Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(MainActivity.this, R.string.not_a_wiki, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			db = readJson(this);
			wikiListAdapter.reload(db);
			wikiListAdapter.notifyDataSetChanged();
			noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Window w = getWindow();
			int color = getColor(R.color.design_default_color_primary);
			w.setStatusBarColor(color);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				w.setNavigationBarColor(color);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES)
				Objects.requireNonNull(w.getInsetsController()).setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS | WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS | WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
			else
				w.getDecorView().setSystemUiVisibility((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : View.SYSTEM_UI_FLAG_VISIBLE) : View.SYSTEM_UI_FLAG_VISIBLE);
		}
	}

	static JSONObject initJson(Context context) {
		File ext = context.getExternalFilesDir(null), file = null;
		if (ext != null)
			try (InputStream is = new FileInputStream(file = new File(ext, DB_FILE_NAME))) {
				byte[] b = new byte[is.available()];
				if (is.read(b) < 0) throw new Exception();
				JSONObject jsonObject = new JSONObject(new String(b));
				if (!jsonObject.has(DB_KEY_WIKI)) jsonObject.put(DB_KEY_WIKI, new JSONObject());
				return jsonObject;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (file != null) file.delete();
			}
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(DB_KEY_WIKI, new JSONObject());
			return jsonObject;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	static JSONObject readJson(Context context) throws Exception {
		try (InputStream is = context.openFileInput(DB_FILE_NAME)) {
			byte[] b = new byte[is.available()];
			if (is.read(b) < 0) throw new Exception();
			return new JSONObject(new String(b));
		}
	}

	static boolean writeJson(Context context, JSONObject vdb) {
		try (FileOutputStream os = context.openFileOutput(DB_FILE_NAME, MODE_PRIVATE)) {
			byte[] b = vdb.toString(2).getBytes();
			os.write(b);
			os.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	static void exportJson(Context context, JSONObject vdb) {
		File ext = context.getExternalFilesDir(null);
		if (ext == null) return;
		try (OutputStream os = new FileOutputStream(new File(ext, DB_FILE_NAME))) {
			byte[] b = vdb.toString(2).getBytes();
			os.write(b);
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String genId() {
		return UUID.randomUUID().toString();
	}

	static boolean isWiki(Context context, Uri uri) {
		try {
			return isWiki(context.getContentResolver().openInputStream(uri), uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	static boolean isWiki(File file) {
		try {
			return isWiki(new FileInputStream(file), Uri.fromFile(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean isWiki(InputStream vis, Uri u) throws IOException {
		try (InputStream is = vis) {
			Document doc = Jsoup.parse(is, StandardCharsets.UTF_8.name(), u.toString());
			Element an = doc.selectFirst(new Evaluator.AttributeKeyPair(KEY_NAME, KEY_APPLICATION_NAME) {
				@Override
				public boolean matches(Element root, Element element) {
					return KEY_APPLICATION_NAME.equals(element.attr(KEY_NAME)) && KEY_TW.equals(element.attr(KEY_CONTENT));
				}
			});
			return an != null;
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

}